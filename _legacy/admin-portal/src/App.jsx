import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
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
