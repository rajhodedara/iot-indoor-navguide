import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

export const cleanupCheckins = functions.pubsub.schedule('every 1 hours').onRun(async (context) => {
  const db = admin.firestore();
  const thirtyMinutesAgo = new Date(Date.now() - 30 * 60 * 1000);

  const snapshot = await db.collection('checkins')
    .where('timestamp', '<', admin.firestore.Timestamp.fromDate(thirtyMinutesAgo))
    .get();

  if (snapshot.empty) {
    return null;
  }

  const batch = db.batch();
  snapshot.docs.forEach((doc) => {
    batch.delete(doc.ref);
  });

  await batch.commit();
  console.log(`Deleted ${snapshot.size} old checkins.`);
  return null;
});
