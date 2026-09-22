import { onCall } from "firebase-functions/v2/https";
import { DocumentSnapshot, getFirestore } from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { HistoryEntry } from "./events";
import { requireFields, requireUid } from "./requests";

const MAX_RANGE_DAYS = 92; // ~3 months

/**
 * On-demand aggregation of another user's full calendar (across groups).
 *
 * Group events live under their group, so a non-member cannot read them
 * directly. This callable runs with admin privileges: it checks the caller is
 * allowed (owner, or listed in the owner's `calendarSharedWith`), then gathers
 * the owner's group events (across all their groups), personal events and one-off events for a
 * bounded date range, plus the lookups needed to render them — each shift's chain
 * of holders included, since the chain is part of the owner's calendar. Nothing is stored
 * — no mirror, no duplication.
 *
 * Request data: `{ ownerUid: string, from: "YYYY-MM-DD", to: "YYYY-MM-DD" }`
 */
export const getSharedCalendar = onCall(async (request) => {
  const viewer = requireUid(request);
  const { ownerUid, from, to } = requireFields(request, "ownerUid", "from", "to");

  const db = getFirestore();
  const ownerDoc = await db.doc(`users/${ownerUid}`).get();

  // Authorization: the owner themselves, or someone the owner listed in `calendarSharedWith`,
  // which is the single source of truth for a grant.
  if (viewer !== ownerUid) {
    const sharedWith = (ownerDoc.get("calendarSharedWith") as string[] | undefined) ?? [];
    if (!sharedWith.includes(viewer)) {
      throw TurniaError.SharedCalendarNotShared;
    }
  }

  // Bound the range to ~3 months.
  const fromMs = Date.parse(from);
  const toMs = Date.parse(to);
  if (isNaN(fromMs) || isNaN(toMs) || toMs < fromMs || (toMs - fromMs) / 86_400_000 > MAX_RANGE_DAYS) {
    throw TurniaError.SharedCalendarRangeTooWide;
  }

  // Group events, one query per group the owner belongs to. A collection-group query is no longer
  // possible — nor needed — now that each group keeps its events in its own collection.
  //
  // Groups they were removed from count too: their leftover shifts are still on the calendar they
  // share, and dropping them here would make those days silently look free. Two queries rather than
  // one `or`, because Firestore allows a single `array-contains` per query.
  const [memberOf, revokedFrom] = await Promise.all([
    db.collection("groups").where("memberUids", "array-contains", ownerUid).get(),
    db.collection("groups").where("revokedUids", "array-contains", ownerUid).get(),
  ]);
  const revokedGroupIds = new Set(revokedFrom.docs.map((group) => group.id));
  const ownerGroups = { docs: [...memberOf.docs, ...revokedFrom.docs] };

  const groupEventsPerGroup = await Promise.all(
    ownerGroups.docs.map(async (group) => {
      const snap = await group.ref
        .collection("events")
        .where("assigneeId", "==", ownerUid)
        .where("date", ">=", from)
        .where("date", "<=", to)
        .get();

      return snap.docs
        .filter((doc) => doc.get("isDeleted") !== true)
        .map((doc) => ({
          groupId: group.id,
          eventId: doc.id,
          groupEventTypeId: doc.get("groupEventTypeId"),
          date: doc.get("date"),
          onSwap: doc.get("onSwap"),
          ownerId: doc.get("ownerId"),
          assigneeId: doc.get("assigneeId"),
          ...holdersOf(doc),
        }));
    })
  );
  const groupEvents = groupEventsPerGroup.flat();

  // Names for the chains, and only for the uids in them: the rest of each roster stays out. A group
  // the owner was revoked from gives none — they cannot read its roster themselves, and sharing a
  // calendar is not a way around that — so those holders render as former members, as they do for
  // the owner. The owner's own name comes from their profile, which covers the revoked case too.
  const userNames: Record<string, string> = {};
  ownerGroups.docs.forEach((group) => {
    if (revokedGroupIds.has(group.id)) return;
    const members = (group.get("members") as Record<string, { name?: string }> | undefined) ?? {};
    groupEvents
      .filter((event) => event.groupId === group.id)
      .flatMap((event) => event.holderUids)
      .forEach((uid) => {
        const name = members[uid]?.name;
        if (name) userNames[uid] = name;
      });
  });
  const ownerName = (ownerDoc.get("name") as string | undefined) ?? "";
  if (ownerName) userNames[ownerUid] = ownerName;

  // Personal events in range. Their `date` is a plain "YYYY-MM-DD", but events written for a while
  // carry a full ISO instant instead — the start of the day in the *writer's* timezone — which a
  // string comparison against a bare date can push a day either side of the range. Both bounds are
  // widened by a day for them; the client never draws the surplus.
  const dayShift = (date: string, days: number) =>
    new Date(Date.parse(date) + days * 86_400_000).toISOString().slice(0, 10);

  const personalSnap = await db
    .collection(`users/${ownerUid}/personalEvents`)
    .where("date", ">=", dayShift(from, -1))
    .where("date", "<=", dayShift(to, 2))
    .get();
  const personalEvents = personalSnap.docs
    .filter((doc) => doc.get("isDeleted") !== true)
    .map((doc) => ({
      eventId: doc.id,
      // The field is `typeId`: `personalEventTypeId` never existed on these documents and read
      // back undefined, which left every personal event here without a type to render it with.
      personalEventTypeId: doc.get("typeId"),
      date: doc.get("date"),
      notes: doc.get("notes") ?? null,
    }));

  // One-off events carry their own name and colour, so they need no type lookup. They may span
  // several months, so the query is by overlap on the month fields — the same index the app's own
  // sync uses — and the days are trimmed here: a month overlapping the range is not a day inside it.
  // `start` and `end` are ISO 8601 date-times, so their first ten characters are the day.
  const oneOffSnap = await db
    .collection(`users/${ownerUid}/personalOneOffEvents`)
    .where("yearMonthStart", "<=", to.slice(0, 7))
    .where("yearMonthEnd", ">=", from.slice(0, 7))
    .get();
  const personalOneOffEvents = oneOffSnap.docs
    .filter((doc) => doc.get("isDeleted") !== true)
    .filter((doc) => day(doc.get("start")) <= to && day(doc.get("end")) >= from)
    .map((doc) => ({
      eventId: doc.id,
      name: doc.get("name"),
      color: doc.get("color"),
      start: doc.get("start"),
      end: doc.get("end"),
      allDay: doc.get("allDay") === true,
      notes: doc.get("notes") ?? null,
    }));

  // Lookups for rendering: personal types, the owner's colors, and group types per group.
  const personalTypesSnap = await db.collection(`users/${ownerUid}/personalEventTypes`).get();
  const personalEventTypes = personalTypesSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

  // The owner's colour picks live under `private`, not on their public profile: nobody but them
  // may read what they chose, and this function is the one thing allowed to render with it.
  const preferencesDoc = await db.doc(`users/${ownerUid}/private/preferences`).get();
  const groupEventTypeColors =
    (preferencesDoc.get("groupEventTypeColors") as Record<string, string> | undefined) ?? {};

  // The viewer may belong to none of these groups, so they cannot read the names themselves.
  const groupNames: Record<string, string> = {};
  ownerGroups.docs.forEach((group) => {
    groupNames[group.id] = (group.get("name") as string | undefined) ?? "";
  });

  // The groups were already read to find the events; their types came along with them. A group the
  // owner was revoked from gives up only the types their own returned events use — the same
  // narrowing the `revokedGroups` snapshot applies, so sharing a calendar is not a way around it.
  const groupEventTypes: Record<string, unknown> = {};
  ownerGroups.docs.forEach((group) => {
    const types = (group.get("groupEventTypes") as Record<string, unknown>[]) ?? [];
    if (!revokedGroupIds.has(group.id)) {
      groupEventTypes[group.id] = types;
      return;
    }
    const usedTypeIds = new Set(
      groupEvents.filter((event) => event.groupId === group.id).map((event) => event.groupEventTypeId)
    );
    groupEventTypes[group.id] = types.filter((type) => usedTypeIds.has(type.id as string));
  });

  return {
    groupEvents,
    personalEvents,
    personalOneOffEvents,
    personalEventTypes,
    groupEventTypeColors,
    groupEventTypes,
    groupNames,
    userNames,
  };
});

/**
 * Every step of the shift's chain, in order: the creator, then whoever each transfer handed it to
 * or each hand-back returned it to. `holderReturned[i]` says the shift came back to `holderUids[i]`,
 * rather than being taken by them; the two lists always have the same length.
 */
function holdersOf(doc: DocumentSnapshot): { holderUids: string[]; holderReturned: boolean[] } {
  const history = (doc.get("history") as HistoryEntry[] | undefined) ?? [];
  const steps = history.filter(
    (entry) => (entry.type === "transferred" || entry.type === "returned") && entry.toUid
  );
  return {
    holderUids: [doc.get("ownerId") as string, ...steps.map((entry) => entry.toUid as string)],
    holderReturned: [false, ...steps.map((entry) => entry.type === "returned")],
  };
}

/** The `YYYY-MM-DD` of a stored ISO 8601 date-time. */
function day(dateTime: string): string {
  return dateTime.slice(0, 10);
}
