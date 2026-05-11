import React, { useState, useEffect, useMemo } from 'react';
import { ClipboardList, Loader2, Trash2, ChevronDown, ChevronRight, Users, Clock, Search } from 'lucide-react';
import { attendanceApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const Attendance = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState(null);
  const [records, setRecords] = useState([]);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => { setLoading(true); try { setSessions(await attendanceApi.getSessions()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetchData(); }, []);

  const toggleExpand = async (id) => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id);
    setLoadingRecords(true);
    try { setRecords(await attendanceApi.getSessionRecords(id)); } catch(e) { showToast(e.message,'error'); }
    finally { setLoadingRecords(false); }
  };

  const handleDelete = async () => { try { await attendanceApi.deleteSession(deleteTarget.id); showToast('Session deleted!'); setDeleteTarget(null); setExpandedId(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  const formatDate = (iso) => {
    if(!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'}) + ' ' + d.toLocaleTimeString('en-IN',{hour:'2-digit',minute:'2-digit'});
  };

  const q = searchTerm.toLowerCase();
  const statuses = [...new Set(sessions.map(s => s.status).filter(Boolean))];
  const filtered = useMemo(() => sessions.filter(s => {
    const matchText = !q || s.subjects?.name?.toLowerCase().includes(q) || s.id?.toLowerCase().includes(q) || s.subjects?.code?.toLowerCase().includes(q);
    return matchText && (!filterStatus || s.status === filterStatus);
  }), [sessions, q, filterStatus]);

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Delete Session" message="Delete this session and all records?" onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      <div>
        <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><ClipboardList size={24} className="text-rose-600" /> Attendance Sessions</h1>
        <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{sessions.length} sessions recorded</p>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
          <input type="text" placeholder="Search subject, code..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
            value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
        </div>
        <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterStatus} onChange={e=>setFilterStatus(e.target.value)}>
          <option value="">All Status</option>
          {statuses.map(s=><option key={s} value={s}>{s.charAt(0).toUpperCase()+s.slice(1)}</option>)}
        </select>
        {(searchTerm||filterStatus) && <button onClick={()=>{setSearchTerm('');setFilterStatus('');}} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear</button>}
        <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} sessions</p>
      </div>

      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading sessions...</p></div>
      : filtered.length===0 ? <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-12 text-center text-gray-400"><ClipboardList size={48} className="mx-auto mb-3 opacity-30"/><p>No sessions found.</p></div>
      : (
        <div className="space-y-3">
          {filtered.map(s => (
            <div key={s.id} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
              <div className="flex items-center gap-4 p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors" onClick={()=>toggleExpand(s.id)}>
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${s.status==='active'?'bg-green-100 dark:bg-green-900/30 text-green-600':'bg-gray-100 dark:bg-gray-800 text-gray-500'}`}><ClipboardList size={18}/></div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-gray-900 dark:text-white">{s.subjects?.name||'Unknown Subject'}</p>
                  <p className="text-xs text-gray-400 mt-0.5 flex items-center gap-2"><Clock size={12}/>{formatDate(s.start_time)}</p>
                </div>
                {s.subjects?.branch && <span className="hidden sm:inline px-2.5 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 rounded-lg text-[10px] font-medium">{s.subjects.branch}</span>}
                <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${s.status==='active'?'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400':'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'}`}>{s.status}</span>
                <button onClick={(e)=>{e.stopPropagation();setDeleteTarget(s);}} className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={16}/></button>
                {expandedId===s.id ? <ChevronDown size={18} className="text-gray-400"/> : <ChevronRight size={18} className="text-gray-400"/>}
              </div>

              {expandedId===s.id && (
                <div className="border-t border-gray-100 dark:border-gray-800 p-4 bg-gray-50/50 dark:bg-gray-800/30 animate-fadeIn">
                  <div className="flex items-center gap-2 mb-3"><Users size={16} className="text-blue-600"/><span className="text-sm font-semibold text-gray-700 dark:text-gray-300">Attendance Records ({records.length})</span></div>
                  {loadingRecords ? <div className="py-6 text-center"><Loader2 size={20} className="mx-auto animate-spin text-blue-500"/></div>
                  : records.length===0 ? <p className="text-sm text-gray-400 py-4 text-center">No records for this session.</p>
                  : (
                    <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                      <table className="w-full text-left text-sm">
                        <thead className="bg-gray-50 dark:bg-gray-800"><tr className="text-gray-500 dark:text-gray-400 uppercase text-[10px] font-bold tracking-wider">
                          <th className="px-4 py-2.5">Student</th><th className="px-4 py-2.5">Reg No</th><th className="px-4 py-2.5">Status</th><th className="px-4 py-2.5">Marked At</th><th className="px-4 py-2.5">Face ✓</th>
                        </tr></thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                          {records.map(r => (
                            <tr key={r.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                              <td className="px-4 py-2.5 font-medium text-gray-900 dark:text-white">{r.app_students?.full_name||'Unknown'}</td>
                              <td className="px-4 py-2.5 text-gray-600 dark:text-gray-400 font-mono text-xs">{r.app_students?.registration_number||'—'}</td>
                              <td className="px-4 py-2.5"><span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${r.status==='present'?'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400':'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400'}`}>{r.status||'present'}</span></td>
                              <td className="px-4 py-2.5 text-gray-500 dark:text-gray-400 text-xs">{formatDate(r.marked_at)}</td>
                              <td className="px-4 py-2.5">{r.face_verified ? <span className="text-green-600 dark:text-green-400 text-xs font-bold">✓ Yes</span> : <span className="text-gray-400 text-xs">No</span>}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Attendance;
