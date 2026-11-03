import { onCall } from "firebase-functions/v2/https";
import { DocumentSnapshot, FieldValue, Firestore, WriteBatch, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
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

/** The file name `storage.rules` accepts under `profilePhotos/{uid}/`, without its `.webp`. */
const PHOTO_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

/** The size `storage.rules` caps an upload at, checked again in case the rules ever loosen. */
const PHOTO_MAX_BYTES = 100 * 1024;

/** Where a user's profile photos live. Only the current one survives a `setProfilePhoto`. */
export function profilePhotoFolder(uid: string): string {
  return `profilePhotos/${uid}/`;
}

/**
 * A member's entry on `groups/{g}.members`, from their profile. The photo is copied only when there
 * is one: an entry without `photoId` is how a member with no photo reads.
 */
export function memberEntry(user: DocumentSnapshot): { name: string; username: string; photoId?: string } {
  const photoId = user.get("photoId") as string | undefined;
  return {
    name: (user.get("name") as string | undefined) ?? "",
    username: (user.get("username") as string | undefined) ?? "",
    ...(photoId ? { photoId } : {}),
  };
}

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

/**
 * Publishes the caller's profile photo, or removes it, and carries the change to every copy.
 *
 * The client uploads the file itself, to `profilePhotos/{uid}/{photoId}.webp`, and then calls this
 * with the id; `{ photoId: null }` removes the photo. Like the name, the photo is copied into every
 * group the user belongs to — a colleague sees it arrive with the group they already listen to,
 * instead of reading the profile — so it needs a keeper, and the rules forbid the client from
 * writing `photoId` on its own document.
 *
 * Afterwards every other file in the user's folder is deleted: the photo this one replaced, and any
 * upload that never got as far as this call. What survives is whatever the profile names once the
 * write is done, read again rather than taken from the request, so two devices publishing at once
 * cannot each delete the other's file and leave the profile pointing at nothing.
 *
 * Request data: `{ photoId: string | null }`
 * Returns: `{ photoId, status: "updated" }`
 */
export const setProfilePhoto = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = requireUid(request);
  const requested = (request.data as Record<string, unknown> | undefined)?.photoId;
  if (requested !== null && (typeof requested !== "string" || !PHOTO_ID_PATTERN.test(requested))) {
    throw TurniaError.InvalidArgument;
  }
  const photoId = requested as string | null;

  const bucket = getStorage().bucket();
  const folder = profilePhotoFolder(uid);
  if (photoId) {
    const file = bucket.file(`${folder}${photoId}.webp`);
    const [exists] = await file.exists();
    if (!exists) throw TurniaError.SetProfilePhotoNotFound;
    const [metadata] = await file.getMetadata();
    if (Number(metadata.size) > PHOTO_MAX_BYTES || metadata.contentType !== "image/webp") {
      throw TurniaError.SetProfilePhotoInvalid;
    }
  }
  const photo = photoId ?? FieldValue.delete();

  const db = getFirestore();
  const userRef = db.doc(`users/${uid}`);

  // The profile and the reservation's marker resolve to the same instant, or a searcher's cache
  // never settles; a user who has no reservation yet has no marker to move.
  await db.runTransaction(async (tx) => {
    const user = await tx.get(userRef);
    const username = user.get("username") as string | undefined;
    const reservationRef = username ? db.doc(`usernames/${username}`) : null;
    const reservation = reservationRef ? await tx.get(reservationRef) : null;

    tx.set(userRef, { photoId: photo, updateAt: FieldValue.serverTimestamp() }, { merge: true });
    if (reservationRef && reservation?.get("uid") === uid) {
      tx.update(reservationRef, { updateAt: FieldValue.serverTimestamp() });
    }
    markUserUpdated(db, tx, uid, "profile");
  });

  // Entry and sync marker in one batch per group, as `updateProfile` writes the name.
  const groups = await db.collection("groups").where("memberUids", "array-contains", uid).get();
  await Promise.all(
    groups.docs.map((group) => {
      const batch = db.batch();
      batch.update(group.ref, {
        [`members.${uid}.photoId`]: photo,
        updateAt: FieldValue.serverTimestamp(),
      });
      markGroupUpdated(db, batch, group.id);
      return batch.commit();
    })
  );

  const current = (await userRef.get()).get("photoId") as string | undefined;
  const keep = current ? `${folder}${current}.webp` : null;
  const [files] = await bucket.getFiles({ prefix: folder });
  await Promise.all(
    files.filter((file) => file.name !== keep).map((file) => file.delete({ ignoreNotFound: true }))
  );

  return { photoId, status: "updated" as const };
});
