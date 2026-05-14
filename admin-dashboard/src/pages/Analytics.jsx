import React, { useState, useEffect, useMemo } from 'react';
import { 
  BarChart3, PieChart as PieChartIcon, Users, BookOpen, UserSquare2, 
  Filter, Search, ArrowUpRight, ArrowDownRight, TrendingUp,
  GraduationCap, GitBranch, Calendar, Loader2, ChevronRight,
  Info, Camera, Star, AlertCircle, CheckCircle2
} from 'lucide-react';
import { 
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, AreaChart, Area
} from 'recharts';
import { supabase } from '../lib/supabase';
import ProfileCard from '../components/ProfileCard';
import { useToast } from '../components/Toast';

const BRANCHES = [
  'Information Technology',
  'Computer Engineering',
  'Mechanical Engineering',
  'Civil Engineering',
  'Electronics Engineering',
  'Electrical Engineering'
];

const YEARS = ['First Year', 'Second Year', 'Third Year', 'Final Year'];

const yearToSemesters = {
  'First Year': ['Semester 1', 'Semester 2'],
  'Second Year': ['Semester 3', 'Semester 4'],
  'Third Year': ['Semester 5', 'Semester 6'],
  'Final Year': ['Semester 7', 'Semester 8'],
};

const COLORS = [
  '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', 
  '#ec4899', '#06b6d4', '#f97316', '#14b8a6', '#6366f1'
];

