import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

/**
 * Notifies the other group members when an event is put up for swap.
 *
 * Triggered on the event doc under its assignee. Only reacts to the
 * `onSwap: false → true` transition; transfers are notified by `takeEvent`.
 */
export const onEventPutOnSwap = onDocumentWritten(
  "groups/{groupId}/members/{memberUid}/event/{eventId}",
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
    const { groupId, memberUid, eventId } = event.params;

    const members = await db.collection(`groups/${groupId}/members`).get();
    const recipients = members.docs.map((doc) => doc.id).filter((uid) => uid !== memberUid);
    if (recipients.length === 0) {
      return;
    }

    const tokens: string[] = [];
    for (const uid of recipients) {
      const user = await db.collection("users").doc(uid).get();
      const userTokens = (user.get("fcmTokens") as string[] | undefined) ?? [];
      tokens.push(...userTokens);
    }
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
