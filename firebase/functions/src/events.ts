import { onCall } from "firebase-functions/v2/https";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { fcmTokensOf } from "./users";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorNotFound,
  HttpErrorPermissionDenied,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

/**
 * Takes a group event offered for swap — the one write a member may not do themselves, since it
 * changes a document assigned to someone else.
 *
 * The event no longer moves between members: it lives in `groups/{groupId}/events` and the transfer
 * is a change of `assigneeId` and one more entry in the event's own history. The
 * transaction verifies `onSwap` before changing anything, which is what keeps two members from
 * taking the same shift.
 *
 * Request data: `{ groupId: string, eventId: string }`
 * Returns: `{ groupId, eventId, assigneeId, status: "taken" }`
 */
export const takeEvent = onCall(async (request) => {
  const taker = request.auth?.uid;
  if (!taker) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.TakeEventUnauthenticated, "Sign in required.");
  }

  const groupId = request.data?.groupId as string | undefined;
  const eventId = request.data?.eventId as string | undefined;
  if (!groupId || !eventId) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.TakeEventMissingArgs, "Missing groupId or eventId.");
  }

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(taker)) {
    throw new HttpErrorPermissionDenied(TurniaErrorCode.TakeEventNotMember, "You are not a member of this group.");
  }

  const eventRef = db.doc(`groups/${groupId}/events/${eventId}`);
  const syncRef = db.doc(`groups/${groupId}/sync/updates`);

  const fromUid = await db.runTransaction(async (tx) => {
    const snap = await tx.get(eventRef);
    if (!snap.exists) {
      throw new HttpErrorNotFound(TurniaErrorCode.TakeEventNotFound, "Event not found.");
    }
    if (snap.get("onSwap") !== true) {
      throw new HttpErrorFailedPrecondition(TurniaErrorCode.TakeEventNotOnSwap, "Event is not offered for swap.");
    }

    const assigneeId = snap.get("assigneeId") as string;
    if (assigneeId === taker) {
      throw new HttpErrorFailedPrecondition(TurniaErrorCode.TakeEventSelf, "You already hold this event.");
    }

    // The chain lives on the event, so it travels with it and costs no read to show. A server
    // timestamp sentinel is not allowed inside an array, hence `Timestamp.now()` — still server
    // time, since this runs on the server.
    tx.update(eventRef, {
      assigneeId: taker,
      onSwap: false,
      updateAt: FieldValue.serverTimestamp(),
      history: FieldValue.arrayUnion({
        type: "transferred",
        actorUid: taker,
        fromUid: assigneeId,
        toUid: taker,
        timestamp: Timestamp.now(),
      }),
    });

    // Same commit as the event, so the marker and the document resolve to one instant and the
    // other members' caches can settle instead of refetching for ever.
    const yearMonth = (snap.get("yearMonth") as string | undefined) ?? "";
    tx.set(syncRef, { events: { [yearMonth]: { updatedAt: FieldValue.serverTimestamp() } } }, { merge: true });

    return assigneeId;
  });

  const tokens = await fcmTokensOf(fromUid);
  if (tokens.length > 0) {
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title: "Turnia", body: "Your shift was taken." },
      data: { groupId, eventId },
    });
  }

  return { groupId, eventId, assigneeId: taker, status: "taken" as const };
});
