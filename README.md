<div align="center">
  <img src="https://img.icons8.com/color/144/000000/fingerprint-scan.png" alt="AttendX Logo" width="100"/>
  <h1>📱 AttendX</h1>
  <p><b>Smart Biometric & Geolocation Attendance Management System</b></p>
  
  [![Kotlin](https://img.shields.io/badge/Kotlin-Mobile_App-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=for-the-badge&logo=android)](https://developer.android.com/jetpack/compose)
  [![FastAPI](https://img.shields.io/badge/FastAPI-Backend-009688?style=for-the-badge&logo=fastapi)](https://fastapi.tiangolo.com/)
  [![Supabase](https://img.shields.io/badge/Supabase-Database-3ECF8E?style=for-the-badge&logo=supabase)](https://supabase.com/)
  [![DeepFace](https://img.shields.io/badge/DeepFace-AI_Verification-FF0000?style=for-the-badge&logo=python)](https://github.com/serengil/deepface)
</div>

<br />

<div align="center">
  <em>A cutting-edge solution leveraging AI, GPS, and Network Fingerprinting to ensure secure, proxy-free, and real-time attendance marking.</em>
</div>

<hr />

## 🌟 Overview

**AttendX** is a comprehensive, full-stack biometric attendance management system designed to eliminate proxy attendance entirely. It features a decoupled architecture with a high-performance **Android app (Kotlin & Jetpack Compose)** and a robust **Python FastAPI** orchestration layer.

By combining three powerful validation layers—**Network Proximity (WiFi BSSID/SSID)**, **Dynamic QR Tokens**, and **AI Face Verification (DeepFace)**—AttendX guarantees that students must be physically present in the right classroom, at the right time, to mark their attendance.

<br />

## 🏗️ Technology Stack

<table align="center" width="100%">
  <tr>
    <td align="center" width="33%">
      <h3>Mobile App (Android)</h3>
      <img src="https://skillicons.dev/icons?i=kotlin,android" />
      <br/><br/>
      <b>Kotlin</b><br/>
      <b>Jetpack Compose</b> (UI)<br/>
      <b>Google ML Kit</b> (Face & QR)<br/>
      <b>CameraX API</b><br/>
      <b>Fused Location Provider</b><br/>
      <b>Retrofit & OkHttp</b>
    </td>
    <td align="center" width="33%">
      <h3>Backend (API)</h3>
      <img src="https://skillicons.dev/icons?i=fastapi,python" />
      <br/><br/>
      <b>Python 3</b><br/>
      <b>FastAPI</b> (Async RESTful)<br/>
      <b>DeepFace</b> (AI Core)<br/>
      <b>OpenCV</b> (Image Processing)<br/>
      <b>Uvicorn</b> (ASGI Server)
    </td>
    <td align="center" width="33%">
      <h3>Database & Infra</h3>
      <img src="https://skillicons.dev/icons?i=supabase,sqlite" />
      <br/><br/>
      <b>Supabase (PostgreSQL)</b><br/>
      <b>SQLAlchemy ORM</b><br/>
      <b>SQLite</b> (Local Fallback)<br/>
      <b>Firebase Cloud Messaging</b><br/>
      <b>Hybrid Storage</b> (Blobs/Files)
    </td>
  </tr>
</table>

<br />

## ✨ Core Features & Security

| Security Layer | Description |
| :--- | :--- |
| 📍 **Network Fingerprinting** | Validates the student is connected to the exact classroom's **WiFi BSSID/SSID**, guaranteeing physical proximity. |
| 🔐 **Dynamic QR Tokens** | Faculty generates short-lived, encrypted QR codes with session-specific UUIDs. Prevents QR sharing. |
| 🤖 **AI Biometric Verification** | Live selfies are compared against a registered "Master Face" using **DeepFace** and Cosine Similarity thresholds. |
| 📱 **Device ID Binding** | Students are securely bound to their hardware device, stopping multi-login fraud. |
| 📊 **Real-Time Analytics** | Faculty dashboard updates instantly via live polling. HODs get shortage reports (under 75%) and CSV/PDF exports. |

<br />

## 🔄 Operational Workflow

### **1. Faculty: Session Initialization**
Faculty selects a scheduled class ➔ System validates time/location ➔ Generates `session_id` and Dynamic QR Token ➔ App starts live polling.

### **2. Student: 3-Layer Verification Loop**
Identity Login ➔ **WiFi Proximity Check** ➔ **Scan Faculty QR** ➔ **Capture Live Selfie** ➔ Backend DeepFace Match ➔ Success!

### **3. Reporting & Notifications**
Student receives FCM notification of success ➔ Faculty sees student on live dashboard ➔ At session close, absentees are logged ➔ Aggregated for HOD Analytics.

<br />

## 🗄️ Database Schema Entities

- **`app_users`**: Central auth table (Username, Email, Role, Pass Hash).
- **`app_students`**: Academic details + Biometrics (Face Embeddings).
- **`classrooms`**: Physical mapping to GPS & WiFi BSSID/SSID.
- **`attendance_sessions`**: Active classes & dynamic QR tokens.
- **`attendance_records`**: Final immutable log of verified attendance with timestamps and location data.

<br />

## 🛠️ Setup & Installation

*(Note: Ensure you have Android Studio and Python 3.9+ installed.)*

<details>
<summary><b>1. Backend (FastAPI & DeepFace)</b> <i>(Click to expand)</i></summary>

```bash
cd backend

# Create virtual environment & install deps
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

**`.env` Configuration:**
Create a `.env` file in the backend root:
```env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your_supabase_anon_key
SECRET_KEY=your_jwt_secret
```

```bash
# Start the Uvicorn server
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
</details>

<details>
<summary><b>2. Frontend (Android / Kotlin)</b> <i>(Click to expand)</i></summary>

1. Open the `/app` folder in **Android Studio**.
2. Sync Gradle projects.
3. Update the `BASE_URL` in your Retrofit client to point to your FastAPI server IP.
4. Run the app on a physical device (Emulators may not support WiFi scanning or CameraX properly).
</details>

<br />

## 📚 API Documentation

FastAPI automatically generates interactive documentation for the backend:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

<br />

<div align="center">
  <p><b>Zero Proxy. Ultimate Security. Effortless Management.</b></p>
</div>
