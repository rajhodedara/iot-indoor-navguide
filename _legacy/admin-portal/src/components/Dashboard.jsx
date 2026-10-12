import { useEffect, useState } from 'react';
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
