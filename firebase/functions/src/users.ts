import { onDocumentWritten } from "firebase-functions/v2/firestore";
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

/**
 * An upload no profile names is left this long before a later `setProfilePhoto` deletes it: younger,
 * it may be another device's photo whose publish is still on its way.
 */
const ORPHAN_AGE_MS = 5 * 60 * 1000;

/**
 * The bucket both apps upload to: `<project>.firebasestorage.app`, as their Firebase configs name it.
 * Named rather than left to the default, which the emulator takes as `<project>.appspot.com` when
 * the CLI is not signed in — as on CI — and the function would look for files in a bucket nobody
 * uploads to.
 */
export function photoBucket() {
  const projectId = process.env.GCLOUD_PROJECT ?? JSON.parse(process.env.FIREBASE_CONFIG ?? "{}").projectId;
  return getStorage().bucket(`${projectId}.firebasestorage.app`);
}

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
      // Field by field, never the whole entry: it also carries the photo, which a rename must keep.
      batch.update(group.ref, {
        [`members.${uid}.name`]: name,
        [`members.${uid}.username`]: username,
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
 * Afterwards the photo this one replaced is deleted, and so is any upload that never got as far as
 * this call once it is a few minutes old. A younger one is left alone: it may be another device's,
 * whose publish is still on its way, and deleting it would leave that profile naming a missing file.
 * Each group copies whatever the profile names when its own transaction runs, not the request, so
 * two devices publishing at once leave the groups agreeing with the profile.
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

  const bucket = photoBucket();
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
  const replaced = await db.runTransaction(async (tx) => {
    const user = await tx.get(userRef);
    const username = user.get("username") as string | undefined;
    const reservationRef = username ? db.doc(`usernames/${username}`) : null;
    const reservation = reservationRef ? await tx.get(reservationRef) : null;

    tx.set(userRef, { photoId: photo, updateAt: FieldValue.serverTimestamp() }, { merge: true });
    if (reservationRef && reservation?.get("uid") === uid) {
      tx.update(reservationRef, { updateAt: FieldValue.serverTimestamp() });
    }
    markUserUpdated(db, tx, uid, "profile");
    return user.get("photoId") as string | undefined;
  });

  // Entry and sync marker together, per group, with the photo the profile names when it runs.
  const groups = await db.collection("groups").where("memberUids", "array-contains", uid).get();
  await Promise.all(
    groups.docs.map((group) =>
      db.runTransaction(async (tx) => {
        const current = (await tx.get(userRef)).get("photoId") as string | undefined;
        tx.update(group.ref, {
          [`members.${uid}.photoId`]: current ?? FieldValue.delete(),
          updateAt: FieldValue.serverTimestamp(),
        });
        markGroupUpdated(db, tx, group.id);
      })
    )
  );

  const current = (await userRef.get()).get("photoId") as string | undefined;
  const fileOf = (id: string | undefined | null) => (id ? `${folder}${id}.webp` : null);
  const [files] = await bucket.getFiles({ prefix: folder });
  const orphanedBefore = Date.now() - ORPHAN_AGE_MS;
  await Promise.all(
    files
      .filter((file) => file.name !== fileOf(current))
      .filter((file) =>
        file.name === fileOf(replaced) ||
        new Date(file.metadata.timeCreated ?? Date.now()).getTime() < orphanedBefore)
      .map((file) => file.delete({ ignoreNotFound: true }))
  );

  return { photoId, status: "updated" as const };
});

type MemberEntry = { name?: string; username?: string; photoId?: string };

/**
 * Puts back a member's photo copy that a write to their group lost.
 *
 * A client released before photos existed knows a member entry as `{ name, username }`, and when an
 * admin on it saves the group it writes the whole document back, every `photoId` dropped. A group
 * such a client creates starts without its creator's photo too. Whatever the profile still names is
 * written back, so the copies heal as soon as the write lands, whoever opens the app.
 *
 * Only a uid still in `memberUids` with a named entry is considered: someone who left or was revoked
 * has no entry to restore into, and a blank name is an anonymized account, whose profile
 * `deleteAccount` has already cleared. Its own write loses nothing, so the run it causes does
 * nothing.
 */
export const onGroupMemberPhotoLost = onDocumentWritten("groups/{groupId}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;
  const before = event.data?.before;
  const created = !before?.exists;

  const afterMembers = (after.get("members") as Record<string, MemberEntry> | undefined) ?? {};
  const beforeMembers = created ? {} : ((before?.get("members") as Record<string, MemberEntry> | undefined) ?? {});
  const memberUids = (after.get("memberUids") as string[] | undefined) ?? [];

  const lost = memberUids.filter((uid) => {
    const entry = afterMembers[uid];
    if (!entry?.name || entry.photoId) return false;
    return created || Boolean(beforeMembers[uid]?.photoId);
  });
  if (lost.length === 0) return;

  const db = getFirestore();
  const profiles = await db.getAll(...lost.map((uid) => db.doc(`users/${uid}`)));
  const restored: Record<string, string> = {};
  profiles.forEach((profile, index) => {
    const photoId = profile.get("photoId") as string | undefined;
    if (photoId) restored[`members.${lost[index]}.photoId`] = photoId;
  });
  if (Object.keys(restored).length === 0) return;

  const { groupId } = event.params;
  const batch = db.batch();
  batch.update(after.ref, { ...restored, updateAt: FieldValue.serverTimestamp() });
  markGroupUpdated(db, batch, groupId);
  await batch.commit();
});
