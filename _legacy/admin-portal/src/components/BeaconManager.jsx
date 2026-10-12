import { useState, useEffect } from 'react';
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
