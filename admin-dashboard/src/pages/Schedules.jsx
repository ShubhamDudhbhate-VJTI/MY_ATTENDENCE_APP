import React, { useState, useEffect, useMemo } from 'react';
import { CalendarClock, Plus, Edit3, Trash2, X, Loader2, Clock, Search, Filter, GitBranch, GraduationCap, BookOpen, Download, FileText } from 'lucide-react';
import { exportPDF, exportCSV } from '../lib/exportUtils';
import { scheduleApi, subjectApi, classroomApi, facultyApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const DAYS = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering'];
const YEARS = ['First Year','Second Year','Third Year','Final Year'];
const SEMESTERS = ['Semester 1','Semester 2','Semester 3','Semester 4','Semester 5','Semester 6','Semester 7','Semester 8'];

const yearToSemesters = {
  'First Year': ['Semester 1','Semester 2'],
  'Second Year': ['Semester 3','Semester 4'],
  'Third Year': ['Semester 5','Semester 6'],
  'Final Year': ['Semester 7','Semester 8'],
};
const semToYear = {};
Object.entries(yearToSemesters).forEach(([y, sems]) => sems.forEach(s => { semToYear[s] = y; }));

const Schedules = () => {
  const [schedules, setSchedules] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [classrooms, setClassrooms] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDay, setFilterDay] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [filterYear, setFilterYear] = useState('');
  const [filterSemester, setFilterSemester] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ subject_id:'', classroom_id:'', faculty_id:'', day_of_week:'Monday', start_time:'09:00', end_time:'10:00', branch:'', year:'', semester:'' });
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

  // When filter branch changes, reset dependent filters
  useEffect(() => { setFilterSemester(''); }, [filterBranch]);
  useEffect(() => { setFilterSemester(''); }, [filterYear]);

  const openModal = (s=null) => {
    if(s){
      const subj = subjects.find(x => x.id === s.subject_id);
      setIsEditing(true);
      setForm({ id:s.id, subject_id:s.subject_id||'', classroom_id:s.classroom_id||'', faculty_id:s.faculty_id||'', day_of_week:s.day_of_week||'Monday', start_time:s.start_time||'09:00', end_time:s.end_time||'10:00', branch: subj?.branch || s.subjects?.branch || '', year: subj?.year || s.subjects?.year || '', semester:'' });
    } else {
      setIsEditing(false);
      setForm({ subject_id:'', classroom_id:'', faculty_id:'', day_of_week:'Monday', start_time:'09:00', end_time:'10:00', branch: filterBranch, year: filterYear, semester: filterSemester });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      const payload = { subject_id: form.subject_id, classroom_id: form.classroom_id, faculty_id: form.faculty_id, day_of_week: form.day_of_week, start_time: form.start_time, end_time: form.end_time };
      if(isEditing){ await scheduleApi.update(form.id, payload); showToast('Schedule updated!'); }
      else { await scheduleApi.create(payload); showToast('Schedule created!'); }
      setIsModalOpen(false); fetchData();
    } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); }
  };

  const handleDelete = async () => { try { await scheduleApi.delete(deleteTarget.id); showToast('Schedule deleted!'); setDeleteTarget(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  // Filter schedules
  const q = searchTerm.toLowerCase();
  const filteredSchedules = useMemo(() => schedules.filter(s => {
    const subName = s.subjects?.name?.toLowerCase() || '';
    const roomName = s.classrooms?.name?.toLowerCase() || '';
    const subBranch = s.subjects?.branch || '';
    const subYear = s.subjects?.year || '';
    const matchText = !q || subName.includes(q) || roomName.includes(q) || s.start_time?.includes(q) || s.day_of_week?.toLowerCase().includes(q);
    const matchDay = !filterDay || s.day_of_week === filterDay;
    const matchBranch = !filterBranch || subBranch === filterBranch;
    const matchYear = !filterYear || subYear === filterYear;
    const matchSem = !filterSemester || (semToYear[filterSemester] && subYear === semToYear[filterSemester]);
    return matchText && matchDay && matchBranch && matchYear && matchSem;
  }), [schedules, q, filterDay, filterBranch, filterYear, filterSemester]);

  const displayDays = filterDay ? [filterDay] : DAYS;
  const grouped = useMemo(() => {
    const g = {};
    displayDays.forEach(day => { g[day] = filteredSchedules.filter(s => s.day_of_week === day); });
    return g;
  }, [displayDays, filteredSchedules]);

  // Modal: filtered subjects based on branch/year selection
  const modalSubjects = useMemo(() => {
    let filtered = subjects;
    if (form.branch) filtered = filtered.filter(s => s.branch === form.branch);
    if (form.year) filtered = filtered.filter(s => s.year === form.year);
    return filtered;
  }, [subjects, form.branch, form.year]);

  // Modal: available semesters based on selected year
  const modalSemesters = form.year ? (yearToSemesters[form.year] || []) : SEMESTERS;

  // Available years for filter based on selected branch
  const availableFilterYears = filterBranch ? [...new Set(schedules.filter(s => s.subjects?.branch === filterBranch).map(s => s.subjects?.year).filter(Boolean))] : YEARS;
  const availableFilterSemesters = filterYear ? (yearToSemesters[filterYear] || []) : SEMESTERS;

  const activeFilters = [filterBranch, filterYear, filterSemester, filterDay, searchTerm].filter(Boolean).length;

  // Branch color mapping
  const branchColors = {
    'Information Technology': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400' },
    'Computer Engineering': { bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400' },
    'Mechanical Engineering': { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-700 dark:text-orange-400' },
    'Civil Engineering': { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400' },
    'Electronics Engineering': { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400' },
    'Electrical Engineering': { bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-700 dark:text-yellow-400' },
  };
  const getBranchColor = (b) => branchColors[b] || { bg:'bg-gray-100 dark:bg-gray-800', text:'text-gray-600 dark:text-gray-400' };
  const shortBranch = (b) => (b||'').replace(' Engineering','').replace('Information Technology','IT');

  // Export functions
  const expHeaders = ['Day','Subject','Code','Classroom','Start Time','End Time','Branch','Year'];
  const getExpRows = () => filteredSchedules.map(s => [s.day_of_week, s.subjects?.name, s.subjects?.code, s.classrooms?.name, s.start_time, s.end_time, s.subjects?.branch, s.subjects?.year]);

  const handleExportCSV = () => {
    exportCSV({ headers: expHeaders, rows: getExpRows(), filename: `schedules_export_${new Date().toISOString().slice(0,10)}.csv` });
    showToast(`Exported ${filteredSchedules.length} schedules to CSV`, 'info');
  };
  const handleExportPDF = () => {
    exportPDF({ title: 'Schedule Report', subtitle: `${filteredSchedules.length} timetable entries • AttendX Admin Dashboard`, headers: expHeaders, rows: getExpRows(), filters: [{ label: 'Branch', value: filterBranch }, { label: 'Year', value: filterYear }, { label: 'Semester', value: filterSemester }, { label: 'Day', value: filterDay }], filename: `Schedule_Report_${new Date().toISOString().slice(0,10)}.pdf` });
    showToast(`Exported ${filteredSchedules.length} schedules to PDF`, 'info');
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Delete Schedule" message="Remove this schedule entry?" onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><CalendarClock size={24} className="text-indigo-600" /> Schedule Management</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{schedules.length} timetable entries {filterBranch && <span className="text-indigo-500 font-medium">• {shortBranch(filterBranch)}</span>} {filterYear && <span className="text-emerald-500 font-medium">• {filterYear}</span>} {filterSemester && <span className="text-amber-500 font-medium">• {filterSemester}</span>}</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExportPDF} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><FileText size={16}/> PDF</button>
          <button onClick={handleExportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><Download size={16}/> CSV</button>
          <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add Schedule</button>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm p-4">
        <div className="flex items-center gap-2 mb-3">
          <Filter size={14} className="text-gray-400" />
          <span className="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider">Filters</span>
          {activeFilters > 0 && <span className="ml-1 px-2 py-0.5 bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-400 rounded-full text-[10px] font-bold">{activeFilters} active</span>}
        </div>
        <div className="flex flex-wrap gap-3 items-center">
          <div className="relative w-full max-w-xs">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
            <input type="text" placeholder="Search subject, room..."
              className="w-full pl-10 pr-4 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
              value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
          </div>
          <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterBranch} onChange={e=>{setFilterBranch(e.target.value); setFilterYear(''); setFilterSemester('');}}>
            <option value="">All Branches</option>{BRANCHES.map(b=><option key={b} value={b}>{shortBranch(b)}</option>)}
          </select>
          <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterYear} onChange={e=>{setFilterYear(e.target.value); setFilterSemester('');}}>
            <option value="">All Years</option>{YEARS.map(y=><option key={y} value={y}>{y}</option>)}
          </select>
          <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterSemester} onChange={e=>setFilterSemester(e.target.value)}>
            <option value="">All Semesters</option>{availableFilterSemesters.map(s=><option key={s} value={s}>{s}</option>)}
          </select>
          <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterDay} onChange={e=>setFilterDay(e.target.value)}>
            <option value="">All Days</option>{DAYS.map(d=><option key={d} value={d}>{d}</option>)}
          </select>
          {activeFilters > 0 && <button onClick={()=>{setSearchTerm('');setFilterDay('');setFilterBranch('');setFilterYear('');setFilterSemester('');}} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear All</button>}
          <p className="text-xs text-gray-400 font-medium ml-auto">{filteredSchedules.length} of {schedules.length} entries</p>
        </div>
      </div>

      {/* Schedule Grid */}
      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading schedules...</p></div>
      : filteredSchedules.length === 0 ? (
        <div className="p-12 text-center bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800">
          <CalendarClock size={48} className="mx-auto mb-3 opacity-20 text-gray-400"/>
          <p className="text-gray-400 text-sm">No schedules found for the selected filters.</p>
          <p className="text-gray-300 dark:text-gray-600 text-xs mt-1">Try changing branch, year, or semester.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {displayDays.map(day => {
            const daySchedules = grouped[day];
            if (!daySchedules || daySchedules.length === 0) return null;
            return (
            <div key={day} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
              <div className="px-5 py-3 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <h3 className="text-sm font-bold text-gray-700 dark:text-gray-200">{day}</h3>
                <span className="text-[10px] text-gray-400 font-medium">{daySchedules.length} classes</span>
              </div>
              <div className="divide-y divide-gray-50 dark:divide-gray-800">
                {daySchedules.map(s => {
                  const bc = getBranchColor(s.subjects?.branch);
                  return (
                  <div key={s.id} className="flex items-center gap-4 px-5 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors group">
                    <div className="w-9 h-9 rounded-lg bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 flex items-center justify-center flex-shrink-0"><Clock size={16}/></div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-gray-900 dark:text-white">{s.subjects?.name || 'Unknown Subject'}</p>
                      <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                        <span className="text-xs text-gray-400">{s.classrooms?.name || '—'} • {s.start_time} - {s.end_time}</span>
                        {s.subjects?.branch && <span className={`px-2 py-0.5 rounded-md text-[10px] font-semibold ${bc.bg} ${bc.text}`}>{shortBranch(s.subjects.branch)}</span>}
                        {s.subjects?.year && <span className="px-2 py-0.5 rounded-md text-[10px] font-medium bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400">{s.subjects.year}</span>}
                      </div>
                    </div>
                    <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={()=>openModal(s)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={14}/></button>
                      <button onClick={()=>setDeleteTarget(s)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={14}/></button>
                    </div>
                  </div>
                  );
                })}
              </div>
            </div>
            );
          })}
        </div>
      )}

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Schedule':'Add New Schedule'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Branch */}
              <div>
                <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5"><GitBranch size={12}/>Branch</label>
                <select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.branch} onChange={e=>setForm({...form, branch:e.target.value, year:'', semester:'', subject_id:''})}>
                  <option value="">Select Branch</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}
                </select>
              </div>
              {/* Year */}
              <div>
                <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5"><GraduationCap size={12}/>Year</label>
                <select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.year} onChange={e=>setForm({...form, year:e.target.value, semester:'', subject_id:''})}>
                  <option value="">Select Year</option>{YEARS.map(y=><option key={y} value={y}>{y}</option>)}
                </select>
              </div>
              {/* Semester */}
              <div>
                <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5"><BookOpen size={12}/>Semester</label>
                <select className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.semester} onChange={e=>setForm({...form, semester:e.target.value})}>
                  <option value="">Select Semester</option>{modalSemesters.map(s=><option key={s} value={s}>{s}</option>)}
                </select>
              </div>
              {/* Subject - cascaded */}
              <div>
                <label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Subject {form.branch && <span className="normal-case text-indigo-500">({shortBranch(form.branch)}{form.year ? ` / ${form.year}` : ''})</span>}</label>
                <select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.subject_id} onChange={e=>setForm({...form,subject_id:e.target.value})}>
                  <option value="">{form.branch ? `Select Subject (${modalSubjects.length} available)` : 'Select branch & year first'}</option>
                  {modalSubjects.map(s=><option key={s.id} value={s.id}>{s.name} ({s.code})</option>)}
                </select>
              </div>
              {/* Classroom */}
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Classroom</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.classroom_id} onChange={e=>setForm({...form,classroom_id:e.target.value})}><option value="">Select Classroom</option>{classrooms.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
              {/* Faculty */}
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Faculty</label><select className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.faculty_id} onChange={e=>setForm({...form,faculty_id:e.target.value})}><option value="">Optional</option>{faculty.map(f=><option key={f.id} value={f.id}>{f.full_name}</option>)}</select></div>
              {/* Day */}
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Day</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.day_of_week} onChange={e=>setForm({...form,day_of_week:e.target.value})}>{DAYS.map(d=><option key={d} value={d}>{d}</option>)}</select></div>
              {/* Time */}
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