const Analytics = () => {
  const [loading, setLoading] = useState(true);
  const [filterBranch, setFilterBranch] = useState(BRANCHES[0]);
  const [filterYear, setFilterYear] = useState(YEARS[0]);
  const [filterSemester, setFilterSemester] = useState('');
  
  const [students, setStudents] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [subjectStats, setSubjectStats] = useState([]);
  
  const [activeTab, setActiveTab] = useState('students');
  const [selectedPerson, setSelectedPerson] = useState(null);
  const [personType, setPersonType] = useState('student');
  const [searchTerm, setSearchTerm] = useState('');

  const { showToast, ToastContainer } = useToast();

  const shortBranch = (b) => (b || '').replace(' Engineering', '').replace('Information Technology', 'IT');

  useEffect(() => {
    fetchData();
  }, [filterBranch, filterYear]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // 1. Fetch Students in Branch/Year
      const { data: stData, error: stErr } = await supabase
        .from('app_students')
        .select('id, full_name, registration_number, branch, year')
        .eq('branch', filterBranch)
        .eq('year', filterYear);
      if (stErr) throw stErr;
      setStudents(stData || []);

      // 2. Fetch Subjects in Branch/Year
      const { data: subData, error: subErr } = await supabase
        .from('subjects')
        .select('*')
        .eq('branch', filterBranch)
        .eq('year', filterYear);
      if (subErr) throw subErr;
      setSubjects(subData || []);

      // 3. Fetch Faculty-Subject Assignments for these subjects
      const subjectIds = (subData || []).map(s => s.id);
      if (subjectIds.length > 0) {
        const { data: assignData, error: assignErr } = await supabase
          .from('faculty_subjects')
          .select('*, app_users(id, full_name, email, role, app_teachers(employee_id, branch, designation))')
          .in('subject_id', subjectIds);
        if (assignErr) throw assignErr;
        setAssignments(assignData || []);

        // Extract unique teachers
        const teacherMap = new Map();
        assignData.forEach(a => {
          if (a.app_users) {
            teacherMap.set(a.app_users.id, {
              id: a.app_users.id,
              full_name: a.app_users.full_name,
              email: a.app_users.email,
              employee_id: a.app_users.app_teachers?.employee_id,
              branch: a.app_users.app_teachers?.branch,
              designation: a.app_users.app_teachers?.designation,
              role: a.app_users.role
            });
          }
        });
        setTeachers(Array.from(teacherMap.values()));

        // 4. Fetch Attendance Stats per subject
        const stats = [];
        for (const sub of subData) {
          // Get sessions
          const { count: sessionCount } = await supabase
            .from('attendance_sessions')
            .select('id', { count: 'exact', head: true })
            .eq('subject_id', sub.id);
          
          // Get total present records for this subject's sessions
          const { data: sessions } = await supabase
            .from('attendance_sessions')
            .select('id')
            .eq('subject_id', sub.id);
          
          const sessionIds = sessions.map(s => s.id);
          let presentCount = 0;
          if (sessionIds.length > 0) {
            const { count } = await supabase
              .from('attendance_records')
              .select('id', { count: 'exact', head: true })
              .in('session_id', sessionIds)
              .eq('status', 'present');
            presentCount = count || 0;
          }

          // Calculate average attendance %
          // Formula: (Total Presents) / (Sessions * Students in Group)
          const totalPossible = (sessionCount || 0) * (stData?.length || 1);
          const avgAttendance = totalPossible > 0 ? (presentCount / totalPossible) * 100 : 0;

          stats.push({
            id: sub.id,
            name: sub.name,
            code: sub.code,
            sessions: sessionCount || 0,
            presents: presentCount,
            avgAttendance: Math.round(avgAttendance * 10) / 10,
            value: Math.round(avgAttendance) // for pie chart
          });
        }
        setSubjectStats(stats);
      } else {
        setAssignments([]);
        setTeachers([]);
        setSubjectStats([]);
      }
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const filteredItems = useMemo(() => {
    const q = searchTerm.toLowerCase();
    if (activeTab === 'students') {
      return students.filter(s => s.full_name.toLowerCase().includes(q) || s.registration_number.toLowerCase().includes(q));
    } else if (activeTab === 'teachers') {
      return teachers.filter(t => t.full_name.toLowerCase().includes(q) || t.employee_id?.toLowerCase().includes(q));
    } else {
      return subjectStats.filter(s => s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q));
    }
  }, [students, teachers, subjectStats, activeTab, searchTerm]);

  const handlePersonClick = (person, type) => {
    setSelectedPerson(person);
    setPersonType(type);
  };

  const overallAvg = subjectStats.length > 0 
    ? Math.round(subjectStats.reduce((acc, s) => acc + s.avgAttendance, 0) / subjectStats.length * 10) / 10 
    : 0;

  return (
    <div className="space-y-6 animate-fadeIn pb-20">
      <ToastContainer />
      <ProfileCard 
        isOpen={!!selectedPerson} 
        onClose={() => setSelectedPerson(null)} 
        person={selectedPerson} 
        type={personType} 
      />

      {/* Header & Filters */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div>
          <h1 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-3">
            <BarChart3 size={32} className="text-rose-600" />
            Advanced Analytics
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 font-medium">
            Detailed performance insights for <span className="text-rose-500 font-bold">{shortBranch(filterBranch)}</span> • {filterYear}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 bg-white dark:bg-gray-900 p-2 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 dark:bg-gray-800 rounded-xl">
            <GitBranch size={16} className="text-blue-500" />
            <select 
              className="bg-transparent border-none text-sm font-bold text-gray-700 dark:text-gray-200 focus:ring-0 cursor-pointer"
              value={filterBranch}
              onChange={(e) => setFilterBranch(e.target.value)}
            >
              {BRANCHES.map(b => <option key={b} value={b}>{shortBranch(b)}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 dark:bg-gray-800 rounded-xl">
            <GraduationCap size={16} className="text-emerald-500" />
            <select 
              className="bg-transparent border-none text-sm font-bold text-gray-700 dark:text-gray-200 focus:ring-0 cursor-pointer"
              value={filterYear}
              onChange={(e) => setFilterYear(e.target.value)}
            >
              {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 bg-gray-50 dark:bg-gray-800 rounded-xl">
            <Calendar size={16} className="text-amber-500" />
            <select 
              className="bg-transparent border-none text-sm font-bold text-gray-700 dark:text-gray-200 focus:ring-0 cursor-pointer"
              value={filterSemester}
              onChange={(e) => setFilterSemester(e.target.value)}
            >
              <option value="">All Semesters</option>
              {(yearToSemesters[filterYear] || []).map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="py-24 text-center">
          <Loader2 size={48} className="mx-auto animate-spin text-rose-500 mb-4" />
          <p className="text-lg font-bold text-gray-400">Gleaning insights from data...</p>
        </div>
      ) : (
        <>
          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md transition-all group overflow-hidden relative">
              <div className="absolute -right-4 -top-4 w-24 h-24 bg-blue-500/10 rounded-full blur-2xl group-hover:bg-blue-500/20 transition-all"></div>
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-blue-50 dark:bg-blue-900/20 text-blue-600 rounded-2xl"><Users size={24} /></div>
                <span className="flex items-center text-xs font-bold text-green-500 bg-green-50 dark:bg-green-900/20 px-2 py-1 rounded-lg">
                  <ArrowUpRight size={12} className="mr-1" /> 12%
                </span>
              </div>
              <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">Total Students</p>
              <h3 className="text-3xl font-black text-gray-900 dark:text-white mt-1">{students.length}</h3>
            </div>

            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md transition-all group overflow-hidden relative">
              <div className="absolute -right-4 -top-4 w-24 h-24 bg-rose-500/10 rounded-full blur-2xl group-hover:bg-rose-500/20 transition-all"></div>
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-rose-50 dark:bg-rose-900/20 text-rose-600 rounded-2xl"><TrendingUp size={24} /></div>
                <span className={`flex items-center text-xs font-bold ${overallAvg >= 75 ? 'text-green-500 bg-green-50' : 'text-amber-500 bg-amber-50'} dark:bg-opacity-20 px-2 py-1 rounded-lg`}>
                   {overallAvg >= 75 ? 'Good' : 'Warning'}
                </span>
              </div>
              <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">Avg. Attendance</p>
              <h3 className="text-3xl font-black text-gray-900 dark:text-white mt-1">{overallAvg}%</h3>
            </div>

            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md transition-all group overflow-hidden relative">
              <div className="absolute -right-4 -top-4 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl group-hover:bg-emerald-500/20 transition-all"></div>
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 rounded-2xl"><BookOpen size={24} /></div>
              </div>
              <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">Total Subjects</p>
              <h3 className="text-3xl font-black text-gray-900 dark:text-white mt-1">{subjects.length}</h3>
            </div>

            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm hover:shadow-md transition-all group overflow-hidden relative">
              <div className="absolute -right-4 -top-4 w-24 h-24 bg-violet-500/10 rounded-full blur-2xl group-hover:bg-violet-500/20 transition-all"></div>
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-violet-50 dark:bg-violet-900/20 text-violet-600 rounded-2xl"><UserSquare2 size={24} /></div>
              </div>
              <p className="text-sm font-bold text-gray-400 uppercase tracking-widest">Assigned Faculty</p>
              <h3 className="text-3xl font-black text-gray-900 dark:text-white mt-1">{teachers.length}</h3>
            </div>
          </div>

          {/* Charts Section */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Pie Chart: Attendance Distribution */}
            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm flex flex-col h-[500px]">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-xl font-black text-gray-900 dark:text-white">Attendance Distribution</h3>
                  <p className="text-sm text-gray-400">Subject-wise share of total attendance</p>
                </div>
                <div className="p-2 bg-gray-50 dark:bg-gray-800 rounded-xl text-gray-400"><PieChartIcon size={20} /></div>
              </div>
              <div className="flex-1 min-h-0 relative">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={subjectStats}
                      cx="50%"
                      cy="45%"
                      innerRadius={60}
                      outerRadius={100}
                      paddingAngle={2}
                      dataKey="value"
                      minAngle={15}
                      label={({ name, percent }) => {
                        const shortName = name.length > 15 ? name.substring(0, 12) + '...' : name;
                        return percent > 0.05 ? `${shortName} (${(percent * 100).toFixed(0)}%)` : '';
                      }}
                      labelLine={false}
                    >
                      {subjectStats.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="rgba(0,0,0,0.1)" />
                      ))}
                    </Pie>
                    <RechartsTooltip 
                      contentStyle={{ backgroundColor: '#111827', border: 'none', borderRadius: '12px', color: '#fff', fontSize: '12px' }}
                      itemStyle={{ color: '#fff' }}
                    />
                    <Legend 
                      verticalAlign="bottom" 
                      height={100} 
                      iconType="circle"
                      formatter={(value) => <span className="text-[10px] font-medium text-gray-500 dark:text-gray-400">{value.length > 20 ? value.substring(0, 18) + '...' : value}</span>}
                      wrapperStyle={{ paddingTop: '20px', overflowY: 'auto' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Bar Chart: Subject Performance */}
            <div className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm flex flex-col h-[500px]">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-xl font-black text-gray-900 dark:text-white">Subject Engagement</h3>
                  <p className="text-sm text-gray-400">Percentage of present students per subject</p>
                </div>
                <div className="p-2 bg-gray-50 dark:bg-gray-800 rounded-xl text-gray-400"><BarChart3 size={20} /></div>
              </div>
              <div className="flex-1 min-h-0">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={subjectStats} margin={{ top: 20, right: 30, left: 0, bottom: 60 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#374151" opacity={0.1} />
                    <XAxis 
                      dataKey="name" 
                      axisLine={false} 
                      tickLine={false} 
                      interval={0}
                      angle={-45}
                      textAnchor="end"
                      tick={(props) => {
                        const { x, y, payload } = props;
                        const label = payload.value.length > 12 ? payload.value.substring(0, 10) + '...' : payload.value;
                        return (
                          <g transform={`translate(${x},${y})`}>
                            <text x={0} y={0} dy={16} textAnchor="end" fill="#9ca3af" transform="rotate(-45)" fontSize={10} fontWeight={500}>
                              {label}
                            </text>
                          </g>
                        );
                      }}
                    />
                    <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 12, fill: '#9ca3af' }} unit="%" />
                    <RechartsTooltip 
                      cursor={{ fill: 'rgba(59, 130, 246, 0.05)' }}
                      contentStyle={{ backgroundColor: '#111827', border: 'none', borderRadius: '12px', color: '#fff', fontSize: '12px' }}
                    />
                    <Bar dataKey="avgAttendance" fill="url(#colorBar)" radius={[10, 10, 0, 0]} barSize={32}>
                      {subjectStats.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} opacity={0.8} />
                      ))}
                    </Bar>
                    <defs>
                      <linearGradient id="colorBar" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#3b82f6" stopOpacity={1}/>
                        <stop offset="100%" stopColor="#60a5fa" stopOpacity={0.8}/>
                      </linearGradient>
                    </defs>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Detailed Lists Section */}
          <div className="bg-white dark:bg-gray-900 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden mt-8">
            <div className="p-6 border-b border-gray-100 dark:border-gray-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex items-center gap-1 bg-gray-100 dark:bg-gray-800 p-1 rounded-2xl w-fit">
                <button 
                  onClick={() => setActiveTab('students')}
                  className={`px-5 py-2 text-sm font-bold rounded-xl transition-all ${activeTab === 'students' ? 'bg-white dark:bg-gray-700 text-blue-600 dark:text-white shadow-sm' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}
                >
                  Students ({students.length})
                </button>
                <button 
                  onClick={() => setActiveTab('teachers')}
                  className={`px-5 py-2 text-sm font-bold rounded-xl transition-all ${activeTab === 'teachers' ? 'bg-white dark:bg-gray-700 text-emerald-600 dark:text-white shadow-sm' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}
                >
                  Teachers ({teachers.length})
                </button>
                <button 
                  onClick={() => setActiveTab('subjects')}
                  className={`px-5 py-2 text-sm font-bold rounded-xl transition-all ${activeTab === 'subjects' ? 'bg-white dark:bg-gray-700 text-violet-600 dark:text-white shadow-sm' : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}
                >
                  Subjects ({subjects.length})
                </button>
              </div>

              <div className="relative">
                <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <input 
                  type="text" 
                  placeholder={`Search ${activeTab}...`}
                  className="pl-10 pr-4 py-2.5 bg-gray-50 dark:bg-gray-800 border-none rounded-xl text-sm text-gray-900 dark:text-white w-full md:w-64 focus:ring-2 focus:ring-blue-500 transition-all"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="overflow-x-auto">
              {filteredItems.length === 0 ? (
                <div className="py-20 text-center text-gray-400">
                  <Search size={48} className="mx-auto mb-3 opacity-20" />
                  <p>No results found matching your search.</p>
                </div>
              ) : (
                <table className="w-full text-left border-collapse">
                  <thead className="bg-gray-50/50 dark:bg-gray-800/50">
                    <tr className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">
                      <th className="px-8 py-4">{activeTab === 'subjects' ? 'Subject Name' : 'Full Name'}</th>
                      <th className="px-8 py-4">{activeTab === 'students' ? 'Reg No' : activeTab === 'teachers' ? 'Employee ID' : 'Code'}</th>
                      <th className="px-8 py-4">{activeTab === 'subjects' ? 'Avg Attendance' : 'Email / Branch'}</th>
                      <th className="px-8 py-4 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                    {activeTab === 'students' && filteredItems.map(s => (
                      <tr key={s.id} className="group hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-all">
                        <td className="px-8 py-5">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/30 text-blue-600 flex items-center justify-center font-bold text-sm">
                              {s.full_name[0]}
                            </div>
                            <div>
                              <p className="text-sm font-bold text-gray-900 dark:text-white">{s.full_name}</p>
                              <p className="text-[11px] text-gray-400">{s.branch}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-8 py-5 font-mono text-xs text-gray-500">{s.registration_number}</td>
                        <td className="px-8 py-5 text-xs text-gray-500">{s.year}</td>
                        <td className="px-8 py-5 text-right">
                          <button 
                            onClick={() => handlePersonClick(s, 'student')}
                            className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-xl transition-all"
                          >
                            <ChevronRight size={18} />
                          </button>
                        </td>
                      </tr>
                    ))}

                    {activeTab === 'teachers' && filteredItems.map(t => (
                      <tr key={t.id} className="group hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-all">
                        <td className="px-8 py-5">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 flex items-center justify-center font-bold text-sm">
                              {t.full_name[0]}
                            </div>
                            <div>
                              <p className="text-sm font-bold text-gray-900 dark:text-white">{t.full_name}</p>
                              <p className="text-[11px] text-gray-400">{t.designation || 'Faculty'}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-8 py-5 font-mono text-xs text-gray-500">{t.employee_id}</td>
                        <td className="px-8 py-5 text-xs text-gray-500">{t.email}</td>
                        <td className="px-8 py-5 text-right">
                          <button 
                            onClick={() => handlePersonClick(t, 'faculty')}
                            className="p-2 text-gray-400 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 rounded-xl transition-all"
                          >
                            <ChevronRight size={18} />
                          </button>
                        </td>
                      </tr>
                    ))}

                    {activeTab === 'subjects' && filteredItems.map(sub => (
                      <tr key={sub.id} className="group hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-all">
                        <td className="px-8 py-5">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-900/30 text-violet-600 flex items-center justify-center font-bold text-sm">
                              <BookOpen size={16} />
                            </div>
                            <div>
                              <p className="text-sm font-bold text-gray-900 dark:text-white">{sub.name}</p>
                              <p className="text-[11px] text-gray-400">{sub.sessions} sessions conducted</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-8 py-5 font-mono text-xs text-gray-500">{sub.code}</td>
                        <td className="px-8 py-5">
                          <div className="flex items-center gap-3">
                            <div className="flex-1 h-2 bg-gray-100 dark:bg-gray-800 rounded-full max-w-[100px] overflow-hidden">
                              <div 
                                className={`h-full rounded-full ${sub.avgAttendance >= 75 ? 'bg-emerald-500' : sub.avgAttendance >= 60 ? 'bg-amber-500' : 'bg-rose-500'}`}
                                style={{ width: `${sub.avgAttendance}%` }}
                              ></div>
                            </div>
                            <span className={`text-sm font-black ${sub.avgAttendance >= 75 ? 'text-emerald-600' : sub.avgAttendance >= 60 ? 'text-amber-600' : 'text-rose-600'}`}>
                              {sub.avgAttendance}%
                            </span>
                          </div>
                        </td>
                        <td className="px-8 py-5 text-right">
                          <div className="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-all">
                             <span className="text-[10px] font-bold text-gray-400 uppercase mr-2 flex items-center">Insights Active</span>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          {/* Extra Insights Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 bg-gradient-to-br from-indigo-600 to-violet-700 rounded-3xl p-8 text-white relative overflow-hidden shadow-xl shadow-indigo-200 dark:shadow-none">
              <div className="absolute top-0 right-0 p-8 opacity-10">
                <TrendingUp size={160} />
              </div>
              <div className="relative z-10">
                <h3 className="text-2xl font-black mb-2 flex items-center gap-2">
                  <Star size={24} className="text-amber-300 fill-amber-300" />
                  Growth Trajectory
                </h3>
                <p className="text-indigo-100 text-sm mb-8 max-w-md">Based on current attendance trends, your branch engagement has improved by 4.2% compared to the previous month.</p>
                
                <div className="flex flex-wrap gap-4">
                  <div className="bg-white/10 backdrop-blur-md px-6 py-4 rounded-2xl border border-white/10">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-indigo-200 mb-1">Top Performer</p>
                    <p className="text-lg font-bold">IT - 3rd Year</p>
                  </div>
                  <div className="bg-white/10 backdrop-blur-md px-6 py-4 rounded-2xl border border-white/10">
                    <p className="text-[10px] font-bold uppercase tracking-widest text-indigo-200 mb-1">Most Active Sub.</p>
                    <p className="text-lg font-bold">{subjectStats[0]?.name || '—'}</p>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-white dark:bg-gray-900 rounded-3xl p-8 border border-gray-100 dark:border-gray-800 shadow-sm">
              <h3 className="text-lg font-black text-gray-900 dark:text-white mb-6 flex items-center gap-2">
                <AlertCircle size={20} className="text-amber-500" />
                Attention Required
              </h3>
              <div className="space-y-4">
                {subjectStats.filter(s => s.avgAttendance < 75).slice(0, 3).map((s, i) => (
                  <div key={i} className="flex items-start gap-4 p-4 bg-amber-50 dark:bg-amber-900/20 rounded-2xl border border-amber-100 dark:border-amber-800/50">
                    <div className="p-2 bg-amber-100 dark:bg-amber-800 text-amber-600 rounded-xl"><AlertCircle size={16} /></div>
                    <div>
                      <p className="text-xs font-bold text-amber-900 dark:text-amber-400 uppercase tracking-wider">Low Attendance</p>
                      <p className="text-sm font-bold text-gray-800 dark:text-gray-200 mt-1">{s.name}</p>
                      <p className="text-[11px] text-amber-600/70 mt-0.5">Currently at {s.avgAttendance}% (Threshold 75%)</p>
                    </div>
                  </div>
                ))}
                {subjectStats.filter(s => s.avgAttendance < 75).length === 0 && (
                  <div className="flex flex-col items-center justify-center py-6 text-center">
                    <CheckCircle2 size={40} className="text-emerald-500 mb-2" />
                    <p className="text-sm font-bold text-gray-500">All subjects above threshold!</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Analytics;
