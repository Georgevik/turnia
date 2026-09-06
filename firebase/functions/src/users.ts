import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { TurniaError } from "./errors";
import { requireFields, requireUid } from "./requests";

/** Mirrors `isValidUsername` in the app: 3-20 of a-z, 0-9, `_` or `.`. */
const USERNAME_PATTERN = /^[a-z0-9_.]{3,20}$/;

/** One device to push to, kept next to its owner so a dead token can be pruned from the right list. */
export type PushTarget = { uid: string; token: string };

/**
 * Every device registered by the given users.
 *
 * Push tokens live under `users/{uid}/private`, unreadable to anyone but their owner — the public
 * user document carries nothing but the name. One `getAll` rather than a read per uid: a group of
 * twenty admins is still a single round trip.
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
export const updateProfile = onCall(async (request) => {
  const uid = requireUid(request);
  const fields = requireFields(request, "name", "username");
  const name = fields.name;
  const username = fields.username.toLowerCase();
  if (!USERNAME_PATTERN.test(username)) {
    throw TurniaError.UpdateProfileUsernameInvalid;
  }

  const db = getFirestore();
  const userRef = db.doc(`users/${uid}`);
  const user = await userRef.get();
  const previousUsername = user.get("username") as string | undefined;

  // Claim first, in a transaction: the reservation is what makes a username unique, and a name
  // published against a handle somebody else holds would be worse than a rejected rename.
  if (username !== previousUsername) {
    const reservationRef = db.doc(`usernames/${username}`);
    await db.runTransaction(async (tx) => {
      const reservation = await tx.get(reservationRef);
      if (reservation.exists && reservation.get("uid") !== uid) {
        throw TurniaError.UpdateProfileUsernameTaken;
      }
      tx.set(reservationRef, { username, uid, name, updateAt: FieldValue.serverTimestamp() });
    });
  } else {
    await db.doc(`usernames/${username}`).set({ name }, { merge: true });
  }

  await userRef.set({ name, username }, { merge: true });

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
      batch.set(
        db.doc(`groups/${group.id}/sync/updates`),
        { group: FieldValue.serverTimestamp() },
        { merge: true },
      );
      return batch.commit();
    })
  );

  // Only once everything points at the new handle is the old one free.
  if (previousUsername && previousUsername !== username) {
    await db.doc(`usernames/${previousUsername}`).delete();
  }

  return { name, username, status: "updated" as const };
});
