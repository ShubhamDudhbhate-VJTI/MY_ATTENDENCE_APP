import { supabase } from '../lib/supabase';

// Helper to generate UUID (browser-compatible)
function generateId() {
  return crypto.randomUUID ? crypto.randomUUID() : 
    'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = Math.random() * 16 | 0;
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
}

// ==================== STUDENTS ====================
export const studentApi = {
  async getAll() {
    const { data, error } = await supabase
      .from('app_students')
      .select('id, registration_number, full_name, branch, year, app_users!inner(email)')
      .order('full_name');
    if (error) throw error;
    return data.map(s => ({
      id: s.id,
      registration_number: s.registration_number,
      full_name: s.full_name,
      branch: s.branch,
      year: s.year,
      email: s.app_users?.email || ''
    }));
  },

  async create({ registration_number, full_name, email, password, branch, year }) {
    const id = generateId();
    // 1. Insert into app_users
    const { error: userErr } = await supabase.from('app_users').insert({
      id, username: registration_number, email, password_hash: password,
      full_name, role: 'student'
    });
    if (userErr) throw userErr;
    // 2. Insert into app_students
    const { error: studentErr } = await supabase.from('app_students').insert({
      id, registration_number, full_name, branch, year
    });
    if (studentErr) throw studentErr;
    return { id };
  },

  async update(id, { full_name, branch, year, registration_number }) {
    const updates = {};
    const userUpdates = {};
    if (full_name) { updates.full_name = full_name; userUpdates.full_name = full_name; }
    if (branch) updates.branch = branch;
    if (year) updates.year = year;
    if (registration_number) { updates.registration_number = registration_number; userUpdates.username = registration_number; }

    if (Object.keys(updates).length) {
      const { error } = await supabase.from('app_students').update(updates).eq('id', id);
      if (error) throw error;
    }
    if (Object.keys(userUpdates).length) {
      const { error } = await supabase.from('app_users').update(userUpdates).eq('id', id);
      if (error) throw error;
    }
  },

  async delete(id) {
    const { error } = await supabase.from('app_users').delete().eq('id', id);
    if (error) throw error;
  }
};

// ==================== FACULTY ====================
export const facultyApi = {
  async getAll() {
    const { data, error } = await supabase
      .from('app_users')
      .select('id, username, email, full_name, app_teachers!inner(employee_id, full_name, branch, designation)')
      .eq('role', 'faculty')
      .order('full_name');
    if (error) throw error;
    return data.map(u => ({
      id: u.id,
      username: u.username,
      email: u.email,
      full_name: u.full_name,
      employee_id: u.app_teachers?.employee_id || u.username,
      branch: u.app_teachers?.branch || '',
      designation: u.app_teachers?.designation || ''
    }));
  },

  async create({ employee_id, full_name, email, password, branch, designation }) {
    const id = generateId();
    const { error: userErr } = await supabase.from('app_users').insert({
      id, username: employee_id, email, password_hash: password,
      full_name, role: 'faculty'
    });
    if (userErr) throw userErr;
    const { error: teacherErr } = await supabase.from('app_teachers').insert({
      id, employee_id, full_name, branch, designation
    });
    if (teacherErr) throw teacherErr;
    return { id };
  },

  async update(id, { full_name, branch, designation, employee_id }) {
    const teacherUpdates = {};
    const userUpdates = {};
    if (full_name) { teacherUpdates.full_name = full_name; userUpdates.full_name = full_name; }
    if (branch) teacherUpdates.branch = branch;
    if (designation) teacherUpdates.designation = designation;
    if (employee_id) { teacherUpdates.employee_id = employee_id; userUpdates.username = employee_id; }

    if (Object.keys(teacherUpdates).length) {
      const { error } = await supabase.from('app_teachers').update(teacherUpdates).eq('id', id);
      if (error) throw error;
    }
    if (Object.keys(userUpdates).length) {
      const { error } = await supabase.from('app_users').update(userUpdates).eq('id', id);
      if (error) throw error;
    }
  },

  async delete(id) {
    const { error } = await supabase.from('app_users').delete().eq('id', id);
    if (error) throw error;
  }
};

// ==================== SUBJECTS ====================
export const subjectApi = {
  async getAll() {
    const { data, error } = await supabase
      .from('subjects')
      .select('*')
      .order('name');
    if (error) throw error;
    return data;
  },

  async create({ name, code, branch, year }) {
    const id = generateId();
    const { error } = await supabase.from('subjects').insert({ id, name, code, branch, year });
    if (error) throw error;
    return { id };
  },

  async update(id, { name, code, branch, year }) {
    const updates = {};
    if (name) updates.name = name;
    if (code) updates.code = code;
    if (branch) updates.branch = branch;
    if (year) updates.year = year;
    const { error } = await supabase.from('subjects').update(updates).eq('id', id);
    if (error) throw error;
  },

  async delete(id) {
    const { error } = await supabase.from('subjects').delete().eq('id', id);
    if (error) throw error;
  }
};

// ==================== CLASSROOMS ====================
export const classroomApi = {
  async getAll() {
    const { data, error } = await supabase
      .from('classrooms')
      .select('*')
      .order('name');
    if (error) throw error;
    return data;
  },

  async create({ name, wifi_ssid, wifi_bssid }) {
    const id = generateId();
    const { error } = await supabase.from('classrooms').insert({ id, name, wifi_ssid, wifi_bssid });
    if (error) throw error;
    return { id };
  },

  async update(id, { name, wifi_ssid, wifi_bssid }) {
    const updates = {};
    if (name !== undefined) updates.name = name;
    if (wifi_ssid !== undefined) updates.wifi_ssid = wifi_ssid;
    if (wifi_bssid !== undefined) updates.wifi_bssid = wifi_bssid;
    const { error } = await supabase.from('classrooms').update(updates).eq('id', id);
    if (error) throw error;
  },

  async delete(id) {
    const { error } = await supabase.from('classrooms').delete().eq('id', id);
    if (error) throw error;
  }
};

