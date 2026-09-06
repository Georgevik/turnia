import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { DocumentSnapshot, FieldValue, getFirestore } from "firebase-admin/firestore";
import { BatchResponse, getMessaging } from "firebase-admin/messaging";
import { PushTarget, pushTargetsOf } from "./users";

/**
 * What the notification is about, carried in the data payload so a tap can open the right screen.
 * The visible text is built here and not on the device: a push has to render while the app is not
 * running, and on both platforms that means the server wrote the words.
 */
type PushType =
  | "join_requested"
  | "join_accepted"
  | "calendar_shared"
  | "event_on_swap"
  | "event_taken";

type Push = {
  title: string;
  body: string;
  type: PushType;
  /** Ids the client needs to route the tap. FCM only carries strings. */
  data?: Record<string, string>;
};

/**
 * The token no longer belongs to an install of this app: the app was uninstalled, its data was
 * cleared, or the token was rotated long enough ago that the old one expired.
 */
const UNREGISTERED = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

/**
 * Sends one notification to every device of every recipient.
 *
 * Never throws. A push is a side effect of whatever just happened — a member joined, a calendar was
 * shared — and none of those should be undone because FCM was unreachable. The caller's work is
 * already committed by the time this runs.
 */
async function notify(uids: string[], push: Push): Promise<void> {
  try {
    const targets = await pushTargetsOf(uids);
    if (targets.length === 0) return;

    const response = await getMessaging().sendEachForMulticast({
      tokens: targets.map((target) => target.token),
      notification: { title: push.title, body: push.body },
      data: { type: push.type, ...(push.data ?? {}) },
    });

    await pruneUnregistered(targets, response);
  } catch (error) {
    logger.error(`Could not send a ${push.type} notification`, error);
  }
}

/**
 * Drops the tokens FCM has just told us are dead.
 *
 * Without this the list only ever grows: every reinstall leaves a token behind, and each one is a
 * device we keep paying to fail to reach. FCM reports per token, so the answer is exact — the
 * failures that are not `UNREGISTERED` (a timeout, a quota) are transient and the token stays.
 */
async function pruneUnregistered(targets: PushTarget[], response: BatchResponse): Promise<void> {
  const deadByUid = new Map<string, string[]>();
  targets.forEach((target, index) => {
    const error = response.responses[index]?.error;
    if (!error || !UNREGISTERED.has(error.code)) return;

    deadByUid.set(target.uid, [...(deadByUid.get(target.uid) ?? []), target.token]);
  });
  if (deadByUid.size === 0) return;

  const db = getFirestore();
  const batch = db.batch();
  for (const [uid, tokens] of deadByUid) {
    batch.set(
      db.doc(`users/${uid}/private/account`),
      { fcmTokens: FieldValue.arrayRemove(...tokens) },
      { merge: true },
    );
  }
  await batch.commit();
}

/** A name to put in a notification, for a profile that may not have filled one in. */
function displayName(name: unknown, username: unknown): string {
  return (name as string) || (username as string) || "Somebody";
}

/**
 * Tells a group's admins that somebody is waiting to be let in.
 *
 * Only the admins: accepting is theirs to do, and the rest of the group has nothing to act on.
 */
export async function notifyJoinRequested(
  group: DocumentSnapshot,
  requester: { name: string; username: string },
) {
  const adminUids = (group.get("adminUids") as string[] | undefined) ?? [];
  await notify(adminUids, {
    title: group.get("name") ?? "Turnia",
    body: `${displayName(requester.name, requester.username)} asked to join the group.`,
    type: "join_requested",
    data: { groupId: group.id },
  });
}

/** Tells someone their request was accepted. Their app may well have been closed since they sent it. */
export async function notifyJoinAccepted(groupId: string, groupName: string, uid: string) {
  await notify([uid], {
    title: groupName || "Turnia",
    body: "You are now a member of the group.",
    type: "join_accepted",
    data: { groupId },
  });
}

/** Tells the member who offered a shift that somebody has taken it, so they know they are free. */
export async function notifyEventTaken(groupId: string, eventId: string, fromUid: string) {
  await notify([fromUid], {
    title: "Turnia",
    body: "Your shift was taken.",
    type: "event_taken",
    data: { groupId, eventId },
  });
}

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

    await notify(recipients, {
      title: group.get("name") ?? "Turnia",
      body: "A shift was put up for swap.",
      type: "event_on_swap",
      data: { groupId, eventId },
    });
  }
);

/**
 * Notifies whoever has just been granted read access to somebody's calendar.
 *
 * A trigger and not a callable because the grant is a plain client write: `calendarSharedWith` sits
 * on the granter's own public document, which is what both the security rules and the "shared with
 * me" query read, and routing it through a function to send a push would not make it any safer.
 *
 * The cost is that this runs on every write to `users/{uid}` — a rename, a colour picked for an
 * event type — so the first thing it does is compare the two lists and leave.
 */
export const onCalendarShared = onDocumentWritten("users/{uid}", async (event) => {
  const before = (event.data?.before.get("calendarSharedWith") as string[] | undefined) ?? [];
  const after = (event.data?.after.get("calendarSharedWith") as string[] | undefined) ?? [];

  const granted = after.filter((uid) => !before.includes(uid));
  if (granted.length === 0) {
    return;
  }

  const owner = displayName(event.data?.after.get("name"), event.data?.after.get("username"));
  await notify(granted, {
    title: "Turnia",
    body: `${owner} shared their calendar with you.`,
    type: "calendar_shared",
    data: { ownerUid: event.params.uid },
  });
});
