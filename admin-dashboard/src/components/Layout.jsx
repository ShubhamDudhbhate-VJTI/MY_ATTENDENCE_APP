import React, { useState, useEffect, useCallback } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, UserSquare2, BookOpen, Building2,
  CalendarClock, ClipboardList, LogOut, Bell, Search, Menu,
  X, ChevronRight, Moon, Sun, Database, Wifi, WifiOff,
  Command, Shield, BarChart3, Link2
} from 'lucide-react';
import { supabase } from '../lib/supabase';
import NotificationsModal from './NotificationsModal';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard', color: 'text-blue-600' },
  { to: '/students', icon: Users, label: 'Students', color: 'text-blue-600' },
  { to: '/faculty', icon: UserSquare2, label: 'Faculty', color: 'text-emerald-600' },
  { to: '/hods', icon: Shield, label: 'HODs', color: 'text-amber-600' },
  { to: '/subjects', icon: BookOpen, label: 'Subjects', color: 'text-violet-600' },
  { to: '/classrooms', icon: Building2, label: 'Classrooms', color: 'text-amber-600' },
  { to: '/attendance', icon: ClipboardList, label: 'Attendance', color: 'text-rose-600' },
  { to: '/schedules', icon: CalendarClock, label: 'Schedules', color: 'text-indigo-600' },
  { to: '/assignments', icon: Link2, label: 'Assignments', color: 'text-indigo-600' },
  { to: '/analytics', icon: BarChart3, label: 'Analytics', color: 'text-rose-600' },
  { to: '/reports', icon: ClipboardList, label: 'Reports', color: 'text-violet-600' },
];

const SidebarLink = ({ to, icon: Icon, label, collapsed, color }) => {
  const location = useLocation();
  const isActive = location.pathname === to;
  return (
    <Link to={to} title={label}
      className={`group flex items-center gap-3 px-3 py-2.5 text-sm font-medium transition-all duration-200 rounded-xl relative ${
        isActive ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-md shadow-blue-200 dark:shadow-blue-900/40'
        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-white'
      }`}>
      <Icon size={20} className={isActive ? 'text-white' : `text-gray-400 group-hover:${color}`} />
      {!collapsed && <span>{label}</span>}
      {isActive && !collapsed && <ChevronRight size={16} className="ml-auto text-white/60" />}
    </Link>
  );
};

