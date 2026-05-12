import React, { useState, useEffect, useMemo } from 'react';
import { Search, Plus, Edit3, Trash2, X, Shield, Loader2, Download, Building2, FileText } from 'lucide-react';
import { exportPDF, exportCSV } from '../lib/exportUtils';
import { hodApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';
import ProfileCard from '../components/ProfileCard';

const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering','Production Engineering','Textile Engineering'];

const branchColors = {
  'Information Technology': { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-700 dark:text-blue-400', grad: 'from-blue-500 to-cyan-500' },
  'Computer Engineering': { bg: 'bg-purple-100 dark:bg-purple-900/30', text: 'text-purple-700 dark:text-purple-400', grad: 'from-purple-500 to-indigo-500' },
  'Mechanical Engineering': { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-700 dark:text-orange-400', grad: 'from-orange-500 to-red-500' },
  'Civil Engineering': { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-700 dark:text-green-400', grad: 'from-green-500 to-emerald-500' },
  'Electronics Engineering': { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-700 dark:text-red-400', grad: 'from-red-500 to-pink-500' },
  'Electrical Engineering': { bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-700 dark:text-yellow-400', grad: 'from-yellow-500 to-amber-500' },
  'Production Engineering': { bg: 'bg-teal-100 dark:bg-teal-900/30', text: 'text-teal-700 dark:text-teal-400', grad: 'from-teal-500 to-cyan-500' },
  'Textile Engineering': { bg: 'bg-pink-100 dark:bg-pink-900/30', text: 'text-pink-700 dark:text-pink-400', grad: 'from-pink-500 to-rose-500' },
};
const getBC = (b) => branchColors[b] || { bg:'bg-gray-100 dark:bg-gray-800', text:'text-gray-600 dark:text-gray-400', grad:'from-gray-500 to-slate-500' };
const shortBranch = (b) => (b||'').replace(' Engineering','').replace('Information Technology','IT').replace('Computer','Comp');

const HODs = () => {
  const [hods, setHods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ employee_id:'', full_name:'', email:'', password:'hod123', branch:'', department:'', designation:'Professor & HOD' });
  const [profileTarget, setProfileTarget] = useState(null);
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => { setLoading(true); try { setHods(await hodApi.getAll()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetchData(); }, []);

  const openModal = (h=null) => {
    if(h){ setIsEditing(true); setForm({ id:h.id, employee_id:h.employee_id||h.username, full_name:h.full_name, email:h.email, branch:h.branch, department:h.department, designation:h.designation }); }
    else { setIsEditing(false); setForm({ employee_id:'', full_name:'', email:'', password:'hod123', branch:'', department:'', designation:'Professor & HOD' }); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    const payload = { ...form, department: form.department || form.branch };
    if(isEditing){ await hodApi.update(form.id, payload); showToast('HOD updated!'); }
    else { await hodApi.create(payload); showToast('HOD created!'); }
    setIsModalOpen(false); fetchData();
  } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await hodApi.delete(deleteTarget.id); showToast('HOD removed!'); setDeleteTarget(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  const expHeaders = ['Full Name','Employee ID','Email','Branch','Department','Designation'];
  const getExpRows = () => filtered.map(h => [h.full_name, h.employee_id, h.email, h.branch, h.department, h.designation]);

  const handleExportCSV = () => {
    exportCSV({ headers: expHeaders, rows: getExpRows(), filename: `hod_export_${new Date().toISOString().slice(0,10)}.csv` });
    showToast(`Exported ${filtered.length} HODs to CSV`, 'info');
  };

  const handleExportPDF = () => {
    exportPDF({ title: 'HOD Report', subtitle: `${filtered.length} Heads of Department • AttendX Admin Dashboard`, headers: expHeaders, rows: getExpRows(), filters: [{ label: 'Branch', value: filterBranch }], filename: `HOD_Report_${new Date().toISOString().slice(0,10)}.pdf` });
    showToast(`Exported ${filtered.length} HODs to PDF`, 'info');
  };

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => hods.filter(h => {
    const matchText = !q || h.full_name?.toLowerCase().includes(q) || h.employee_id?.toLowerCase().includes(q) || h.email?.toLowerCase().includes(q) || h.branch?.toLowerCase().includes(q) || h.department?.toLowerCase().includes(q);
    return matchText && (!filterBranch || h.branch === filterBranch);
  }), [hods, q, filterBranch]);

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Remove HOD" message={`Remove "${deleteTarget?.full_name}" as HOD?`} onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><Shield size={24} className="text-amber-600" /> HOD Management</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{hods.length} Head{hods.length !== 1 ? 's' : ''} of Department registered</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExportPDF} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><FileText size={16}/>PDF</button>
          <button onClick={handleExportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><Download size={16}/>CSV</button>
          <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add HOD</button>
        </div>
      </div>

      {/* Stats */}
      {!loading && hods.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800">
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Total HODs</p>
            <p className="text-2xl font-extrabold text-gray-900 dark:text-white mt-0.5">{hods.length}</p>
          </div>
          <div className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800">
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Departments</p>
            <p className="text-2xl font-extrabold text-gray-900 dark:text-white mt-0.5">{new Set(hods.map(h=>h.branch).filter(Boolean)).size}</p>
          </div>
          <div className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800 col-span-2">
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1.5">Branches Covered</p>
            <div className="flex flex-wrap gap-1.5">
              {[...new Set(hods.map(h=>h.branch).filter(Boolean))].map(b => {
                const bc = getBC(b);
                return <span key={b} className={`px-2 py-0.5 rounded-md text-[10px] font-semibold ${bc.bg} ${bc.text}`}>{shortBranch(b)}</span>;
              })}
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
          <input type="text" placeholder="Search name, employee ID, email, branch..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
            value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
        </div>
        <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white" value={filterBranch} onChange={e=>setFilterBranch(e.target.value)}>
          <option value="">All Branches</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}
        </select>
        {(searchTerm||filterBranch) && <button onClick={()=>{setSearchTerm('');setFilterBranch('');}} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear</button>}
        <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} results</p>
      </div>

      {/* HOD Cards */}
      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-amber-500 mb-3"/><p className="text-sm text-gray-400">Loading HODs...</p></div>
      : filtered.length === 0 ? (
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-12 text-center text-gray-400">
          <Shield size={48} className="mx-auto mb-3 opacity-30"/>
          <p className="font-medium">No HODs found</p>
          <p className="text-xs mt-1">Add a Head of Department to get started</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(h => {
            const bc = getBC(h.branch);
            return (
            <div key={h.id} className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden stat-card group">
              {/* Color header bar */}
              <div className={`h-1.5 bg-gradient-to-r ${bc.grad}`}></div>
              <div className="p-5">
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-center gap-3">
                    <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${bc.grad} text-white flex items-center justify-center font-bold text-sm uppercase shadow-lg cursor-pointer`} onClick={()=>setProfileTarget(h)}>
                      {h.full_name?.split(' ').map(n=>n[0]).join('').slice(0,2)}
                    </div>
                    <div className="cursor-pointer" onClick={()=>setProfileTarget(h)}>
                      <h3 className="font-bold text-gray-900 dark:text-white text-sm hover:text-amber-600 dark:hover:text-amber-400 transition-colors">{h.full_name}</h3>
                      <p className="text-[11px] text-gray-400">{h.designation}</p>
                    </div>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button onClick={()=>openModal(h)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={14}/></button>
                    <button onClick={()=>setDeleteTarget(h)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={14}/></button>
                  </div>
                </div>
                <div className="space-y-2 text-xs text-gray-500 dark:text-gray-400">
                  <div className="flex items-center gap-2"><span className="text-gray-400">📧</span> {h.email}</div>
                  <div className="flex items-center gap-2"><span className="text-gray-400">🆔</span> <span className="font-mono">{h.employee_id}</span></div>
                  <div className="flex items-center gap-2 mt-2">
                    <Building2 size={12} className="text-gray-400"/>
                    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-semibold ${bc.bg} ${bc.text}`}>{h.branch || 'Unassigned'}</span>
                    {h.department && h.department !== h.branch && <span className="px-2 py-0.5 bg-gray-100 dark:bg-gray-800 rounded text-[10px] font-medium text-gray-500">{h.department}</span>}
                  </div>
                </div>
              </div>
            </div>
            );
          })}
        </div>
      )}

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit HOD':'Add New HOD'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Full Name</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="e.g. Dr. V. B. Nikam" value={form.full_name} onChange={e=>setForm({...form,full_name:e.target.value})}/></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Employee ID</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" placeholder="EMP_HOD_IT" value={form.employee_id} onChange={e=>setForm({...form,employee_id:e.target.value})}/></div>
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Email</label><input type="email" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="hod@academic.edu" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>
              </div>
              {!isEditing && <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Password</label><input type="password" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>}
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Designation</label><input type="text" className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="Professor & HOD" value={form.designation} onChange={e=>setForm({...form,designation:e.target.value})}/></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Branch</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.branch} onChange={e=>setForm({...form,branch:e.target.value, department: form.department || e.target.value})}><option value="">Select Branch</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}</select></div>
                <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Department ID</label><input type="text" className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="e.g. IT" value={form.department} onChange={e=>setForm({...form,department:e.target.value})}/></div>
              </div>
              <div className="flex gap-3 mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
                <button type="button" onClick={()=>setIsModalOpen(false)} className="flex-1 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm">Cancel</button>
                <button type="submit" disabled={saving} className="flex-1 btn-primary px-4 py-2.5 text-white rounded-xl font-medium text-sm flex items-center justify-center gap-2">{saving&&<Loader2 size={16} className="animate-spin"/>}{isEditing?'Save':'Create HOD'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Profile Detail Card */}
      <ProfileCard isOpen={!!profileTarget} onClose={() => setProfileTarget(null)} person={profileTarget} type="hod" />
    </div>
  );
};

export default HODs;
