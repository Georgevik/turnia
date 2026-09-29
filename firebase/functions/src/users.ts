import { onCall } from "firebase-functions/v2/https";
import { FieldValue, Firestore, WriteBatch, getFirestore } from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";
import { markGroupUpdated, markUserUpdated } from "./sync";

/**
 * Points a requester at a join request of theirs, on their own private document.
 *
 * Three writes that must never travel apart: the pointer itself, the `updateAt` a reader compares
 * against, and the `private` marker on their sync document. The client reads this pointer through
 * that marker — it is how it learns the request exists at all — so a pointer written without moving
 * the marker is a pointer nobody ever sees.
 *
 * @param groupIds an `arrayUnion` to add the pointer, an `arrayRemove` to drop it.
 */
export function writeJoinRequestPointer(
  db: Firestore,
  batch: WriteBatch,
  uid: string,
  groupIds: FieldValue,
) {
  batch.set(
    db.doc(`users/${uid}/private/joinRequests`),
    { groupIds, updateAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  markUserUpdated(db, batch, uid, "joinRequests");
}

/** Mirrors `isValidUsername` in the app: 3-20 of a-z, 0-9, `_` or `.`. */
const USERNAME_PATTERN = /^[a-z0-9_.]{3,20}$/;

/** One device to push to, kept next to its owner so a dead token can be pruned from the right list. */
export type PushTarget = { uid: string; token: string };

/**
 * Every device registered by the given users.
 *
 * Push tokens live under `users/{uid}/private`, unreadable to anyone but their owner — the public
 * user document carries nothing but the name and the avatar. One `getAll` rather than a read per
 * uid: a group of twenty admins is still a single round trip.
 */
export async function pushTargetsOf(uids: string[]): Promise<PushTarget[]> {
  if (uids.length === 0) return [];

  const db = getFirestore();
  const accounts = await db.getAll(...uids.map((uid) => db.doc(`users/${uid}/private/account`)));

  return accounts.flatMap((account, index) => {
    const tokens = (account.get("fcmTokens") as string[] | undefined) ?? [];
    return tokens.map((token) => ({ uid: uids[index], token }));
  });
}

/**
 * Changes a user's name and username, and carries the change to every copy of it.
 *
 * This runs on the server and not on the device because the name is **denormalized**: it is copied
 * into every group the user belongs to so that showing who covers a shift costs no read. Copies
 * need a keeper, and a keeper that ships inside the app is only as current as the oldest version
 * still installed — one stale client renaming someone would leave their old name in every group.
 *
 * The security rules forbid the client from writing `name` and `username` on its own document, so
 * this is the only way in.
 *
 * Request data: `{ name: string, username: string }`
 * Returns: `{ name, username, status: "updated" }`
 */
export const updateProfile = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUid(request);
  const fields = requireFields(request, "name", "username");
  const name = fields.name;
  const username = fields.username.toLowerCase();
  if (!USERNAME_PATTERN.test(username)) {
    throw TurniaError.UpdateProfileUsernameInvalid;
  }

  const db = getFirestore();
  const userRef = db.doc(`users/${uid}`);
  const reservationRef = db.doc(`usernames/${username}`);

  // Claim, release and profile in one commit. The reservation is what makes a username unique, so
  // a name published against a handle somebody else holds would be worse than a rejected rename; and
  // a release left for a later commit is a handle the user keeps forever once anything in between
  // fails. Reading the current username inside the transaction is what lets two renames racing
  // from the same handle each release the one they actually replaced.
  await db.runTransaction(async (tx) => {
    const user = await tx.get(userRef);
    const previousUsername = user.get("username") as string | undefined;
    const renamed = previousUsername !== undefined && previousUsername !== username;
    const previousRef = renamed ? db.doc(`usernames/${previousUsername}`) : null;

    const reservation = await tx.get(reservationRef);
    if (reservation.exists && reservation.get("uid") !== uid) {
      throw TurniaError.UpdateProfileUsernameTaken;
    }
    const previous = previousRef ? await tx.get(previousRef) : null;

    // The reservation's `updateAt` is the marker a searcher compares their cached profile against,
    // so it has to resolve to the same instant as the profile's own.
    tx.set(reservationRef, { username, uid, updateAt: FieldValue.serverTimestamp() });
    if (previousRef && previous?.get("uid") === uid) tx.delete(previousRef);

    tx.set(userRef, { name, username, updateAt: FieldValue.serverTimestamp() }, { merge: true });
    markUserUpdated(db, tx, uid, "profile");
  });

  // Every group carrying a copy of this name. Entry and sync marker in one batch per group, so a
  // reader sees them as equally old — written apart, the marker is always the later of the two and
  // no cache ever settles.
  const groups = await db.collection("groups").where("memberUids", "array-contains", uid).get();
  await Promise.all(
    groups.docs.map((group) => {
      const batch = db.batch();
      batch.update(group.ref, {
        [`members.${uid}`]: { name, username },
        updateAt: FieldValue.serverTimestamp(),
      });
      markGroupUpdated(db, batch, group.id);
      return batch.commit();
    })
  );

  return { name, username, status: "updated" as const };
});
