import React, { useState, useEffect, useMemo } from 'react';
import { CalendarClock, Plus, Edit3, Trash2, X, Loader2, Clock, Search } from 'lucide-react';
import { scheduleApi, subjectApi, classroomApi, facultyApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const DAYS = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];

const Schedules = () => {
  const [schedules, setSchedules] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDay, setFilterDay] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ subject_id:'', classroom_id:'', faculty_id:'', day_of_week:'Monday', start_time:'09:00', end_time:'10:00' });
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [sch, sub, cls, fac] = await Promise.all([scheduleApi.getAll(), subjectApi.getAll(), classroomApi.getAll(), facultyApi.getAll()]);
      setSchedules(sch); setSubjects(sub); setClassrooms(cls); setFaculty(fac);
    } catch(e) { showToast(e.message,'error'); }
    finally { setLoading(false); }
  };
  useEffect(() => { fetchData(); }, []);

  const openModal = (s=null) => {
    if(s){ setIsEditing(true); setForm({id:s.id, subject_id:s.subject_id||'', classroom_id:s.classroom_id||'', faculty_id:s.faculty_id||'', day_of_week:s.day_of_week||'Monday', start_time:s.start_time||'09:00', end_time:s.end_time||'10:00'}); }
    else { setIsEditing(false); setForm({subject_id:'',classroom_id:'',faculty_id:'',day_of_week:'Monday',start_time:'09:00',end_time:'10:00'}); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    if(isEditing){ await scheduleApi.update(form.id, form); showToast('Schedule updated!'); }
    else { await scheduleApi.create(form); showToast('Schedule created!'); }
    setIsModalOpen(false); fetchData();
  } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await scheduleApi.delete(deleteTarget.id); showToast('Schedule deleted!'); setDeleteTarget(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  const q = searchTerm.toLowerCase();
  const filteredSchedules = useMemo(() => schedules.filter(s => {
    const subName = s.subjects?.name?.toLowerCase() || '';
    const roomName = s.classrooms?.name?.toLowerCase() || '';
    const matchText = !q || subName.includes(q) || roomName.includes(q) || s.start_time?.includes(q) || s.day_of_week?.toLowerCase().includes(q);
    return matchText && (!filterDay || s.day_of_week === filterDay);
  }), [schedules, q, filterDay]);

  const displayDays = filterDay ? [filterDay] : DAYS;
  const grouped = useMemo(() => {
    const g = {};
    displayDays.forEach(day => { g[day] = filteredSchedules.filter(s => s.day_of_week === day); });
    return g;
  }, [displayDays, filteredSchedules]);

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Delete Schedule" message="Remove this schedule entry?" onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><CalendarClock size={24} className="text-indigo-600" /> Schedule Management</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{schedules.length} timetable entries</p>
        </div>
        <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add Schedule</button>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
          <input type="text" placeholder="Search subject, room, time, day..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
            value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
        </div>
        <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterDay} onChange={e=>setFilterDay(e.target.value)}>
          <option value="">All Days</option>{DAYS.map(d=><option key={d} value={d}>{d}</option>)}
        </select>
        {(searchTerm||filterDay) && <button onClick={()=>{setSearchTerm('');setFilterDay('');}} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear</button>}
        <p className="text-xs text-gray-400 font-medium ml-auto">{filteredSchedules.length} entries</p>
      </div>

      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading schedules...</p></div>
      : (
        <div className="space-y-4">
          {displayDays.map(day => (
            <div key={day} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
              <div className="px-5 py-3 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <h3 className="text-sm font-bold text-gray-700 dark:text-gray-200">{day}</h3>
                <span className="text-[10px] text-gray-400 font-medium">{grouped[day]?.length || 0} classes</span>
              </div>
              {(!grouped[day] || grouped[day].length === 0) ? (
                <p className="px-5 py-4 text-sm text-gray-400 italic">No classes scheduled</p>
              ) : (
                <div className="divide-y divide-gray-50 dark:divide-gray-800">
                  {grouped[day].map(s => (
                    <div key={s.id} className="flex items-center gap-4 px-5 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors group">
                      <div className="w-9 h-9 rounded-lg bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 flex items-center justify-center flex-shrink-0"><Clock size={16}/></div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-gray-900 dark:text-white">{s.subjects?.name || 'Unknown Subject'}</p>
                        <p className="text-xs text-gray-400">{s.classrooms?.name || 'Unknown Room'} • {s.start_time} - {s.end_time}</p>
                      </div>
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onClick={()=>openModal(s)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={14}/></button>
                        <button onClick={()=>setDeleteTarget(s)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={14}/></button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Schedule':'Add New Schedule'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Subject</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.subject_id} onChange={e=>setForm({...form,subject_id:e.target.value})}><option value="">Select Subject</option>{subjects.map(s=><option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}</select></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Classroom</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.classroom_id} onChange={e=>setForm({...form,classroom_id:e.target.value})}><option value="">Select Classroom</option>{classrooms.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Faculty</label><select className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.faculty_id} onChange={e=>setForm({...form,faculty_id:e.target.value})}><option value="">Optional</option>{faculty.map(f=><option key={f.id} value={f.id}>{f.full_name}</option>)}</select></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Day</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.day_of_week} onChange={e=>setForm({...form,day_of_week:e.target.value})}>{DAYS.map(d=><option key={d} value={d}>{d}</option>)}</select></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Start Time</label><input type="time" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.start_time} onChange={e=>setForm({...form,start_time:e.target.value})}/></div>
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">End Time</label><input type="time" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.end_time} onChange={e=>setForm({...form,end_time:e.target.value})}/></div>
              </div>
              <div className="flex gap-3 mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
                <button type="button" onClick={()=>setIsModalOpen(false)} className="flex-1 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm">Cancel</button>
                <button type="submit" disabled={saving} className="flex-1 btn-primary px-4 py-2.5 text-white rounded-xl font-medium text-sm flex items-center justify-center gap-2">{saving&&<Loader2 size={16} className="animate-spin"/>}{isEditing?'Save':'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Schedules;
