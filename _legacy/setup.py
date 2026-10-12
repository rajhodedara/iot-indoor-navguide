import os
import json

base_dir = r"c:\Users\odeda\Desktop\Projects\IOT Project"
firebase_dir = os.path.join(base_dir, "firebase")
admin_dir = os.path.join(base_dir, "admin-portal")

firebase_files = {
    "firebase.json": '''{
  "functions": {
    "source": "functions",
    "predeploy": [
      "npm --prefix \"$RESOURCE_DIR\" run build"
    ]
  },
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "hosting": {
    "public": "../admin-portal/dist",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}''',

    "firestore.rules": '''rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    function isStudent() {
      return request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'student';
    }
    
    function isStaff() {
      return request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'staff';
    }
    
    function isAdmin() {
      return request.auth != null && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    match /users/{userId} {
      allow read: if request.auth.uid == userId || isStaff() || isAdmin();
      allow write: if isAdmin();
    }
    
    match /buildings/{buildingId} {
      allow read: if request.auth != null; // Anyone logged in can read maps
      allow write: if isAdmin();
      
      match /beacons/{beaconId} {
        allow read: if request.auth != null;
        allow write: if isAdmin();
      }
      
      match /edges/{edgeId} {
        allow read: if request.auth != null;
        allow write: if isAdmin();
      }
    }
    
    match /schedules/{scheduleId} {
      // Students can read their own schedule, admins can read/write all
      allow read: if (request.auth != null && resource.data.student_uid == request.auth.uid) || isAdmin();
      allow write: if isAdmin();
    }
    
    match /sos_events/{eventId} {
      // Students can write SOS events
      allow create: if isStudent();
      allow read: if isStaff() || isAdmin() || (request.auth.uid == resource.data.student_uid);
      allow update: if isStaff() || isAdmin();
      allow delete: if isAdmin();
    }
    
    match /checkins/{checkinId} {
      allow create: if isStudent();
      allow read: if isAdmin(); // Only admins can see checkin history, staff gets live via SOS
      allow update, delete: if isAdmin();
    }
  }
}''',

    "firestore.indexes.json": '''{
  "indexes": [
    {
      "collectionGroup": "sos_events",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "checkins",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "beacon_zone", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "schedules",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "student_uid", "order": "ASCENDING" },
        { "fieldPath": "day", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}''',

    "functions/package.json": '''{
  "name": "functions",
  "scripts": {
    "build": "tsc",
    "serve": "npm run build && firebase emulators:start --only functions",
    "shell": "npm run build && firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "logs": "firebase functions:log"
  },
  "engines": {
    "node": "18"
  },
  "main": "lib/index.js",
  "dependencies": {
    "firebase-admin": "^11.8.0",
    "firebase-functions": "^4.3.1"
  },
  "devDependencies": {
    "typescript": "^4.9.0"
  },
  "private": true
}''',

    "functions/tsconfig.json": '''{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2017"
  },
  "compileOnSave": true,
  "include": [
    "src"
  ]
}''',

    "functions/src/index.ts": '''import * as admin from 'firebase-admin';

admin.initializeApp();

export { triggerSos } from './triggerSos';
export { resolveSos } from './resolveSos';
export { syncMap } from './syncMap';
export { cleanupCheckins } from './cleanupCheckins';
''',

    "functions/src/triggerSos.ts": '''import * as functions from 'firebase-functions';
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
''',

    "functions/src/resolveSos.ts": '''import * as functions from 'firebase-functions';
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
''',

    "functions/src/syncMap.ts": '''import * as functions from 'firebase-functions';
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
''',

    "functions/src/cleanupCheckins.ts": '''import * as functions from 'firebase-functions';
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
''',

    "functions/src/utils/notifications.ts": '''import * as admin from 'firebase-admin';

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
'''
}

