import React, { useState, useEffect, useRef } from 'react';
import {
  Users, UserSquare2, BookOpen, Building2, ClipboardList,
  ArrowUpRight, Activity, Clock, GraduationCap, BarChart3,
  RefreshCcw, TrendingUp
} from 'lucide-react';
import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend, AreaChart, Area
} from 'recharts';
import { analyticsApi } from '../api';

const COLORS = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#64748b'];
const PIE_COLORS = ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899'];

// Animated counter hook
const useAnimatedCounter = (target, duration = 1200) => {
  const [count, setCount] = useState(0);
  const prevTarget = useRef(0);
  useEffect(() => {
    if (target === 0) return;
    const start = prevTarget.current;
    prevTarget.current = target;
    const startTime = Date.now();
    const tick = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
      setCount(Math.round(start + (target - start) * eased));
      if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }, [target, duration]);
  return count;
};

const StatCard = ({ title, value, icon: Icon, gradient, sub, loading }) => {
  const animatedValue = useAnimatedCounter(loading ? 0 : value);
  return (
    <div className="bg-white dark:bg-gray-900 p-5 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm stat-card">
      <div className="flex items-center justify-between mb-4">
        <div className={`p-2.5 rounded-xl bg-gradient-to-br ${gradient}`}>
          <Icon className="text-white" size={20} />
        </div>
        <ArrowUpRight size={16} className="text-gray-300" />
      </div>
      <p className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider">{title}</p>
      {loading ? (
        <div className="skeleton h-8 w-16 mt-1.5"></div>
      ) : (
        <>
          <p className="text-2xl font-extrabold text-gray-900 dark:text-white mt-1">{animatedValue.toLocaleString()}</p>
          {sub && <p className="text-[11px] text-gray-400 mt-0.5">{sub}</p>}
        </>
      )}
    </div>
  );
};

const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-white dark:bg-gray-800 px-4 py-2.5 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 text-sm">
        <p className="font-semibold text-gray-900 dark:text-white">{label || payload[0].name}</p>
        <p className="text-gray-500 dark:text-gray-400 mt-0.5">{payload[0].value} {payload[0].value === 1 ? 'entry' : 'entries'}</p>
      </div>
    );
  }
  return null;
};

