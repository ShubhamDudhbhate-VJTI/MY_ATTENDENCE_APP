import React, { useState, useEffect, useMemo } from 'react';
import { Search, Plus, Edit3, Trash2, X, UserSquare2, Loader2, Filter, Download, CheckSquare, Square } from 'lucide-react';
import { facultyApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering','Production Engineering','Textile Engineering'];

const Faculty = () => {
  const [faculty, setFaculty] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [bulkMode, setBulkMode] = useState(false);
  const [form, setForm] = useState({ employee_id:'', full_name:'', email:'', password:'password123', branch:'', designation:'' });
  const { showToast, ToastContainer } = useToast();

  const fetch = async () => { setLoading(true); try { setFaculty(await facultyApi.getAll()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetch(); }, []);

  const openModal = (f=null) => {
    if(f){ setIsEditing(true); setForm({id:f.id, employee_id:f.employee_id||f.username, full_name:f.full_name, email:f.email, branch:f.branch, designation:f.designation}); }
    else { setIsEditing(false); setForm({employee_id:'',full_name:'',email:'',password:'password123',branch:'',designation:''}); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    if(isEditing){ await facultyApi.update(form.id, form); showToast('Faculty updated!'); }
    else { await facultyApi.create(form); showToast('Faculty created!'); }
    setIsModalOpen(false); fetch();
  } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await facultyApi.delete(deleteTarget.id); showToast('Faculty deleted!'); setDeleteTarget(null); fetch(); } catch(e){ showToast(e.message,'error'); } };

  const handleBulkDelete = async () => {
    let d = 0;
    for (const id of selectedIds) { try { await facultyApi.delete(id); d++; } catch{} }
    showToast(`Deleted ${d} faculty`); setSelectedIds(new Set()); setBulkMode(false); setDeleteTarget(null); fetch();
  };

  const exportCSV = () => {
    const headers = ['Full Name','Employee ID','Email','Branch','Designation'];
    const rows = filtered.map(f => [f.full_name, f.employee_id, f.email, f.branch, f.designation]);
    const csv = [headers, ...rows].map(r => r.map(c => `"${(c||'').replace(/"/g,'""')}"`).join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `faculty_export_${new Date().toISOString().slice(0,10)}.csv`; a.click();
    showToast(`Exported ${filtered.length} faculty to CSV`, 'info');
  };

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => faculty.filter(f => {
    const matchText = !q || f.full_name?.toLowerCase().includes(q) || f.employee_id?.toLowerCase().includes(q) || f.email?.toLowerCase().includes(q) || f.branch?.toLowerCase().includes(q) || f.designation?.toLowerCase().includes(q);
    const matchBranch = !filterBranch || f.branch === filterBranch;
    return matchText && matchBranch;
  }), [faculty, q, filterBranch]);

  const toggleSelect = (id) => { const n = new Set(selectedIds); n.has(id)?n.delete(id):n.add(id); setSelectedIds(n); };

  const branchCounts = useMemo(() => {
    const c = {};
    faculty.forEach(f => { c[f.branch||'General'] = (c[f.branch||'General']||0)+1; });
    return Object.entries(c).sort((a,b)=>b[1]-a[1]).slice(0,4);
  }, [faculty]);

  const designationCounts = useMemo(() => {
    const c = {};
    faculty.forEach(f => { c[f.designation||'Faculty'] = (c[f.designation||'Faculty']||0)+1; });
    return Object.entries(c).sort((a,b)=>b[1]-a[1]).slice(0,4);
  }, [faculty]);

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title={bulkMode?"Bulk Delete":"Delete Faculty"} message={bulkMode?`Delete ${selectedIds.size} faculty?`:`Delete "${deleteTarget?.full_name}"?`} onConfirm={bulkMode?handleBulkDelete:handleDelete} onCancel={()=>{setDeleteTarget(null);setBulkMode(false);}} confirmText={bulkMode?`Delete ${selectedIds.size}`:'Delete'} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div><h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><UserSquare2 size={24} className="text-emerald-600" /> Faculty Management</h1><p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{faculty.length} faculty members</p></div>
        <div className="flex gap-2">
          <button onClick={exportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800"><Download size={16}/> Export</button>
          <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add Faculty</button>
        </div>
      </div>

      {/* Mini Stats */}
      {!loading && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800">
            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Total Faculty</p>
            <p className="text-lg font-extrabold text-gray-900 dark:text-white mt-0.5">{faculty.length}</p>
          </div>
          {branchCounts.slice(0,3).map(([b,c]) => (
            <div key={b} className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider truncate">{b?.replace(' Engineering','')}</p>
              <p className="text-lg font-extrabold text-gray-900 dark:text-white mt-0.5">{c}</p>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/><input type="text" placeholder="Search all fields..." className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400" value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/></div>
        <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white" value={filterBranch} onChange={e=>setFilterBranch(e.target.value)}>
          <option value="">All Branches</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}
        </select>
        {selectedIds.size > 0 && <button onClick={()=>{setBulkMode(true);setDeleteTarget({id:'bulk'});}} className="flex items-center gap-2 px-3.5 py-2 text-sm font-medium rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400"><Trash2 size={15}/> Delete ({selectedIds.size})</button>}
        <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} results</p>
      </div>

      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading...</p></div>
      : filtered.length === 0 ? <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-12 text-center text-gray-400"><UserSquare2 size={48} className="mx-auto mb-3 opacity-30"/><p>No faculty found.</p></div>
      : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(f => (
            <div key={f.id} className={`bg-white dark:bg-gray-900 p-5 rounded-2xl border shadow-sm stat-card group ${selectedIds.has(f.id) ? 'border-blue-300 dark:border-blue-700 ring-2 ring-blue-100 dark:ring-blue-900/30' : 'border-gray-100 dark:border-gray-800'}`}>
              <div className="flex justify-between items-start mb-3">
                <div className="flex items-center gap-3">
                  <button onClick={()=>toggleSelect(f.id)} className="text-gray-400 hover:text-blue-600">{selectedIds.has(f.id)?<CheckSquare size={16} className="text-blue-600"/>:<Square size={16}/>}</button>
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-100 to-green-100 dark:from-emerald-900/30 dark:to-green-900/30 text-emerald-600 flex items-center justify-center font-bold text-sm uppercase">{f.full_name?.split(' ').map(n=>n[0]).join('').slice(0,2)}</div>
                  <div><h3 className="font-bold text-gray-900 dark:text-white text-sm">{f.full_name}</h3><p className="text-[11px] text-gray-400">{f.designation||'Faculty'} • {f.employee_id}</p></div>
                </div>
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={()=>openModal(f)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg"><Edit3 size={14}/></button>
                  <button onClick={()=>setDeleteTarget(f)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"><Trash2 size={14}/></button>
                </div>
              </div>
              <div className="space-y-1.5 text-xs text-gray-500 dark:text-gray-400"><p>📧 {f.email}</p><p>🏛️ {f.branch||'General'}</p></div>
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Faculty':'Add New Faculty'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Full Name</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.full_name} onChange={e=>setForm({...form,full_name:e.target.value})}/></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Employee ID</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" value={form.employee_id} onChange={e=>setForm({...form,employee_id:e.target.value})}/></div>
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Email</label><input type="email" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>
              </div>
              {!isEditing && <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Password</label><input type="password" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>}
              <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Designation</label><input type="text" className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="e.g. Assistant Professor" value={form.designation} onChange={e=>setForm({...form,designation:e.target.value})}/></div>
              <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Branch</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.branch} onChange={e=>setForm({...form,branch:e.target.value})}><option value="">Select</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}</select></div>
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

export default Faculty;
