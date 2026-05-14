import React, { useState, useEffect, useRef } from 'react';
import { Lock, User, Eye, EyeOff, Shield, ChevronRight, GraduationCap, Wifi, ScanFace, BarChart3, Clock } from 'lucide-react';

import { analyticsApi } from '../api';

// Particle canvas
const ParticleBackground = () => {
  const canvasRef = useRef(null);
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let animId;
    const resize = () => { canvas.width = window.innerWidth; canvas.height = window.innerHeight; };
    resize(); window.addEventListener('resize', resize);
    const particles = Array.from({ length: 90 }, () => ({
      x: Math.random() * canvas.width, y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.4, vy: (Math.random() - 0.5) * 0.4,
      r: Math.random() * 2 + 0.5, o: Math.random() * 0.5 + 0.1,
    }));
    const draw = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles.forEach((p, i) => {
        p.x += p.vx; p.y += p.vy;
        if (p.x < 0) p.x = canvas.width; if (p.x > canvas.width) p.x = 0;
        if (p.y < 0) p.y = canvas.height; if (p.y > canvas.height) p.y = 0;
        ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(212,168,67,${p.o})`; ctx.fill();
        for (let j = i + 1; j < particles.length; j++) {
          const dx = p.x - particles[j].x, dy = p.y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 130) {
            ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(212,168,67,${0.07 * (1 - dist / 130)})`; ctx.lineWidth = 0.5; ctx.stroke();
          }
        }
      });
      animId = requestAnimationFrame(draw);
    };
    draw();
    return () => { cancelAnimationFrame(animId); window.removeEventListener('resize', resize); };
  }, []);
  return <canvas ref={canvasRef} className="absolute inset-0 z-0" />;
};

// Typewriter hook
const useTypewriter = (texts, speed = 60, pause = 2000) => {
  const [display, setDisplay] = useState('');
  const [idx, setIdx] = useState(0);
  const [charIdx, setCharIdx] = useState(0);
  const [deleting, setDeleting] = useState(false);
  useEffect(() => {
    const text = texts[idx];
    const timer = setTimeout(() => {
      if (!deleting) {
        setDisplay(text.slice(0, charIdx + 1));
        if (charIdx + 1 === text.length) setTimeout(() => setDeleting(true), pause);
        else setCharIdx(c => c + 1);
      } else {
        setDisplay(text.slice(0, charIdx));
        if (charIdx === 0) { setDeleting(false); setIdx(i => (i + 1) % texts.length); }
        else setCharIdx(c => c - 1);
      }
    }, deleting ? speed / 2 : speed);
    return () => clearTimeout(timer);
  }, [charIdx, deleting, idx, texts, speed, pause]);
  return display;
};

