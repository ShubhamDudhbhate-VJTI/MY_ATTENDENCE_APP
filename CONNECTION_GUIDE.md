# AttendX Connection & Database Guide

This document provides instructions for establishing a stable connection between the Android application and the backend server, as well as details on the database architecture.

---

## 1. Network Connectivity (Phone to PC)

To allow the Android app to communicate with the Python backend, both devices must be on the **same network** (WiFi or Mobile Hotspot).

### Step A: Find your PC's IP Address
1. Open a terminal (CMD or PowerShell) on your PC.
2. Type `ipconfig` and press Enter.
3. Look for the **IPv4 Address** under your active network adapter (e.g., `10.86.1.244`).

### Step B: Update the Android App
1. Open `app/src/main/java/com/example/dbms_shubham_application/network/RetrofitClient.kt`.
2. Update the `PC_IP` constant with your current IPv4 address:
   ```kotlin
   private const val PC_IP = "10.86.1.244"
   ```

### Step C: Configure Windows Firewall
Windows often blocks incoming requests from mobile devices. You must allow Port 8000:
1. Open **PowerShell as Administrator**.
2. Run the following command:
   ```powershell
   New-NetFirewallRule -DisplayName "Allow AttendX" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow
   ```
3. Ensure your WiFi profile is set to **"Private"** in Windows Settings.

---

## 2. Backend Configuration (`main.py`)

### Hosting
The backend must be hosted on `0.0.0.0` to listen for external requests from the phone:
```python
# In main.py
uvicorn.run(app, host="0.0.0.0", port=8000)
```

### Database Connection (Supabase vs. SQLite)
The backend uses a **Hybrid Database Logic**:
1. **Supabase (Primary)**: 
   - The app looks for a `DATABASE_URL` in the `.env` file.
   - If a valid Supabase PostgreSQL URL is found, all attendance data is synced to the cloud.
2. **SQLite (Fallback)**: 
   - If the internet is down or the Supabase connection fails, the backend automatically switches to `attendance.db` (local file).
   - **Note**: Data saved in SQLite will *not* appear in the Supabase dashboard until manually migrated.

---

## 3. Face Image Storage
Captured face images during attendance are stored locally on the phone for debugging:
- **Path**: `context.cacheDir/face/`
- **Format**: `face_{studentId}_{timestamp}.jpg`
- The app sends these images to the backend via a `Multipart` request for AI verification.

---

## 4. Troubleshooting
- **Failed to connect**: Verify your PC IP hasn't changed. Run `ipconfig` again.
- **Loading Forever**: Check if the PC and Phone are on the same WiFi. Try a Mobile Hotspot if the router has "AP Isolation" enabled.
- **Browser Test**: Open the phone's browser and go to `http://<YOUR_PC_IP>:8000/`. If you see `{"status":"online"}`, the connection is working.

---

## 5. Face Data Management

### How to Reset a Student's Face Data
If a student needs to re-register their face (e.g., due to a bad initial scan or to sync with a new database like Supabase), you can reset their profile using the following command from your PC:

**Using PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://10.86.1.244:8000/attendance/reset-face/YOUR_STUDENT_ID" -Method Post
```

**Using cURL:**
```bash
curl -X POST http://10.86.1.244:8000/attendance/reset-face/YOUR_STUDENT_ID
```

Replace `YOUR_STUDENT_ID` with the actual registration number (e.g., `241080017`).

### Where is the Face Data Stored?
1. **Supabase (Cloud)**: The actual image bytes are stored in the `face_image` column of the `app_students` table.
2. **Backend (Local)**: A copy of the "Master" face is saved in the `backend/static/faces/` folder as `STUDENT_ID.jpg`.
3. **App (Local Cache)**: Temporary scans are kept in the app's cache directory and cleared after verification.

### How to View a Student's Registered Face
You can view the "Master" face currently stored in the system (either in local storage or Supabase) by visiting this URL in your browser:
`http://10.86.1.244:8000/faces/YOUR_STUDENT_ID.jpg`
(Example: `http://10.86.1.244:8000/faces/241080017.jpg`)

---

## 6. Global Data Wipe
To completely remove a student from all databases (Supabase + SQLite) and delete their local face images, run:
```powershell
python D:/AndroidProjects/DBMS_Shubham_Application/backend/reset_student.py YOUR_STUDENT_ID
```
