import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { DocumentSnapshot, FieldValue, getFirestore } from "firebase-admin/firestore";
import { BatchResponse, getMessaging } from "firebase-admin/messaging";
import { PushTarget, pushTargetsOf } from "./users";

/** What the notification is about, carried in the data payload so a tap can open the right screen. */
type PushType =
  | "join_requested"
  | "join_accepted"
  | "calendar_shared"
  | "event_on_swap"
  | "event_taken";

/**
 * The sentence a push shows, by name rather than in words.
 *
 * A push has to render while the app is not running, so the operating system draws it — and both
 * can look the words up in the app itself: Android in the app's `res/values(-es)/strings.xml`, iOS
 * in its `{en,es}.lproj/Localizable.strings`. The server sends the key and the values to fill in, so
 * it stays in English whatever language the app speaks. Every key here must exist, with the same
 * arguments in the same order, in all four of those files. `fallback` is only for an app that
 * predates the key.
 */
type PushText = {
  key: string;
  args: string[];
  fallback: string;
};

type Push = {
  /** The group's name, or the app's: a name rather than a sentence, so it needs no key. */
  title: string;
  body: PushText;
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

    const { key, args, fallback } = push.body;
    const response = await getMessaging().sendEachForMulticast({
      tokens: targets.map((target) => target.token),
      notification: { title: push.title, body: fallback },
      android: { notification: { bodyLocKey: key, bodyLocArgs: args } },
      apns: { payload: { aps: { alert: { title: push.title, locKey: key, locArgs: args } } } },
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

/** A member's name from the group's own roster, which every group document already carries. */
function memberName(group: DocumentSnapshot, uid: string): string {
  const members =
    (group.get("members") as Record<string, { name?: string; username?: string }> | undefined) ?? {};
  return displayName(members[uid]?.name, members[uid]?.username);
}

/** The name of a shift's type, which is the group's own words — so it goes to the device as is. */
function shiftTypeName(group: DocumentSnapshot, typeId: unknown): string | undefined {
  const types = (group.get("groupEventTypes") as { id: string; name?: string }[] | undefined) ?? [];
  return types.find((type) => type.id === typeId)?.name || undefined;
}

/**
 * "21/09" from a group event's `YYYY-MM-DD`. Day and month only, and in numbers: the device only
 * slots values into a fixed sentence, so a weekday or a month name would have to be in the
 * reader's language, which the server does not know.
 */
function shortDate(date: unknown): string {
  const [, month, day] = String(date ?? "").split("-");
  return month && day ? `${day}/${month}` : "";
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
  const name = displayName(requester.name, requester.username);
  await notify(adminUids, {
    title: group.get("name") ?? "Turnia",
    body: {
      key: "push_join_requested",
      args: [name],
      fallback: `${name} asked to join the group.`,
    },
    type: "join_requested",
    data: { groupId: group.id },
  });
}

/** Tells someone their request was accepted. Their app may well have been closed since they sent it. */
export async function notifyJoinAccepted(groupId: string, groupName: string, uid: string) {
  await notify([uid], {
    title: groupName || "Turnia",
    body: {
      key: "push_join_accepted",
      args: [],
      fallback: "You are now a member of the group.",
    },
    type: "join_accepted",
    data: { groupId },
  });
}

/**
 * Tells whoever asked for the swap that a colleague is covering it, so they know they are free —
 * and who to thank.
 */
export async function notifyEventTaken(
  group: DocumentSnapshot,
  eventId: string,
  shift: { groupEventTypeId?: string; date?: string },
  fromUid: string,
  takerUid: string,
) {
  const taker = memberName(group, takerUid);
  // Covered through the app, so its type was there a moment ago; blank only if it just went.
  const type = shiftTypeName(group, shift.groupEventTypeId) ?? "";
  const date = shortDate(shift.date);
  await notify([fromUid], {
    title: group.get("name") ?? "Turnia",
    body: {
      key: "push_swap_covered",
      args: [taker, type, date],
      fallback: `${taker} is covering your shift: ${type} on ${date}.`,
    },
    type: "event_taken",
    data: { groupId: group.id, eventId },
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

    // A shift whose type the admin removed does not render in the app, so nobody could cover it.
    const type = shiftTypeName(group, after.groupEventTypeId);
    if (!type) return;

    const requester = memberName(group, after.assigneeId as string);
    const date = shortDate(after.date);
    await notify(recipients, {
      title: group.get("name") ?? "Turnia",
      body: {
        key: "push_swap_requested",
        args: [requester, type, date],
        fallback: `${requester} asked for a swap: ${type} on ${date}. Can you help out?`,
      },
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
 * The cost is that this runs on every write to `users/{uid}` — a rename, a new avatar — so the
 * first thing it does is compare the two lists and leave.
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
    body: {
      key: "push_calendar_shared",
      args: [owner],
      fallback: `${owner} shared their calendar with you.`,
    },
    type: "calendar_shared",
    data: { ownerUid: event.params.uid },
  });
});
