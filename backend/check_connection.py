import os
from dotenv import load_dotenv
from sqlalchemy import create_engine, text

load_dotenv()
url = os.getenv("DATABASE_URL")

print(f"Checking connection to: {url.split('@')[-1] if url else 'NONE'}")

try:
    engine = create_engine(url)
    with engine.connect() as conn:
        # Test 1: Basic Ping
        conn.execute(text("SELECT 1"))
        print("✅ SUCCESS: Database connection established!")

        # Test 2: Check Tables
        result = conn.execute(text("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"))
        tables = [row[0] for row in result]
        print(f"✅ Tables found: {', '.join(tables)}")

        required = ['app_users', 'app_students', 'attendance_sessions', 'attendance_records']
        missing = [t for t in required if t not in tables]

        if not missing:
            print("✅ ALL required tables are present in Supabase.")
        else:
            print(f"⚠️ MISSING tables: {missing}")

except Exception as e:
    print(f"❌ CONNECTION FAILED: {str(e)}")
