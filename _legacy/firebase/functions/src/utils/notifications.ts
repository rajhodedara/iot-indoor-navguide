import * as admin from 'firebase-admin';

export async function sendNotificationToRole(role: string, payload: { title: string; body: string; data?: any }) {
  const db = admin.firestore();
  const usersSnap = await db.collection('users').where('role', '==', role).get();
  
  const tokens: string[] = [];
  usersSnap.docs.forEach(doc => {
    const data = doc.data();
    if (data.fcmToken) {
      tokens.push(data.fcmToken);
    }
  });

  if (tokens.length === 0) return;

  const message = {
    notification: {
      title: payload.title,
      body: payload.body
    },
    data: payload.data || {},
    tokens: tokens
  };

  try {
    const response = await admin.messaging().sendMulticast(message);
    console.log(`${response.successCount} messages sent successfully to role ${role}`);
  } catch (error) {
    console.error('Error sending message:', error);
  }
}
