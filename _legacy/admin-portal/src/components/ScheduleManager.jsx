import { useState, useEffect } from 'react';
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