admin_files = {
    "package.json": '''{
  "name": "admin-portal",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "firebase": "^10.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.10.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.0.0",
    "vite": "^4.3.2"
  }
}''',

    "vite.config.js": '''import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
})
''',

    "index.html": '''<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>IoT Indoor Navigation Admin Portal</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
''',

    "src/main.jsx": '''import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './styles/index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
''',

    "src/App.jsx": '''import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { auth } from './firebase';
import { onAuthStateChanged } from 'firebase/auth';

import Login from './components/Login';
import Dashboard from './components/Dashboard';
import MapEditor from './components/MapEditor';
import BeaconManager from './components/BeaconManager';
import ScheduleManager from './components/ScheduleManager';
import SOSMonitor from './components/SOSMonitor';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (u) => {
      setUser(u);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  if (loading) return <div>Loading...</div>;

  return (
    <Router>
      <div className="app-container dark-mode">
        {user && (
          <nav className="sidebar">
            <h2>Admin Portal</h2>
            <ul>
              <li><a href="/">Dashboard</a></li>
              <li><a href="/map">Map Editor</a></li>
              <li><a href="/beacons">Beacon Manager</a></li>
              <li><a href="/schedules">Schedules</a></li>
              <li><a href="/sos">SOS Monitor</a></li>
              <li><button onClick={() => auth.signOut()}>Logout</button></li>
            </ul>
          </nav>
        )}
        <main className="content">
          <Routes>
            <Route path="/login" element={!user ? <Login /> : <Navigate to="/" />} />
            <Route path="/" element={user ? <Dashboard /> : <Navigate to="/login" />} />
            <Route path="/map" element={user ? <MapEditor /> : <Navigate to="/login" />} />
            <Route path="/beacons" element={user ? <BeaconManager /> : <Navigate to="/login" />} />
            <Route path="/schedules" element={user ? <ScheduleManager /> : <Navigate to="/login" />} />
            <Route path="/sos" element={user ? <SOSMonitor /> : <Navigate to="/login" />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
''',

    "src/firebase.js": '''import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getFunctions } from "firebase/functions";

// Replace with your actual Firebase config
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT_ID.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const functions = getFunctions(app);
''',

    "src/components/Login.jsx": '''import { useState } from 'react';
import { auth } from '../firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="login-container">
      <h2>Admin Login</h2>
      {error && <p className="error">{error}</p>}
      <form onSubmit={handleLogin}>
        <input 
          type="email" 
          placeholder="Email" 
          value={email} 
          onChange={(e) => setEmail(e.target.value)} 
          required 
        />
        <input 
          type="password" 
          placeholder="Password" 
          value={password} 
          onChange={(e) => setPassword(e.target.value)} 
          required 
        />
        <button type="submit">Login</button>
      </form>
    </div>
  );
}
''',

    "src/components/Dashboard.jsx": '''import { useEffect, useState } from 'react';
import { db } from '../firebase';
import { collection, getCountFromServer, query, where } from 'firebase/firestore';

export default function Dashboard() {
  const [stats, setStats] = useState({ beacons: 0, activeSos: 0, schedules: 0 });

  useEffect(() => {
    async function fetchStats() {
      try {
        const beaconSnap = await getCountFromServer(collection(db, 'buildings', 'main_building', 'beacons'));
        const sosSnap = await getCountFromServer(query(collection(db, 'sos_events'), where('status', '==', 'active')));
        const schedSnap = await getCountFromServer(collection(db, 'schedules'));

        setStats({
          beacons: beaconSnap.data().count,
          activeSos: sosSnap.data().count,
          schedules: schedSnap.data().count
        });
      } catch (err) {
        console.error("Error fetching stats:", err);
      }
    }
    fetchStats();
  }, []);

  return (
    <div>
      <h1>Dashboard</h1>
      <div className="stats-grid">
        <div className="stat-card">
          <h3>Total Beacons</h3>
          <p>{stats.beacons}</p>
        </div>
        <div className="stat-card">
          <h3>Active SOS Events</h3>
          <p className="danger">{stats.activeSos}</p>
        </div>
        <div className="stat-card">
          <h3>Schedules Loaded</h3>
          <p>{stats.schedules}</p>
        </div>
      </div>
    </div>
  );
}
''',

    "src/components/MapEditor.jsx": '''import { useState } from 'react';

export default function MapEditor() {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  
  // Dummy implementation for visual layout
  return (
    <div className="map-editor">
      <h1>Map Editor</h1>
      <p>Upload a floor plan and define navigation nodes (beacons) and walkable paths (edges).</p>
      
      <div className="editor-controls">
        <button>Upload Floor Plan</button>
        <button>Add Node</button>
        <button>Draw Edge</button>
        <button className="primary">Save Map</button>
      </div>

      <div className="canvas-container">
        <div className="placeholder-canvas">
          <p>Canvas Area: Click to place nodes</p>
        </div>
      </div>
    </div>
  );
}
''',

    "src/components/BeaconManager.jsx": '''import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, getDocs, addDoc, deleteDoc, doc } from 'firebase/firestore';

export default function BeaconManager() {
  const [beacons, setBeacons] = useState([]);
  const [newBeacon, setNewBeacon] = useState({ minorId: '', location: '', description: '' });

  useEffect(() => {
    loadBeacons();
  }, []);

  const loadBeacons = async () => {
    // Assuming single building for now
    const snap = await getDocs(collection(db, 'buildings', 'main_building', 'beacons'));
    setBeacons(snap.docs.map(d => ({ id: d.id, ...d.data() })));
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    await addDoc(collection(db, 'buildings', 'main_building', 'beacons'), newBeacon);
    setNewBeacon({ minorId: '', location: '', description: '' });
    loadBeacons();
  };

  const handleDelete = async (id) => {
    await deleteDoc(doc(db, 'buildings', 'main_building', 'beacons', id));
    loadBeacons();
  };

  return (
    <div>
      <h1>Beacon Manager</h1>
      
      <form onSubmit={handleAdd} className="form-group">
        <input placeholder="Minor ID" value={newBeacon.minorId} onChange={e => setNewBeacon({...newBeacon, minorId: e.target.value})} required />
        <input placeholder="Location" value={newBeacon.location} onChange={e => setNewBeacon({...newBeacon, location: e.target.value})} required />
        <input placeholder="Description" value={newBeacon.description} onChange={e => setNewBeacon({...newBeacon, description: e.target.value})} />
        <button type="submit">Add Beacon</button>
      </form>

      <table className="data-table">
        <thead>
          <tr>
            <th>Minor ID</th>
            <th>Location</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {beacons.map(b => (
            <tr key={b.id}>
              <td>{b.minorId}</td>
              <td>{b.location}</td>
              <td>{b.description}</td>
              <td><button onClick={() => handleDelete(b.id)} className="danger">Delete</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
''',

    "src/components/ScheduleManager.jsx": '''import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, getDocs, addDoc, deleteDoc, doc } from 'firebase/firestore';

export default function ScheduleManager() {
  const [schedules, setSchedules] = useState([]);
  const [form, setForm] = useState({ student_uid: '', courseName: '', room: '', day: '', time: '' });

  useEffect(() => {
    loadSchedules();
  }, []);

  const loadSchedules = async () => {
    const snap = await getDocs(collection(db, 'schedules'));
    setSchedules(snap.docs.map(d => ({ id: d.id, ...d.data() })));
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    await addDoc(collection(db, 'schedules'), form);
    setForm({ student_uid: '', courseName: '', room: '', day: '', time: '' });
    loadSchedules();
  };

  const handleDelete = async (id) => {
    await deleteDoc(doc(db, 'schedules', id));
    loadSchedules();
  };

  return (
    <div>
      <h1>Schedule Manager</h1>
      
      <form onSubmit={handleAdd} className="form-group">
        <input placeholder="Student UID" value={form.student_uid} onChange={e => setForm({...form, student_uid: e.target.value})} required />
        <input placeholder="Course Name" value={form.courseName} onChange={e => setForm({...form, courseName: e.target.value})} required />
        <input placeholder="Room" value={form.room} onChange={e => setForm({...form, room: e.target.value})} required />
        <select value={form.day} onChange={e => setForm({...form, day: e.target.value})} required>
          <option value="">Select Day</option>
          <option value="Monday">Monday</option>
          <option value="Tuesday">Tuesday</option>
          <option value="Wednesday">Wednesday</option>
          <option value="Thursday">Thursday</option>
          <option value="Friday">Friday</option>
        </select>
        <input type="time" value={form.time} onChange={e => setForm({...form, time: e.target.value})} required />
        <button type="submit">Add Schedule</button>
      </form>

      <table className="data-table">
        <thead>
          <tr>
            <th>Student UID</th>
            <th>Course</th>
            <th>Room</th>
            <th>Day</th>
            <th>Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {schedules.map(s => (
            <tr key={s.id}>
              <td>{s.student_uid}</td>
              <td>{s.courseName}</td>
              <td>{s.room}</td>
              <td>{s.day}</td>
              <td>{s.time}</td>
              <td><button onClick={() => handleDelete(s.id)} className="danger">Delete</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
''',

    "src/components/SOSMonitor.jsx": '''import { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, query, where, onSnapshot, doc, updateDoc } from 'firebase/firestore';

export default function SOSMonitor() {
  const [activeSOS, setActiveSOS] = useState([]);

  useEffect(() => {
    const q = query(collection(db, 'sos_events'), where('status', '==', 'active'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const events = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setActiveSOS(events);
    });

    return () => unsubscribe();
  }, []);

  const handleResolve = async (id) => {
    await updateDoc(doc(db, 'sos_events', id), {
      status: 'resolved',
      resolvedAt: new Date(),
    });
  };

  return (
    <div>
      <h1>SOS Monitor</h1>
      <div className="sos-grid">
        {activeSOS.length === 0 ? (
          <p>No active SOS events.</p>
        ) : (
          activeSOS.map(sos => (
            <div key={sos.id} className="sos-card danger-border">
              <h3>🚨 SOS Alert!</h3>
              <p><strong>Student:</strong> {sos.student_name} ({sos.student_uid})</p>
              <p><strong>Location:</strong> {sos.location}</p>
              <p><strong>Time:</strong> {sos.timestamp?.toDate().toLocaleString()}</p>
              <button onClick={() => handleResolve(sos.id)} className="primary">Resolve Incident</button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
''',

    "src/styles/index.css": '''
:root {
  --bg-color: #121212;
  --text-color: #e0e0e0;
  --sidebar-bg: #1e1e1e;
  --primary: #bb86fc;
  --danger: #cf6679;
  --card-bg: #2d2d2d;
  --border: #3d3d3d;
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background-color: var(--bg-color);
  color: var(--text-color);
}

.app-container {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 250px;
  background-color: var(--sidebar-bg);
  padding: 20px;
  border-right: 1px solid var(--border);
}

.sidebar h2 {
  margin-top: 0;
  color: var(--primary);
}

.sidebar ul {
  list-style: none;
  padding: 0;
}

.sidebar li {
  margin-bottom: 15px;
}

.sidebar a, .sidebar button {
  color: var(--text-color);
  text-decoration: none;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  width: 100%;
  text-align: left;
}

.sidebar a:hover, .sidebar button:hover {
  color: var(--primary);
}

.content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

button {
  background-color: var(--border);
  color: var(--text-color);
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

button.primary {
  background-color: var(--primary);
  color: #000;
}

button.danger {
  background-color: var(--danger);
  color: #000;
}

.login-container {
  max-width: 400px;
  margin: 100px auto;
  padding: 30px;
  background: var(--sidebar-bg);
  border-radius: 8px;
}

.login-container input {
  display: block;
  width: 100%;
  margin-bottom: 15px;
  padding: 10px;
  box-sizing: border-box;
}

.stats-grid, .sos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.stat-card, .sos-card {
  background: var(--card-bg);
  padding: 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
}

.sos-card.danger-border {
  border-color: var(--danger);
}

.danger {
  color: var(--danger);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

.data-table th, .data-table td {
  border: 1px solid var(--border);
  padding: 10px;
  text-align: left;
}

.form-group input, .form-group select {
  margin-right: 10px;
  padding: 8px;
}

.placeholder-canvas {
  width: 100%;
  height: 500px;
  background-color: #2a2a2a;
  border: 2px dashed #444;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
}
'''
}

def write_files(base_path, file_dict):
    for rel_path, content in file_dict.items():
        full_path = os.path.join(base_path, rel_path)
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(content)

write_files(firebase_dir, firebase_files)
write_files(admin_dir, admin_files)
print("Success")
