import React, { useState, useEffect } from 'react';
import { X, Send, Bell, Users, UserSquare2, AlertCircle, CheckCircle2, User, Search } from 'lucide-react';
import { supabase } from '../lib/supabase';
import { studentReportApi, facultyReportApi } from '../api';

const NotificationsModal = ({ isOpen, onClose }) => {
  const [targetType, setTargetType] = useState('all'); // all, student, faculty, branch_year, single_student, single_faculty
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [branch, setBranch] = useState('');
  const [year, setYear] = useState('');
  const [selectedPersonId, setSelectedPersonId] = useState('');
  const [personSearch, setPersonSearch] = useState('');
  const [status, setStatus] = useState(null); // 'sending', 'success', 'error'
  const [errorMsg, setErrorMsg] = useState('');
  
  const [students, setStudents] = useState([]);
  const [faculty, setFaculty] = useState([]);
  const [isLoadingData, setIsLoadingData] = useState(false);

  useEffect(() => {
    if (isOpen && (students.length === 0 || faculty.length === 0)) {
      setIsLoadingData(true);
      Promise.all([
        studentReportApi.getAllStudents(),
        facultyReportApi.getAllFaculty()
      ]).then(([sData, fData]) => {
        setStudents(sData);
        setFaculty(fData);
      }).catch(console.error).finally(() => setIsLoadingData(false));
    }
  }, [isOpen, students.length, faculty.length]);

  if (!isOpen) return null;

  const handleSend = async () => {
    if (!title.trim() || !body.trim()) {
      setErrorMsg('Title and message are required.');
      return;
    }
    
    setStatus('sending');
    setErrorMsg('');
    
    try {
      // Determine the target users
      let targetUsers = [];
      if (targetType === 'single_student' || targetType === 'single_faculty') {
        if (!selectedPersonId) throw new Error("Please select a person.");
        targetUsers = [{ id: selectedPersonId }];
      } else if (targetType === 'student') {
        targetUsers = students;
      } else if (targetType === 'faculty') {
        targetUsers = faculty;
      } else if (targetType === 'all') {
        targetUsers = [...students, ...faculty];
      } else if (targetType === 'branch_year') {
        if (!branch || !year) throw new Error("Please select branch and year.");
        targetUsers = students.filter(s => s.branch === branch && s.year === year);
      }

      if (targetUsers.length === 0) {
        throw new Error("No users found matching the selected criteria.");
      }

      // Format records for the notifications table
      const records = targetUsers.map(u => ({
        user_id: u.id,
        title: title,
        message: body,
        created_at: new Date().toISOString()
      }));

      // Insert all notifications in chunks to avoid large payload errors
      const chunkSize = 100;
      for (let i = 0; i < records.length; i += chunkSize) {
        const chunk = records.slice(i, i + chunkSize);
        const { error: insertError } = await supabase.from('notifications').insert(chunk);
        if (insertError) console.error("Batch insert error:", insertError);
      }

      setStatus('success');
      setTimeout(() => {
        onClose();
        setStatus(null);
        setTitle('');
        setBody('');
        setPersonSearch('');
        setSelectedPersonId('');
      }, 2000);
    } catch (err) {
      setStatus('error');
      setErrorMsg(err.message || 'Failed to send notification');
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white dark:bg-gray-900 rounded-2xl w-full max-w-md shadow-2xl border border-gray-200 dark:border-gray-800 overflow-hidden animate-scaleIn">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-100 dark:border-gray-800 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 flex items-center justify-center">
              <Bell size={20} className={status === 'sending' ? 'animate-bounce' : ''} />
            </div>
            <div>
              <h2 className="text-base font-bold text-gray-900 dark:text-white">Send Notification</h2>
              <p className="text-[11px] text-gray-500 font-medium">Push to mobile devices</p>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 space-y-4">
          {status === 'success' ? (
            <div className="py-8 text-center space-y-3">
              <div className="w-16 h-16 bg-green-100 dark:bg-green-500/20 text-green-600 dark:text-green-400 rounded-full flex items-center justify-center mx-auto mb-4 animate-scaleIn">
                <CheckCircle2 size={32} />
              </div>
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">Notification Sent!</h3>
              <p className="text-sm text-gray-500">The message has been dispatched successfully.</p>
            </div>
          ) : (
            <>
              {errorMsg && (
                <div className="p-3 text-sm text-red-600 bg-red-50 dark:bg-red-500/10 dark:text-red-400 border border-red-200 dark:border-red-500/20 rounded-xl flex items-center gap-2">
                  <AlertCircle size={16} /> {errorMsg}
                </div>
              )}

              <div className="space-y-3">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">Target Audience</label>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                  <button onClick={() => setTargetType('all')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'all' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <Bell size={14} /> Everyone
                  </button>
                  <button onClick={() => setTargetType('student')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'student' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <Users size={14} /> All Students
                  </button>
                  <button onClick={() => setTargetType('faculty')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'faculty' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <UserSquare2 size={14} /> All Faculty
                  </button>
                  <button onClick={() => setTargetType('branch_year')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'branch_year' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <Users size={14} /> Specific Class
                  </button>
                  <button onClick={() => setTargetType('single_student')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'single_student' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <User size={14} /> One Student
                  </button>
                  <button onClick={() => setTargetType('single_faculty')} className={`p-2 rounded-xl border flex items-center gap-2 text-[13px] font-semibold transition-all ${targetType === 'single_faculty' ? 'border-blue-500 bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-400' : 'border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <User size={14} /> One Faculty
                  </button>
                </div>
              </div>

              {targetType === 'branch_year' && (
                <div className="flex gap-3 animate-fadeIn">
                  <div className="flex-1 space-y-1.5">
                    <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">Branch</label>
                    <select value={branch} onChange={(e) => setBranch(e.target.value)} className="w-full bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl px-4 py-2.5 outline-none focus:border-blue-500 text-sm font-medium">
                      <option value="">Select Branch</option>
                      <option value="Computer Science Engineering">Computer Science</option>
                      <option value="Information Technology">Information Technology</option>
                      <option value="Electronics Engineering">Electronics</option>
                      <option value="Electrical Engineering">Electrical</option>
                      <option value="Mechanical Engineering">Mechanical</option>
                    </select>
                  </div>
                  <div className="flex-1 space-y-1.5">
                    <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">Year</label>
                    <select value={year} onChange={(e) => setYear(e.target.value)} className="w-full bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl px-4 py-2.5 outline-none focus:border-blue-500 text-sm font-medium">
                      <option value="">Select Year</option>
                      <option value="First Year">First Year</option>
                      <option value="Second Year">Second Year</option>
                      <option value="Third Year">Third Year</option>
                      <option value="Fourth Year">Fourth Year</option>
                    </select>
                  </div>
                </div>
              )}

              {(targetType === 'single_student' || targetType === 'single_faculty') && (
                <div className="space-y-1.5 animate-fadeIn relative">
                  <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">
                    Select {targetType === 'single_student' ? 'Student' : 'Faculty'}
                  </label>
                  <div className="relative">
                    <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input 
                      type="text" 
                      placeholder="Search by name or ID..."
                      value={personSearch}
                      onChange={(e) => { setPersonSearch(e.target.value); setSelectedPersonId(''); }}
                      className="w-full bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl pl-10 pr-4 py-2.5 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm font-medium transition-all"
                    />
                  </div>
                  {personSearch && !selectedPersonId && (
                    <div className="absolute z-10 mt-1 w-full max-h-40 overflow-y-auto bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg">
                      {isLoadingData ? (
                        <p className="p-3 text-sm text-gray-500 text-center">Loading...</p>
                      ) : (targetType === 'single_student' ? students : faculty)
                        .filter(p => p.full_name?.toLowerCase().includes(personSearch.toLowerCase()) || 
                                     p.registration_number?.toLowerCase().includes(personSearch.toLowerCase()) || 
                                     p.employee_id?.toLowerCase().includes(personSearch.toLowerCase()))
                        .slice(0, 10).map(p => (
                          <button 
                            key={p.id}
                            onClick={() => {
                              setSelectedPersonId(p.id);
                              setPersonSearch(`${p.full_name} (${p.registration_number || p.employee_id})`);
                            }}
                            className="w-full text-left px-4 py-2 text-sm hover:bg-blue-50 dark:hover:bg-blue-900/20 text-gray-700 dark:text-gray-300 border-b border-gray-100 dark:border-gray-700 last:border-0"
                          >
                            <span className="font-semibold">{p.full_name}</span>
                            <span className="text-xs text-gray-400 ml-2">{p.registration_number || p.employee_id}</span>
                          </button>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">Notification Title</label>
                <input 
                  type="text" 
                  value={title} 
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g., Tomorrow's Holiday" 
                  className="w-full bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl px-4 py-2.5 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm font-medium transition-all"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-gray-500 uppercase tracking-wider">Message</label>
                <textarea 
                  value={body} 
                  onChange={(e) => setBody(e.target.value)}
                  placeholder="Enter the notification message..." 
                  rows={4}
                  className="w-full bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 text-gray-900 dark:text-white rounded-xl px-4 py-3 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm font-medium transition-all resize-none"
                />
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        {status !== 'success' && (
          <div className="p-4 border-t border-gray-100 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/50 flex justify-end gap-3">
            <button onClick={onClose} disabled={status === 'sending'} className="px-5 py-2.5 text-sm font-semibold text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-xl transition-colors disabled:opacity-50">
              Cancel
            </button>
            <button onClick={handleSend} disabled={status === 'sending'} className="flex items-center gap-2 px-5 py-2.5 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-500/30 rounded-xl transition-all disabled:opacity-70 disabled:shadow-none">
              {status === 'sending' ? (
                <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Sending...</>
              ) : (
                <><Send size={16} /> Send Alert</>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsModal;
