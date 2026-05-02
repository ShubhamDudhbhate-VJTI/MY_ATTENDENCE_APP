# =================================================================
# AttendX: AI-Powered Attendance System - Backend Core (FastAPI)
# Developed by: Shubham Dudhbhate (Backend & AI Lead)
# Version: 2.2 (Final Professional Submission)
#
# Description:
# This server manages the entire lifecycle of an attendance session:
# 1. User Authentication (Role-based access)
# 2. Dynamic Scheduling (Faculty-specific class templates)
# 3. Session Security (QR Token rotation + WiFi BSSID Geofencing)
# 4. AI Biometrics (DeepFace integration via Hugging Face/Local)
# 5. Real-time Notifications (FCM for class starts and alerts)
# =================================================================

from contextlib import asynccontextmanager
import os
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Body, UploadFile, File, Form, Depends, Response, BackgroundTasks
from fastapi.responses import JSONResponse, FileResponse
from pydantic import BaseModel
from typing import List, Optional, Dict
import uvicorn
import uuid
import time
from datetime import datetime, timedelta
from sqlalchemy import create_engine, Column, String, Integer, DateTime, ForeignKey, Boolean, Float, Text, Date, func, Numeric, LargeBinary, text, or_
from sqlalchemy.orm import sessionmaker, Session, declarative_base
from sqlalchemy.dialects.postgresql import UUID, BYTEA
import numpy as np
import requests
import tempfile
import json
import traceback
from fpdf import FPDF
import io


# Optional Local AI Import (used in OFFLINE_DEBUG_MODE)
try:
    from deepface import DeepFace
except ImportError:
    DeepFace = None

0
# --- CONFIGURATION SETTINGS ---

# OFFLINE_DEBUG_MODE: Set to True if running AI locally on a machine with GPU.
# Set to False to use the cloud-based Hugging Face AI pipeline (Scalable Production Mode).
OFFLINE_DEBUG_MODE = False

# Load environment variables (Supabase URL, HF Tokens, etc.)
load_dotenv()

# --- CLOUD AI CONFIG (Hugging Face) ---
# AttendX uses a dedicated AI Inference Space on Hugging Face for face embeddings and verification.
HF_API_URL = os.getenv("HF_API_URL")
HF_TOKEN = os.getenv("HF_TOKEN")

# --- DATABASE ARCHITECTURE ---
DATABASE_URL = os.getenv("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    # Fix for Heroku/Supabase style connection strings
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

def create_db_engine(url):
    """
    Creates the SQLAlchemy engine.
    Handles connection pooling and timeout settings for Supabase (PostgreSQL).
    """
    if not url or "sqlite" in url:
        return create_engine("sqlite:///./attendance.db", connect_args={"check_same_thread": False})
    return create_engine(
        url,
        pool_pre_ping=True,
        pool_recycle=300,
        connect_args={
            "connect_timeout": 15,
            "application_name": "AttendX_Backend"
        }
    )

# Establish Connection: Attempt Primary Cloud DB (Supabase), Fallback to Local SQLite
try:
    print(f"--- DATABASE CHECK: Attempting Supabase Connection ---")
    if not DATABASE_URL:
        raise ValueError("DATABASE_URL not found in environment variables")

    engine = create_db_engine(DATABASE_URL)
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
        print("--- SUCCESS: Connected to Supabase PostgreSQL ---")
except Exception as e:
    print(f"--- ERROR: Supabase Connection Failed. ---")
    print(f"Details: {str(e)}")
    print("--- FALLBACK: Using local SQLite (Dev Only) ---")
    engine = create_db_engine("sqlite:///./attendance.db")

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def run_migrations(engine):
    """
    Automatic Schema Migration:
    Ensures that columns added during development (like face_image or fcm_token)
    exist in the database without requiring manual SQL commands.
    """
    if "sqlite" in str(engine.url):
        with engine.begin() as conn:
            # Check classrooms table
            try: conn.execute(text("ALTER TABLE classrooms ADD COLUMN wifi_ssid TEXT"))
            except Exception: pass
            try: conn.execute(text("ALTER TABLE classrooms ADD COLUMN wifi_bssid TEXT"))
            except Exception: pass
            # Check schedules table
            try: conn.execute(text("ALTER TABLE schedules ADD COLUMN is_official BOOLEAN DEFAULT 1"))
            except Exception: pass
            # Check students table
            try: conn.execute(text("ALTER TABLE app_students ADD COLUMN face_image BLOB"))
            except Exception: pass
            try: conn.execute(text("ALTER TABLE app_students ADD COLUMN device_id TEXT"))
            except Exception: pass
            try: conn.execute(text("ALTER TABLE app_students ADD COLUMN department_id TEXT"))
            except Exception: pass

    with engine.begin() as conn:
        try: conn.execute(text("ALTER TABLE app_users ADD COLUMN fcm_token TEXT"))
        except Exception: pass

# Run migrations immediately on server start
run_migrations(engine)

# --- DATABASE MODELS (SQLAlchemy Schemas) ---

class User(Base):
    """Master User table for Login and Session management"""
    __tablename__ = "app_users"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String, unique=True)
    email = Column(String, unique=True)
    password_hash = Column(Text)
    full_name = Column(String)
    role = Column(String) # 'student' or 'faculty'
    fcm_token = Column(String, nullable=True)

class Student(Base):
    """Extends User with academic and biometric data"""
    __tablename__ = "app_students"
    id = Column(String, ForeignKey("app_users.id", ondelete="CASCADE"), primary_key=True)
    registration_number = Column(String, unique=True)
    full_name = Column(String)
    branch = Column(String, nullable=True)
    year = Column(String, nullable=True)
    face_embedding = Column(LargeBinary, nullable=True) # AI Vector
    face_image = Column(LargeBinary, nullable=True)     # Master image
    device_id = Column(String, nullable=True)
    department_id = Column(String, nullable=True)

class Teacher(Base):
    """Extends User with professional details"""
    __tablename__ = "app_teachers"
    id = Column(String, ForeignKey("app_users.id", ondelete="CASCADE"), primary_key=True)
    employee_id = Column(String, unique=True)
    full_name = Column(String)
    department_id = Column(String, nullable=True)
    branch = Column(String, nullable=True)
    designation = Column(String, nullable=True)
    qualification = Column(String, nullable=True)
    specialization = Column(String, nullable=True)
    phone = Column(String, nullable=True)

class Classroom(Base):
    """Physical Location metadata including WiFi BSSID for proximity verification"""
    __tablename__ = "classrooms"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String)
    wifi_ssid = Column(String, nullable=True)
    wifi_bssid = Column(String, nullable=True)

class Subject(Base):
    """Academic Course metadata"""
    __tablename__ = "subjects"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String)
    code = Column(String, unique=True, nullable=True)
    department_id = Column(String, nullable=True)
    branch = Column(String)
    year = Column(String)

class BranchSubject(Base):
    """Maps subjects to specific branches and years"""
    __tablename__ = "branch_subjects"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    branch = Column(String)
    year = Column(String)
    subject_id = Column(String, ForeignKey("subjects.id"))

class Enrollment(Base):
    """Handles students taking subjects outside their branch (Electives)"""
    __tablename__ = "enrollments"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    subject_id = Column(String, ForeignKey("subjects.id"))
    student_id = Column(String, ForeignKey("app_students.id"))

class FacultySubject(Base):
    """Direct mapping of teacher-subject assignment"""
    __tablename__ = "faculty_subjects"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    faculty_id = Column(String, ForeignKey("app_users.id"))
    subject_id = Column(String, ForeignKey("subjects.id"))

class Schedule(Base):
    """Weekly timetable entries"""
    __tablename__ = "schedules"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    subject_id = Column(String, ForeignKey("subjects.id"))
    classroom_id = Column(String, ForeignKey("classrooms.id"))
    faculty_id = Column(String, ForeignKey("app_users.id"))
    day_of_week = Column(String)
    start_time = Column(String)
    end_time = Column(String)
    is_official = Column(Boolean, default=True)

class AttendanceSession(Base):
    """A live instance of a class with a unique QR token and expiration"""
    __tablename__ = "attendance_sessions"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    subject_id = Column(String, ForeignKey("subjects.id"))
    faculty_id = Column(String, ForeignKey("app_users.id"))
    classroom_id = Column(String, ForeignKey("classrooms.id"))
    status = Column(String, default="active")
    qr_token = Column(Text)
    start_time = Column(DateTime, default=datetime.utcnow)
    qr_expires_at = Column(DateTime)

class AttendanceRecord(Base):
    """Log of a student being marked present"""
    __tablename__ = "attendance_records"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String, ForeignKey("attendance_sessions.id"))
    student_id = Column(String, ForeignKey("app_students.id"))
    status = Column(String, default="present")
    marked_at = Column(DateTime, default=datetime.utcnow)
    latitude = Column(Numeric(9, 6), nullable=True)
    longitude = Column(Numeric(9, 6), nullable=True)
    face_verified = Column(Boolean, default=False)
    wifi_bssid_matched = Column(String, nullable=True)

class Notification(Base):
    """Alerts for session starts and system messages"""
    __tablename__ = "notifications"
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("app_users.id"))
    title = Column(String)
    message = Column(Text)
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

# --- UTILITY FUNCTIONS ---

def create_notification(db: Session, user_id: str, title: str, message: str):
    """Adds notification to DB and prints log"""
    try:
        new_notif = Notification(id=str(uuid.uuid4()), user_id=user_id, title=title, message=message)
        db.add(new_notif)
        db.commit()
    except Exception as e:
        print(f"Error creating notification: {e}")
        db.rollback()