const renderCustomPieLabel = ({ name, percent }) => {
  if (percent < 0.05) return null;
  return `${name} ${(percent * 100).toFixed(0)}%`;
};

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [studentsByBranch, setStudentsByBranch] = useState([]);
  const [studentsByYear, setStudentsByYear] = useState([]);
  const [facultyByBranch, setFacultyByBranch] = useState([]);
  const [subjectsByBranch, setSubjectsByBranch] = useState([]);
  const [sessionStatus, setSessionStatus] = useState([]);
  const [recentSessions, setRecentSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchAll = async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true); else setLoading(true);
    try {
      const [s, sb, sy, fb, sub, ss, rs] = await Promise.all([
        analyticsApi.getDashboardStats(),
        analyticsApi.getStudentsByBranch(),
        analyticsApi.getStudentsByYear(),
        analyticsApi.getFacultyByBranch(),
        analyticsApi.getSubjectsByBranch(),
        analyticsApi.getSessionStatusBreakdown(),
        analyticsApi.getRecentSessions(6),
      ]);
      setStats(s); setStudentsByBranch(sb); setStudentsByYear(sy);
      setFacultyByBranch(fb); setSubjectsByBranch(sub);
      setSessionStatus(ss); setRecentSessions(rs);
      setLastUpdated(new Date());
    } catch (err) { console.error('Dashboard error:', err); }
    finally { setLoading(false); setRefreshing(false); }
  };

  useEffect(() => { fetchAll(); }, []);

  // Auto-refresh every 60 seconds
  useEffect(() => {
    const interval = setInterval(() => fetchAll(true), 60000);
    return () => clearInterval(interval);
  }, []);

  const formatTime = (iso) => {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diff = now - d;
    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`;
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  };

  const statCards = [
    { title: 'Total Students', value: stats?.totalStudents || 0, icon: Users, gradient: 'from-blue-500 to-blue-600', sub: 'Registered in system' },
    { title: 'Total Faculty', value: stats?.totalFaculty || 0, icon: UserSquare2, gradient: 'from-emerald-500 to-green-600', sub: 'Active professors' },
    { title: 'Subjects', value: stats?.totalSubjects || 0, icon: BookOpen, gradient: 'from-violet-500 to-purple-600', sub: 'Academic courses' },
    { title: 'Classrooms', value: stats?.totalClassrooms || 0, icon: Building2, gradient: 'from-amber-500 to-orange-500', sub: 'Physical rooms' },
    { title: 'Sessions', value: stats?.totalSessions || 0, icon: ClipboardList, gradient: 'from-rose-500 to-red-600', sub: 'Attendance sessions' },
    { title: 'Records', value: stats?.totalRecords || 0, icon: GraduationCap, gradient: 'from-cyan-500 to-teal-600', sub: 'Attendance marks' },
  ];

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
            <BarChart3 size={24} className="text-blue-600" />
            College Dashboard
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">
            Real-time analytics from Supabase
            {lastUpdated && <span className="ml-2 text-gray-400">• Updated {formatTime(lastUpdated)}</span>}
          </p>
        </div>
        <button
          onClick={() => fetchAll(true)}
          disabled={refreshing}
          className="flex items-center gap-2 px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all disabled:opacity-50"
        >
          <RefreshCcw size={16} className={refreshing ? 'animate-spin' : ''} />
          {refreshing ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        {statCards.map((s, i) => <StatCard key={i} {...s} loading={loading} />)}
      </div>

      {/* Row 1: Students by Branch (Pie) + Students by Year (Pie) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Students by Branch - Pie Chart */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <h3 className="text-sm font-bold text-gray-900 mb-1 flex items-center gap-2">
            <Users size={16} className="text-blue-600" />
            Students by Branch
          </h3>
          <p className="text-[11px] text-gray-400 mb-4">Branch-wise student distribution</p>
          {loading ? (
            <div className="skeleton h-64 w-full"></div>
          ) : studentsByBranch.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-16">No student data</p>
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={studentsByBranch}
                    cx="50%" cy="50%"
                    outerRadius={100}
                    innerRadius={50}
                    paddingAngle={3}
                    dataKey="value"
                    label={renderCustomPieLabel}
                    labelLine={true}
                  >
                    {studentsByBranch.map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                  <Legend
                    layout="horizontal"
                    verticalAlign="bottom"
                    wrapperStyle={{ fontSize: '11px', paddingTop: '8px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* Students by Year - Pie Chart */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <h3 className="text-sm font-bold text-gray-900 mb-1 flex items-center gap-2">
            <GraduationCap size={16} className="text-violet-600" />
            Students by Year
          </h3>
          <p className="text-[11px] text-gray-400 mb-4">Year-wise student distribution</p>
          {loading ? (
            <div className="skeleton h-64 w-full"></div>
          ) : studentsByYear.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-16">No data</p>
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={studentsByYear}
                    cx="50%" cy="50%"
                    outerRadius={100}
                    innerRadius={50}
                    paddingAngle={3}
                    dataKey="value"
                    label={renderCustomPieLabel}
                    labelLine={true}
                  >
                    {studentsByYear.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                  <Legend
                    layout="horizontal"
                    verticalAlign="bottom"
                    wrapperStyle={{ fontSize: '11px', paddingTop: '8px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Row 2: Faculty by Branch (Bar) + Subjects by Branch (Bar) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Faculty by Branch - Bar Chart */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <h3 className="text-sm font-bold text-gray-900 mb-1 flex items-center gap-2">
            <UserSquare2 size={16} className="text-emerald-600" />
            Faculty by Branch
          </h3>
          <p className="text-[11px] text-gray-400 mb-4">Department-wise faculty strength</p>
          {loading ? (
            <div className="skeleton h-64 w-full"></div>
          ) : facultyByBranch.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-16">No faculty data</p>
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={facultyByBranch} barSize={36}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {facultyByBranch.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* Subjects by Branch - Bar Chart */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <h3 className="text-sm font-bold text-gray-900 mb-1 flex items-center gap-2">
            <BookOpen size={16} className="text-purple-600" />
            Subjects by Branch
          </h3>
          <p className="text-[11px] text-gray-400 mb-4">Course distribution across departments</p>
          {loading ? (
            <div className="skeleton h-64 w-full"></div>
          ) : subjectsByBranch.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-16">No data</p>
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={subjectsByBranch} barSize={36}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#94a3b8' }} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {subjectsByBranch.map((_, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Row 3: Session Status (Pie) + Recent Sessions (List) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Session Status */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <h3 className="text-sm font-bold text-gray-900 mb-1 flex items-center gap-2">
            <ClipboardList size={16} className="text-rose-600" />
            Session Status
          </h3>
          <p className="text-[11px] text-gray-400 mb-4">Active vs completed sessions</p>
          {loading ? (
            <div className="skeleton h-52 w-full"></div>
          ) : sessionStatus.length === 0 ? (
            <p className="text-gray-400 text-sm text-center py-12">No sessions</p>
          ) : (
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={sessionStatus}
                    cx="50%" cy="50%"
                    outerRadius={80}
                    innerRadius={45}
                    paddingAngle={4}
                    dataKey="value"
                    label={renderCustomPieLabel}
                  >
                    {sessionStatus.map((entry, i) => (
                      <Cell key={i} fill={entry.name === 'Active' ? '#10b981' : entry.name === 'Stopped' ? '#94a3b8' : COLORS[i]} />
                    ))}
                  </Pie>
                  <Tooltip content={<CustomTooltip />} />
                  <Legend wrapperStyle={{ fontSize: '11px' }} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* Recent Sessions */}
        <div className="lg:col-span-2 bg-white dark:bg-gray-900 p-6 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-2">
                <Activity size={16} className="text-blue-600" />
                Recent Attendance Sessions
              </h3>
              <p className="text-[11px] text-gray-400 mt-0.5">Live from Supabase</p>
            </div>
          </div>

          {loading ? (
            <div className="space-y-3">
              {[1,2,3,4].map(i => <div key={i} className="skeleton h-14 w-full"></div>)}
            </div>
          ) : recentSessions.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <ClipboardList size={40} className="mx-auto mb-3 opacity-40" />
              <p className="text-sm">No sessions found</p>
            </div>
          ) : (
            <div className="space-y-1.5">
              {recentSessions.map((s) => (
                <div key={s.id} className="flex items-center gap-4 p-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                  <div className={`w-9 h-9 rounded-lg flex items-center justify-center flex-shrink-0 ${
                    s.status === 'active' ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-500'
                  }`}>
                    <ClipboardList size={16} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-white truncate">{s.subjects?.name || 'Unknown'}</p>
                    <p className="text-[11px] text-gray-400">{s.app_users?.full_name || 'N/A'}</p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <span className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                      s.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>{s.status}</span>
                    <p className="text-[10px] text-gray-400 mt-0.5">{formatTime(s.start_time)}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
