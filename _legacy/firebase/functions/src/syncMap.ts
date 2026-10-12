import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import * as crypto from 'crypto';

export const syncMap = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Must be logged in.');
  }

  const { buildingId, clientVersion } = data;

  const db = admin.firestore();
  
  const buildingDoc = await db.collection('buildings').doc(buildingId).get();
  if (!buildingDoc.exists) {
    throw new functions.https.HttpsError('not-found', 'Building not found.');
  }

  const beaconsSnap = await db.collection('buildings').doc(buildingId).collection('beacons').get();
  const edgesSnap = await db.collection('buildings').doc(buildingId).collection('edges').get();

  const mapData = {
    building: buildingDoc.data(),
    beacons: beaconsSnap.docs.map(doc => ({ id: doc.id, ...doc.data() })),
    edges: edgesSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }))
  };

  const hash = crypto.createHash('md5').update(JSON.stringify(mapData)).digest('hex');

  if (clientVersion === hash) {
    return { status: 304, message: 'Map up to date' };
  }

  return { status: 200, data: mapData, version: hash };
});
