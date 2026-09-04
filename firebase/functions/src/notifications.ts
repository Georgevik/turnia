import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { fcmTokensOf } from "./users";

/**
 * Notifies the other group members when an event is put up for swap.
 *
 * Only reacts to the `onSwap: false → true` transition; a transfer is notified by `takeEvent`.
 */
export const onEventPutOnSwap = onDocumentWritten(
  "groups/{groupId}/events/{eventId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) {
      return;
    }
    if (before.onSwap === true || after.onSwap !== true) {
      return;
    }

    const db = getFirestore();
    const { groupId, eventId } = event.params;

    // Membership is a field of the group, so this is one read instead of a subcollection listing.
    const group = await db.doc(`groups/${groupId}`).get();
    const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
    const recipients = memberUids.filter((uid) => uid !== after.assigneeId);
    if (recipients.length === 0) {
      return;
    }

    const tokens = (await Promise.all(recipients.map(fcmTokensOf))).flat();
    if (tokens.length === 0) {
      return;
    }

    await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title: "Turnia", body: "A shift was put up for swap." },
      data: { groupId, eventId },
    });
  }
);
