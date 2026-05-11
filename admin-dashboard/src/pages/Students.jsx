import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Search, Plus, Edit3, Trash2, X, Users, Loader2, Filter, Download, ChevronUp, ChevronDown, ChevronLeft, ChevronRight, CheckSquare, Square, Upload } from 'lucide-react';
import { studentApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering','Production Engineering','Textile Engineering'];
const YEARS = ['First Year','Second Year','Third Year','Final Year'];
const PAGE_SIZES = [10, 25, 50, 100];

const Students = () => {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [filterYear, setFilterYear] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [bulkDeleteMode, setBulkDeleteMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [sortKey, setSortKey] = useState('full_name');
  const [sortDir, setSortDir] = useState('asc');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [form, setForm] = useState({ registration_number:'', full_name:'', email:'', password:'password123', branch:'', year:'' });
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => { setLoading(true); try { setStudents(await studentApi.getAll()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetchData(); }, []);

  const openModal = (s = null) => {
    if (s) { setIsEditing(true); setForm({ ...s, password:'' }); }
    else { setIsEditing(false); setForm({ registration_number:'', full_name:'', email:'', password:'password123', branch:'', year:'' }); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    if (isEditing) { await studentApi.update(form.id, form); showToast('Student updated!'); }
    else { await studentApi.create(form); showToast('Student created!'); }
    setIsModalOpen(false); fetchData();
  } catch(e) { showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await studentApi.delete(deleteTarget.id); showToast('Student deleted!'); setDeleteTarget(null); fetchData(); } catch(e) { showToast(e.message,'error'); } };

  const handleBulkDelete = async () => {
    const ids = [...selectedIds];
    let deleted = 0;
    for (const id of ids) {
      try { await studentApi.delete(id); deleted++; } catch(e) { console.error(e); }
    }
    showToast(`Deleted ${deleted} of ${ids.length} students`);
    setSelectedIds(new Set());
    setBulkDeleteMode(false);
    setDeleteTarget(null);
    fetchData();
  };

  const exportCSV = () => {
    const headers = ['Full Name','Registration No','Email','Branch','Year'];
    const rows = filtered.map(s => [s.full_name, s.registration_number, s.email, s.branch, s.year]);
    const csv = [headers, ...rows].map(r => r.map(c => `"${(c||'').replace(/"/g,'""')}"`).join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `students_export_${new Date().toISOString().slice(0,10)}.csv`; a.click();
    URL.revokeObjectURL(url);
    showToast(`Exported ${filtered.length} students to CSV`, 'info');
  };

  // CSV Import
  const fileInputRef = useRef(null);
  const [importing, setImporting] = useState(false);

  const handleCSVImport = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setImporting(true);
    try {
      const text = await file.text();
      const lines = text.split('\n').filter(l => l.trim());
      if (lines.length < 2) { showToast('CSV must have a header row + data rows', 'error'); return; }
      const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, '').toLowerCase());
      const nameIdx = headers.findIndex(h => h.includes('name'));
      const regIdx = headers.findIndex(h => h.includes('reg') || h.includes('registration'));
      const emailIdx = headers.findIndex(h => h.includes('email'));
      const branchIdx = headers.findIndex(h => h.includes('branch'));
      const yearIdx = headers.findIndex(h => h.includes('year'));

      if (nameIdx === -1 || emailIdx === -1) { showToast('CSV must have "name" and "email" columns', 'error'); return; }

      let created = 0, failed = 0;
      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(c => c.trim().replace(/^"|"$/g, ''));
        const row = {
          full_name: cols[nameIdx] || '',
          registration_number: regIdx >= 0 ? cols[regIdx] : '',
          email: cols[emailIdx] || '',
          password: 'password123',
          branch: branchIdx >= 0 ? cols[branchIdx] : '',
          year: yearIdx >= 0 ? cols[yearIdx] : '',
        };
        if (!row.full_name || !row.email) { failed++; continue; }
        try { await studentApi.create(row); created++; } catch { failed++; }
      }
      showToast(`Imported ${created} students (${failed} skipped)`);
      fetchData();
    } catch (err) { showToast('Failed to parse CSV: ' + err.message, 'error'); }
    finally { setImporting(false); if (fileInputRef.current) fileInputRef.current.value = ''; }
  };

  // Filter
  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => students.filter(s => {
    const matchText = !q || s.full_name?.toLowerCase().includes(q) || s.registration_number?.toLowerCase().includes(q) || s.email?.toLowerCase().includes(q) || s.branch?.toLowerCase().includes(q) || s.year?.toLowerCase().includes(q);
    const matchBranch = !filterBranch || s.branch === filterBranch;
    const matchYear = !filterYear || s.year === filterYear;
    return matchText && matchBranch && matchYear;
  }), [students, q, filterBranch, filterYear]);

  // Sort
  const sorted = useMemo(() => [...filtered].sort((a, b) => {
    const va = (a[sortKey] || '').toLowerCase();
    const vb = (b[sortKey] || '').toLowerCase();
    return sortDir === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va);
  }), [filtered, sortKey, sortDir]);

  // Pagination
  const totalPages = Math.ceil(sorted.length / pageSize);
  const paginated = sorted.slice((page - 1) * pageSize, page * pageSize);
  useEffect(() => { setPage(1); }, [searchTerm, filterBranch, filterYear, pageSize]);

  const toggleSort = (key) => {
    if (sortKey === key) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortKey(key); setSortDir('asc'); }
  };

  const SortIcon = ({ col }) => {
    if (sortKey !== col) return <ChevronUp size={12} className="text-gray-300 opacity-0 group-hover:opacity-100 transition-opacity" />;
    return sortDir === 'asc' ? <ChevronUp size={12} className="text-blue-600" /> : <ChevronDown size={12} className="text-blue-600" />;
  };

  const toggleSelect = (id) => {
    const next = new Set(selectedIds);
    next.has(id) ? next.delete(id) : next.add(id);
    setSelectedIds(next);
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === paginated.length) setSelectedIds(new Set());
    else setSelectedIds(new Set(paginated.map(s => s.id)));
  };

  const activeFilters = [filterBranch, filterYear].filter(Boolean).length;
  // Branch breakdown mini stats
  const branchCounts = useMemo(() => {
    const c = {};
    students.forEach(s => { c[s.branch] = (c[s.branch] || 0) + 1; });
    return Object.entries(c).sort((a,b) => b[1]-a[1]).slice(0, 4);
  }, [students]);

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog
        isOpen={!!deleteTarget}
        title={bulkDeleteMode ? 'Bulk Delete' : 'Delete Student'}
        message={bulkDeleteMode ? `Delete ${selectedIds.size} selected students permanently?` : `Delete "${deleteTarget?.full_name}"?`}
        onConfirm={bulkDeleteMode ? handleBulkDelete : handleDelete}
        onCancel={() => { setDeleteTarget(null); setBulkDeleteMode(false); }}
        confirmText={bulkDeleteMode ? `Delete ${selectedIds.size}` : 'Delete'}
      />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><Users size={24} className="text-blue-600" /> Student Management</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">Manage student records — {students.length} total</p>
        </div>
        <div className="flex gap-2 flex-wrap">
          <input type="file" accept=".csv" ref={fileInputRef} onChange={handleCSVImport} className="hidden" />
          <button onClick={() => fileInputRef.current?.click()} disabled={importing}
            className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900 disabled:opacity-50">
            {importing ? <Loader2 size={16} className="animate-spin" /> : <Upload size={16} />}
            {importing ? 'Importing...' : 'Import CSV'}
          </button>
          <button onClick={exportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900">
            <Download size={16} /> Export
          </button>
          <button onClick={() => openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold">
            <Plus size={18} /> Add Student
          </button>
        </div>
      </div>

      {/* Mini Stats */}
      {!loading && branchCounts.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {branchCounts.map(([branch, count]) => (
            <div key={branch} className="bg-white dark:bg-gray-900 px-4 py-3 rounded-xl border border-gray-100 dark:border-gray-800">
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider truncate">{branch?.replace(' Engineering','')}</p>
              <p className="text-lg font-extrabold text-gray-900 dark:text-white mt-0.5">{count}</p>
            </div>
          ))}
        </div>
      )}

      {/* Table */}
      <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-gray-100 dark:border-gray-800 space-y-3">
          <div className="flex flex-col sm:flex-row gap-3 items-center justify-between">
            <div className="relative w-full sm:w-96">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input type="text" placeholder="Search all fields..."
                className="w-full pl-10 pr-4 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
                value={searchTerm} onChange={e => setSearchTerm(e.target.value)} />
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <button onClick={() => setShowFilters(!showFilters)}
                className={`flex items-center gap-2 px-3.5 py-2 text-sm font-medium rounded-xl border transition-all ${showFilters || activeFilters ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800 text-blue-700 dark:text-blue-400' : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400'}`}>
                <Filter size={15} /> Filters {activeFilters > 0 && <span className="w-5 h-5 bg-blue-600 text-white rounded-full text-[10px] flex items-center justify-center font-bold">{activeFilters}</span>}
              </button>
              {selectedIds.size > 0 && (
                <button onClick={() => { setBulkDeleteMode(true); setDeleteTarget({ id: 'bulk' }); }}
                  className="flex items-center gap-2 px-3.5 py-2 text-sm font-medium rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400">
                  <Trash2 size={15} /> Delete ({selectedIds.size})
                </button>
              )}
              <p className="text-xs text-gray-400 font-medium">{filtered.length} results</p>
            </div>
          </div>
          {showFilters && (
            <div className="flex flex-wrap gap-3 pt-1 animate-fadeIn">
              <select className="px-3 py-2 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white" value={filterBranch} onChange={e => setFilterBranch(e.target.value)}>
                <option value="">All Branches</option>{BRANCHES.map(b => <option key={b} value={b}>{b}</option>)}
              </select>
              <select className="px-3 py-2 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white" value={filterYear} onChange={e => setFilterYear(e.target.value)}>
                <option value="">All Years</option>{YEARS.map(y => <option key={y} value={y}>{y}</option>)}
              </select>
              {activeFilters > 0 && <button onClick={() => { setFilterBranch(''); setFilterYear(''); }} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear</button>}
            </div>
          )}
        </div>

        <div className="overflow-x-auto">
          {loading ? (
            <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3" /><p className="text-sm text-gray-400">Loading from Supabase...</p></div>
          ) : paginated.length === 0 ? (
            <div className="p-12 text-center text-gray-400"><Users size={48} className="mx-auto mb-3 opacity-30" /><p className="font-medium">No students found</p></div>
          ) : (
            <table className="w-full text-left">
              <thead className="bg-gray-50/80 dark:bg-gray-800/50"><tr className="text-gray-500 dark:text-gray-400 uppercase text-[10px] font-bold tracking-wider">
                <th className="px-4 py-3.5 w-10">
                  <button onClick={toggleSelectAll} className="text-gray-400 hover:text-blue-600">
                    {selectedIds.size === paginated.length && paginated.length > 0 ? <CheckSquare size={16} className="text-blue-600" /> : <Square size={16} />}
                  </button>
                </th>
                <th className="px-4 py-3.5 cursor-pointer group" onClick={() => toggleSort('full_name')}><span className="flex items-center gap-1">Student <SortIcon col="full_name" /></span></th>
                <th className="px-4 py-3.5 cursor-pointer group" onClick={() => toggleSort('registration_number')}><span className="flex items-center gap-1">Reg No <SortIcon col="registration_number" /></span></th>
                <th className="px-4 py-3.5 cursor-pointer group" onClick={() => toggleSort('branch')}><span className="flex items-center gap-1">Branch <SortIcon col="branch" /></span></th>
                <th className="px-4 py-3.5 cursor-pointer group" onClick={() => toggleSort('year')}><span className="flex items-center gap-1">Year <SortIcon col="year" /></span></th>
                <th className="px-4 py-3.5 text-right">Actions</th>
              </tr></thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {paginated.map(s => (
                  <tr key={s.id} className={`table-row-hover ${selectedIds.has(s.id) ? 'bg-blue-50/50 dark:bg-blue-900/10' : ''}`}>
                    <td className="px-4 py-3">
                      <button onClick={() => toggleSelect(s.id)} className="text-gray-400 hover:text-blue-600">
                        {selectedIds.has(s.id) ? <CheckSquare size={16} className="text-blue-600" /> : <Square size={16} />}
                      </button>
                    </td>
                    <td className="px-4 py-3"><div className="flex items-center gap-3"><div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-100 to-indigo-100 dark:from-blue-900/30 dark:to-indigo-900/30 text-blue-600 flex items-center justify-center font-bold text-[10px] uppercase">{s.full_name?.split(' ').map(n=>n[0]).join('').slice(0,2)}</div><div><p className="text-sm font-semibold text-gray-900 dark:text-white">{s.full_name}</p><p className="text-[11px] text-gray-400">{s.email}</p></div></div></td>
                    <td className="px-4 py-3 font-mono text-xs text-gray-600 dark:text-gray-400">{s.registration_number}</td>
                    <td className="px-4 py-3"><span className="px-2 py-0.5 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 rounded-lg text-[10px] font-medium">{s.branch||'—'}</span></td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{s.year||'—'}</td>
                    <td className="px-4 py-3 text-right"><div className="flex items-center justify-end gap-1">
                      <button onClick={()=>openModal(s)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={14}/></button>
                      <button onClick={()=>setDeleteTarget(s)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={14}/></button>
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination */}
        {!loading && sorted.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 dark:border-gray-800 text-sm">
            <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
              <span className="text-xs">Rows:</span>
              <select className="px-2 py-1 border border-gray-200 dark:border-gray-700 rounded-lg text-xs bg-white dark:bg-gray-800 text-gray-900 dark:text-white" value={pageSize} onChange={e => setPageSize(Number(e.target.value))}>
                {PAGE_SIZES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
              <span className="text-xs">{(page-1)*pageSize+1}–{Math.min(page*pageSize, sorted.length)} of {sorted.length}</span>
            </div>
            <div className="flex items-center gap-1">
              <button disabled={page<=1} onClick={()=>setPage(p=>p-1)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-30 text-gray-500"><ChevronLeft size={16}/></button>
              {Array.from({length: Math.min(totalPages, 5)}, (_, i) => {
                const p = totalPages <= 5 ? i+1 : page <= 3 ? i+1 : page >= totalPages-2 ? totalPages-4+i : page-2+i;
                return <button key={p} onClick={()=>setPage(p)} className={`w-8 h-8 rounded-lg text-xs font-medium transition-all ${p===page ? 'bg-blue-600 text-white' : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'}`}>{p}</button>;
              })}
              <button disabled={page>=totalPages} onClick={()=>setPage(p=>p+1)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 disabled:opacity-30 text-gray-500"><ChevronRight size={16}/></button>
            </div>
          </div>
        )}
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Student':'Add New Student'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Full Name</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.full_name} onChange={e=>setForm({...form,full_name:e.target.value})}/></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Registration No</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" value={form.registration_number} onChange={e=>setForm({...form,registration_number:e.target.value})}/></div>
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Email</label><input type="email" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>
              </div>
              {!isEditing && <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Password</label><input type="password" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>}
              <div className="grid grid-cols-2 gap-4">
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Branch</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.branch} onChange={e=>setForm({...form,branch:e.target.value})}><option value="">Select</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}</select></div>
                <div><label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Year</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.year} onChange={e=>setForm({...form,year:e.target.value})}><option value="">Select</option>{YEARS.map(y=><option key={y} value={y}>{y}</option>)}</select></div>
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

export default Students;
