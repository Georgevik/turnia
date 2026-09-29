import { onCall } from "firebase-functions/v2/https";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { deleteGroupTree, isLastAdminOfOthers } from "./membership";
import { requireUid } from "./requests";
import { markGroupUpdated, markJoinRequestSettled } from "./sync";

/**
 * Deletes the caller's account by **anonymizing** it, then deletes the Auth user.
 *
 * The uid stays wherever it already is — `memberUids`, `adminUids`, `revokedUids`, every event's
 * `ownerId` and `assigneeId`, every `history` entry — so nothing that refers to it has to be
 * chased down, and the A→B→C chain of a shift the account once held stays whole, with one link that
 * no longer says who. What goes is everything that says who they were: the name and username (on
 * the profile and on every group's copy), the avatar, the username reservation, and everything under
 * their own document — email, push tokens, preferences, personal events and their notes. A blank
 * name is what the app already renders as a former member.
 *
 * Two things still move, because leaving them would do harm rather than merely leave a trace:
 * a group the account is alone in is deleted, since nobody could ever open it again; and requests
 * still waiting for an admin are withdrawn, so nobody lets an account that does not exist in.
 *
 * The only admin of a group that still has members is refused, as they are when leaving: the group
 * would be left with nobody able to run it. Checked before anything is touched, so a refusal never
 * leaves an account half anonymized.
 *
 * Idempotent: a call that failed halfway can simply be made again, and the Auth user goes last so
 * the caller can still make it.
 *
 * Returns: `{ status: "deleted" }`
 */
export const deleteAccount = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUid(request);
  const db = getFirestore();

  const memberOf = await db.collection("groups").where("memberUids", "array-contains", uid).get();
  if (memberOf.docs.some((group) => isLastAdminOfOthers(group, uid))) {
    throw TurniaError.DeleteAccountLastAdmin;
  }

  // The name every group copies. Entry and sync marker in one batch per group, as `updateProfile`
  // writes them, so a reader's cache can settle.
  for (const group of memberOf.docs) {
    const memberUids = (group.get("memberUids") as string[] | undefined) ?? [];
    if (memberUids.every((member) => member === uid)) {
      await deleteGroupTree(db, group);
      continue;
    }
    const batch = db.batch();
    batch.update(group.ref, {
      [`members.${uid}`]: { name: "", username: "" },
      updateAt: FieldValue.serverTimestamp(),
    });
    markGroupUpdated(db, batch, group.id);
    await batch.commit();
  }

  // Requests still waiting for an admin. Their only index is the pointer list under the account's
  // own document, which is deleted below, so it is read first.
  const pending = await db.doc(`users/${uid}/private/joinRequests`).get();
  for (const groupId of (pending.get("groupIds") as string[] | undefined) ?? []) {
    const batch = db.batch();
    batch.delete(db.doc(`groups/${groupId}/joinRequests/${uid}`));
    markGroupUpdated(db, batch, groupId);
    markJoinRequestSettled(db, batch, groupId, uid);
    await batch.commit();
  }

  const userRef = db.doc(`users/${uid}`);
  const user = await userRef.get();
  const username = user.get("username") as string | undefined;
  if (username) {
    const reservationRef = db.doc(`usernames/${username}`);
    const reservation = await reservationRef.get();
    if (reservation.get("uid") === uid) await reservationRef.delete();
  }

  // Private documents, personal events and types, sync markers, revoked-group snapshots.
  for (const collection of await userRef.listCollections()) {
    await db.recursiveDelete(collection);
  }

  // The profile itself stays, so the uid still resolves — to nobody. `calendarSharedWith` is
  // emptied rather than kept: a grant to read a calendar that no longer exists is not a trace
  // worth keeping, and `getSharedCalendar` would otherwise still answer it.
  if (user.exists) {
    await userRef.set({
      name: "",
      username: "",
      calendarSharedWith: [],
      isDeleted: true,
      updateAt: FieldValue.serverTimestamp(),
    });
  }

  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    // A retry after the Auth user is already gone has nothing left to do.
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }

  return { status: "deleted" as const };
});
