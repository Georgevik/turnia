import { onCall } from "firebase-functions/v2/https";
import { getFirestore } from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";

const MAX_RANGE_DAYS = 92; // ~3 months

/**
 * On-demand aggregation of another user's full calendar (across groups).
 *
 * Group events live under their group, so a non-member cannot read them
 * directly. This callable runs with admin privileges: it checks the caller is
 * allowed (owner, or listed in the owner's `calendarSharedWith`), then gathers
 * the owner's group events (across all their groups) and personal events for a
 * bounded date range, plus the lookups needed to render them. Nothing is stored
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
        }));
    })
  );
  const groupEvents = groupEventsPerGroup.flat();

  // Personal events in range.
  const personalSnap = await db
    .collection(`users/${ownerUid}/personalEvents`)
    .where("date", ">=", from)
    .where("date", "<=", to)
    .get();
  const personalEvents = personalSnap.docs.map((doc) => ({
    eventId: doc.id,
    personalEventTypeId: doc.get("personalEventTypeId"),
    date: doc.get("date"),
    notes: doc.get("notes") ?? null,
  }));

  // Lookups for rendering: personal types, the owner's colors, and group types per group.
  const personalTypesSnap = await db.collection(`users/${ownerUid}/personalEventTypes`).get();
  const personalEventTypes = personalTypesSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

  const groupEventTypeColors = (ownerDoc.get("groupEventTypeColors") as Record<string, string> | undefined) ?? {};

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

  return { groupEvents, personalEvents, personalEventTypes, groupEventTypeColors, groupEventTypes };
});
