import React, { useState, useEffect, useMemo } from 'react';
import { Search, FileText, Download, Loader2, User, Mail, Hash, GitBranch, GraduationCap, Camera, BookOpen, BarChart3, CheckCircle2, Calendar, Clock } from 'lucide-react';
import { studentReportApi, getFaceUrl, getProfilePhotoUrl } from '../api';
import { supabase } from '../lib/supabase';
import { useToast } from '../components/Toast';
import { exportPDF, exportCSV } from '../lib/exportUtils';

const BRANCHES = ['Information Technology', 'Computer Engineering', 'Mechanical Engineering', 'Civil Engineering', 'Electronics Engineering', 'Electrical Engineering', 'Production Engineering', 'Textile Engineering'];
const YEARS = ['First Year', 'Second Year', 'Third Year', 'Final Year'];

// ── Inline style objects ──
const styles = {
  card: { background: 'var(--sr-card-bg)', borderRadius: '20px', border: '1px solid var(--sr-card-border)', boxShadow: '0 2px 12px var(--sr-card-shadow)' },
  gradientHeader: { background: 'linear-gradient(135deg, #6d28d9 0%, #4f46e5 50%, #2563eb 100%)', padding: '32px 32px 32px', position: 'relative', overflow: 'hidden', borderRadius: '20px 20px 0 0' },
  photoBox: { width: 120, height: 120, borderRadius: 18, overflow: 'hidden', border: '5px solid var(--sr-card-bg)', boxShadow: '0 12px 32px rgba(109,40,217,0.25)', flexShrink: 0 },
  photoFallback: { width: '100%', height: '100%', background: 'linear-gradient(135deg, #7c3aed, #4f46e5)', display: 'flex', alignItems: 'center', justifyContent: 'center' },
  infoTile: { background: 'var(--sr-tile-bg)', borderRadius: 14, padding: '16px 18px', border: '1px solid var(--sr-tile-border)', transition: 'box-shadow 0.2s', cursor: 'default' },
  progressTrack: { width: '100%', height: 10, background: 'var(--sr-track-bg)', borderRadius: 99, overflow: 'hidden' },
  sectionHeader: { padding: '20px 24px', borderBottom: '1px solid var(--sr-section-border)', display: 'flex', alignItems: 'center', gap: 10 },
};