def get_db():
    """DB Session Dependency"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def is_valid(val):
    """Global helper to handle 'All', 'null', and 'undefined' strings from mobile clients consistently."""
    return val and val not in ["All", "null", "None", "undefined", "", "null\n"]

def clean_id(val: str) -> str:
    """Sanitize IDs from common formatting errors"""
    if not val: return val
    # Handle newlines and quotes that occasionally slip through from mobile/storage
    return str(val).replace('"', '').replace("'", "").replace('\n', '').strip()

# --- SERVER LIFESPAN & INITIAL SEEDING ---

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifecycle manager for the FastAPI application"""
    # No initial seeding for production - strictly using Supabase data
    print("--- AttendX Backend Started: Connected to Supabase ---")
    yield

Base.metadata.create_all(engine)
app = FastAPI(title="AttendX Core API", lifespan=lifespan)

# --- REQUEST MODELS (Pydantic) ---

class StartSessionRequest(BaseModel):
    faculty_id: str
    subject_id: str
    classroom_id: str
    duration_minutes: int = 45

class SessionResponse(BaseModel):
    session_id: str
    qr_token: str
    expires_at: str
    classroom_name: str

class VerifyWifiRequest(BaseModel):
    session_id: str
    bssid: str
    ssid: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None

# --- API ENDPOINTS ---

@app.get("/")
async def root():
    return {"status": "online", "system": "AttendX", "version": "2.2"}

# --- AUTHENTICATION ---

@app.post("/auth/login")
async def login(credentials: dict = Body(...), db: Session = Depends(get_db)):
    login_id = credentials.get("username")
    password = credentials.get("password")
    user = db.query(User).filter((User.email == login_id) | (User.username == login_id)).first()
    if user and user.password_hash == password:
        return {"success": True, "user_id": str(user.id), "role": user.role, "name": user.full_name}
    raise HTTPException(status_code=401, detail="Invalid credentials")

@app.post("/auth/signup")
async def signup(user_data: dict = Body(...), db: Session = Depends(get_db)):
    existing = db.query(User).filter((User.email == user_data.get("email")) | (User.username == user_data.get("username"))).first()
    if existing: raise HTTPException(status_code=400, detail="User already exists")

    new_id = str(user_data.get("id") or uuid.uuid4())
    user = User(id=new_id, username=user_data.get("username"), email=user_data.get("email"), password_hash=user_data.get("password"), full_name=user_data.get("full_name"), role=user_data.get("role"))
    db.add(user); db.flush()

    if user_data.get("role") == "student":
        db.add(Student(id=new_id, full_name=user.full_name, registration_number=user.username, branch=user_data.get("branch"), year=user_data.get("year")))
    else:
        db.add(Teacher(id=new_id, full_name=user.full_name, employee_id=user.username, branch=user_data.get("branch")))

    db.commit()
    return {"success": True, "user_id": new_id, "role": user.role}

@app.get("/auth/me/{user_id}")
async def get_user_profile(user_id: str, db: Session = Depends(get_db)):
    uid = clean_id(user_id)
    user = db.query(User).filter((User.id == uid) | (User.username == uid)).first()
    if not user: raise HTTPException(404, "User not found")

    profile = {"id": str(user.id), "username": user.username, "email": user.email, "full_name": user.full_name, "role": user.role, "academic": {}}
    if user.role == "student":
        s = db.query(Student).filter(Student.id == user.id).first()
        if s: profile["academic"] = {"branch": s.branch, "year": s.year, "reg_no": s.registration_number}
    else:
        t = db.query(Teacher).filter(Teacher.id == user.id).first()
        if t: profile["academic"] = {"branch": t.branch, "designation": t.designation, "employee_id": t.employee_id}
    return profile

# --- ACADEMIC DATA FETCHING ---

@app.get("/classrooms")
async def get_classrooms(db: Session = Depends(get_db)):
    return [{"id": str(r.id), "name": str(r.name or "Unknown Room"), "wifi_bssid": str(r.wifi_bssid or "")} for r in db.query(Classroom).all()]

@app.get("/subjects")
async def get_subjects(db: Session = Depends(get_db)):
    return [{"id": str(s.id), "name": str(s.name or "Unknown Subject"), "code": str(s.code or "")} for s in db.query(Subject).all()]

@app.get("/faculty/subjects/{faculty_id}")
async def get_faculty_subjects(faculty_id: str, db: Session = Depends(get_db)):
    fid = clean_id(faculty_id)
    # 1. Get subjects from FacultySubject assignments
    subjects = db.query(Subject).join(FacultySubject).filter(FacultySubject.faculty_id == fid).all()

    # 2. Get subjects from existing Schedule entries
    if not subjects:
        subjects = db.query(Subject).join(Schedule).filter(Schedule.faculty_id == fid).all()

    # 3. Fallback for new faculty: Get all subjects in their branch
    if not subjects:
        teacher = db.query(Teacher).filter(Teacher.id == fid).first()
        if teacher and teacher.branch:
            subjects = db.query(Subject).filter(Subject.branch == teacher.branch).all()

    # 4. Global Fallback: If still nothing, return all subjects so they can choose
    if not subjects:
        subjects = db.query(Subject).all()

    results = []
    seen = set()
    for s in subjects:
        if s.id not in seen:
            results.append({
                "id": str(s.id),
                "name": str(s.name or "Unknown"),
                "code": str(s.code or ""),
                "branch": str(s.branch or ""),
                "year": str(s.year or "")
            })
            seen.add(s.id)
    return results

@app.get("/student/subjects/{student_id}")
async def get_student_subjects(student_id: str, db: Session = Depends(get_db)):
    sid = clean_id(student_id)
    student = db.query(Student).filter(Student.id == sid).first()
    if not student: raise HTTPException(404, "Student not found")

    branch_subs = db.query(Subject).filter(Subject.branch == student.branch, Subject.year == student.year).all()
    minor_subs = db.query(Subject).join(Enrollment).filter(Enrollment.student_id == sid).all()
    return [{"id": str(s.id), "name": str(s.name or "Unknown"), "code": str(s.code or "")} for s in set(branch_subs + minor_subs)]

# --- TIMETABLE & SCHEDULING ---

@app.get("/faculty/schedule/{faculty_id}")
async def get_faculty_schedule(faculty_id: str, day: Optional[str] = None, db: Session = Depends(get_db)):
    fid = clean_id(faculty_id)
    query = db.query(Schedule, Subject, Classroom).join(Subject).join(Classroom).filter(Schedule.faculty_id == fid)
    if day: query = query.filter(Schedule.day_of_week == day)
    return [{
        "id": str(s.id), "day": s.day_of_week, "subject": sub.name, "subject_id": str(sub.id),
        "room": c.name, "classroom_id": str(c.id), "time": f"{s.start_time} - {s.end_time}", "is_official": s.is_official
    } for s, sub, c in query.all()]

@app.post("/faculty/schedule/{faculty_id}")
async def add_schedule_record(faculty_id: str, record: dict = Body(...), db: Session = Depends(get_db)):
    fid = clean_id(faculty_id)
    time_parts = record.get("time", "09:00 - 10:00").split(" - ")
    new_s = Schedule(id=str(uuid.uuid4()), faculty_id=fid, subject_id=record.get("subject_id"), classroom_id=record.get("classroom_id"),
                     day_of_week=record.get("day"), start_time=time_parts[0], end_time=time_parts[1] if len(time_parts)>1 else "10:00", is_official=False)
    db.add(new_s); db.commit(); db.refresh(new_s)
    sub = db.query(Subject).filter(Subject.id == new_s.subject_id).first()
    room = db.query(Classroom).filter(Classroom.id == new_s.classroom_id).first()
    return {"id": str(new_s.id), "day": new_s.day_of_week, "subject": sub.name if sub else "Unknown", "room": room.name if room else "Unknown", "time": f"{new_s.start_time} - {new_s.end_time}"}

@app.delete("/faculty/schedule/{record_id}")
async def delete_schedule_record(record_id: str, db: Session = Depends(get_db)):
    s = db.query(Schedule).filter(Schedule.id == clean_id(record_id)).first()
    if s: db.delete(s); db.commit(); return {"success": True}
    raise HTTPException(404, "Record not found")

@app.get("/student/schedule/{student_id}")
async def get_student_schedule(student_id: str, day: Optional[str] = None, db: Session = Depends(get_db)):
    sid = clean_id(student_id)
    student = db.query(Student).filter(Student.id == sid).first()
    if not student: return []
    sub_ids = [s[0] for s in db.query(Subject.id).filter(Subject.branch == student.branch, Subject.year == student.year).all()]
    minor_ids = [s[0] for s in db.query(Enrollment.subject_id).filter(Enrollment.student_id == sid).all()]
    all_sub_ids = list(set(sub_ids + minor_ids))
    query = db.query(Schedule, Subject, Classroom).join(Subject).join(Classroom).filter(Schedule.subject_id.in_(all_sub_ids))
    if day: query = query.filter(Schedule.day_of_week == day)
    return [{"day": s.day_of_week, "subject": sub.name, "room": c.name, "time": f"{s.start_time} - {s.end_time}"} for s, sub, c in query.all()]

# --- SESSION & ATTENDANCE CORE ---

@app.post("/sessions/start", response_model=SessionResponse)
async def start_session(req: StartSessionRequest, background_tasks: BackgroundTasks, db: Session = Depends(get_db)):
    classroom = db.query(Classroom).filter(Classroom.id == req.classroom_id).first()
    sid = str(uuid.uuid4()); qr_token = f"QR_{uuid.uuid4().hex[:8].upper()}"
    expiry = datetime.utcnow() + timedelta(minutes=req.duration_minutes)
    session = AttendanceSession(id=sid, faculty_id=req.faculty_id, subject_id=req.subject_id, classroom_id=req.classroom_id,
                                qr_token=qr_token, qr_expires_at=expiry, status="active")
    db.add(session); db.commit()
    background_tasks.add_task(send_session_notifications, req.faculty_id, req.subject_id, classroom.name, sid)
    return SessionResponse(session_id=sid, qr_token=qr_token, expires_at=expiry.isoformat(), classroom_name=classroom.name)

