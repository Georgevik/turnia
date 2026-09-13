import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

/**
 * One-off: copies every request still pending into its group's `sync/updates.joinRequests` map.
 *
 * Admins list pending requests off that map, so a request made before it existed is invisible to
 * them until this runs. Safe to run twice: each entry is stamped with the request's own
 * `requestedAt`, exactly what `requestToJoinGroup` writes.
 *
 * Not deployed — index.ts does not export it. Run from `firebase/functions` with credentials for the
 * project (`gcloud auth application-default login`):
 *   npm run build && GCLOUD_PROJECT=turnia-23ebc node lib/scripts/backfillJoinRequestMarkers.js
 */
async function main() {
  initializeApp();
  const db = getFirestore();

  const pending = await db.collectionGroup("joinRequests").where("status", "==", "pending").get();
  const byGroup = new Map<string, Record<string, FirebaseFirestore.Timestamp>>();

  for (const request of pending.docs) {
    const groupId = request.ref.parent.parent?.id;
    const requestedAt = request.get("requestedAt");
    if (!groupId || !requestedAt) continue;

    byGroup.set(groupId, { ...byGroup.get(groupId), [request.id]: requestedAt });
  }

  for (const [groupId, joinRequests] of byGroup) {
    await db.doc(`groups/${groupId}/sync/updates`).set({ joinRequests }, { merge: true });
    console.log(`${groupId}: ${Object.keys(joinRequests).length} pending`);
  }
  console.log(`Done: ${pending.size} pending requests in ${byGroup.size} groups`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
