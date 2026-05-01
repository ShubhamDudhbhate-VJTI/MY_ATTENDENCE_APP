# AttendX Technical Workflow Documentation

This document provides a deep-dive into the technical processes of the AttendX system, focusing on the data flow between the Android Client, FastAPI Backend, and Supabase Database.

---

## 1. User Registration & Biometric Enrollment
This is the one-time setup process for students to register their "Master Face" for future comparisons.

1.  **Client-Side (Android):**
    *   Student fills out the registration form (Name, Roll No, Branch).
    *   **CameraX** launches in "Selfie Mode".
    *   **ML Kit Face Detection** ensures a clear face is visible before allowing the capture.
    *   The image is compressed and sent via a `Multipart/Form-Data` POST request to `/register/student`.
2.  **Server-Side (FastAPI):**
    *   The backend saves the raw image to the `/static/faces/` directory.
    *   **DeepFace** processes the image to extract a **Face Embedding** (a numerical representation/vector of the face).
    *   The user record is created in `app_users`, and the student profile (including the path to the image and the embedding) is stored in `app_students`.
3.  **Database (Supabase):**
    *   Stores the metadata and the association between the `user_id` and the `student_id`.

---

## 2. Faculty Session Initialization
How a teacher starts a class and generates the security "Fence".

1.  **Frontend:** Teacher selects a subject. The app fetches the current schedule from `/get_faculty_schedule/{faculty_id}`.
2.  **Backend:** 
    *   Generates a `session_id`.
    *   Creates a **Dynamic QR Token** (a unique UUID/Hash) that is valid only for that specific classroom and time.
3.  **Client:** The teacher's app displays the QR code. It also starts a "Live Refresh" loop (using a `LaunchedEffect` in Compose) that polls `/get_session_attendance/{session_id}` every 5 seconds to show students appearing in real-time.

---

## 3. The 3-Layer Attendance Verification (Student)
This is the core logic that prevents proxy attendance.

### **Layer 1: Network Proximity (WiFi)**
*   **Android:** The app scans for nearby WiFi access points.
*   **Logic:** It retrieves the current **BSSID (MAC Address)** and **SSID**.
*   **Validation:** These are sent to the backend. The backend compares them against the `classrooms` table. If the BSSID doesn't match the designated classroom, the process is aborted.

### **Layer 2: Session Binding (QR Scan)**
*   **Android:** Student scans the teacher's QR code using **ML Kit Barcode Scanning**.
*   **Logic:** The QR contains the `session_id` and the `dynamic_token`.
*   **Validation:** The backend verifies that the `session_id` is currently "ACTIVE" and that the token is valid.

### **Layer 3: Biometric Identity (Face Match)**
*   **Android:** App captures a "Live Selfie".
*   **Backend (The AI Core):**
    *   Receives the `live_selfie` and the `student_id`.
    *   Retrieves the "Master Face" (from registration) from the server storage.
    *   **DeepFace.verify()** compares the `live_selfie` against the `master_face`.
    *   It uses a **Cosine Similarity** threshold (default 0.4). If the distance is below this threshold, it's a match.

---

## 4. Attendance Confirmation & Logging
Once all 3 layers pass:

1.  **Backend:** Inserts a new row into `attendance_records`.
    *   Fields: `session_id`, `student_id`, `timestamp`, `status='PRESENT'`, `verification_method='AI_FACE_WIFI'`.
2.  **FCM Notification:** The backend triggers a **Firebase Cloud Messaging** alert to the student's phone: *"Attendance marked successfully for [Subject Name]"*.
3.  **Real-Time Sync:** The Teacher's dashboard updates instantly via the polling mechanism mentioned in Section 2.

---

## 5. HOD Analytics & Reporting
1.  **Data Aggregation:** The HOD app calls `/get_department_analytics`.
2.  **Logic:** The FastAPI backend performs complex SQL joins:
    *   `attendance_records` JOIN `app_students` JOIN `subjects`.
3.  **Output:** Computes percentage-based attendance per student and identifies "Shortage" cases (students below 75%).
4.  **Export:** The HOD can trigger a CSV/PDF export generation on the server.

---

## 6. Error Handling & Security Edge Cases
*   **Zombie Sessions:** A background task on the server automatically marks any session older than 2 hours as "CLOSED".
*   **Device Spoofing:** The `device_id` is checked during every login. If a student tries to log in on a friend's phone, the system flags a "Device Mismatch".
*   **Cleartext Traffic:** The Android `Manifest` is configured to allow local IP communication (`usesCleartextTraffic="true"`) specifically for the development environment.
