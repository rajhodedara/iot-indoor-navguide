import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { sendNotificationToRole } from './utils/notifications';

export const triggerSos = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Must be logged in.');
  }

  const { location, beaconId } = data;
  const uid = context.auth.uid;

  const userDoc = await admin.firestore().collection('users').doc(uid).get();
  if (!userDoc.exists || userDoc.data()?.role !== 'student') {
    throw new functions.https.HttpsError('permission-denied', 'Only students can trigger SOS.');
  }

  const sosRef = admin.firestore().collection('sos_events').doc();
  const eventData = {
    student_uid: uid,
    student_name: userDoc.data()?.name || 'Unknown Student',
    location: location || 'Unknown Location',
    beaconId: beaconId || null,
    status: 'active',
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  };

  await sosRef.set(eventData);

  // Send push notification to all staff
  await sendNotificationToRole('staff', {
    title: 'SOS Alert!',
    body: `SOS triggered by ${eventData.student_name} at ${eventData.location}`,
    data: {
      eventId: sosRef.id,
      student_uid: uid,
      location: eventData.location,
    }
  });

  return { success: true, eventId: sosRef.id };
});
