import React, { useState, useEffect } from 'react';
import { X, Mail, Hash, GitBranch, GraduationCap, Building2, Shield, User, Camera, Briefcase } from 'lucide-react';
import { supabase } from '../lib/supabase';

/**
 * ProfileCard — Premium detail card shown when clicking a Student, Faculty, or HOD.
 * Fetches photo from Supabase if available, otherwise shows initials avatar.
 *
 * Props:
 *  - isOpen: boolean
 *  - onClose: () => void
 *  - person: { id, full_name, email, registration_number?, employee_id?, branch, year?, department?, designation?, role? }
 *  - type: 'student' | 'faculty' | 'hod'
 */

const gradients = {
  student: 'from-blue-600 to-indigo-600',
  faculty: 'from-emerald-600 to-teal-600',
  hod: 'from-amber-500 to-orange-500',
};

const accents = {
  student: { bg: 'bg-blue-50 dark:bg-blue-900/20', text: 'text-blue-700 dark:text-blue-400', border: 'border-blue-200 dark:border-blue-800' },
  faculty: { bg: 'bg-emerald-50 dark:bg-emerald-900/20', text: 'text-emerald-700 dark:text-emerald-400', border: 'border-emerald-200 dark:border-emerald-800' },
  hod: { bg: 'bg-amber-50 dark:bg-amber-900/20', text: 'text-amber-700 dark:text-amber-400', border: 'border-amber-200 dark:border-amber-800' },
};

const typeLabels = { student: 'Student Profile', faculty: 'Faculty Profile', hod: 'Head of Department' };
const typeIcons = { student: User, faculty: Briefcase, hod: Shield };

