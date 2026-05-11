import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, AlertTriangle, X, Info } from 'lucide-react';

const icons = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const colors = {
  success: 'from-emerald-500 to-green-600',
  error: 'from-red-500 to-rose-600',
  warning: 'from-amber-500 to-orange-500',
  info: 'from-blue-500 to-indigo-600',
};

const bgColors = {
  success: 'bg-emerald-50 border-emerald-200',
  error: 'bg-red-50 border-red-200',
  warning: 'bg-amber-50 border-amber-200',
  info: 'bg-blue-50 border-blue-200',
};

const Toast = ({ message, type = 'success', onClose, duration = 4000 }) => {
  const [isVisible, setIsVisible] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const Icon = icons[type];

  useEffect(() => {
    setTimeout(() => setIsVisible(true), 10);
    const timer = setTimeout(() => {
      setIsExiting(true);
      setTimeout(onClose, 300);
    }, duration);
    return () => clearTimeout(timer);
  }, [duration, onClose]);

  return (
    <div
      className={`fixed top-6 right-6 z-[9999] flex items-center gap-3 px-5 py-4 rounded-xl border shadow-2xl backdrop-blur-sm transition-all duration-300 max-w-md ${bgColors[type]} ${
        isVisible && !isExiting ? 'translate-x-0 opacity-100' : 'translate-x-full opacity-0'
      }`}
    >
      <div className={`p-1.5 rounded-lg bg-gradient-to-br ${colors[type]}`}>
        <Icon size={18} className="text-white" />
      </div>
      <p className="text-sm font-medium text-gray-800 flex-1">{message}</p>
      <button onClick={() => { setIsExiting(true); setTimeout(onClose, 300); }} className="text-gray-400 hover:text-gray-600 transition-colors">
        <X size={16} />
      </button>
    </div>
  );
};

export default Toast;

// Hook for toast management
export function useToast() {
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type, key: Date.now() });
  };

  const ToastContainer = () =>
    toast ? <Toast key={toast.key} message={toast.message} type={toast.type} onClose={() => setToast(null)} /> : null;

  return { showToast, ToastContainer };
}