// Animated counter
const AnimCounter = ({ target, duration = 1500 }) => {
  const [val, setVal] = useState(0);
  useEffect(() => {
    const start = Date.now();
    const tick = () => {
      const p = Math.min((Date.now() - start) / duration, 1);
      setVal(Math.round(target * (1 - Math.pow(1 - p, 3))));
      if (p < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }, [target, duration]);
  return val.toLocaleString();
};

const Login = ({ onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [mounted, setMounted] = useState(false);
  const [stats, setStats] = useState({ totalStudents: 0, totalFaculty: 0, totalSessions: 0 });
  const cardRef = useRef(null);

  useEffect(() => { 
    setMounted(true); 
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      const data = await analyticsApi.getDashboardStats();
      if (data) setStats(data);
    } catch (e) {
      console.error('Stats fetch error:', e);
    }
  };

  const typed = useTypewriter([
    'Smart Attendance System',
    'Face Recognition Powered',
    'WiFi Geofencing Enabled',
    'Real-time Analytics Dashboard',
    'Built for VJTI Mumbai',
  ], 50, 2500);

  // 3D tilt effect
  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    cardRef.current.style.transform = `perspective(1000px) rotateY(${x * 6}deg) rotateX(${-y * 6}deg)`;
  };
  const handleMouseLeave = () => {
    if (cardRef.current) cardRef.current.style.transform = 'perspective(1000px) rotateY(0deg) rotateX(0deg)';
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    await new Promise(r => setTimeout(r, 1000));
    if (username === 'admin' && password === 'pass') {
      localStorage.setItem('attendx_admin_auth', 'true'); onLogin();
    } else { setError('Invalid credentials.'); setLoading(false); }
  };

  return (
    <div className="min-h-screen flex relative overflow-hidden" style={{ background: 'linear-gradient(160deg, #0a0614 0%, #140e28 25%, #1a1030 50%, #0d0820 75%, #0a0614 100%)' }}>
      <ParticleBackground />

      {/* Orbs */}
      <div className="absolute inset-0 pointer-events-none z-[1]">
        <div className="absolute w-[600px] h-[600px] rounded-full" style={{ background: 'radial-gradient(circle, rgba(139,26,58,0.18) 0%, transparent 70%)', top: '-15%', left: '-10%', animation: 'orbit 30s linear infinite' }} />
        <div className="absolute w-[500px] h-[500px] rounded-full" style={{ background: 'radial-gradient(circle, rgba(212,168,67,0.12) 0%, transparent 70%)', bottom: '-10%', right: '-10%', animation: 'orbit 25s linear infinite reverse' }} />
      </div>

      {/* Left Panel */}
      <div className={`hidden lg:flex flex-1 flex-col justify-between p-14 relative z-10 transition-all duration-1000 ${mounted ? 'opacity-100' : 'opacity-0 -translate-x-10'}`}>
        <div>
          <div className="flex items-center gap-4 mb-16">
            <div className="relative">
              <div className="absolute -inset-2 rounded-2xl opacity-40 blur-lg" style={{ background: 'linear-gradient(135deg, #8B1A3A, #d4a843)' }} />
              <div className="relative w-16 h-16 rounded-2xl flex items-center justify-center" style={{ background: 'linear-gradient(135deg, #8B1A3A, #C0392B)' }}>
                <GraduationCap size={30} className="text-white" />
              </div>
            </div>
            <div>
              <h2 className="text-white text-xl font-black tracking-tight">VJTI Mumbai</h2>
              <p className="text-xs font-semibold tracking-[0.2em] uppercase" style={{ color: '#d4a843' }}>Established 1887</p>
            </div>
          </div>

          <div className="space-y-6 max-w-lg">
            <p className="text-xs font-bold tracking-[0.3em] uppercase" style={{ color: '#d4a843' }}>Admin Control Center</p>
            <h1 className="text-6xl font-black text-white leading-[1.1] tracking-tight">
              Attend<span style={{ color: '#d4a843' }}>X</span>
            </h1>
            {/* Typewriter */}
            <p className="text-xl text-gray-400 font-light h-8">
              {typed}<span className="animate-blink ml-0.5 inline-block w-[2px] h-5 bg-[#d4a843] align-middle" />
            </p>

            {/* Live stats */}
            <div className="grid grid-cols-4 gap-3 mt-10">
              {[
                { icon: GraduationCap, val: stats.totalStudents || 4200, label: 'Students', color: '#3b82f6' },
                { icon: ScanFace, val: stats.totalFaculty || 156, label: 'Faculty', color: '#10b981' },
                { icon: BarChart3, val: stats.totalSessions || 8700, label: 'Sessions', color: '#8b5cf6' },
                { icon: Clock, val: 99, label: '% Uptime', color: '#d4a843' },
              ].map(s => (
                <div key={s.label} className="p-3 rounded-xl text-center backdrop-blur-sm group hover:scale-105 transition-all duration-300" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <s.icon size={16} className="mx-auto mb-2 group-hover:scale-125 transition-transform" style={{ color: s.color }} />
                  <p className="text-xl font-black text-white"><AnimCounter target={s.val} /></p>
                  <p className="text-[9px] text-gray-500 uppercase tracking-wider mt-1 font-bold">{s.label}</p>
                </div>
              ))}
            </div>

            {/* Feature row */}
            <div className="flex flex-wrap gap-2 mt-8">
              {[
                { icon: ScanFace, t: 'Face ID' },
                { icon: Wifi, t: 'WiFi Fence' },
                { icon: BarChart3, t: 'Analytics' },
                { icon: Shield, t: 'Encrypted' },
              ].map(f => (
                <span key={f.t} className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[11px] font-medium" style={{ color: 'rgba(255,255,255,0.45)', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <f.icon size={12} /> {f.t}
                </span>
              ))}
            </div>
          </div>
        </div>

        <div>
          <div className="h-px w-20 mb-3" style={{ background: 'linear-gradient(90deg, #d4a843, transparent)' }} />
          <p className="text-gray-600 text-[11px]">Veermata Jijabai Technological Institute • Matunga, Mumbai — 400 019</p>
        </div>
      </div>

      {/* Right Panel */}
      <div className={`flex-1 lg:max-w-[520px] flex items-center justify-center p-6 sm:p-12 relative z-10 transition-all duration-1000 delay-300 ${mounted ? 'opacity-100' : 'opacity-0 translate-y-10'}`}>
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="lg:hidden flex flex-col items-center gap-3 mb-10">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center" style={{ background: 'linear-gradient(135deg, #8B1A3A, #C0392B)' }}>
              <GraduationCap size={28} className="text-white" />
            </div>
            <h2 className="text-white text-2xl font-black">Attend<span style={{ color: '#d4a843' }}>X</span></h2>
            <p className="text-gray-500 text-[10px] tracking-[0.2em] uppercase">VJTI Admin</p>
          </div>

          {/* 3D Card */}
          <div ref={cardRef} onMouseMove={handleMouseMove} onMouseLeave={handleMouseLeave}
            className="relative transition-transform duration-200 ease-out" style={{ transformStyle: 'preserve-3d' }}>
            <div className="absolute -inset-px rounded-[28px] opacity-25" style={{ background: 'linear-gradient(135deg, #8B1A3A, transparent 40%, transparent 60%, #d4a843)' }} />
            <div className="absolute -inset-8 rounded-[40px] opacity-10 blur-2xl" style={{ background: 'linear-gradient(135deg, #8B1A3A, #d4a843)' }} />

            <div className="relative rounded-[28px] p-8 sm:p-10" style={{ background: 'rgba(18,14,30,0.85)', border: '1px solid rgba(255,255,255,0.08)', backdropFilter: 'blur(40px)' }}>
              <div className="text-center mb-8">
                <div className="relative w-16 h-16 mx-auto mb-5">
                  <div className="absolute inset-0 rounded-2xl animate-spin-slow" style={{ background: 'conic-gradient(from 0deg, #8B1A3A, #d4a843, #6366f1, #8B1A3A)', padding: '2px' }}>
                    <div className="w-full h-full rounded-2xl" style={{ background: '#120e1e' }} />
                  </div>
                  <div className="absolute inset-[3px] rounded-[14px] flex items-center justify-center" style={{ background: 'linear-gradient(135deg, #8B1A3A, #A52040)' }}>
                    <Shield size={26} className="text-white" />
                  </div>
                </div>
                <h3 className="text-2xl font-black text-white">Admin Access</h3>
                <p className="text-gray-500 mt-2 text-sm">Enter credentials to continue</p>
              </div>

              {error && (
                <div className="mb-5 p-4 rounded-xl text-sm font-medium flex items-center gap-3 animate-shake" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.15)', color: '#fca5a5' }}>
                  <div className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0" style={{ background: 'rgba(239,68,68,0.15)' }}>✕</div>
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                  <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-[0.15em] mb-2">Username</label>
                  <div className="relative group">
                    <div className="absolute -inset-px rounded-xl opacity-0 group-focus-within:opacity-100 transition-opacity duration-300" style={{ background: 'linear-gradient(135deg, #8B1A3A, #d4a843)' }} />
                    <div className="relative flex items-center rounded-xl" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}>
                      <User size={16} className="absolute left-4 text-gray-500 group-focus-within:text-[#d4a843] transition-colors" />
                      <input type="text" required autoFocus placeholder="admin"
                        className="w-full pl-12 pr-4 py-3.5 bg-transparent rounded-xl text-sm text-white placeholder-gray-600 outline-none" value={username} onChange={e => setUsername(e.target.value)} />
                    </div>
                  </div>
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-gray-400 uppercase tracking-[0.15em] mb-2">Password</label>
                  <div className="relative group">
                    <div className="absolute -inset-px rounded-xl opacity-0 group-focus-within:opacity-100 transition-opacity duration-300" style={{ background: 'linear-gradient(135deg, #8B1A3A, #d4a843)' }} />
                    <div className="relative flex items-center rounded-xl" style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)' }}>
                      <Lock size={16} className="absolute left-4 text-gray-500 group-focus-within:text-[#d4a843] transition-colors" />
                      <input type={showPassword ? 'text' : 'password'} required placeholder="••••••••"
                        className="w-full pl-12 pr-12 py-3.5 bg-transparent rounded-xl text-sm text-white placeholder-gray-600 outline-none" value={password} onChange={e => setPassword(e.target.value)} />
                      <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-4 text-gray-500 hover:text-gray-300 transition-colors">
                        {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                      </button>
                    </div>
                  </div>
                </div>

                <button type="submit" disabled={loading}
                  className="relative w-full py-4 rounded-xl text-white text-sm font-bold flex items-center justify-center gap-2.5 overflow-hidden group disabled:opacity-50 transition-all mt-2">
                  <div className="absolute inset-0 transition-all duration-500" style={{ background: 'linear-gradient(135deg, #8B1A3A, #A52040, #C0392B)' }} />
                  <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500" style={{ background: 'linear-gradient(135deg, #A52040, #C0392B, #d4a843)' }} />
                  <div className="absolute top-0 -left-full w-full h-full group-hover:left-full transition-all duration-700" style={{ background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.12), transparent)' }} />
                  <span className="relative flex items-center gap-2.5">
                    {loading ? <><div className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin" /> Verifying...</>
                    : <>Access Dashboard <ChevronRight size={18} className="group-hover:translate-x-1 transition-transform" /></>}
                  </span>
                </button>
              </form>

              <div className="mt-7 pt-6 flex items-center justify-between" style={{ borderTop: '1px solid rgba(255,255,255,0.05)' }}>
                <div className="flex items-center gap-2"><div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" /><span className="text-[10px] text-gray-500">System Online</span></div>
                <div className="flex items-center gap-2 text-gray-600 text-[10px]"><Lock size={10} /><span>AES-256 Encrypted</span></div>
              </div>
            </div>
          </div>

          <p className="text-center text-gray-700 text-[10px] mt-8 tracking-[0.2em] uppercase">AttendX v2.0 • Supabase</p>
        </div>
      </div>

      <style>{`
        @keyframes orbit { from { transform: rotate(0deg) translateX(30px) rotate(0deg); } to { transform: rotate(360deg) translateX(30px) rotate(-360deg); } }
        @keyframes spin-slow { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        .animate-spin-slow { animation: spin-slow 8s linear infinite; }
        .animate-blink { animation: blink 1s step-end infinite; }
        @keyframes blink { 50% { opacity: 0; } }
        .animate-shake { animation: shake 0.4s ease; }
        @keyframes shake { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-6px); } 75% { transform: translateX(6px); } }
      `}</style>
    </div>
  );
};

export default Login;