const ProfileCard = ({ isOpen, onClose, person, type = 'student' }) => {
  const [photoUrl, setPhotoUrl] = useState(null);
  const [loadingPhoto, setLoadingPhoto] = useState(false);

  useEffect(() => {
    if (!isOpen || !person?.id) { setPhotoUrl(null); return; }

    const fetchPhoto = async () => {
      setLoadingPhoto(true);
      setPhotoUrl(null);
      try {
        if (type === 'student') {
          // Try face_image from app_students
          const { data } = await supabase
            .from('app_students')
            .select('face_image')
            .eq('id', person.id)
            .single();
          if (data?.face_image) {
            // face_image is stored as bytea — Supabase returns it as a base64 string or hex
            const bytes = data.face_image;
            if (typeof bytes === 'string' && bytes.length > 100) {
              // Check if it's already base64 or hex-encoded
              if (bytes.startsWith('\\x')) {
                // hex format — convert to base64
                const hex = bytes.slice(2);
                const byteArray = new Uint8Array(hex.match(/.{1,2}/g).map(b => parseInt(b, 16)));
                const blob = new Blob([byteArray], { type: 'image/jpeg' });
                setPhotoUrl(URL.createObjectURL(blob));
              } else {
                // Assume base64
                setPhotoUrl(`data:image/jpeg;base64,${bytes}`);
              }
            }
          }
        }
        // Also try profile_photo from app_users for all types
        if (!photoUrl) {
          const { data: userData } = await supabase
            .from('app_users')
            .select('profile_photo')
            .eq('id', person.id)
            .single();
          if (userData?.profile_photo) {
            const bytes = userData.profile_photo;
            if (typeof bytes === 'string' && bytes.length > 100) {
              if (bytes.startsWith('\\x')) {
                const hex = bytes.slice(2);
                const byteArray = new Uint8Array(hex.match(/.{1,2}/g).map(b => parseInt(b, 16)));
                const blob = new Blob([byteArray], { type: 'image/jpeg' });
                setPhotoUrl(URL.createObjectURL(blob));
              } else {
                setPhotoUrl(`data:image/jpeg;base64,${bytes}`);
              }
            }
          }
        }
      } catch (e) {
        console.log('Photo fetch skipped:', e.message);
      } finally {
        setLoadingPhoto(false);
      }
    };

    fetchPhoto();
    return () => { if (photoUrl) URL.revokeObjectURL(photoUrl); };
  }, [isOpen, person?.id]);

  if (!isOpen || !person) return null;

  const grad = gradients[type] || gradients.student;
  const accent = accents[type] || accents.student;
  const TypeIcon = typeIcons[type] || User;
  const initials = person.full_name?.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() || '??';

  const infoRows = [];
  if (person.email) infoRows.push({ icon: Mail, label: 'Email', value: person.email });
  if (person.registration_number) infoRows.push({ icon: Hash, label: 'Registration No', value: person.registration_number });
  if (person.employee_id) infoRows.push({ icon: Hash, label: 'Employee ID', value: person.employee_id });
  if (person.branch) infoRows.push({ icon: GitBranch, label: 'Branch', value: person.branch });
  if (person.year) infoRows.push({ icon: GraduationCap, label: 'Year', value: person.year });
  if (person.department) infoRows.push({ icon: Building2, label: 'Department', value: person.department });
  if (person.designation) infoRows.push({ icon: Briefcase, label: 'Designation', value: person.designation });

  return (
    <div className="fixed inset-0 modal-backdrop flex items-center justify-center z-50 p-4 animate-fadeIn" onClick={onClose}>
      <div className="bg-white dark:bg-gray-900 rounded-2xl max-w-md w-full shadow-2xl animate-scaleIn border border-gray-200 dark:border-gray-700 overflow-hidden" onClick={e => e.stopPropagation()}>

        {/* Gradient Header */}
        <div className={`relative bg-gradient-to-r ${grad} px-6 pt-6 pb-16`}>
          <button onClick={onClose} className="absolute top-4 right-4 p-1.5 bg-white/20 hover:bg-white/30 rounded-lg transition-colors backdrop-blur-sm">
            <X size={18} className="text-white" />
          </button>
          <div className="flex items-center gap-2 mb-1">
            <TypeIcon size={16} className="text-white/80" />
            <span className="text-white/80 text-xs font-semibold uppercase tracking-wider">{typeLabels[type]}</span>
          </div>
          <h2 className="text-xl font-bold text-white">{person.full_name}</h2>
          {person.designation && <p className="text-white/70 text-sm mt-0.5">{person.designation}</p>}
        </div>

        {/* Avatar (overlapping header) */}
        <div className="flex justify-center -mt-12 relative z-10">
          <div className="w-24 h-24 rounded-2xl border-4 border-white dark:border-gray-900 shadow-xl overflow-hidden bg-white dark:bg-gray-800">
            {loadingPhoto ? (
              <div className="w-full h-full flex items-center justify-center bg-gray-100 dark:bg-gray-800">
                <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : photoUrl ? (
              <img src={photoUrl} alt={person.full_name} className="w-full h-full object-cover" />
            ) : (
              <div className={`w-full h-full bg-gradient-to-br ${grad} flex items-center justify-center`}>
                <span className="text-white text-2xl font-bold">{initials}</span>
              </div>
            )}
          </div>
        </div>

        {/* Photo status badge */}
        <div className="flex justify-center mt-2 mb-4">
          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-semibold ${photoUrl ? 'bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400' : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400'}`}>
            <Camera size={10} />
            {photoUrl ? 'Biometric Photo Available' : 'No Photo Registered'}
          </span>
        </div>

        {/* Info Card */}
        <div className="px-6 pb-6">
          <div className={`rounded-xl border ${accent.border} overflow-hidden`}>
            {infoRows.map((row, i) => (
              <div key={i} className={`flex items-center gap-3 px-4 py-3 ${i !== infoRows.length - 1 ? 'border-b border-gray-100 dark:border-gray-800' : ''} ${i % 2 === 0 ? 'bg-white dark:bg-gray-900' : 'bg-gray-50/50 dark:bg-gray-800/30'}`}>
                <div className={`w-8 h-8 rounded-lg ${accent.bg} ${accent.text} flex items-center justify-center flex-shrink-0`}>
                  <row.icon size={14} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider">{row.label}</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-white truncate">{row.value}</p>
                </div>
              </div>
            ))}
          </div>

          {/* ID Badge */}
          <div className="mt-4 flex items-center justify-between px-3 py-2.5 bg-gray-50 dark:bg-gray-800/50 rounded-xl">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">System ID</span>
            <span className="text-[10px] font-mono text-gray-500 dark:text-gray-400">{person.id?.slice(0, 20)}...</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfileCard;
