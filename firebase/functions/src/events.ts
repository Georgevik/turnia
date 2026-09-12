import { onCall } from "firebase-functions/v2/https";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { notifyEventTaken } from "./notifications";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";
import { markGroupEventsUpdated } from "./sync";

/**
 * Takes a group event offered for swap — the one write a member may not do themselves, since it
 * changes a document assigned to someone else.
 *
 * The event no longer moves between members: it lives in `groups/{groupId}/events` and the transfer
 * is a change of `assigneeId` and one more entry in the event's own history. The
 * transaction verifies `onSwap` before changing anything, which is what keeps two members from
 * taking the same shift.
 *
 * That check is the whole of the mutual exclusion, and it resolves a race in the order the requests
 * reach Firestore. `tx.get` takes a read lock on the event, so two members tapping at once both read
 * `onSwap: true`; Firestore aborts the later commit, re-runs its transaction body, and the re-run
 * finds the flag already cleared and throws `TakeEventNotOnSwap`. The guard doubles as the retry's
 * exit, which is why the loser gets a domain error and not an internal one.
 *
 * There is no queue and no fairness beyond arrival: a member who tapped earlier but was offline
 * longer does not get priority, and nobody is told their position. That is deliberate — a waiting
 * list would need a claims subcollection, a trigger to award them and rules to protect them, for a
 * race that is measured in milliseconds and settles correctly without any of it.
 *
 * Request data: `{ groupId: string, eventId: string }`
 * Returns: `{ groupId, eventId, assigneeId, status: "taken" }`
 */
export const takeEvent = onCall(async (request) => {
  const taker = requireUid(request);
  const { groupId, eventId } = requireFields(request, "groupId", "eventId");

  const db = getFirestore();
  const group = await db.doc(`groups/${groupId}`).get();
  const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
  if (!memberUids.includes(taker)) {
    throw TurniaError.TakeEventNotMember;
  }

  const eventRef = db.doc(`groups/${groupId}/events/${eventId}`);

  const taken = await db.runTransaction(async (tx) => {
    const snap = await tx.get(eventRef);
    if (!snap.exists) {
      throw TurniaError.TakeEventNotFound;
    }
    if (snap.get("onSwap") !== true) {
      throw TurniaError.TakeEventNotOnSwap;
    }

    const assigneeId = snap.get("assigneeId") as string;
    if (assigneeId === taker) {
      throw TurniaError.TakeEventSelf;
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
    markGroupEventsUpdated(db, tx, groupId, yearMonth);

    return {
      fromUid: assigneeId,
      groupEventTypeId: snap.get("groupEventTypeId") as string | undefined,
      date: snap.get("date") as string | undefined,
    };
  });

  await notifyEventTaken(group, eventId, taken, taken.fromUid, taker);

  return { groupId, eventId, assigneeId: taker, status: "taken" as const };
});
