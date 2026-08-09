import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorNotFound,
  HttpErrorPermissionDenied,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

/**
 * Takes an on-sale group event: a cross-member move the client cannot do itself.
 *
 * Verifies `onSale == true` in a transaction, then moves the event from the
 * current assignee (`fromUid`) to the caller, copying the history forward and
 * appending a `transferred` entry so the new holder has the full chain.
 *
 * Request data: `{ groupId: string, fromUid: string, eventId: string }`
 * Returns: `{ groupId, eventId, assigneeId, status: "taken" }`
 */
export const takeEvent = onCall(async (request) => {
  const taker = request.auth?.uid;
  if (!taker) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.TakeEventUnauthenticated, "Sign in required.");
  }

  const groupId = request.data?.groupId as string | undefined;
  const fromUid = request.data?.fromUid as string | undefined;
  const eventId = request.data?.eventId as string | undefined;
  if (!groupId || !fromUid || !eventId) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.TakeEventMissingArgs, "Missing groupId, fromUid or eventId.");
  }
  if (fromUid === taker) {
    throw new HttpErrorFailedPrecondition(TurniaErrorCode.TakeEventSelf, "You already hold this event.");
  }

  const db = getFirestore();
  const takerMember = await db.doc(`groups/${groupId}/members/${taker}`).get();
  if (!takerMember.exists) {
    throw new HttpErrorPermissionDenied(TurniaErrorCode.TakeEventNotMember, "You are not a member of this group.");
  }

  const fromEventRef = db.doc(`groups/${groupId}/members/${fromUid}/event/${eventId}`);
  const toEventRef = db.doc(`groups/${groupId}/members/${taker}/event/${eventId}`);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(fromEventRef);
    if (!snap.exists) {
      throw new HttpErrorNotFound(TurniaErrorCode.TakeEventNotFound, "Event not found.");
    }
    if (snap.get("onSale") !== true) {
      throw new HttpErrorFailedPrecondition(TurniaErrorCode.TakeEventNotOnSale, "Event is not on sale.");
    }
    const data = snap.data() as FirebaseFirestore.DocumentData;
    const history = await tx.get(fromEventRef.collection("history").orderBy("timestamp"));

    // Create the event under the taker.
    tx.set(toEventRef, {
      groupId,
      ownerId: data.ownerId,
      assigneeId: taker,
      groupEventTypeId: data.groupEventTypeId ?? null,
      date: data.date ?? null,
      onSale: false,
      createdAt: data.createdAt ?? FieldValue.serverTimestamp(),
    });

    // Copy the history forward, then append the transfer entry.
    let parentEventId: string | null = null;
    history.forEach((entry) => {
      tx.set(toEventRef.collection("history").doc(entry.id), entry.data());
      parentEventId = entry.id;
    });
    tx.set(toEventRef.collection("history").doc(), {
      type: "transferred",
      actorUid: taker,
      fromUid,
      toUid: taker,
      timestamp: FieldValue.serverTimestamp(),
      parentEventId,
    });

    // Remove the previous assignee's event and its history.
    history.forEach((entry) => tx.delete(entry.ref));
    tx.delete(fromEventRef);
  });

  // Notify the previous assignee.
  const fromUser = await db.doc(`users/${fromUid}`).get();
  const tokens = (fromUser.get("fcmTokens") as string[] | undefined) ?? [];
  if (tokens.length > 0) {
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title: "Turnia", body: "Your shift was taken." },
      data: { groupId, eventId },
    });
  }

  return { groupId, eventId, assigneeId: taker, status: "taken" as const };
});
