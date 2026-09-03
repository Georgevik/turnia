import { onCall } from "firebase-functions/v2/https";
import { getFirestore } from "firebase-admin/firestore";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorPermissionDenied,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

const MAX_RANGE_DAYS = 92; // ~3 months

/**
 * On-demand aggregation of another user's full calendar (across groups).
 *
 * Group events live under group members, so a non-member cannot read them
 * directly. This callable runs with admin privileges: it checks the caller is
 * allowed (owner, or listed in the owner's `calendarSharedWith`), then gathers
 * the owner's group events (across all their groups) and personal events for a
 * bounded date range, plus the lookups needed to render them. Nothing is stored
 * — no mirror, no duplication.
 *
 * Request data: `{ ownerUid: string, from: "YYYY-MM-DD", to: "YYYY-MM-DD" }`
 */
export const getSharedCalendar = onCall(async (request) => {
  const viewer = request.auth?.uid;
  if (!viewer) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.SharedCalendarUnauthenticated, "Sign in required.");
  }

  const ownerUid = request.data?.ownerUid as string | undefined;
  const from = request.data?.from as string | undefined;
  const to = request.data?.to as string | undefined;
  if (!ownerUid || !from || !to) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.SharedCalendarMissingArgs, "Missing ownerUid, from or to.");
  }

  const db = getFirestore();
  const ownerDoc = await db.doc(`users/${ownerUid}`).get();

  // Authorization: the owner themselves, or someone the owner listed in `calendarSharedWith`,
  // which is the single source of truth for a grant.
  if (viewer !== ownerUid) {
    const sharedWith = (ownerDoc.get("calendarSharedWith") as string[] | undefined) ?? [];
    if (!sharedWith.includes(viewer)) {
      throw new HttpErrorPermissionDenied(TurniaErrorCode.SharedCalendarNotShared, "This calendar is not shared with you.");
    }
  }

  // Bound the range to ~3 months.
  const fromMs = Date.parse(from);
  const toMs = Date.parse(to);
  if (isNaN(fromMs) || isNaN(toMs) || toMs < fromMs || (toMs - fromMs) / 86_400_000 > MAX_RANGE_DAYS) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.SharedCalendarRangeTooWide, "Date range must be within 3 months.");
  }

  // Group events across all the owner's groups (admin bypasses security rules).
  const groupEventsSnap = await db
    .collectionGroup("event")
    .where("assigneeId", "==", ownerUid)
    .where("date", ">=", from)
    .where("date", "<=", to)
    .get();
  const groupEvents = groupEventsSnap.docs.map((doc) => ({
    groupId: doc.get("groupId"),
    eventId: doc.id,
    groupEventTypeId: doc.get("groupEventTypeId"),
    date: doc.get("date"),
    onSwap: doc.get("onSwap"),
    ownerId: doc.get("ownerId"),
    assigneeId: doc.get("assigneeId"),
  }));

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

  const groupIds = [...new Set(groupEvents.map((event) => event.groupId).filter(Boolean))];
  const groupEventTypes: Record<string, unknown> = {};
  for (const groupId of groupIds) {
    const groupDoc = await db.doc(`groups/${groupId}`).get();
    groupEventTypes[groupId as string] = groupDoc.get("groupEventTypes") ?? [];
  }

  return { groupEvents, personalEvents, personalEventTypes, groupEventTypeColors, groupEventTypes };
});
