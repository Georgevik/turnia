import { onCall } from "firebase-functions/v2/https";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  HttpErrorFailedPrecondition,
  HttpErrorInvalidArgument,
  HttpErrorUnauthenticated,
  TurniaErrorCode,
} from "./errors";

/** Mirrors `isValidUsername` in the app: 3-20 of a-z, 0-9, `_` or `.`. */
const USERNAME_PATTERN = /^[a-z0-9_.]{3,20}$/;

/**
 * Push tokens live under `users/{uid}/private`, unreadable to anyone but their owner — the public
 * user document carries nothing but the name.
 */
export async function fcmTokensOf(uid: string): Promise<string[]> {
  const account = await getFirestore().doc(`users/${uid}/private/account`).get();
  return (account.get("fcmTokens") as string[] | undefined) ?? [];
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
  const uid = request.auth?.uid;
  if (!uid) {
    throw new HttpErrorUnauthenticated(TurniaErrorCode.UpdateProfileUnauthenticated, "Sign in required.");
  }

  const name = (request.data?.name as string | undefined)?.trim();
  const username = (request.data?.username as string | undefined)?.trim().toLowerCase();
  if (!name || !username) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.UpdateProfileMissingArgs, "Missing name or username.");
  }
  if (!USERNAME_PATTERN.test(username)) {
    throw new HttpErrorInvalidArgument(TurniaErrorCode.UpdateProfileUsernameInvalid, "Invalid username.");
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
        throw new HttpErrorFailedPrecondition(TurniaErrorCode.UpdateProfileUsernameTaken, "Username already taken.");
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