const StudentReport = () => {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [filterYear, setFilterYear] = useState('');
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [studentDetails, setStudentDetails] = useState(null);
  const [attendanceData, setAttendanceData] = useState([]);
  const [subjectTotals, setSubjectTotals] = useState({});
  const [loadingReport, setLoadingReport] = useState(false);
  const [photoUrl, setPhotoUrl] = useState(null);
  const [loadingPhoto, setLoadingPhoto] = useState(false);
  const { showToast, ToastContainer } = useToast();

  useEffect(() => { (async () => { setLoading(true); try { setStudents(await studentReportApi.getAllStudents()); } catch (e) { showToast(e.message, 'error'); } finally { setLoading(false); } })(); }, []);

  const q = searchTerm.toLowerCase();
  const filtered = useMemo(() => students.filter(s => {
    const matchText = !q || s.full_name?.toLowerCase().includes(q) || s.registration_number?.toLowerCase().includes(q);
    return matchText && (!filterBranch || s.branch === filterBranch) && (!filterYear || s.year === filterYear);
  }), [students, q, filterBranch, filterYear]);

  const selectStudent = async (student) => {
    setSelectedStudent(student); setLoadingReport(true); setPhotoUrl(null);
    try {
      const [details, records, totals] = await Promise.all([
        studentReportApi.getStudentDetails(student.id),
        studentReportApi.getStudentAttendance(student.id),
        studentReportApi.getTotalSessionsBySubjects(student.branch, student.year),
      ]);
      setStudentDetails(details); setAttendanceData(records); setSubjectTotals(totals);
      setLoadingPhoto(true);
      try {
        let photoFound = false;

        // 1. Try Backend face endpoint (stored via biometric registration)
        try {
          const faceUrl = getFaceUrl(student.registration_number || student.id);
          const faceResp = await fetch(faceUrl);
          if (faceResp.ok) {
            const blob = await faceResp.blob();
            if (blob.size > 100) {
              setPhotoUrl(URL.createObjectURL(blob));
              photoFound = true;
            }
          }
        } catch (_) { }

        // 2. Try Backend profile-photo endpoint (uploaded profile photo from app)
        if (!photoFound) {
          try {
            const profileUrl = getProfilePhotoUrl(student.id);
            const profResp = await fetch(profileUrl);
            if (profResp.ok) {
              const blob = await profResp.blob();
              if (blob.size > 100) {
                setPhotoUrl(URL.createObjectURL(blob));
                photoFound = true;
              }
            }
          } catch (_) { }
        }

        // 3. Fallback to Supabase face_image from app_students
        if (!photoFound) {
          try {
            const { data } = await supabase.from('app_students').select('face_image').eq('id', student.id).single();
            if (data?.face_image && typeof data.face_image === 'string' && data.face_image.length > 100) {
              if (data.face_image.startsWith('\\x')) {
                const hex = data.face_image.slice(2);
                const ba = new Uint8Array(hex.match(/.{1,2}/g).map(b => parseInt(b, 16)));
                setPhotoUrl(URL.createObjectURL(new Blob([ba], { type: 'image/jpeg' })));
              } else {
                setPhotoUrl(`data:image/jpeg;base64,${data.face_image}`);
              }
              photoFound = true;
            }
          } catch (_) { }
        }

        // 4. Fallback to Supabase profile_photo from app_users
        if (!photoFound) {
          try {
            const { data: userData } = await supabase.from('app_users').select('profile_photo').eq('id', student.id).single();
            if (userData?.profile_photo && typeof userData.profile_photo === 'string' && userData.profile_photo.length > 100) {
              if (userData.profile_photo.startsWith('\\x')) {
                const hex = userData.profile_photo.slice(2);
                const ba = new Uint8Array(hex.match(/.{1,2}/g).map(b => parseInt(b, 16)));
                setPhotoUrl(URL.createObjectURL(new Blob([ba], { type: 'image/jpeg' })));
              } else {
                setPhotoUrl(`data:image/jpeg;base64,${userData.profile_photo}`);
              }
            }
          } catch (_) { }
        }
      } catch (_) { }
      finally { setLoadingPhoto(false); }
    } catch (e) { showToast(e.message, 'error'); }
    finally { setLoadingReport(false); }
  };

  const subjectStats = useMemo(() => {
    const map = {};
    attendanceData.forEach(r => {
      const sub = r.attendance_sessions?.subjects; if (!sub) return;
      if (!map[sub.id]) map[sub.id] = { name: sub.name, code: sub.code, attended: 0 };
      if (r.status === 'present') map[sub.id].attended++;
    });
    return Object.entries(map).map(([subId, info]) => {
      const total = subjectTotals[subId]?.totalSessions || info.attended;
      return { subjectId: subId, ...info, total, percentage: total > 0 ? Math.round((info.attended / total) * 100) : 0 };
    }).sort((a, b) => a.name.localeCompare(b.name));
  }, [attendanceData, subjectTotals]);

  const overallStats = useMemo(() => {
    const totalSessions = Object.values(subjectTotals).reduce((s, v) => s + v.totalSessions, 0);
    const attended = attendanceData.filter(r => r.status === 'present').length;
    return { totalSessions, attended, percentage: totalSessions > 0 ? Math.round((attended / totalSessions) * 100) : 0, faceVerified: attendanceData.filter(r => r.face_verified).length };
  }, [attendanceData, subjectTotals]);

  const recentRecords = useMemo(() => attendanceData.slice(0, 10), [attendanceData]);
  const initials = selectedStudent?.full_name?.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() || '??';

  const pctColor = (p) => p >= 75 ? '#059669' : p >= 50 ? '#d97706' : '#dc2626';
  const pctBg = (p) => p >= 75 ? 'linear-gradient(90deg,#10b981,#34d399)' : p >= 50 ? 'linear-gradient(90deg,#f59e0b,#fbbf24)' : 'linear-gradient(90deg,#ef4444,#f87171)';

  const handleExportPDF = () => {
    if (!studentDetails) return;
    const s = studentDetails;
    const headers = ['Subject', 'Code', 'Attended', 'Total', 'Percentage'];
    const rows = subjectStats.map(ss => [ss.name, ss.code, ss.attended, ss.total, `${ss.percentage}%`]);
    rows.push(['', '', '', '', '']);
    rows.push(['OVERALL', '', overallStats.attended, overallStats.totalSessions, `${overallStats.percentage}%`]);
    exportPDF({
      title: `Student Report — ${s.full_name}`,
      headers, rows,
      filename: `Student_Report_${s.registration_number}_${new Date().toISOString().slice(0, 10)}.pdf`,
      studentPhoto: photoUrl || null,
      studentInfo: {
        name: s.full_name,
        regNo: s.registration_number,
        branch: s.branch,
        year: s.year,
        email: s.email,
        overallPct: overallStats.percentage,
        attended: overallStats.attended,
        totalSessions: overallStats.totalSessions,
        faceVerified: overallStats.faceVerified,
      },
    });
    showToast('PDF exported!', 'info');
  };

  const handleExportCSV = () => {
    if (!studentDetails) return;
    const headers = ['Subject', 'Code', 'Attended', 'Total', 'Percentage'];
    const rows = subjectStats.map(ss => [ss.name, ss.code, ss.attended, ss.total, `${ss.percentage}%`]);
    rows.push(['OVERALL', '', overallStats.attended, overallStats.totalSessions, `${overallStats.percentage}%`]);
    exportCSV({ headers, rows, filename: `Student_Report_${studentDetails.registration_number}_${new Date().toISOString().slice(0, 10)}.csv` });
    showToast('CSV exported!', 'info');
  };

  const fmtDate = (iso) => iso ? new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) + ' ' + new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '—';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <ToastContainer />

      {/* ── Page Header ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 800, color: 'var(--sr-text-primary)', display: 'flex', alignItems: 'center', gap: 8, margin: 0 }}>
            <BarChart3 size={24} style={{ color: '#7c3aed' }} /> Student Report
          </h1>
          <p style={{ fontSize: 13, color: 'var(--sr-text-secondary)', marginTop: 4 }}>Individual attendance report with subject-wise breakdown</p>
        </div>
        {selectedStudent && (
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={handleExportPDF} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 16px', background: 'var(--sr-btn-bg)', border: '1px solid var(--sr-btn-border)', borderRadius: 12, fontSize: 13, fontWeight: 500, color: 'var(--sr-btn-text)', cursor: 'pointer' }}><FileText size={16} /> PDF</button>
            <button onClick={handleExportCSV} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 16px', background: 'var(--sr-btn-bg)', border: '1px solid var(--sr-btn-border)', borderRadius: 12, fontSize: 13, fontWeight: 500, color: 'var(--sr-btn-text)', cursor: 'pointer' }}><Download size={16} /> CSV</button>
          </div>
        )}
      </div>

      {/* ── Search & Filter ── */}
      <div style={styles.card}>
        <div style={{ padding: 16, display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: 220 }}>
            <Search size={16} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} />
            <input type="text" placeholder="Search by name or reg no..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)}
              style={{ width: '100%', paddingLeft: 36, paddingRight: 12, paddingTop: 10, paddingBottom: 10, border: '1px solid var(--sr-input-border)', borderRadius: 12, fontSize: 13, outline: 'none', background: 'var(--sr-input-bg)', color: 'var(--sr-text-primary)' }} />
          </div>
          <select value={filterBranch} onChange={e => setFilterBranch(e.target.value)} style={{ padding: '10px 12px', border: '1px solid var(--sr-input-border)', borderRadius: 12, fontSize: 13, background: 'var(--sr-select-bg)', color: 'var(--sr-text-primary)' }}>
            <option value="">All Branches</option>
            {BRANCHES.map(b => <option key={b} value={b}>{b}</option>)}
          </select>
          <select value={filterYear} onChange={e => setFilterYear(e.target.value)} style={{ padding: '10px 12px', border: '1px solid var(--sr-input-border)', borderRadius: 12, fontSize: 13, background: 'var(--sr-select-bg)', color: 'var(--sr-text-primary)' }}>
            <option value="">All Years</option>
            {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
          </select>
          {(filterBranch || filterYear || searchTerm) && <button onClick={() => { setFilterBranch(''); setFilterYear(''); setSearchTerm(''); }} style={{ padding: '8px 12px', fontSize: 11, color: '#dc2626', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }}>Clear All</button>}
          <span style={{ fontSize: 11, color: 'var(--sr-text-muted)', fontWeight: 500, marginLeft: 'auto' }}>{filtered.length} students</span>
        </div>

        {/* Student List */}
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center' }}><Loader2 size={28} className="animate-spin" style={{ color: '#7c3aed', margin: '0 auto 8px' }} /><p style={{ fontSize: 13, color: '#9ca3af' }}>Loading...</p></div>
        ) : filtered.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}><User size={40} style={{ margin: '0 auto 8px', opacity: 0.3 }} /><p>No students found</p></div>
        ) : (
          <div style={{ maxHeight: 240, overflowY: 'auto', padding: '0 12px 12px' }}>
            {filtered.map(s => (
              <button key={s.id} onClick={() => selectStudent(s)} style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px', borderRadius: 12, border: selectedStudent?.id === s.id ? '2px solid #7c3aed' : '2px solid transparent',
                background: selectedStudent?.id === s.id ? 'var(--sr-student-selected-bg)' : 'transparent', cursor: 'pointer', textAlign: 'left', marginTop: 4, transition: 'all 0.15s'
              }}>
                <div style={{
                  width: 36, height: 36, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 11, flexShrink: 0,
                  background: selectedStudent?.id === s.id ? 'linear-gradient(135deg,#7c3aed,#4f46e5)' : 'var(--sr-student-avatar-bg)', color: selectedStudent?.id === s.id ? '#fff' : 'var(--sr-student-avatar-text)'
                }}>{s.full_name?.split(' ').map(n => n[0]).join('').slice(0, 2)}</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--sr-text-primary)', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.full_name}</p>
                  <p style={{ fontSize: 11, color: 'var(--sr-text-muted)', margin: 0 }}>{s.registration_number} • {s.branch?.replace(' Engineering', '')} • {s.year}</p>
                </div>
                {selectedStudent?.id === s.id && <CheckCircle2 size={18} style={{ color: '#7c3aed', flexShrink: 0 }} />}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* ── Report Content ── */}
      {loadingReport ? (
        <div style={{ ...styles.card, padding: 60, textAlign: 'center' }}><Loader2 size={36} className="animate-spin" style={{ color: '#7c3aed', margin: '0 auto 12px' }} /><p style={{ fontSize: 14, color: '#9ca3af', fontWeight: 500 }}>Generating report...</p></div>
      ) : selectedStudent && studentDetails ? (
        <>
          {/* ═══ STUDENT PROFILE CARD — Identity Info ═══ */}
          <div style={styles.card}>
            {/* Rich Gradient Header */}
            <div style={styles.gradientHeader}>
              <div style={{ position: 'absolute', top: -30, right: -30, width: 120, height: 120, borderRadius: '50%', background: 'rgba(255,255,255,0.06)' }}></div>
              <div style={{ position: 'absolute', bottom: -20, right: 80, width: 80, height: 80, borderRadius: '50%', background: 'rgba(255,255,255,0.04)' }}></div>
              <div style={{ position: 'absolute', top: 10, right: 160, width: 40, height: 40, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }}></div>
              <p style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.7)', letterSpacing: '0.12em', textTransform: 'uppercase', margin: '0 0 8px', position: 'relative', zIndex: 1 }}>📋 INDIVIDUAL STUDENT REPORT</p>
              <h2 style={{ fontSize: 28, fontWeight: 800, color: '#fff', margin: 0, position: 'relative', zIndex: 1 }}>{studentDetails.full_name}</h2>
              <p style={{ fontSize: 14, color: 'rgba(255,255,255,0.6)', marginTop: 6, position: 'relative', zIndex: 1 }}>{studentDetails.registration_number} &nbsp;•&nbsp; {studentDetails.email}</p>
            </div>

            {/* Photo + Student Details side by side */}
            <div style={{ padding: '24px 32px 28px' }}>
              <div style={{ display: 'flex', gap: 28, alignItems: 'flex-start', flexWrap: 'wrap' }}>
                {/* Photo */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                  <div style={styles.photoBox}>
                    {loadingPhoto ? (
                      <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--sr-photo-loader-bg)' }}>
                        <Loader2 size={22} className="animate-spin" style={{ color: '#7c3aed' }} />
                      </div>
                    ) : photoUrl ? (
                      <img src={photoUrl} alt={studentDetails.full_name} style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
                    ) : (
                      <div style={styles.photoFallback}><span style={{ color: '#fff', fontSize: 36, fontWeight: 800 }}>{initials}</span></div>
                    )}
                  </div>
                  <div style={{ marginTop: 10, display: 'inline-flex', alignItems: 'center', gap: 5, padding: '5px 14px', borderRadius: 99, fontSize: 11, fontWeight: 600, background: photoUrl ? 'var(--sr-badge-success-bg)' : 'var(--sr-photo-badge-bg)', color: photoUrl ? 'var(--sr-badge-success-text)' : 'var(--sr-photo-badge-text)' }}>
                    <Camera size={11} />{photoUrl ? 'Photo Available' : 'No Photo'}
                  </div>
                </div>

                {/* Student Info Grid — next to photo */}
                <div style={{ flex: 1, minWidth: 280 }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 14 }}>
                    {[
                      { icon: GitBranch, label: 'Branch', value: studentDetails.branch, accent: '#7c3aed' },
                      { icon: GraduationCap, label: 'Year', value: studentDetails.year, accent: '#2563eb' },
                      { icon: Hash, label: 'Registration No', value: studentDetails.registration_number, accent: '#059669' },
                      { icon: Mail, label: 'Email Address', value: studentDetails.email, accent: '#d97706' },
                    ].map((item, i) => (
                      <div key={i} style={{ ...styles.infoTile, borderLeft: `4px solid ${item.accent}` }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                          <item.icon size={14} style={{ color: item.accent }} />
                          <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--sr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>{item.label}</span>
                        </div>
                        <p style={{ fontSize: 15, fontWeight: 600, color: 'var(--sr-text-primary)', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.value || '—'}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* ═══ OVERALL STATS ═══ */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14 }}>
            {[
              { label: 'Overall Attendance', value: `${overallStats.percentage}%`, sub: `${overallStats.attended} / ${overallStats.totalSessions}`, color: pctColor(overallStats.percentage), emoji: '📊' },
              { label: 'Total Present', value: overallStats.attended, sub: 'Sessions attended', color: '#059669', emoji: '✅' },
              { label: 'Total Sessions', value: overallStats.totalSessions, sub: 'All subjects combined', color: '#2563eb', emoji: '📅' },
              { label: 'Face Verified', value: overallStats.faceVerified, sub: 'Biometric confirmed', color: '#7c3aed', emoji: '🔐' },
            ].map((s, i) => (
              <div key={i} style={{ background: 'var(--sr-card-bg)', borderRadius: 18, border: '1px solid var(--sr-stat-border)', padding: '22px 18px', borderLeft: `4px solid ${s.color}`, boxShadow: `0 2px 8px var(--sr-stat-shadow)` }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
                  <span style={{ fontSize: 16 }}>{s.emoji}</span>
                  <p style={{ fontSize: 10, fontWeight: 700, color: 'var(--sr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', margin: 0 }}>{s.label}</p>
                </div>
                <p style={{ fontSize: 34, fontWeight: 800, color: s.color, margin: '0 0 4px', lineHeight: 1 }}>{s.value}</p>
                <p style={{ fontSize: 11, color: 'var(--sr-text-faint)', margin: 0 }}>{s.sub}</p>
              </div>
            ))}
          </div>

          {/* ═══ SUBJECT-WISE BREAKDOWN ═══ */}
          <div style={styles.card}>
            <div style={styles.sectionHeader}>
              <div style={{ width: 36, height: 36, borderRadius: 10, background: 'var(--sr-icon-box-bg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <BookOpen size={18} style={{ color: '#7c3aed' }} />
              </div>
              <div>
                <h3 style={{ fontSize: 17, fontWeight: 700, color: 'var(--sr-text-primary)', margin: 0 }}>Subject-wise Attendance</h3>
                <p style={{ fontSize: 12, color: 'var(--sr-text-muted)', margin: 0 }}>{subjectStats.length} subjects tracked</p>
              </div>
            </div>
            {subjectStats.length === 0 ? (
              <div style={{ padding: 48, textAlign: 'center', color: '#9ca3af' }}><BookOpen size={44} style={{ margin: '0 auto 10px', opacity: 0.25 }} /><p style={{ fontSize: 14 }}>No attendance records found</p></div>
            ) : (
              <div>
                {subjectStats.map((ss, i) => (
                  <div key={ss.subjectId} style={{ display: 'flex', alignItems: 'center', gap: 18, padding: '18px 24px', borderBottom: i < subjectStats.length - 1 ? '1px solid var(--sr-section-border)' : 'none', background: i % 2 === 1 ? 'var(--sr-row-alt)' : 'var(--sr-row-default)' }}>
                    <div style={{ width: 46, height: 46, borderRadius: 12, background: `${pctColor(ss.percentage)}10`, color: pctColor(ss.percentage), display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 12, flexShrink: 0, border: `1px solid ${pctColor(ss.percentage)}20` }}>
                      {ss.code?.slice(0, 3) || '#'}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                        <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--sr-text-primary)' }}>{ss.name}</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span style={{ fontSize: 11, color: 'var(--sr-text-muted)' }}>{ss.attended}/{ss.total}</span>
                          <span style={{ fontSize: 13, fontWeight: 800, color: '#fff', background: pctBg(ss.percentage), padding: '3px 12px', borderRadius: 99, minWidth: 48, textAlign: 'center' }}>{ss.percentage}%</span>
                        </div>
                      </div>
                      <div style={styles.progressTrack}>
                        <div style={{ height: '100%', borderRadius: 99, background: pctBg(ss.percentage), width: `${ss.percentage}%`, transition: 'width 0.8s cubic-bezier(0.4,0,0.2,1)' }}></div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ═══ RECENT RECORDS TABLE ═══ */}
          {recentRecords.length > 0 && (
            <div style={styles.card}>
              <div style={styles.sectionHeader}>
                <Calendar size={18} style={{ color: '#7c3aed' }} />
                <div>
                  <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--sr-text-primary)', margin: 0 }}>Recent Attendance Records</h3>
                  <p style={{ fontSize: 11, color: 'var(--sr-text-muted)', margin: 0 }}>Last {recentRecords.length} entries</p>
                </div>
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ background: 'var(--sr-table-header-bg)' }}>
                    {['Subject', 'Date & Time', 'Status', 'Face ✓'].map(h => (
                      <th key={h} style={{ padding: '10px 20px', textAlign: 'left', fontSize: 10, fontWeight: 700, color: 'var(--sr-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {recentRecords.map((r, i) => (
                    <tr key={r.id} style={{ borderTop: '1px solid var(--sr-table-border)' }}>
                      <td style={{ padding: '10px 20px', fontWeight: 500, color: 'var(--sr-text-primary)' }}>{r.attendance_sessions?.subjects?.name || '—'}</td>
                      <td style={{ padding: '10px 20px', color: 'var(--sr-text-secondary)', fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}><Clock size={12} />{fmtDate(r.marked_at)}</td>
                      <td style={{ padding: '10px 20px' }}>
                        <span style={{ padding: '3px 10px', borderRadius: 99, fontSize: 10, fontWeight: 700, background: r.status === 'present' ? 'var(--sr-badge-success-bg)' : 'var(--sr-badge-error-bg)', color: r.status === 'present' ? 'var(--sr-badge-success-text)' : 'var(--sr-badge-error-text)' }}>{r.status || 'present'}</span>
                      </td>
                      <td style={{ padding: '10px 20px', fontSize: 12, fontWeight: 600, color: r.face_verified ? '#059669' : 'var(--sr-text-muted)' }}>{r.face_verified ? '✓ Yes' : 'No'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      ) : (
        <div style={{ background: 'var(--sr-card-bg)', borderRadius: '20px', borderWidth: '1px', borderColor: 'var(--sr-card-border)', borderStyle: 'dashed', boxShadow: '0 2px 12px var(--sr-card-shadow)', padding: 60, textAlign: 'center' }}>
          <BarChart3 size={56} style={{ margin: '0 auto 16px', color: 'var(--sr-empty-icon)' }} />
          <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--sr-empty-title)', margin: 0 }}>Select a Student</h3>
          <p style={{ fontSize: 13, color: 'var(--sr-text-muted)', marginTop: 6 }}>Search and click on a student above to generate their individual attendance report</p>
        </div>
      )}
    </div>
  );
};

export default StudentReport;
