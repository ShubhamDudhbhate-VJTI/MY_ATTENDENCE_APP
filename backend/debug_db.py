import os
from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from main import DATABASE_URL, engine

print(f"Using Engine: {engine}")

try:
    with engine.connect() as conn:
        # Check Classrooms
        res = conn.execute(text("SELECT COUNT(*) FROM classrooms"))
        classroom_count = res.scalar()
        print(f"Classrooms count: {classroom_count}")

        # Check Subjects
        res = conn.execute(text("SELECT COUNT(*) FROM subjects"))
        subject_count = res.scalar()
        print(f"Subjects count: {subject_count}")

        # Check Users
        res = conn.execute(text("SELECT COUNT(*) FROM app_users"))
        user_count = res.scalar()
        print(f"Users count: {user_count}")

except Exception as e:
    print(f"Error checking DB: {e}")
