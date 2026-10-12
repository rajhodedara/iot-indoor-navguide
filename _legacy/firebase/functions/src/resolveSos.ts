import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

export const resolveSos = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Must be logged in.');
  }

  const { eventId, resolutionNotes } = data;
  const uid = context.auth.uid;

  const userDoc = await admin.firestore().collection('users').doc(uid).get();
  if (!userDoc.exists || (userDoc.data()?.role !== 'staff' && userDoc.data()?.role !== 'admin')) {
    throw new functions.https.HttpsError('permission-denied', 'Only staff/admins can resolve SOS.');
  }

  const sosRef = admin.firestore().collection('sos_events').doc(eventId);
  
  await sosRef.update({
    status: 'resolved',
    resolvedBy: uid,
    resolvedAt: admin.firestore.FieldValue.serverTimestamp(),
    resolutionNotes: resolutionNotes || '',
  });

  return { success: true };
});
