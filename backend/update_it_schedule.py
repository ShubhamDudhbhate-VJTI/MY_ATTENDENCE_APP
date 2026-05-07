import os
import uuid
from sqlalchemy import create_engine, text
from main import DATABASE_URL, SessionLocal, User, Teacher, Subject, Classroom, Schedule

def update_schedule():
    db = SessionLocal()
    branch = "Information Technology"
    year = "Second Year"

    # 1. Ensure Subjects exist for SY IT according to the timetable image
    it_subjects = {
        "MDM II: AIML - Introduction to Data Analytics": "MDM2",
        "Automata Theory": "AT",
        "Artificial Intelligence": "AI",
        "Database System": "DS",
        "Computer Networks": "CN",
        "Environmental Science": "ES",
        "Artificial Intelligence Lab": "AI_LAB",
        "Database System Lab": "DS_LAB",
        "Computer Networks Lab": "CN_LAB",
        "Linux administration lab": "LA_LAB",
        "Community Engineering Project": "CEP"
    }

    sub_map = {}
    for name, code in it_subjects.items():
        sub = db.query(Subject).filter(Subject.name == name).first()
        if not sub:
            sub = Subject(id=str(uuid.uuid4()), name=name, code=code, branch=branch, year=year, department_id="2")
            db.add(sub)
            db.commit()
            db.refresh(sub)
        else:
            sub.branch = branch
            sub.year = year
            sub.code = code
            db.commit()
        sub_map[code] = sub.id

    # 2. Ensure Faculty exist based on the image acronyms
    # Acronyms: VA(VVA), PSS, SSU, VBN, PDK, SS, SK
    faculty_data = [
        ("VBNIKAM", "Dr. V. B. Nikam", "vbnikam@it.academic.edu"),
        ("SSUDMALE", "Dr. S. S. Udmale", "ssudmale@it.academic.edu"),
        ("PDKOKARE", "Prof. Pooja D. Kokare", "pdkokare@ce.academic.edu"),
        ("VAWATI", "Prof. Vedashree V. Awati", "vawati@ce.academic.edu"),
        ("PSSSHINDE", "Prof. Prachi S. Shinde", "psshinde@ce.academic.edu"),
        ("SSONAWANE", "Prof. Sachin Sonawane", "ssonawane@ce.academic.edu"),
        ("SKATARIA", "Prof. Saksham Kataria", "skataria@it.academic.edu"),
    ]

    fac_map = {}
    for fid, name, email in faculty_data:
        # Check by email or username first to avoid unique constraint violations
        username = email.split('@')[0]
        user = db.query(User).filter((User.username == username) | (User.email == email)).first()

        if not user:
            user = User(
                id=fid,
                username=username,
                email=email,
                password_hash=fid.capitalize()+"@123",
                full_name=name,
                role="faculty"
            )
            db.add(user)
            db.commit()
            db.refresh(user)
        else:
            # Update existing user to match the ID we want if possible, or just use their ID
            fid = user.id

        fac_map[fid] = fid

        # Ensure Teacher entry exists
        teacher = db.query(Teacher).filter(Teacher.id == fid).first()
        if not teacher:
            teacher = Teacher(id=fid, employee_id=fid, full_name=name, branch=branch)
            db.add(teacher)
            db.commit()

    # 3. Ensure Classrooms exist
    rooms = ["AL 207", "AL 201", "AL 001", "Lab 2A", "Lab 2B", "Lab 2C", "Lab 3A", "Lab 3B"]
    room_map = {}
    for rname in rooms:
        room = db.query(Classroom).filter(Classroom.name == rname).first()
        if not room:
            room = Classroom(id=str(uuid.uuid4()), name=rname, wifi_ssid="Academic_Campus_WiFi")
            db.add(room)
            db.commit()
            db.refresh(room)
        room_map[rname] = room.id

    # 4. Clear all existing schedules (as per user request "only take subject for update scedule of IT class")
    # Using the subjects we mapped for SY IT.
    db.query(Schedule).filter(Schedule.subject_id.in_(sub_map.values())).delete(synchronize_session=False)
    db.commit()

    # 5. Insert New Schedule from image
    # Format: (Day, Start, End, SubCode, RoomName, FacultyAcronym)
    # Acronym Mapping for the entries:
    acro_to_id = {
        "VA": "VAWATI",
        "PSS": "PSSSHINDE",
        "SSU": "SSUDMALE",
        "VBN": "VBNIKAM",
        "PDK": "PDKOKARE",
        "SS": "SSONAWANE",
        "SK": "SKATARIA"
    }

    sched_entries = [
        # Monday
        ("Monday", "08:30", "09:25", "MDM2", "AL 207", "VA"),
        ("Monday", "09:30", "10:25", "CN", "AL 207", "PSS"),
        ("Monday", "10:30", "11:25", "AI", "AL 207", "SSU"),
        ("Monday", "11:30", "12:25", "DS", "AL 207", "VBN"),
        ("Monday", "13:30", "15:25", "AI_LAB", "Lab 2A", "SSU"),
        ("Monday", "13:30", "15:25", "DS_LAB", "Lab 2B", "VBN"),
        ("Monday", "13:30", "15:25", "CN_LAB", "Lab 2C", "PSS"),
        ("Monday", "13:30", "15:25", "LA_LAB", "Lab 3A", "PDK"),

        # Tuesday
        ("Tuesday", "08:30", "09:25", "MDM2", "AL 207", "VA"),
        ("Tuesday", "09:30", "10:25", "CN", "AL 207", "PSS"),
        ("Tuesday", "10:30", "11:25", "DS", "AL 207", "VBN"),
        ("Tuesday", "11:30", "12:25", "DS", "AL 207", "VBN"),
        ("Tuesday", "13:30", "15:25", "AI_LAB", "Lab 2A", "SSU"),
        ("Tuesday", "13:30", "15:25", "DS_LAB", "Lab 2B", "VBN"),
        ("Tuesday", "13:30", "15:25", "CN_LAB", "Lab 2C", "PSS"),
        ("Tuesday", "13:30", "15:25", "LA_LAB", "Lab 3A", "PDK"),
        ("Tuesday", "15:30", "16:25", "AT", "AL 201", "PDK"),

        # Wednesday
        ("Wednesday", "09:30", "10:25", "AT", "AL 207", "PDK"),
        ("Wednesday", "10:30", "11:25", "AT", "AL 207", "PDK"),
        ("Wednesday", "11:30", "12:25", "CN", "AL 207", "PSS"),
        ("Wednesday", "13:30", "15:25", "AI_LAB", "Lab 2A", "SSU"),
        ("Wednesday", "13:30", "15:25", "DS_LAB", "Lab 2B", "VBN"),
        ("Wednesday", "13:30", "15:25", "CN_LAB", "Lab 2C", "PSS"),
        ("Wednesday", "13:30", "15:25", "LA_LAB", "Lab 3B", "PDK"),
        ("Wednesday", "15:30", "16:25", "ES", "AL 001", "SS"),

        # Thursday
        ("Thursday", "08:30", "09:25", "ES", "AL 207", "SS"),
        ("Thursday", "09:30", "10:25", "AI", "AL 207", "SSU"),
        ("Thursday", "10:30", "11:25", "AI", "AL 207", "SSU"),
        ("Thursday", "11:30", "12:25", "AT", "AL 207", "PDK"),
        ("Thursday", "13:30", "15:25", "AI_LAB", "Lab 2A", "SSU"),
        ("Thursday", "13:30", "15:25", "DS_LAB", "Lab 2B", "VBN"),
        ("Thursday", "13:30", "15:25", "CN_LAB", "Lab 2C", "PSS"),
        ("Thursday", "13:30", "15:25", "LA_LAB", "Lab 3B", "PDK"),

        # Friday
        ("Friday", "08:30", "12:25", "CEP", "Lab 2B", "SK"),
        ("Friday", "13:30", "17:25", "CEP", "Lab 2C", "SK"),
    ]

    for day, start, end, scode, rname, fac_acro in sched_entries:
        fid = acro_to_id.get(fac_acro)
        # Verify user still exists with this ID after potential name mapping
        user = db.query(User).filter(User.id == fid).first()
        if not user:
            # Fallback if the user was found by username and had a different ID
            username = faculty_data[[f[0] for f in faculty_data].index(fid)][2].split('@')[0] if fid in [f[0] for f in faculty_data] else ""
            user = db.query(User).filter(User.username == username).first()
            if user: fid = user.id

        new_sched = Schedule(
            id=str(uuid.uuid4()),
            subject_id=sub_map[scode],
            classroom_id=room_map[rname],
            faculty_id=fid,
            day_of_week=day,
            start_time=start,
            end_time=end,
            is_official=True
        )
        db.add(new_sched)

    db.commit()
    print("SUCCESS: S.Y. BTech (Information Technology) Schedule updated accurately from timetable image.")

if __name__ == "__main__":
    update_schedule()