def send_session_notifications(faculty_id: str, subject_id: str, room_name: str, session_id: str):
    db = SessionLocal()
    try:
        subject = db.query(Subject).filter(Subject.id == subject_id).first()
        create_notification(db, faculty_id, "Session Started", f"Session for {subject.name} started in {room_name}.")
        students = db.query(Student).filter(Student.branch == subject.branch, Student.year == subject.year).all()
        for s in students: create_notification(db, s.id, "Class Started", f"{subject.name} has started in {room_name}.")
    finally: db.close()

@app.post("/sessions/stop/{session_id}")
async def stop_session(session_id: str, db: Session = Depends(get_db)):
    sid = clean_id(session_id)
    session = db.query(AttendanceSession, Subject).join(Subject).filter(AttendanceSession.id == sid).first()
    if not session: raise HTTPException(404, "Session not found")
    session_obj, sub_obj = session
    session_obj.status = "stopped"
    db.commit()

    records = db.query(AttendanceRecord, Student).join(Student).filter(AttendanceRecord.session_id == sid).all()
    count = len(records)

    student_list = []
    for rec, stu in records:
        student_list.append({
            "id": str(stu.id),
            "name": str(stu.full_name or "Unknown Student"),
            "time": rec.marked_at.isoformat() if rec.marked_at else datetime.utcnow().isoformat()
        })

    create_notification(db, session_obj.faculty_id, "Session Summary", f"Session for {sub_obj.name} closed. Present: {count}.")

    return {
        "session_id": sid,
        "total_present": count,
        "students": student_list,
        "course_id": str(sub_obj.name or "Unknown Subject")
    }

@app.get("/faculty/sessions/{faculty_id}")
async def get_faculty_sessions(faculty_id: str, subject_id: Optional[str] = None, classroom_id: Optional[str] = None, date: Optional[str] = None, db: Session = Depends(get_db)):
    fid = clean_id(faculty_id)

    # Performance Optimization: Subquery for student counts to avoid N+1 queries
    count_subquery = db.query(
        AttendanceRecord.session_id,
        func.count(AttendanceRecord.id).label('student_count')
    ).group_by(AttendanceRecord.session_id).subquery()

    # Base query with Outer Joins for resilience
    query = db.query(
        AttendanceSession,
        Subject,
        func.coalesce(count_subquery.c.student_count, 0).label('student_count')
    ).outerjoin(Subject, AttendanceSession.subject_id == Subject.id)\
     .outerjoin(count_subquery, AttendanceSession.id == count_subquery.c.session_id)\
     .filter(AttendanceSession.faculty_id == fid)

    if subject_id:
        query = query.filter(AttendanceSession.subject_id == subject_id)
    if classroom_id:
        query = query.filter(AttendanceSession.classroom_id == classroom_id)
    if date:
        try:
            target_date = datetime.strptime(date, '%Y-%m-%d').date()
            query = query.filter(func.date(AttendanceSession.start_time) == target_date)
        except Exception: pass

    sessions = query.order_by(AttendanceSession.start_time.desc()).limit(100).all()

    results = []
    for sess, sub, count in sessions:
        results.append({
            "session_id": str(sess.id),
            "subject_id": str(sub.id) if sub else (str(sess.subject_id) if sess.subject_id else "N/A"),
            "subject_name": str(sub.name or "Unknown Subject") if sub else "Unknown Subject",
            "classroom_id": str(sess.classroom_id or "N/A"),
            "start_time": sess.start_time.isoformat() if sess.start_time else datetime.utcnow().isoformat(),
            "expires_at": sess.qr_expires_at.isoformat() if sess.qr_expires_at else None,
            "status": str(sess.status or "active"),
            "student_count": int(count)
        })
    return results

@app.get("/sessions/{session_id}/details")
async def get_session_details(session_id: str, db: Session = Depends(get_db)):
    sid = clean_id(session_id)
    session_data = db.query(AttendanceSession, Subject).join(Subject).filter(AttendanceSession.id == sid).first()
    if not session_data:
        raise HTTPException(404, "Session not found")

    sess, sub = session_data
    records = db.query(AttendanceRecord, Student).join(Student).filter(AttendanceRecord.session_id == sid).all()

    student_details = []
    for rec, stu in records:
        student_details.append({
            "student_id": str(stu.id),
            "student_name": str(stu.full_name or "Unknown Student"),
            "marked_at": rec.marked_at.isoformat() if rec.marked_at else sess.start_time.isoformat(),
            "status": str(rec.status or "present")
        })

    return {
        "session_id": sid,
        "subject_name": str(sub.name or "Unknown Subject"),
        "start_time": sess.start_time.isoformat(),
        "total_students": len(records),
        "students": student_details
    }

# --- THE 3-LAYER VERIFICATION PIPELINE ---

@app.post("/attendance/verify-wifi")
async def verify_wifi(req: VerifyWifiRequest, db: Session = Depends(get_db)):
    """LAYER 1: Proximity Verification via WiFi BSSID"""
    session = db.query(AttendanceSession).filter(AttendanceSession.id == clean_id(req.session_id)).first()
    if not session: raise HTTPException(404, "Session not found")
    classroom = db.query(Classroom).filter(Classroom.id == session.classroom_id).first()

    client_bssid = (req.bssid or "").strip().lower()
    target_bssid = (classroom.wifi_bssid or "").strip().lower()
    if client_bssid == target_bssid or not target_bssid: return {"success": True, "message": "WiFi Verified"}
    raise HTTPException(403, "Location verification failed: WiFi BSSID mismatch")

@app.post("/attendance/verify-qr")
async def verify_qr(req: dict = Body(...), db: Session = Depends(get_db)):
    """LAYER 2: Dynamic Token Validation"""
    sid = clean_id(req.get("session_id", "")); token = req.get("token", "").strip()
    session = db.query(AttendanceSession).filter(AttendanceSession.id == sid).first()
    if session and (session.qr_token == token): return {"success": True}
    return {"success": False, "message": "Invalid or Expired QR Token"}

@app.post("/attendance/verify-face")
async def verify_face(student_id: str = Form(...), session_id: str = Form(...), image: Optional[UploadFile] = File(None), db: Session = Depends(get_db)):
    """LAYER 3: AI Biometric Authentication"""
    try:
        sid = clean_id(student_id); sess_id = clean_id(session_id)
        student = db.query(Student).filter((Student.id == sid) | (Student.registration_number == sid)).first()
        if not image: raise HTTPException(400, "No face captured")
        img_bytes = await image.read()

        is_verified = False; msg = "Attendance marked!"
        if not student.face_embedding:
            # First Time: Extract & Store Embedding
            await image.seek(0)
            resp = requests.post(f"{HF_API_URL}/represent", files={"image": (image.filename, await image.read(), image.content_type)},
                                 headers={"Authorization": f"Bearer {HF_TOKEN}"}, timeout=60)
            if resp.status_code == 200:
                student.face_embedding = json.dumps(resp.json()["embedding"]).encode('utf-8')
                student.face_image = img_bytes; db.commit(); is_verified = True; msg = "Face Registered!"
        else:
            # Subsequent Times: Verify Identity
            await image.seek(0)
            resp = requests.post(f"{HF_API_URL}/verify", files={"image": (image.filename, await image.read(), image.content_type)},
                                 data={"stored_embedding": student.face_embedding.decode('utf-8')},
                                 headers={"Authorization": f"Bearer {HF_TOKEN}"}, timeout=60)
            if resp.status_code == 200 and resp.json().get("is_match"): is_verified = True; msg = "Face Verified!"
            else: raise HTTPException(401, "Biometric Mismatch")

        # Log Record
        if not db.query(AttendanceRecord).filter(AttendanceRecord.session_id == sess_id, AttendanceRecord.student_id == student.id).first():
            db.add(AttendanceRecord(id=str(uuid.uuid4()), session_id=sess_id, student_id=student.id, face_verified=is_verified))
            db.commit()
            create_notification(db, student.id, "Attendance Marked", "Marked present via biometrics.")
        return {"success": True, "message": msg}
    except Exception as e:
        db.rollback(); raise HTTPException(500, f"AI Error: {str(e)}")

@app.post("/attendance/manual")
async def manual_attendance(req: dict = Body(...), db: Session = Depends(get_db)):
    """LAYER 4: Manual Override (Faculty Authorized)"""
    try:
        sid = clean_id(req.get("session_id", ""))
        student_query = req.get("student_id", "").strip()

        # Resolve student by ID or Reg No
        student = db.query(Student).filter((Student.id == student_query) | (Student.registration_number == student_query)).first()
        if not student:
            raise HTTPException(404, f"Student '{student_query}' not found")

        session = db.query(AttendanceSession).filter(AttendanceSession.id == sid).first()
        if not session:
            raise HTTPException(404, "Session not found")

        # Check for duplicate
        existing = db.query(AttendanceRecord).filter(AttendanceRecord.session_id == sid, AttendanceRecord.student_id == student.id).first()
        if existing:
            return {"success": True, "message": "Attendance already marked"}

        # Record Manual Attendance
        db.add(AttendanceRecord(
            id=str(uuid.uuid4()),
            session_id=sid,
            student_id=student.id,
            status="present",
            face_verified=False
        ))
        db.commit()

        create_notification(db, student.id, "Manual Attendance", f"Marked present manually by Faculty.")
        return {"success": True, "message": f"Attendance marked for {student.full_name}"}
    except HTTPException:
        raise
    except Exception as e:
        db.rollback()
        raise HTTPException(500, f"Error marking manual attendance: {str(e)}")

# --- REPORTING & UTILITIES ---

