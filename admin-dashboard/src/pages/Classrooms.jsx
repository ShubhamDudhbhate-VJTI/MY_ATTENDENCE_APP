import React, { useState, useEffect, useMemo } from 'react';
import { Search, Plus, Edit3, Trash2, X, Building2, Loader2, Wifi, Download, FileText } from 'lucide-react';
import { exportPDF, exportCSV } from '../lib/exportUtils';
import { classroomApi } from '../api';
import { useToast } from '../components/Toast';
import ConfirmDialog from '../components/ConfirmDialog';

const Classrooms = () => {
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ name:'', wifi_ssid:'', wifi_bssid:'' });
  const { showToast, ToastContainer } = useToast();

  const fetchData = async () => { setLoading(true); try { setClassrooms(await classroomApi.getAll()); } catch(e) { showToast(e.message,'error'); } finally { setLoading(false); } };
  useEffect(() => { fetchData(); }, []);

  const openModal = (c=null) => {
    if(c){ setIsEditing(true); setForm({id:c.id, name:c.name||'', wifi_ssid:c.wifi_ssid||'', wifi_bssid:c.wifi_bssid||''}); }
    else { setIsEditing(false); setForm({name:'', wifi_ssid:'', wifi_bssid:''}); }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => { e.preventDefault(); setSaving(true); try {
    if(isEditing){ await classroomApi.update(form.id, form); showToast('Classroom updated!'); }
    else { await classroomApi.create(form); showToast('Classroom created!'); }
    setIsModalOpen(false); fetchData();
  } catch(e){ showToast(e.message,'error'); } finally { setSaving(false); } };

  const handleDelete = async () => { try { await classroomApi.delete(deleteTarget.id); showToast('Classroom deleted!'); setDeleteTarget(null); fetchData(); } catch(e){ showToast(e.message,'error'); } };

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => classrooms.filter(c => {
    return !q || c.name?.toLowerCase().includes(q) || c.wifi_ssid?.toLowerCase().includes(q) || c.wifi_bssid?.toLowerCase().includes(q);
  }), [classrooms, q]);

  const expHeaders = ['Room Name','WiFi SSID','WiFi BSSID'];
  const getExpRows = () => filtered.map(c => [c.name, c.wifi_ssid || '', c.wifi_bssid || '']);

  const handleExportCSV = () => {
    exportCSV({ headers: expHeaders, rows: getExpRows(), filename: `classrooms_export_${new Date().toISOString().slice(0,10)}.csv` });
  };
  const handleExportPDF = () => {
    exportPDF({ title: 'Classroom Report', subtitle: `${filtered.length} classrooms • AttendX Admin Dashboard`, headers: expHeaders, rows: getExpRows(), filename: `Classrooms_Report_${new Date().toISOString().slice(0,10)}.pdf` });
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <ToastContainer />
      <ConfirmDialog isOpen={!!deleteTarget} title="Delete Classroom" message={`Delete "${deleteTarget?.name}"?`} onConfirm={handleDelete} onCancel={()=>setDeleteTarget(null)} />

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2"><Building2 size={24} className="text-amber-600" /> Classroom Management</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">{classrooms.length} rooms configured</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExportPDF} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><FileText size={16}/> PDF</button>
          <button onClick={handleExportCSV} className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded-xl text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-800 transition-all bg-white dark:bg-gray-900"><Download size={16}/> CSV</button>
          <button onClick={()=>openModal()} className="btn-primary flex items-center gap-2 px-5 py-2.5 text-white rounded-xl text-sm font-semibold"><Plus size={18}/>Add Classroom</button>
        </div>
      </div>

      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16}/>
          <input type="text" placeholder="Search room name, SSID, BSSID..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white placeholder-gray-400"
            value={searchTerm} onChange={e=>setSearchTerm(e.target.value)}/>
        </div>
        <p className="text-xs text-gray-400 font-medium ml-auto">{filtered.length} of {classrooms.length}</p>
      </div>

      {loading ? <div className="p-12 text-center"><Loader2 size={32} className="mx-auto animate-spin text-blue-500 mb-3"/><p className="text-sm text-gray-400">Loading classrooms...</p></div>
      : filtered.length===0 ? <div className="bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-300 dark:border-gray-700 p-12 text-center text-gray-400"><Building2 size={48} className="mx-auto mb-3 opacity-30"/><p>No classrooms found.</p></div>
      : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {filtered.map(c => (
            <div key={c.id} className="bg-white dark:bg-gray-900 p-5 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm stat-card group">
              <div className="flex justify-between items-start mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-amber-100 to-orange-100 dark:from-amber-900/30 dark:to-orange-900/30 text-amber-600 flex items-center justify-center"><Building2 size={20}/></div>
                  <div>
                    <h3 className="font-bold text-gray-900 dark:text-white text-sm">{c.name}</h3>
                    <p className="text-[11px] text-gray-400">ID: {c.id?.slice(0,8)}...</p>
                  </div>
                </div>
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={()=>openModal(c)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"><Edit3 size={14}/></button>
                  <button onClick={()=>setDeleteTarget(c)} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-all"><Trash2 size={14}/></button>
                </div>
              </div>
              <div className="space-y-1.5 text-xs text-gray-500 dark:text-gray-400 mt-3 pt-3 border-t border-gray-50 dark:border-gray-800">
                <p className="flex items-center gap-2"><Wifi size={13} className="text-blue-500"/> SSID: <span className="font-mono text-gray-700 dark:text-gray-300">{c.wifi_ssid || '—'}</span></p>
                <p className="flex items-center gap-2"><Wifi size={13} className="text-green-500"/> BSSID: <span className="font-mono text-gray-700 dark:text-gray-300">{c.wifi_bssid || '—'}</span></p>
              </div>
            </div>
          ))}
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn">
          <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-lg w-full p-6 shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700">
            <div className="flex justify-between items-center mb-6"><h2 className="text-xl font-bold text-gray-900 dark:text-white">{isEditing?'Edit Classroom':'Add New Classroom'}</h2><button onClick={()=>setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600 p-1"><X size={22}/></button></div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">Room Name</label><input type="text" required className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm" placeholder="e.g. Room 302" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">WiFi SSID</label><input type="text" className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" placeholder="e.g. VJTI_Class_302" value={form.wifi_ssid} onChange={e=>setForm({...form,wifi_ssid:e.target.value})}/></div>
              <div><label className="block text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-1.5">WiFi BSSID</label><input type="text" className="w-full px-4 py-2.5 border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-xl text-sm font-mono" placeholder="e.g. AA:BB:CC:DD:EE:FF" value={form.wifi_bssid} onChange={e=>setForm({...form,wifi_bssid:e.target.value})}/></div>
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

export default Classrooms;
