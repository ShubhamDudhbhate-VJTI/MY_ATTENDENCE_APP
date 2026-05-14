import React, { useState, useEffect, useMemo } from 'react';
import { Search, Plus, Trash2, X, Link2, Loader2, BookOpen, User, Filter, Download, FileText } from 'lucide-react';
import { exportPDF, exportCSV } from '../lib/exportUtils';
import { assignmentApi, facultyApi, subjectApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const BRANCHES = ['Information Technology','Computer Engineering','Mechanical Engineering','Civil Engineering','Electronics Engineering','Electrical Engineering','Production Engineering','Textile Engineering'];

const Assignments = () => {
  const [assignments, setAssignments] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ faculty_id: '', subject_id: '' });
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [a, f, s] = await Promise.all([
        assignmentApi.getAll(),
        facultyApi.getAll(),
        subjectApi.getAll()
      ]);
      setAssignments(a);
      setFaculty(f);
      setSubjects(s);
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await assignmentApi.create(form);
      showToast('Assignment created successfully!');
      setIsModalOpen(false);
      setForm({ faculty_id: '', subject_id: '' });
      fetchData();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    try {
      await assignmentApi.delete(deleteTarget.id);
      showToast('Assignment removed!');
      setDeleteTarget(null);
      fetchData();
    } catch (e) {
      showToast(e.message, 'error');
    }
  };

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => assignments.filter(a => {
    const matchText = !q ||
      a.faculty_name?.toLowerCase().includes(q) ||
      a.subject_name?.toLowerCase().includes(q) ||
      a.employee_id?.toLowerCase().includes(q) ||
      a.subject_code?.toLowerCase().includes(q);
    const matchBranch = !filterBranch || a.branch === filterBranch;
    return matchText && matchBranch;
  }), [assignments, q, filterBranch]);

  const expHeaders = ['Faculty Name', 'Employee ID', 'Subject', 'Code', 'Branch', 'Year'];
  const getExpRows = () => filtered.map(a => [
    a.faculty_name, a.employee_id, a.subject_name, a.subject_code, a.branch, a.year
  ]);

  const handleExportCSV = () => {
    exportCSV({
      headers: expHeaders,
      rows: getExpRows(),
      filename: `assignments_export_${new Date().toISOString().slice(0,10)}.csv`
    });
    showToast(`Exported ${filtered.length} assignments to CSV`, 'info');
  };

  const handleExportPDF = () => {
    exportPDF({
      title: 'Faculty-Subject Assignments',
      subtitle: `${filtered.length} mappings found`,
      headers: expHeaders,
      rows: getExpRows(),
      filename: `Assignments_Report_${new Date().toISOString().slice(0,10)}.pdf`
    });
    showToast(`Exported ${filtered.length} assignments to PDF`, 'info');
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog
        isOpen={!!deleteTarget}
        title="Delete Assignment"
        message={`Remove assignment of "${deleteTarget?.subject_name}" from "${deleteTarget?.faculty_name}"?`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
            <Link2 size={24} className="text-indigo-600" /> Faculty Assignments
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{assignments.length} total mappings</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExportPDF} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800"><FileText size={16}/> PDF</button>
          <button onClick={handleExportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800"><Download size={16}/> CSV</button>
          <button onClick={() => setIsModalOpen(true)} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold">
            <Plus size={18}/> Assign Subject
          </button>
        </div>
      </div>

      {/* Search and Filter */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
          <input
            type="text"
            placeholder="Search faculty or subject..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          className="px-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
          value={filterBranch}
          onChange={e => setFilterBranch(e.target.value)}
        >
          <option value="">All Branches</option>
          {BRANCHES.map(b => <option key={b} value={b}>{b}</option>)}
        </select>
        <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} results</p>
      </div>

      {loading ? (
        <div className="p-12 text-center">
          <Loader2 size={32} className="mx-auto animate-spin text-indigo-500 mb-3"/>
          <p className="text-sm text-gray-400">Loading assignments...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-12 text-center text-gray-400">
          <Link2 size={48} className="mx-auto mb-3 opacity-30"/>
          <p>No assignments found.</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-900 border border-gray-100 dark:border-gray-800 rounded-2xl overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50/50 dark:bg-gray-800/50 border-bottom border-gray-100 dark:border-gray-800">
                  <th className="px-6 py-4 text-[11px] font-bold text-gray-400 uppercase tracking-wider">Faculty</th>
                  <th className="px-6 py-4 text-[11px] font-bold text-gray-400 uppercase tracking-wider">Subject</th>
                  <th className="px-6 py-4 text-[11px] font-bold text-gray-400 uppercase tracking-wider">Branch & Year</th>
                  <th className="px-6 py-4 text-[11px] font-bold text-gray-400 uppercase tracking-wider text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                {filtered.map(a => (
                  <tr key={a.id} className="hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-indigo-50 dark:bg-indigo-900/20 text-indigo-600 flex items-center justify-center font-bold text-[10px]">
                          <User size={14} />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-gray-900 dark:text-white">{a.faculty_name}</p>
                          <p className="text-[11px] text-gray-400 font-mono">{a.employee_id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 flex items-center justify-center">
                          <BookOpen size={14} />
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-gray-900 dark:text-white">{a.subject_name}</p>
                          <p className="text-[11px] text-gray-400 font-mono">{a.subject_code}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="px-2 py-1 rounded-md bg-gray-100 dark:bg-gray-800 text-[10px] font-bold text-gray-600 dark:text-gray-400">
                        {a.branch?.replace(' Engineering', '')} • Year {a.year}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => setDeleteTarget(a)}
                        className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl transition-all"
                      >
                        <Trash2 size={16}/>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
                <Plus size={20} className="text-indigo-600" /> New Assignment
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Select Faculty</label>
                <select
                  required
                  className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm"
                  value={form.faculty_id}
                  onChange={e => setForm({...form, faculty_id: e.target.value})}
                >
                  <option value="">-- Choose Faculty Member --</option>
                  {faculty.map(f => (
                    <option key={f.id} value={f.id}>{f.full_name} ({f.employee_id})</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5">Select Subject</label>
                <select
                  required
                  className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm"
                  value={form.subject_id}
                  onChange={e => setForm({...form, subject_id: e.target.value})}
                >
                  <option value="">-- Choose Subject --</option>
                  {subjects.map(s => (
                    <option key={s.id} value={s.id}>[{s.code}] {s.name} - {s.branch}</option>
                  ))}
                </select>
              </div>
              <div className="flex gap-3 mt-6 pt-4 border-t border-gray-100 dark:border-gray-800">
                <button type="button" onClick={() => setIsModalOpen(false)} className="flex-1 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm">Cancel</button>
                <button type="submit" disabled={saving || !form.faculty_id || !form.subject_id} className="flex-1 btn-primary px-4 py-2.5 text-white rounded-xl font-medium text-sm flex items-center justify-center gap-2">
                  {saving && <Loader2 size={16} className="animate-spin"/>}
                  Create Assignment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Assignments;