@app.get("/sessions/{session_id}/attendance")
async def get_attendance(session_id: str, db: Session = Depends(get_db)):
    sid = clean_id(session_id)
    records = db.query(AttendanceRecord, Student).join(Student).filter(AttendanceRecord.session_id == sid).all()
    return {
        "total_count": len(records),
        "students": [
            {
                "student_id": str(r.student_id),
                "student_name": str(s.full_name or "Unknown Student"),
                "timestamp": r.marked_at.isoformat() if r.marked_at else datetime.utcnow().isoformat(),
                "face_verified": bool(r.face_verified)
            } for r, s in records
        ]
    }

@app.get("/student/attendance/{student_id}")
async def get_student_history(student_id: str, db: Session = Depends(get_db)):
    sid = clean_id(student_id)
    print(f"DEBUG: Fetching history for student_id={sid}")
    student = db.query(Student).filter(Student.id == sid).first()
    if not student:
        print(f"DEBUG: Student {sid} not found")
        return []

    # 1. Get subjects from branch/year
    branch_subs = [s[0] for s in db.query(Subject.id).filter(
        Subject.branch == student.branch,
        Subject.year == student.year
    ).all()]

    # 2. Get subjects from enrollments (Electives)
    enrolled_subs = [e[0] for e in db.query(Enrollment.subject_id).filter(
        Enrollment.student_id == sid
    ).all()]

    all_sub_ids = list(set(branch_subs + enrolled_subs))
    print(f"DEBUG: Found {len(all_sub_ids)} relevant subjects for student {sid}")

    # Fetch sessions for these subjects
    sessions = db.query(AttendanceSession, Subject).join(Subject).filter(
        AttendanceSession.subject_id.in_(all_sub_ids)
    ).order_by(AttendanceSession.start_time.desc()).all()

    # Get attendance records for this student
    records = {r.session_id: r for r in db.query(AttendanceRecord).filter(
        AttendanceRecord.student_id == sid
    ).all()}

    print(f"DEBUG: Found {len(sessions)} sessions and {len(records)} attendance records")

    return [
        {
            "subject_id": str(sub.id),
            "subject_name": str(sub.name or "Unknown Subject"),
            "session_id": str(s.id),
            "timestamp": (records[s.id].marked_at if s.id in records else s.start_time).isoformat(),
            "status": "present" if s.id in records else "absent"
        } for s, sub in sessions
    ]

@app.post("/auth/update-fcm")
async def update_fcm_token(data: dict = Body(...), db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == clean_id(data.get("user_id"))).first()
    if user: user.fcm_token = data.get("fcm_token"); db.commit(); return {"success": True}
    raise HTTPException(404, "User not found")

@app.get("/faces/{student_id}.jpg")
async def get_student_face(student_id: str, db: Session = Depends(get_db)):
    student = db.query(Student).filter((Student.registration_number == student_id) | (Student.id == student_id)).first()
    if student and student.face_image: return Response(content=student.face_image, media_type="image/jpeg")
    raise HTTPException(404, "Face not found")

@app.get("/notifications/{user_id}")
async def get_notifications(user_id: str, db: Session = Depends(get_db)):
    notifs = db.query(Notification).filter(Notification.user_id == user_id).order_by(Notification.created_at.desc()).all()
    return [{"id": str(n.id), "title": n.title, "message": n.message, "is_read": n.is_read, "created_at": n.created_at.isoformat()} for n in notifs]

@app.post("/notifications/read/{notification_id}")
async def mark_read(notification_id: str, db: Session = Depends(get_db)):
    n = db.query(Notification).filter(Notification.id == notification_id).first()
    if n: n.is_read = True; db.commit(); return {"success": True}
    raise HTTPException(404, "Notfound")

@app.post("/notifications/delete/{notification_id}")
async def delete_notification(notification_id: str, db: Session = Depends(get_db)):
    n = db.query(Notification).filter(Notification.id == notification_id).first()
    if n: db.delete(n); db.commit(); return {"success": True}
    raise HTTPException(404, "Notfound")

@app.post("/notifications/clear/{user_id}")
async def clear_notifications(user_id: str, db: Session = Depends(get_db)):
    db.query(Notification).filter(Notification.user_id == user_id).delete()
    db.commit()
    return {"success": True, "message": "All notifications cleared"}

@app.delete("/sessions/{session_id}")
async def delete_session(session_id: str, db: Session = Depends(get_db)):
    db.query(AttendanceRecord).filter(AttendanceRecord.session_id == session_id).delete()
    session = db.query(AttendanceSession).filter(AttendanceSession.id == session_id).first()
    if session:
        db.delete(session)
        db.commit()
        return {"success": True}
    raise HTTPException(404, "Session not found")

@app.post("/sessions/clear/{faculty_id}")
async def clear_faculty_sessions(faculty_id: str, db: Session = Depends(get_db)):
    sessions = db.query(AttendanceSession).filter(AttendanceSession.faculty_id == faculty_id).all()
    session_ids = [s.id for s in sessions]
    if session_ids:
        db.query(AttendanceRecord).filter(AttendanceRecord.session_id.in_(session_ids)).delete(synchronize_session=False)
        db.query(AttendanceSession).filter(AttendanceSession.id.in_(session_ids)).delete(synchronize_session=False)
        db.commit()
    return {"success": True, "message": f"Cleared {len(session_ids)} sessions"}

# --- ADVANCED PDF REPORTING SYSTEM ---

# --- ADVANCED PDF REPORTING SYSTEM (Enterprise Edition) ---

class PDFReport(FPDF):
    def header(self):
        # Branding
        self.set_fill_color(33, 150, 243) # Material Blue
        self.rect(0, 0, 210, 40, 'F')

        # VJTI Logo
        # Correct path for D:\AndroidProjects\DBMS_Shubham_Application\vjti.jpg
        # The main.py is in backend/, so we go up one level.
        logo_path = os.path.join(os.path.dirname(__file__), "..", "vjti.jpg")
        if os.path.exists(logo_path):
            try:
                self.image(logo_path, 170, 5, 25) # Positioned on the right
            except Exception as e:
                print(f"Logo error: {e}")

        self.set_text_color(255, 255, 255)
        self.set_font('helvetica', 'B', 24)
        self.cell(0, 10, 'AttendX: Academic Audit', 0, 1, 'L')

        self.set_font('helvetica', 'I', 10)
        self.cell(0, 5, 'Official Departmental Attendance & Performance Documentation', 0, 1, 'L')
        self.ln(20)

    def footer(self):
        self.set_y(-25)
        self.set_font('helvetica', 'I', 8)
        self.set_text_color(100, 100, 100)
        self.cell(0, 10, 'This is a computer-generated report. No physical signature required.', 0, 1, 'C')
        self.cell(0, 10, f'Page {self.page_no()} | Generated by AttendX AI System', 0, 0, 'C')

    def chapter_title(self, title, color=(33, 150, 243)):
        self.set_font('helvetica', 'B', 12)
        self.set_text_color(*color)
        self.cell(0, 10, title.upper(), 0, 1, 'L')
        self.set_draw_color(*color)
        self.line(10, self.get_y(), 200, self.get_y())
        self.ln(5)

