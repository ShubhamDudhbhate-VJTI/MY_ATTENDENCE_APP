import React, { useState, useEffect, useMemo } from 'react';
import { Search, Plus, Edit3, Trash2, X, BookOpen, Loader2, Layers, Download, FileText } from 'lucide-react';
import { exportPDF, exportCSV } from '../lib/exportUtils';
import { subjectApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering'];
const YEARS = ['First Year','Second Year','Third Year','Final Year'];

const Subjects = () => {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [filterYear, setFilterYear] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ name:'', code:'', branch:'', year:'' });
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => { setLoading(true); try { setSubjects(await subjectApi.getAll()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetchData(); }, []);

  const openModal = (s=null) => {
    if(s){ setIsEditing(true); setForm({id:s.id,name:s.name,code:s.code,branch:s.branch,year:s.year}); }
    else { setIsEditing(false); setForm({name:'',code:'',branch:'',year:''}); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    if(isEditing){ await subjectApi.update(form.id, form); showToast('Subject updated!'); }
    else { await subjectApi.create(form); showToast('Subject created!'); }
    setIsModalOpen(false); fetchData();
  } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await subjectApi.delete(deleteTarget.id); showToast('Subject deleted!'); setDeleteTarget(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  const expHeaders = ['Name','Code','Branch','Year'];
  const getExpRows = () => filtered.map(s => [s.name, s.code, s.branch, s.year]);

  const handleExportCSV = () => {
    exportCSV({ headers: expHeaders, rows: getExpRows(), filename: `subjects_export_${new Date().toISOString().slice(0,10)}.csv` });
    showToast(`Exported ${filtered.length} subjects to CSV`, 'info');
  };

  const handleExportPDF = () => {
    exportPDF({ title: 'Subjects Report', subtitle: `${filtered.length} subjects • AttendX Admin Dashboard`, headers: expHeaders, rows: getExpRows(), filters: [{ label: 'Branch', value: filterBranch }, { label: 'Year', value: filterYear }], filename: `Subjects_Report_${new Date().toISOString().slice(0,10)}.pdf` });
    showToast(`Exported ${filtered.length} subjects to PDF`, 'info');
  };

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => subjects.filter(s => {
    const matchText = !q || s.name?.toLowerCase().includes(q) || s.code?.toLowerCase().includes(q) || s.branch?.toLowerCase().includes(q) || s.year?.toLowerCase().includes(q);
    return matchText && (!filterBranch || s.branch === filterBranch) && (!filterYear || s.year === filterYear);
  }), [subjects, q, filterBranch, filterYear]);

  const activeFilters = [filterBranch, filterYear].filter(Boolean).length;

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Delete Subject" message={`Delete "${deleteTarget?.name}"?`} onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
            <BookOpen size={24} className="text-violet-600" /> Subject Repository
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{subjects.length} subjects registered</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExportPDF} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><FileText size={16}/> PDF</button>
          <button onClick={handleExportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><Download size={16}/> CSV</button>
          <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add Subject</button>
        </div>
      </div>

      <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-gray-100 dark:border-gray-800">
          <div className="flex flex-wrap gap-3 items-center">
            <div className="relative w-full max-w-sm">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
              <input type="text" placeholder="Search name, code, branch, year..."
                className="w-full pl-10 pr-4 py-2.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
                value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
            </div>
            <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterBranch} onChange={e=>setFilterBranch(e.target.value)}>
              <option value="">All Branches</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}
            </select>
            <select className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300" value={filterYear} onChange={e=>setFilterYear(e.target.value)}>
              <option value="">All Years</option>{YEARS.map(y=><option key={y} value={y}>{y}</option>)}
            </select>
            {activeFilters > 0 && <button onClick={()=>{setFilterBranch('');setFilterYear('');}} className="px-3 py-2 text-xs text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl font-medium">Clear</button>}
            <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} of {subjects.length}</p>
          </div>
        </div>
        <div className="overflow-x-auto">
          {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading subjects...</p></div>
          : filtered.length===0 ? <div className="p-12 text-center text-gray-400"><BookOpen size={48} className="mx-auto mb-3 opacity-30"/><p>No subjects found.</p></div>
          : (
            <table className="w-full text-left">
              <thead className="bg-gray-50 dark:bg-gray-800/50"><tr className="text-gray-500 dark:text-gray-400 uppercase text-[10px] font-bold tracking-wider">
                <th className="px-6 py-3.5">Subject</th><th className="px-6 py-3.5">Code</th><th className="px-6 py-3.5">Branch</th><th className="px-6 py-3.5">Year</th><th className="px-6 py-3.5 text-right">Actions</th>
              </tr></thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {filtered.map(s => (
                  <tr key={s.id} className="table-row-hover">
                    <td className="px-6 py-3.5"><div className="flex items-center gap-3"><div className="w-9 h-9 rounded-lg bg-violet-100 dark:bg-violet-900/30 text-violet-600 flex items-center justify-center"><BookOpen size={16}/></div><span className="text-sm font-semibold text-gray-900 dark:text-white">{s.name}</span></div></td>
                    <td className="px-6 py-3.5 font-mono text-xs text-gray-600 dark:text-gray-400">{s.code||'—'}</td>
                    <td className="px-6 py-3.5"><span className="px-2.5 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-400 rounded-lg text-xs font-medium">{s.branch||'Common'}</span></td>
                    <td className="px-6 py-3.5 text-sm text-gray-600 dark:text-gray-400 flex items-center gap-1.5"><Layers size={14} className="text-gray-400"/>{s.year||'All'}</td>
                    <td className="px-6 py-3.5 text-right"><div className="flex items-center justify-end gap-1">
                      <button onClick={()=>openModal(s)} className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={16}/></button>
                      <button onClick={()=>setDeleteTarget(s)} className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={16}/></button>
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Subject':'Add New Subject'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Subject Name</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Course Code</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" value={form.code} onChange={e=>setForm({...form,code:e.target.value})}/></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Branch</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.branch} onChange={e=>setForm({...form,branch:e.target.value})}><option value="">Select</option>{BRANCHES.map(b=><option key={b} value={b}>{b}</option>)}</select></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Year</label><select required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" value={form.year} onChange={e=>setForm({...form,year:e.target.value})}><option value="">Select</option>{YEARS.map(y=><option key={y} value={y}>{y}</option>)}</select></div>
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

export default Subjects;
