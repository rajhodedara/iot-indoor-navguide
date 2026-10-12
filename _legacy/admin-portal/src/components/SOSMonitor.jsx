import { useState, useEffect } from 'react';
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