// ==================== ATTENDANCE ====================
export const attendanceApi = {
  async getSessions() {
    const { data, error } = await supabase
      .from('attendance_sessions')
      .select('*, subjects(name, code, branch)')
      .order('start_time', { ascending: false })
      .limit(200);
    if (error) throw error;
    return data;
  },

  async getSessionRecords(sessionId) {
    const { data, error } = await supabase
      .from('attendance_records')
      .select('*, app_students(full_name, registration_number, branch)')
      .eq('session_id', sessionId)
      .order('marked_at');
    if (error) throw error;
    return data;
  },

  async deleteSession(sessionId) {
    // Delete records first, then session
    await supabase.from('attendance_records').delete().eq('session_id', sessionId);
    const { error } = await supabase.from('attendance_sessions').delete().eq('id', sessionId);
    if (error) throw error;
  }
};

// ==================== SCHEDULES ====================
export const scheduleApi = {
  async getAll() {
    const { data, error } = await supabase
      .from('schedules')
      .select('*, subjects(name, code), classrooms(name)')
      .order('day_of_week');
    if (error) throw error;
    return data;
  },

  async create({ subject_id, classroom_id, faculty_id, day_of_week, start_time, end_time }) {
    const id = generateId();
    const { error } = await supabase.from('schedules').insert({
      id, subject_id, classroom_id, faculty_id, day_of_week, start_time, end_time, is_official: true
    });
    if (error) throw error;
    return { id };
  },

  async update(id, updates) {
    const { error } = await supabase.from('schedules').update(updates).eq('id', id);
    if (error) throw error;
  },

  async delete(id) {
    const { error } = await supabase.from('schedules').delete().eq('id', id);
    if (error) throw error;
  }
};

// ==================== ANALYTICS (Dashboard) ====================
export const analyticsApi = {
  async getDashboardStats() {
    const [students, faculty, subjects, classrooms, sessions, records] = await Promise.all([
      supabase.from('app_students').select('id', { count: 'exact', head: true }),
      supabase.from('app_teachers').select('id', { count: 'exact', head: true }),
      supabase.from('subjects').select('id', { count: 'exact', head: true }),
      supabase.from('classrooms').select('id', { count: 'exact', head: true }),
      supabase.from('attendance_sessions').select('id', { count: 'exact', head: true }),
      supabase.from('attendance_records').select('id', { count: 'exact', head: true }),
    ]);
    return {
      totalStudents: students.count || 0,
      totalFaculty: faculty.count || 0,
      totalSubjects: subjects.count || 0,
      totalClassrooms: classrooms.count || 0,
      totalSessions: sessions.count || 0,
      totalRecords: records.count || 0,
    };
  },

  async getStudentsByBranch() {
    const { data, error } = await supabase.from('app_students').select('branch');
    if (error) throw error;
    const counts = {};
    data.forEach(s => { const b = s.branch || 'Unknown'; counts[b] = (counts[b] || 0) + 1; });
    return Object.entries(counts).map(([name, value]) => ({ name: name.replace(' Engineering', '').replace('Information Technology', 'IT'), fullName: name, value }));
  },

  async getStudentsByYear() {
    const { data, error } = await supabase.from('app_students').select('year');
    if (error) throw error;
    const counts = {};
    data.forEach(s => { const y = s.year || 'Unknown'; counts[y] = (counts[y] || 0) + 1; });
    return Object.entries(counts).map(([name, value]) => ({ name, value }));
  },

  async getFacultyByBranch() {
    const { data, error } = await supabase.from('app_teachers').select('branch');
    if (error) throw error;
    const counts = {};
    data.forEach(f => { const b = f.branch || 'Unknown'; counts[b] = (counts[b] || 0) + 1; });
    return Object.entries(counts).map(([name, value]) => ({ name: name.replace(' Engineering', '').replace('Information Technology', 'IT'), fullName: name, value }));
  },

  async getSubjectsByBranch() {
    const { data, error } = await supabase.from('subjects').select('branch');
    if (error) throw error;
    const counts = {};
    data.forEach(s => { const b = s.branch || 'Common'; counts[b] = (counts[b] || 0) + 1; });
    return Object.entries(counts).map(([name, value]) => ({ name: name.replace(' Engineering', '').replace('Information Technology', 'IT'), fullName: name, value }));
  },

  async getSessionStatusBreakdown() {
    const { data, error } = await supabase.from('attendance_sessions').select('status');
    if (error) throw error;
    const counts = {};
    data.forEach(s => { const st = s.status || 'unknown'; counts[st] = (counts[st] || 0) + 1; });
    return Object.entries(counts).map(([name, value]) => ({ name: name.charAt(0).toUpperCase() + name.slice(1), value }));
  },

  async getRecentSessions(limit = 6) {
    const { data, error } = await supabase
      .from('attendance_sessions')
      .select('id, status, start_time, qr_expires_at, subjects(name), app_users!attendance_sessions_faculty_id_fkey(full_name)')
      .order('start_time', { ascending: false })
      .limit(limit);
    if (error) throw error;
    return data;
  },

  async getRecentStudents(limit = 5) {
    const { data, error } = await supabase
      .from('app_students')
      .select('id, full_name, registration_number, branch, year')
      .order('full_name')
      .limit(limit);
    if (error) throw error;
    return data;
  }
};