@app.get("/reports/pdf/{session_id}")
async def export_session_pdf(session_id: str, student_id: Optional[str] = None, db: Session = Depends(get_db)):
    """Generates a professional PDF for a single attendance session using Live Supabase Data"""
    sid = clean_id(session_id)
    # Use Outer Join for Classroom and User to handle NULL values in Supabase
    session_data = db.query(AttendanceSession, Subject, Classroom, User)\
        .join(Subject, AttendanceSession.subject_id == Subject.id)\
        .outerjoin(Classroom, AttendanceSession.classroom_id == Classroom.id)\
        .outerjoin(User, AttendanceSession.faculty_id == User.id)\
        .filter(AttendanceSession.id == sid).first()

    if not session_data:
        raise HTTPException(404, "Session data not found in Supabase. Check if subject or session exists.")

    sess, sub, room, user_obj = session_data

    records_query = db.query(AttendanceRecord, Student).join(Student).filter(AttendanceRecord.session_id == sid)
    if student_id and student_id != "All":
        records_query = records_query.filter(or_(Student.id == student_id, Student.registration_number == student_id))

    records = records_query.all()

    pdf = PDFReport()
    pdf.add_page()

    pdf.chapter_title('Session Overview')
    pdf.set_font('helvetica', '', 10)
    pdf.cell(100, 7, f"Subject: {sub.name if sub else 'Unknown'} ({sub.code if sub else 'N/A'})", 0, 0)
    pdf.cell(0, 7, f"Faculty: {user_obj.full_name if user_obj else 'System User'}", 0, 1)
    pdf.cell(100, 7, f"Location: {room.name if room else 'Remote/Not Specified'}", 0, 0)
    pdf.cell(0, 7, f"Date/Time: {sess.start_time.strftime('%Y-%m-%d %H:%M') if sess.start_time else 'N/A'}", 0, 1)
    pdf.ln(10)

    pdf.chapter_title('Attendance Register')
    pdf.set_font('helvetica', 'B', 10); pdf.set_fill_color(33, 150, 243); pdf.set_text_color(255,255,255)
    pdf.cell(60, 10, 'Reg No', 1, 0, 'C', 1)
    pdf.cell(100, 10, 'Student Name', 1, 0, 'C', 1)
    pdf.cell(30, 10, 'Status', 1, 1, 'C', 1)

    pdf.set_font('helvetica', '', 10); pdf.set_text_color(0,0,0)
    for rec, stu in records:
        pdf.cell(60, 10, str(stu.registration_number), 1)
        pdf.cell(100, 10, str(stu.full_name), 1)
        pdf.cell(30, 10, 'PRESENT', 1, 1, 'C')

    pdf_output = pdf.output()
    if isinstance(pdf_output, (bytearray, bytes)):
        return Response(content=bytes(pdf_output), media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename=Session_Report_{sid[:8]}.pdf"})
    else:
        return Response(content=str(pdf_output).encode('utf-8'), media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename=Session_Report_{sid[:8]}.pdf"})

@app.get("/reports/bulk-pdf")
async def export_bulk_pdf(
    faculty_id: str,
    branch: Optional[str] = "All",
    year: Optional[str] = "All",
    subject_id: Optional[str] = "All",
    student_id: Optional[str] = "All",
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Generates a professional consolidated PDF report for Faculty"""
    fid = clean_id(faculty_id)
    # Start with a subquery or join to ensure we have Subject info for filtering
    query = db.query(AttendanceSession).join(Subject, AttendanceSession.subject_id == Subject.id).filter(AttendanceSession.faculty_id == fid)

    if is_valid(branch):
        query = query.filter(Subject.branch.ilike(f"%{branch}%"))
    if is_valid(year):
        query = query.filter(Subject.year.ilike(f"%{year}%"))
    if is_valid(subject_id):
        query = query.filter(AttendanceSession.subject_id == clean_id(subject_id))

    if start_date:
        try: query = query.filter(AttendanceSession.start_time >= datetime.fromisoformat(start_date.replace('Z', '+00:00')))
        except: pass
    if end_date:
        try: query = query.filter(AttendanceSession.start_time <= datetime.fromisoformat(end_date.replace('Z', '+00:00')))
        except: pass

    # Subqueries to avoid parameter limits (1000+) in SQL 'IN' clauses
    session_id_sub = query.with_entities(AttendanceSession.id)
    subject_id_sub = query.with_entities(AttendanceSession.subject_id).distinct()

    # We need to check if any sessions exist before proceeding
    # Using a count query is more efficient than fetching all
    total_sess = db.query(func.count(AttendanceSession.id)).filter(AttendanceSession.id.in_(session_id_sub)).scalar()

    if total_sess == 0:
        print(f"DEBUG: No sessions found for faculty_id={fid}, branch={branch}, year={year}, subject_id={subject_id}")
        raise HTTPException(status_code=404, detail="No attendance sessions found for the selected filters.")

    print(f"DEBUG: Found {total_sess} sessions for bulk report")

    # REFINED STUDENT SCOPING (Optimized for Large Data):
    # Find relevant student population
    # 1. Students who attended at least one filtered session
    attended_q = db.query(AttendanceRecord.student_id).filter(AttendanceRecord.session_id.in_(session_id_sub))
    # 2. Students enrolled in any of the filtered subjects (Electives)
    enrolled_q = db.query(Enrollment.student_id).filter(Enrollment.subject_id.in_(subject_id_sub))

    # 3. Main Audience (Students in same branch/year as subjects)
    subject_info = db.query(Subject.branch, Subject.year).filter(Subject.id.in_(subject_id_sub)).distinct().all()

    target_q = attended_q.union(enrolled_q)
    if subject_info:
        audience_conds = [((Student.branch == br) & (Student.year == yr)) for br, yr in subject_info]
        audience_q = db.query(Student.id).filter(or_(*audience_conds))
        target_q = target_q.union(audience_q)

    # FINAL AGGREGATED QUERY
    results_query = db.query(
        Student.registration_number,
        Student.full_name,
        func.count(AttendanceRecord.id)
    ).outerjoin(AttendanceRecord, (Student.id == AttendanceRecord.student_id) & (AttendanceRecord.session_id.in_(session_id_sub)))\
     .filter(Student.id.in_(target_q))

    if is_valid(student_id):
        results_query = results_query.filter(or_(Student.id == student_id, Student.registration_number == student_id))

    results = results_query.group_by(Student.registration_number, Student.full_name)\
     .order_by(Student.registration_number).all()

    pdf = PDFReport()
    pdf.add_page()

    # Advanced Summary Section
    pdf.chapter_title('Faculty Consolidation & Audit Report')
    pdf.set_font('helvetica', '', 10)

    # Calculate High-Level Metrics
    total_students = len(results)
    avg_attendance = sum([(att/total_sess)*100 for _, _, att in results]) / total_students if total_students > 0 else 0
    defaulters = [r for r in results if (r[2]/total_sess)*100 < 75]

    # Summary Grid
    pdf.set_fill_color(245, 247, 249)
    pdf.rect(10, pdf.get_y(), 190, 30, 'F')
    pdf.set_xy(15, pdf.get_y() + 5)

    pdf.set_font('helvetica', 'B', 11); pdf.set_text_color(33, 150, 243)
    pdf.cell(60, 10, "OVERALL STATISTICS", 0, 1)

    pdf.set_font('helvetica', '', 10); pdf.set_text_color(50, 50, 50)
    pdf.cell(45, 8, f"Sessions: {total_sess}", 0)
    pdf.cell(45, 8, f"Avg. Attendance: {avg_attendance:.1f}%", 0)
    pdf.cell(45, 8, f"Students: {total_students}", 0)
    pdf.set_text_color(200, 0, 0)
    pdf.cell(45, 8, f"Defaulters: {len(defaulters)}", 0, 1)
    pdf.ln(10)

    # Attendance Distribution Chart (Simple Bar Chart)
    pdf.set_font('helvetica', 'B', 11); pdf.set_text_color(33, 150, 243)
    pdf.cell(0, 10, "ATTENDANCE DISTRIBUTION", 0, 1)

    # Basic logic to draw distribution
    ranges = [0, 0, 0, 0] # <50, 50-75, 75-90, >90
    for _, _, att in results:
        p = (att/total_sess)*100
        if p < 50: ranges[0]+=1
        elif p < 75: ranges[1]+=1
        elif p < 90: ranges[2]+=1
        else: ranges[3]+=1

    max_range = max(ranges) if max(ranges) > 0 else 1
    chart_y = pdf.get_y()
    labels = ["<50%", "50-75%", "75-90%", ">90%"]
    colors = [(244, 67, 54), (255, 152, 0), (76, 175, 80), (33, 150, 243)]

    for i, count in enumerate(ranges):
        bar_width = (count / max_range) * 120
        pdf.set_fill_color(*colors[i])
        pdf.rect(50, chart_y + (i*7), bar_width, 5, 'F')
        pdf.set_xy(10, chart_y + (i*7))
        pdf.set_font('helvetica', '', 8); pdf.set_text_color(0,0,0)
        pdf.cell(40, 5, f"{labels[i]} ({count})", 0, 0, 'R')

    pdf.set_xy(10, chart_y + 30)
    pdf.ln(5)

    # Detailed Student Table
    pdf.chapter_title('Student Performance Matrix')
    pdf.set_font('helvetica', 'B', 10); pdf.set_fill_color(33, 150, 243); pdf.set_text_color(255, 255, 255)
    pdf.cell(35, 10, 'Reg No', 1, 0, 'C', 1)
    pdf.cell(85, 10, 'Student Name', 1, 0, 'C', 1)
    pdf.cell(35, 10, 'Classes', 1, 0, 'C', 1)
    pdf.cell(35, 10, 'Status', 1, 1, 'C', 1)

    pdf.set_font('helvetica', '', 10); pdf.set_text_color(0,0,0)
    for reg, name, att in results:
        perc = (att / total_sess) * 100
        status_text = "ELIGIBLE" if perc >= 75 else "DEFAULTER"

        if perc < 75:
            pdf.set_fill_color(255, 235, 238); pdf.set_text_color(200, 0, 0)
        else:
            pdf.set_fill_color(255,255,255); pdf.set_text_color(0,0,0)

        pdf.cell(35, 10, str(reg), 1, 0, 'C', 1)
        pdf.cell(85, 10, f" {name}", 1, 0, 'L', 1)
        pdf.cell(35, 10, f"{att}/{total_sess}", 1, 0, 'C', 1)

        # Color coding status text
        if perc < 75: pdf.set_font('helvetica', 'B', 9)
        pdf.cell(35, 10, f"{perc:.1f}% ({status_text})", 1, 1, 'C', 1)
        pdf.set_font('helvetica', '', 10)

    pdf_output = pdf.output()
    if isinstance(pdf_output, (bytearray, bytes)):
        return Response(content=bytes(pdf_output), media_type="application/pdf", headers={"Content-Disposition": "attachment; filename=Faculty_Bulk_Report.pdf"})
    else:
        return Response(content=str(pdf_output).encode('utf-8'), media_type="application/pdf", headers={"Content-Disposition": "attachment; filename=Faculty_Bulk_Report.pdf"})

@app.get("/reports/summary")
async def get_reports_summary(
    faculty_id: Optional[str] = None,
    department_id: Optional[str] = None,
    branch: Optional[str] = "All",
    year: Optional[str] = "All",
    subject_id: Optional[str] = "All",
    student_id: Optional[str] = "All",
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Returns a quick summary (counts) of sessions and students matching filters."""
    query = db.query(AttendanceSession).join(Subject, AttendanceSession.subject_id == Subject.id)

    if is_valid(faculty_id):
        fid = clean_id(faculty_id)
        query = query.filter(AttendanceSession.faculty_id == fid)

    if is_valid(department_id):
        did = clean_id(department_id)
        teacher_lookup = db.query(Teacher).filter(Teacher.id == did).first()
        actual_dept = teacher_lookup.department_id or teacher_lookup.branch if teacher_lookup else did
        query = query.filter((Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%")))

    if is_valid(branch):
        query = query.filter(Subject.branch.ilike(f"%{branch}%"))
    if is_valid(year):
        query = query.filter(Subject.year.ilike(f"%{year}%"))
    if is_valid(subject_id):
        query = query.filter(AttendanceSession.subject_id == clean_id(subject_id))

    if start_date:
        try: query = query.filter(AttendanceSession.start_time >= datetime.fromisoformat(start_date.replace('Z', '+00:00')))
        except: pass
    if end_date:
        try: query = query.filter(AttendanceSession.start_time <= datetime.fromisoformat(end_date.replace('Z', '+00:00')))
        except: pass

    session_ids = [s.id for s in query.with_entities(AttendanceSession.id).all()]
    total_sessions = len(session_ids)

    if total_sessions == 0:
        return {"total_sessions": 0, "total_students": 0}

    # Count unique students who attended these sessions
    student_count = db.query(AttendanceRecord.student_id).filter(AttendanceRecord.session_id.in_(session_ids)).distinct().count()

    # If student_id is provided, check if that specific student exists in these sessions
    if is_valid(student_id):
        is_present = db.query(AttendanceRecord).filter(
            AttendanceRecord.session_id.in_(session_ids),
            or_(AttendanceRecord.student_id == student_id)
        ).first() is not None
        student_count = 1 if is_present else 0

    return {
        "total_sessions": total_sessions,
        "total_students": student_count
    }

@app.get("/analytics/department/{dept_id}")
async def get_department_analytics(dept_id: str, db: Session = Depends(get_db)):
    # 1. Resolve Department from HOD/Teacher ID
    uid = clean_id(dept_id)
    teacher = db.query(Teacher).filter(Teacher.id == uid).first()
    actual_dept = teacher.department_id or teacher.branch if teacher else uid

    # 2. Base Stats with Lenient Matching
    sessions = db.query(AttendanceSession).join(Subject).filter(
        (Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%"))
    ).all()
    session_ids = [s.id for s in sessions]

    total_faculty = db.query(Teacher).filter(
        (Teacher.department_id.ilike(f"%{actual_dept}%")) | (Teacher.branch.ilike(f"%{actual_dept}%"))
    ).count()

    total_students = db.query(Student).filter(
        (Student.department_id.ilike(f"%{actual_dept}%")) | (Student.branch.ilike(f"%{actual_dept}%"))
    ).count()

    # 3. Calculate Performance Metrics
    if session_ids and total_students > 0:
        total_attendance_recs = db.query(AttendanceRecord).filter(AttendanceRecord.session_id.in_(session_ids)).count()
        # Max possible attendance = sessions * students
        avg_perc = (total_attendance_recs / (len(session_ids) * total_students)) * 100
        avg_attendance_str = f"{avg_perc:.1f}%"

        # Count Defaulters (<75% attendance)
        # Note: We group by student_id to get individual counts across filtered sessions
        student_counts = db.query(
            Student.id, func.count(AttendanceRecord.id)
        ).outerjoin(AttendanceRecord, (Student.id == AttendanceRecord.student_id) & (AttendanceRecord.session_id.in_(session_ids)))\
         .filter((Student.department_id == actual_dept) | (Student.branch == actual_dept))\
         .group_by(Student.id).all()

        defaulter_count = 0
        for sid, count in student_counts:
            if (count / len(session_ids)) * 100 < 75:
                defaulter_count += 1
    else:
        avg_attendance_str = "0.0%"
        defaulter_count = 0

    # 4. Real-Time Monthly Trends
    # We aggregate attendance percentages by month for the current department
    trends = []
    from sqlalchemy import extract
    for i in range(5, -1, -1):
        # Calculate date for 'i' months ago
        target_date = datetime.utcnow() - timedelta(days=i*30)
        month_num = target_date.month
        year_num = target_date.year
        month_name = target_date.strftime("%b")

        month_sessions = db.query(AttendanceSession).join(Subject).filter(
            ((Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%"))),
            extract('month', AttendanceSession.start_time) == month_num,
            extract('year', AttendanceSession.start_time) == year_num
        ).all()

        if not month_sessions:
            trends.append({"month": month_name, "value": 0})
            continue

        m_session_ids = [s.id for s in month_sessions]
        m_attendance = db.query(AttendanceRecord).filter(AttendanceRecord.session_id.in_(m_session_ids)).count()
        m_possible = len(m_session_ids) * (total_students or 1)
        m_perc = min(100, int((m_attendance / m_possible) * 100))
        trends.append({"month": month_name, "value": m_perc})

    return {
        "avg_attendance": avg_attendance_str,
        "total_classes": len(sessions),
        "defaulter_count": max(0, defaulter_count),
        "total_faculty": total_faculty,
        "total_students": total_students,
        "trends": trends
    }

@app.get("/analytics/faculty/{faculty_id}")
async def get_faculty_analytics(faculty_id: str, db: Session = Depends(get_db)):
    """Faculty specific analytics for their own reports screen - Live Supabase Data"""
    fid = clean_id(faculty_id)
    # Get all sessions for this faculty (active or stopped)
    sessions = db.query(AttendanceSession).filter(AttendanceSession.faculty_id == fid).all()
    session_ids = [s.id for s in sessions]

    if not session_ids:
        return {"avg_attendance": "0%", "total_classes": 0, "defaulter_count": 0, "trends": []}

    # 1. Resolve the "Target Audience" for this faculty's subjects
    subject_ids = list(set([s.subject_id for s in sessions]))
    target_info = db.query(Subject.branch, Subject.year).filter(Subject.id.in_(subject_ids)).distinct().all()

    audience_student_ids = set()
    for br, yr in target_info:
        # Using ILike for robustness across Supabase naming variations
        ids = [s[0] for s in db.query(Student.id).filter(Student.branch.ilike(f"%{br}%"), Student.year.ilike(f"%{yr}%")).all()]
        audience_student_ids.update(ids)

    # Also include students who actually attended (catches electives/late enrollees)
    actual_attendees = [r[0] for r in db.query(AttendanceRecord.student_id).filter(AttendanceRecord.session_id.in_(session_ids)).distinct().all()]
    audience_student_ids.update(actual_attendees)

    total_potential_students = len(audience_student_ids)

    # 2. Calculate real avg attendance %
    total_records = db.query(AttendanceRecord).filter(AttendanceRecord.session_id.in_(session_ids)).count()
    max_possible = len(session_ids) * (total_potential_students or 1)
    avg_perc = (total_records / max_possible) * 100

    # 3. Calculate Defaulters (Students with < 75% in this faculty's classes)
    defaulter_count = 0
    if total_potential_students > 0:
        # Grouped query for efficiency
        att_counts = db.query(
            AttendanceRecord.student_id, func.count(AttendanceRecord.id)
        ).filter(AttendanceRecord.session_id.in_(session_ids)).group_by(AttendanceRecord.student_id).all()

        att_map = {sid: count for sid, count in att_counts}
        for sid in audience_student_ids:
            s_count = att_map.get(sid, 0)
            if (s_count / len(session_ids)) * 100 < 75:
                defaulter_count += 1

    # 4. Faculty Trends (Last 3 months)
    trends = []
    from sqlalchemy import extract
    for i in range(2, -1, -1):
        target_date = datetime.utcnow() - timedelta(days=i*30)
        m_name = target_date.strftime("%b")
        m_sessions = [s.id for s in sessions if s.start_time.month == target_date.month and s.start_time.year == target_date.year]

        if not m_sessions:
            trends.append({"month": m_name, "value": 0})
        else:
            m_rec_count = db.query(AttendanceRecord).filter(AttendanceRecord.session_id.in_(m_sessions)).count()
            m_poss = len(m_sessions) * (total_potential_students or 1)
            trends.append({"month": m_name, "value": min(100, int((m_rec_count / m_poss) * 100))})

    return {
        "avg_attendance": f"{avg_perc:.1f}%",
        "total_classes": len(sessions),
        "defaulter_count": defaulter_count,
        "trends": trends
    }

@app.get("/reports/hod-master-pdf")
async def export_hod_master_pdf(
    department_id: str,
    faculty_id: Optional[str] = "All",
    branch: Optional[str] = "All",
    year: Optional[str] = "All",
    subject_id: Optional[str] = "All",
    student_id: Optional[str] = "All",
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """HOD Level: High-Standard Departmental Audit with Advanced Filtering & Null Handling"""
    did = clean_id(department_id)

    # 1. Resolve Department
    teacher_lookup = db.query(Teacher).filter(Teacher.id == did).first()
    actual_dept = teacher_lookup.department_id or teacher_lookup.branch if teacher_lookup else did

    # 2. Gather Sessions
    query = db.query(AttendanceSession).join(Subject, AttendanceSession.subject_id == Subject.id).outerjoin(User, AttendanceSession.faculty_id == User.id)

    # Base filter: Must match department or branch
    query = query.filter((Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%")))

    # Apply valid filters
    if is_valid(faculty_id):
        fid = clean_id(faculty_id)
        query = query.filter((AttendanceSession.faculty_id == fid) | (User.username.ilike(f"%{fid}%")) | (User.full_name.ilike(f"%{fid}%")))

    if is_valid(subject_id):
        query = query.filter(AttendanceSession.subject_id == clean_id(subject_id))

    if is_valid(branch):
        query = query.filter(Subject.branch.ilike(f"%{branch}%"))

    if is_valid(year):
        y_val = year.lower()
        if "second" in y_val or "se" in y_val: y_patterns = ["%Second%", "%SE%", "%2nd%"]
        elif "first" in y_val or "fe" in y_val: y_patterns = ["%First%", "%FE%", "%1st%"]
        elif "third" in y_val or "te" in y_val: y_patterns = ["%Third%", "%TE%", "%3rd%"]
        elif "final" in y_val or "be" in y_val or "fourth" in y_val: y_patterns = ["%Final%", "%BE%", "%4th%", "%Fourth%"]
        else: y_patterns = [f"%{year}%"]
        query = query.filter(or_(*[Subject.year.ilike(p) for p in y_patterns]))

    if start_date:
        try: query = query.filter(AttendanceSession.start_time >= datetime.fromisoformat(start_date.replace('Z', '+00:00')))
        except: pass
    if end_date:
        try: query = query.filter(AttendanceSession.start_time <= datetime.fromisoformat(end_date.replace('Z', '+00:00')))
        except: pass

    # Subqueries for optimized child queries
    session_id_sub = query.with_entities(AttendanceSession.id)
    subject_id_sub = query.with_entities(AttendanceSession.subject_id).distinct()

    # We need session data for report headers/faculty matrix
    # Using a subquery in IN is safe
    sessions_full = db.query(AttendanceSession, Subject, User).join(Subject).outerjoin(User, AttendanceSession.faculty_id == User.id).filter(AttendanceSession.id.in_(session_id_sub)).all()

    if not sessions_full:
        msg = f"No sessions found for {actual_dept}"
        if is_valid(branch): msg += f", Branch: {branch}"
        if is_valid(year): msg += f", Year: {year}"
        if is_valid(faculty_id): msg += f", Faculty: {faculty_id}"
        raise HTTPException(status_code=404, detail=msg)

    total_sess = len(sessions_full)

    # 3. Define the Student Audit Scope
    student_query = db.query(Student).filter(
        or_(
            Student.department_id.ilike(f"%{actual_dept}%"),
            Student.branch.ilike(f"%{actual_dept}%")
        )
    )
    if is_valid(branch):
        student_query = student_query.filter(Student.branch.ilike(f"%{branch}%"))
    if is_valid(year):
        y_val = year.lower()
        if "second" in y_val or "se" in y_val: y_patterns = ["%Second%", "%SE%", "%2nd%"]
        elif "first" in y_val or "fe" in y_val: y_patterns = ["%First%", "%FE%", "%1st%"]
        elif "third" in y_val or "te" in y_val: y_patterns = ["%Third%", "%TE%", "%3rd%"]
        elif "final" in y_val or "be" in y_val or "fourth" in y_val: y_patterns = ["%Final%", "%BE%", "%4th%", "%Fourth%"]
        else: y_patterns = [f"%{year}%"]
        student_query = student_query.filter(or_(*[Student.year.ilike(p) for p in y_patterns]))

    dept_student_ids_q = student_query.with_entities(Student.id)
    attended_student_ids_q = db.query(AttendanceRecord.student_id).filter(AttendanceRecord.session_id.in_(session_id_sub)).distinct()
    target_student_id_sub = dept_student_ids_q.union(attended_student_ids_q)

    # 4. Aggregate Performance
    results_query = db.query(
        Student.registration_number,
        Student.full_name,
        Student.branch,
        func.count(AttendanceRecord.id).label('attended_count')
    ).outerjoin(AttendanceRecord, (Student.id == AttendanceRecord.student_id) & (AttendanceRecord.session_id.in_(session_id_sub)))\
     .filter(Student.id.in_(target_student_id_sub))

    if is_valid(student_id):
        results_query = results_query.filter(or_(Student.id == student_id, Student.registration_number == student_id))

    student_stats = results_query.group_by(Student.registration_number, Student.full_name, Student.branch)\
     .order_by(Student.registration_number).all()

    if not student_stats:
        raise HTTPException(status_code=404, detail="No students found matching these criteria.")

    # AI Stats
    ai_verified = db.query(func.count(AttendanceRecord.id)).filter(
        AttendanceRecord.session_id.in_(session_id_sub),
        AttendanceRecord.face_verified == True
    ).scalar() or 0
    total_recs = db.query(func.count(AttendanceRecord.id)).filter(
        AttendanceRecord.session_id.in_(session_id_sub)
    ).scalar() or 0
    ai_accuracy = (ai_verified / total_recs * 100) if total_recs > 0 else 0

    # 5. Build Elite PDF
    pdf = PDFReport()
    pdf.add_page()

    # Executive Summary
    pdf.chapter_title('Departmental Executive Summary')
    pdf.set_font('helvetica', '', 10)

    col_w = 45
    pdf.cell(col_w, 10, 'Target Dept:', 0); pdf.set_font('helvetica', 'B', 10); pdf.cell(col_w, 10, str(actual_dept), 0)
    pdf.set_font('helvetica', '', 10); pdf.cell(col_w, 10, 'Total Sessions:', 0); pdf.set_font('helvetica', 'B', 10); pdf.cell(col_w, 10, str(total_sess), 0, 1)

    pdf.set_font('helvetica', '', 10); pdf.cell(col_w, 10, 'AI Confidence:', 0); pdf.set_font('helvetica', 'B', 10); pdf.cell(col_w, 10, f"{ai_accuracy:.1f}% Match", 0)
    pdf.set_font('helvetica', '', 10); pdf.cell(col_w, 10, 'Audit Period:', 0); pdf.set_font('helvetica', 'B', 10); pdf.cell(col_w, 10, f"{start_date or 'N/A'} - {end_date or 'Today'}", 0, 1)
    pdf.ln(10)

    # Faculty Performance
    pdf.chapter_title('Faculty Engagement Matrix')
    pdf.set_font('helvetica', 'B', 9); pdf.set_fill_color(240, 240, 240)
    pdf.cell(80, 8, 'Faculty Name', 1, 0, 'L', 1)
    pdf.cell(60, 8, 'Subject', 1, 0, 'L', 1)
    pdf.cell(50, 8, 'Classes Conducted', 1, 1, 'C', 1)

    pdf.set_font('helvetica', '', 9)
    fac_data = {}
    for s, sub, user in sessions_full:
        name = user.full_name if user else "Unknown Faculty"
        key = (name, sub.name)
        fac_data[key] = fac_data.get(key, 0) + 1

    for (name, sub_name), count in fac_data.items():
        pdf.cell(80, 8, name, 1)
        pdf.cell(60, 8, sub_name, 1)
        pdf.cell(50, 8, str(count), 1, 1, 'C')
    pdf.ln(10)

    # Student Compliance
    pdf.chapter_title('Student Academic Compliance Audit')
    pdf.set_font('helvetica', 'B', 9); pdf.set_fill_color(33, 150, 243); pdf.set_text_color(255, 255, 255)
    pdf.cell(35, 10, 'Reg No', 1, 0, 'C', 1)
    pdf.cell(80, 10, 'Student Name', 1, 0, 'C', 1)
    pdf.cell(40, 10, 'Attendance %', 1, 0, 'C', 1)
    pdf.cell(35, 10, 'Status', 1, 1, 'C', 1)

    pdf.set_text_color(0,0,0); pdf.set_font('helvetica', '', 9)
    for reg, name, br, att in student_stats:
        perc = (att / total_sess) * 100
        is_defaulter = perc < 75
        status = "DEFAULTER" if is_defaulter else "COMPLIANT"

        if is_defaulter:
            pdf.set_fill_color(255, 235, 238)
            pdf.set_text_color(200, 0, 0)
        else:
            pdf.set_fill_color(255, 255, 255)
            pdf.set_text_color(0, 0, 0)

        pdf.cell(35, 8, str(reg), 1, 0, 'C', 1)
        pdf.cell(80, 8, f" {name}", 1, 0, 'L', 1)
        pdf.cell(40, 8, f"{perc:.1f}%", 1, 0, 'C', 1)

        if is_defaulter: pdf.set_font('helvetica', 'B', 9)
        pdf.cell(35, 8, status, 1, 1, 'C', 1)
        pdf.set_font('helvetica', '', 9); pdf.set_text_color(0,0,0)

    pdf_output = pdf.output(dest='S')
    filename = f"HOD_Audit_{actual_dept}_{datetime.now().strftime('%Y%m%d')}.pdf"
    if isinstance(pdf_output, (bytearray, bytes)):
        return Response(content=bytes(pdf_output), media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename={filename}"})
    else:
        return Response(content=pdf_output.encode('latin-1'), media_type="application/pdf", headers={"Content-Disposition": f"attachment; filename={filename}"})

@app.get("/reports/hod-master-excel")
async def export_hod_master_excel(
    department_id: str,
    faculty_id: Optional[str] = "All",
    branch: Optional[str] = "All",
    year: Optional[str] = "All",
    subject_id: Optional[str] = "All",
    student_id: Optional[str] = "All",
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Exports HOD Master Audit data as CSV (Excel compatible) with BOM and Full Filtering"""
    import io, csv
    did = clean_id(department_id)

    def is_valid(val):
        return val and val not in ["All", "null", "None", "undefined", ""]

    # 1. Resolve Department
    teacher_lookup = db.query(Teacher).filter(Teacher.id == did).first()
    actual_dept = teacher_lookup.department_id or teacher_lookup.branch if teacher_lookup else did

    # 2. Gather Sessions
    query = db.query(AttendanceSession).join(Subject, AttendanceSession.subject_id == Subject.id).outerjoin(User, AttendanceSession.faculty_id == User.id)

    # Base filter: Must match department or branch
    query = query.filter((Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%")))

    # Apply valid filters
    if is_valid(faculty_id):
        fid = clean_id(faculty_id)
        query = query.filter((AttendanceSession.faculty_id == fid) | (User.username.ilike(f"%{fid}%")) | (User.full_name.ilike(f"%{fid}%")))

    if is_valid(subject_id):
        query = query.filter(AttendanceSession.subject_id == clean_id(subject_id))

    if is_valid(branch):
        query = query.filter(Subject.branch.ilike(f"%{branch}%"))

    if is_valid(year):
        y_val = year.lower()
        if "second" in y_val or "se" in y_val: y_patterns = ["%Second%", "%SE%", "%2nd%"]
        elif "first" in y_val or "fe" in y_val: y_patterns = ["%First%", "%FE%", "%1st%"]
        elif "third" in y_val or "te" in y_val: y_patterns = ["%Third%", "%TE%", "%3rd%"]
        elif "final" in y_val or "be" in y_val or "fourth" in y_val: y_patterns = ["%Final%", "%BE%", "%4th%", "%Fourth%"]
        else: y_patterns = [f"%{year}%"]
        query = query.filter(or_(*[Subject.year.ilike(p) for p in y_patterns]))

    if start_date:
        try: query = query.filter(AttendanceSession.start_time >= datetime.fromisoformat(start_date.replace('Z', '+00:00')))
        except: pass
    if end_date:
        try: query = query.filter(AttendanceSession.start_time <= datetime.fromisoformat(end_date.replace('Z', '+00:00')))
        except: pass

    # Subqueries for optimized child queries
    session_id_sub = query.with_entities(AttendanceSession.id)

    # Count sessions
    total_sess = db.query(func.count(AttendanceSession.id)).filter(AttendanceSession.id.in_(session_id_sub)).scalar()

    if total_sess == 0:
        raise HTTPException(status_code=404, detail="No data found matching these filters.")

    # 3. Define the Student Audit Scope
    student_query = db.query(Student).filter((Student.department_id.ilike(f"%{actual_dept}%")) | (Student.branch.ilike(f"%{actual_dept}%")))
    if is_valid(branch):
        student_query = student_query.filter(Student.branch.ilike(f"%{branch}%"))
    if is_valid(year):
        y_val = year.lower()
        if "second" in y_val or "se" in y_val: y_patterns = ["%Second%", "%SE%", "%2nd%"]
        elif "first" in y_val or "fe" in y_val: y_patterns = ["%First%", "%FE%", "%1st%"]
        elif "third" in y_val or "te" in y_val: y_patterns = ["%Third%", "%TE%", "%3rd%"]
        elif "final" in y_val or "be" in y_val or "fourth" in y_val: y_patterns = ["%Final%", "%BE%", "%4th%", "%Fourth%"]
        else: y_patterns = [f"%{year}%"]
        student_query = student_query.filter(or_(*[Student.year.ilike(p) for p in y_patterns]))

    dept_student_ids_q = student_query.with_entities(Student.id)
    attended_student_ids_q = db.query(AttendanceRecord.student_id).filter(AttendanceRecord.session_id.in_(session_id_sub)).distinct()
    target_student_id_sub = dept_student_ids_q.union(attended_student_ids_q)

    # 4. Aggregate Performance
    results_query = db.query(
        Student.registration_number,
        Student.full_name,
        Student.branch,
        func.count(AttendanceRecord.id)
    ).outerjoin(AttendanceRecord, (Student.id == AttendanceRecord.student_id) & (AttendanceRecord.session_id.in_(session_id_sub)))\
     .filter(Student.id.in_(target_student_id_sub))

    if is_valid(student_id):
        results_query = results_query.filter(or_(Student.id == student_id, Student.registration_number == student_id))

    stats = results_query.group_by(Student.registration_number, Student.full_name, Student.branch)\
     .order_by(Student.registration_number).all()

    # Create CSV with BOM for Excel compatibility
    output = io.StringIO()
    output.write('\ufeff') # UTF-8 BOM
    writer = csv.writer(output)
    writer.writerow(["Registration No", "Full Name", "Branch", "Classes Attended", "Total Classes", "Percentage", "Status"])

    for reg, name, br, att in stats:
        perc = (att/total_sess)*100 if total_sess > 0 else 0
        status = "ELIGIBLE" if perc >= 75 else "DEFAULTER"
        writer.writerow([reg, name, br, att, total_sess, f"{perc:.1f}%", status])

    filename = f"HOD_Master_Audit_{actual_dept}.csv"
    return Response(
        content=output.getvalue(),
        media_type="text/csv",
        headers={
            "Content-Disposition": f"attachment; filename={filename}",
            "Cache-Control": "no-cache"
        }
    )

@app.get("/analytics/department/{dept_id}/export")
async def export_department_excel(dept_id: str, db: Session = Depends(get_db)):
    """Exports departmental raw data to CSV for Excel with Real Supabase Data"""
    import io, csv
    uid = clean_id(dept_id)
    teacher = db.query(Teacher).filter(Teacher.id == uid).first()
    actual_dept = teacher.department_id or teacher.branch if teacher else uid

    # Fetch real data
    sessions = db.query(AttendanceSession).join(Subject).filter(
        (Subject.department_id.ilike(f"%{actual_dept}%")) | (Subject.branch.ilike(f"%{actual_dept}%"))
    ).all()

    session_ids = [s.id for s in sessions]

    students = db.query(Student).filter(
        (Student.department_id.ilike(f"%{actual_dept}%")) | (Student.branch.ilike(f"%{actual_dept}%"))
    ).all()

    output = io.StringIO()
    output.write('\ufeff') # BOM for Excel
    writer = csv.writer(output)
    writer.writerow(["Registration No", "Student Name", "Branch", "Year", "Attendance %", "Status"])

    if session_ids:
        for s in students:
            count = db.query(AttendanceRecord).filter(
                AttendanceRecord.student_id == s.id,
                AttendanceRecord.session_id.in_(session_ids)
            ).count()
            perc = (count / len(session_ids)) * 100
            status = "Compliant" if perc >= 75 else "DEFAULTER"
            writer.writerow([s.registration_number, s.full_name, s.branch, s.year, f"{perc:.1f}%", status])

    return Response(
        content=output.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": f"attachment; filename=Dept_{actual_dept}_Report.csv"}
    )

@app.post("/faculty/schedule/{faculty_id}/sync-official")
async def sync_official_schedule(faculty_id: str, day: Optional[str] = None, db: Session = Depends(get_db)):
    """Copies official schedule entries to user's personalized schedule"""
    fid = clean_id(faculty_id)
    teacher = db.query(Teacher).filter(Teacher.id == fid).first()

    # 1. Identify relevant subjects (assigned or in teacher's branch)
    sub_query = db.query(Subject.id)
    if teacher and teacher.branch:
        sub_query = sub_query.filter(Subject.branch == teacher.branch)

    assigned_sub_ids = [s[0] for s in db.query(FacultySubject.subject_id).filter(FacultySubject.faculty_id == fid).all()]
    relevant_sub_ids = list(set([s[0] for s in sub_query.all()] + assigned_sub_ids))

    # 2. Find Official schedules for these subjects that aren't already assigned to this faculty
    official_query = db.query(Schedule).filter(
        Schedule.is_official == True,
        Schedule.subject_id.in_(relevant_sub_ids)
    )
    if day:
        official_query = official_query.filter(Schedule.day_of_week == day)

    official_schedules = official_query.all()

    # 3. Assign this faculty to official schedules if they match
    # Or copy them if they are 'master' records
    sync_count = 0
    for s in official_schedules:
        # If it's an official template with no faculty or wrong faculty, we 'claim' it for the sync
        # In a more complex system, we'd copy it. Here we just ensure the link exists.
        if s.faculty_id != fid:
            # We create a PERSONAL copy of the official schedule
            existing = db.query(Schedule).filter(
                Schedule.faculty_id == fid,
                Schedule.subject_id == s.subject_id,
                Schedule.day_of_week == s.day_of_week,
                Schedule.start_time == s.start_time
            ).first()

            if not existing:
                new_s = Schedule(
                    id=str(uuid.uuid4()),
                    subject_id=s.subject_id,
                    classroom_id=s.classroom_id,
                    faculty_id=fid,
                    day_of_week=s.day_of_week,
                    start_time=s.start_time,
                    end_time=s.end_time,
                    is_official=True # Keep it marked as official source
                )
                db.add(new_s)
                sync_count += 1

    db.commit()

    # 4. Return updated schedule
    query = db.query(Schedule, Subject, Classroom).join(Subject).join(Classroom).filter(Schedule.faculty_id == fid)
    if day: query = query.filter(Schedule.day_of_week == day)

    records = []
    for s, sub, c in query.all():
        records.append({
            "id": str(s.id), "day": s.day_of_week, "subject": sub.name, "subject_id": str(sub.id),
            "room": c.name, "classroom_id": str(c.id), "time": f"{s.start_time} - {s.end_time}", "is_official": s.is_official
        })

    return {
        "date": datetime.utcnow().strftime("%Y-%m-%d"),
        "day": day or "All Days",
        "synced": sync_count,
        "schedule": records
    }

@app.put("/faculty/schedule/{record_id}")
async def update_schedule_record(record_id: str, record: dict = Body(...), db: Session = Depends(get_db)):
    rid = clean_id(record_id)
    s = db.query(Schedule).filter(Schedule.id == rid).first()
    if not s: raise HTTPException(404, "Record not found")

    if "subject_id" in record: s.subject_id = record["subject_id"]
    if "classroom_id" in record: s.classroom_id = record["classroom_id"]
    if "day" in record: s.day_of_week = record["day"]
    if "time" in record:
        parts = record["time"].split(" - ")
        s.start_time = parts[0]
        if len(parts) > 1: s.end_time = parts[1]

    db.commit(); db.refresh(s)
    sub = db.query(Subject).filter(Subject.id == s.subject_id).first()
    room = db.query(Classroom).filter(Classroom.id == s.classroom_id).first()
    return {
        "id": str(s.id), "day": s.day_of_week, "subject": sub.name if sub else "Unknown",
        "room": room.name if room else "Unknown", "time": f"{s.start_time} - {s.end_time}"
    }

@app.get("/faculty/all")
async def get_all_faculty(db: Session = Depends(get_db)):
    """Returns a list of all faculty members with full profile for HOD views"""
    results = db.query(User, Teacher).join(Teacher, User.id == Teacher.id).filter(User.role == "faculty").all()

    profiles = []
    for user, teacher in results:
        profiles.append({
            "id": str(user.id),
            "username": str(user.username),
            "email": str(user.email or f"{user.username}@vjti.ac.in"),
            "full_name": str(user.full_name),
            "role": "faculty",
            "academic": {
                "branch": str(teacher.branch or ""),
                "designation": str(teacher.designation or ""),
                "employee_id": str(teacher.employee_id or "")
            }
        })
    return profiles

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
