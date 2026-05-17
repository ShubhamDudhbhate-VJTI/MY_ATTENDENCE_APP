import React, { useState, useEffect } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Faculty from './pages/Faculty'
import Subjects from './pages/Subjects'
import HODs from './pages/HODs'
import Classrooms from './pages/Classrooms'
import Attendance from './pages/Attendance'
import Schedules from './pages/Schedules'
import Assignments from './pages/Assignments'
import Analytics from './pages/Analytics'
import StudentReport from './pages/StudentReport'
import Login from './pages/Login'

function App() {
  const [isAuth, setIsAuth] = useState(() => localStorage.getItem('attendx_admin_auth') === 'true');

  if (!isAuth) {
    return <Login onLogin={() => setIsAuth(true)} />;
  }

  return (
    <Routes>
      <Route path="/" element={<Layout onLogout={() => { localStorage.removeItem('attendx_admin_auth'); setIsAuth(false); }} />}>
        <Route index element={<Dashboard />} />
        <Route path="students" element={<Students />} />
        <Route path="faculty" element={<Faculty />} />
        <Route path="hods" element={<HODs />} />
        <Route path="subjects" element={<Subjects />} />
        <Route path="classrooms" element={<Classrooms />} />
        <Route path="attendance" element={<Attendance />} />
        <Route path="schedules" element={<Schedules />} />
        <Route path="assignments" element={<Assignments />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="reports" element={<StudentReport />} />
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