const Layout = ({ onLogout }) => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('attendx_dark') === 'true');
  const [dbStatus, setDbStatus] = useState('checking');
  const [globalSearch, setGlobalSearch] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isNotificationModalOpen, setIsNotificationModalOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  // Dark mode toggle
  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('attendx_dark', String(darkMode));
  }, [darkMode]);

  // DB health check
  useEffect(() => {
    const check = async () => {
      try {
        const { error } = await supabase.from('app_users').select('id', { count: 'exact', head: true });
        setDbStatus(error ? 'error' : 'connected');
      } catch { setDbStatus('error'); }
    };
    check();
    const interval = setInterval(check, 30000);
    return () => clearInterval(interval);
  }, []);

  // Ctrl+K shortcut
  useEffect(() => {
    const handler = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setGlobalSearch(true);
      }
      if (e.key === 'Escape') setGlobalSearch(false);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // Global search navigation
  const searchItems = [
    { label: 'Dashboard', path: '/', icon: LayoutDashboard },
    { label: 'Students', path: '/students', icon: Users },
    { label: 'Faculty', path: '/faculty', icon: UserSquare2 },
    { label: 'HODs', path: '/hods', icon: Shield },
    { label: 'Subjects', path: '/subjects', icon: BookOpen },
    { label: 'Classrooms', path: '/classrooms', icon: Building2 },
    { label: 'Attendance', path: '/attendance', icon: ClipboardList },
    { label: 'Schedules', path: '/schedules', icon: CalendarClock },
    { label: 'Assignments', path: '/assignments', icon: Link2 },
    { label: 'Analytics', path: '/analytics', icon: BarChart3 },
    { label: 'Reports', path: '/reports', icon: ClipboardList },
    { label: 'Add Student', path: '/students', icon: Users },
    { label: 'Add Faculty', path: '/faculty', icon: UserSquare2 },
    { label: 'Add Subject', path: '/subjects', icon: BookOpen },
  ];

  const filteredSearchItems = searchItems.filter(i => i.label.toLowerCase().includes(searchQuery.toLowerCase()));

  const pageName = navItems.find(n => n.to === location.pathname)?.label || 'Dashboard';

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-950">
      {/* Global Search Overlay (Ctrl+K) */}
      {globalSearch && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100] flex items-start justify-center pt-[20vh] p-4 animate-fadeIn" onClick={() => setGlobalSearch(false)}>
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden animate-scaleIn" onClick={e => e.stopPropagation()}>
            <div className="flex items-center gap-3 px-4 border-b border-gray-100 dark:border-gray-800">
              <Search size={18} className="text-gray-400" />
              <input autoFocus type="text" placeholder="Search pages, actions..." className="w-full py-4 bg-transparent text-sm text-gray-900 dark:text-white placeholder-gray-400 outline-none" value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
              <kbd className="px-2 py-0.5 bg-gray-100 dark:bg-gray-800 rounded text-[10px] font-mono text-gray-500">ESC</kbd>
            </div>
            <div className="max-h-64 overflow-y-auto p-2">
              {filteredSearchItems.map((item, i) => (
                <button key={i} onClick={() => { navigate(item.path); setGlobalSearch(false); setSearchQuery(''); }}
                  className="flex items-center gap-3 w-full px-3 py-2.5 text-sm text-gray-700 dark:text-gray-300 hover:bg-blue-50 dark:hover:bg-gray-800 rounded-xl transition-colors text-left">
                  <item.icon size={16} className="text-gray-400" />
                  {item.label}
                </button>
              ))}
              {filteredSearchItems.length === 0 && <p className="text-sm text-gray-400 text-center py-6">No results</p>}
            </div>
          </div>
        </div>
      )}

      {/* Mobile overlay */}
      {mobileOpen && <div className="fixed inset-0 bg-black/40 z-40 lg:hidden" onClick={() => setMobileOpen(false)} />}

      {/* Sidebar */}
      <aside className={`fixed lg:static inset-y-0 left-0 z-50 ${sidebarOpen ? 'w-64' : 'w-[72px]'} ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        bg-white dark:bg-gray-900 border-r border-gray-200/80 dark:border-gray-800 flex flex-col transition-all duration-300 ease-in-out shadow-xl lg:shadow-none`}>
        {/* Logo */}
        <div className={`p-5 ${sidebarOpen ? '' : 'px-3'}`}>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-200 dark:shadow-blue-900/40 flex-shrink-0">
              <span className="text-white text-lg font-black">A</span>
            </div>
            {sidebarOpen && (
              <div>
                <h1 className="text-lg font-extrabold gradient-text tracking-tight">AttendX</h1>
                <p className="text-[10px] text-gray-400 font-medium uppercase tracking-widest">Admin Panel</p>
              </div>
            )}
          </div>
        </div>

        {/* Nav Links */}
        <nav className={`flex-1 ${sidebarOpen ? 'px-3' : 'px-2'} space-y-1 mt-2 overflow-y-auto`}>
          <p className={`text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-3 ${sidebarOpen ? 'px-3' : 'text-center'}`}>
            {sidebarOpen ? 'Navigation' : '•••'}
          </p>
          {navItems.map(item => <SidebarLink key={item.to} {...item} collapsed={!sidebarOpen} />)}
        </nav>

        {/* DB Status + Actions */}
        <div className="p-3 border-t border-gray-100 dark:border-gray-800 space-y-1">
          {/* DB Health */}
          {sidebarOpen && (
            <div className="flex items-center gap-2 px-3 py-2 text-xs font-medium">
              {dbStatus === 'connected' ? (
                <><div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div><span className="text-green-600 dark:text-green-400">Supabase Connected</span></>
              ) : dbStatus === 'checking' ? (
                <><div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse"></div><span className="text-amber-600">Checking...</span></>
              ) : (
                <><div className="w-2 h-2 bg-red-500 rounded-full"></div><span className="text-red-600">DB Error</span></>
              )}
            </div>
          )}
          <button onClick={() => setDarkMode(!darkMode)}
            className="flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl w-full transition-colors">
            {darkMode ? <Sun size={20} /> : <Moon size={20} />}
            {sidebarOpen && <span>{darkMode ? 'Light Mode' : 'Dark Mode'}</span>}
          </button>
          <button onClick={() => setSidebarOpen(!sidebarOpen)}
            className="hidden lg:flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl w-full transition-colors">
            <Menu size={20} />{sidebarOpen && <span>Collapse</span>}
          </button>
          <button onClick={onLogout}
            className="flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl w-full transition-colors">
            <LogOut size={20} />{sidebarOpen && <span>Logout</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden min-w-0">
        {/* Topbar */}
        <header className="h-16 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-200/60 dark:border-gray-800 flex items-center justify-between px-6 sticky top-0 z-30">
          <div className="flex items-center gap-4 flex-1">
            <button onClick={() => setMobileOpen(true)} className="lg:hidden p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg"><Menu size={20} /></button>
            <h2 className="text-lg font-bold text-gray-900 dark:text-white hidden sm:block">{pageName}</h2>
            <button onClick={() => setGlobalSearch(true)}
              className="flex items-center gap-2 px-4 py-2 bg-gray-100/80 dark:bg-gray-800 border border-transparent hover:border-blue-300 dark:hover:border-blue-700 rounded-xl text-sm text-gray-400 transition-all ml-4 cursor-pointer">
              <Search size={14} /> <span className="hidden md:inline">Search...</span>
              <kbd className="ml-2 px-1.5 py-0.5 bg-white dark:bg-gray-700 rounded text-[10px] font-mono text-gray-500 border border-gray-200 dark:border-gray-600 hidden md:inline">⌘K</kbd>
            </button>
          </div>
          <div className="flex items-center gap-3">
            <div className={`hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[10px] font-bold uppercase tracking-wider ${
              dbStatus === 'connected' ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-400' : 'bg-red-50 dark:bg-red-900/20 text-red-700'
            }`}>
              <Database size={12} />
              {dbStatus === 'connected' ? 'Live' : 'Offline'}
            </div>
            
            <button 
              onClick={() => setIsNotificationModalOpen(true)}
              className="relative p-2 text-gray-500 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-colors ml-2"
              title="Notifications Management"
            >
              <Bell size={20} />
              <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-red-500 border-2 border-white dark:border-gray-900 rounded-full animate-pulse"></span>
            </button>

            <div className="h-8 w-px bg-gray-200 dark:bg-gray-700 mx-1"></div>
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-semibold text-gray-900 dark:text-white leading-none">Admin</p>
                <p className="text-[11px] text-gray-400 mt-0.5">Super Admin</p>
              </div>
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-white flex items-center justify-center font-bold text-sm shadow-md shadow-blue-200 dark:shadow-blue-900/40">SD</div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-6 lg:p-8 bg-gray-50/80 dark:bg-gray-950">
          <Outlet />
        </main>
      </div>

      <NotificationsModal 
        isOpen={isNotificationModalOpen} 
        onClose={() => setIsNotificationModalOpen(false)} 
      />
    </div>
  );
};

export default Layout;
