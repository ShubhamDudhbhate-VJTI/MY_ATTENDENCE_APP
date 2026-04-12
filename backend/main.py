from fastapi import FastAPI, HTTPException, Body, UploadFile, File, Form
from pydantic import BaseModel
from typing import List, Optional
import uvicorn
import time
import os
import shutil
from deepface import DeepFace
import cv2
import numpy as np

app = FastAPI(title="DBMS Attendance Backend")

# Configuration
FACES_DIR = "data/faces"
os.makedirs(FACES_DIR, exist_ok=True)

# In-memory storage for demonstration
VALID_WIFI_BSSIDS = ["00:11:22:33:44:55", "AA:BB:CC:DD:EE:FF", "02:00:00:00:00:00"]
VALID_QR_TOKENS = {"CLASS_101": "VALID_SESSION_QR"}

class WifiRequest(BaseModel):
    bssid: str
    ssid: str

class WifiResponse(BaseModel):
    success: bool
    message: str
    classroomName: Optional[str] = None

class LoginRequest(BaseModel):
    username: str
    password: str

@app.get("/")
async def root():
    return {"message": "DBMS Attendance System API is running"}

@app.post("/auth/login")
async def login(request: dict = Body(...)):
    username = request.get("username")
    password = request.get("password")

    if username == "student" and password == "password":
        return {"token": "dummy_student_token", "role": "STUDENT", "name": "Shubham", "student_id": "ST123"}
    elif username == "teacher" and password == "password":
        return {"token": "dummy_teacher_token", "role": "TEACHER", "name": "Admin"}
    raise HTTPException(status_code=401, detail="Invalid credentials")

@app.post("/attendance/verify-wifi", response_model=WifiResponse)
async def verify_wifi(request: WifiRequest):
    if request.bssid in VALID_WIFI_BSSIDS:
        return WifiResponse(success=True, message="WiFi Verified", classroomName="Main Hall (Room 101)")
    return WifiResponse(success=False, message="Unauthorized WiFi Network.")

@app.post("/attendance/verify-qr")
async def verify_qr(request: dict = Body(...)):
    if request.get("token") == "VALID_SESSION_QR":
        return {"success": True, "message": "QR Verified"}
    return {"success": False, "message": "Invalid QR Code"}

@app.post("/attendance/verify-face")
async def verify_face(
    image: UploadFile = File(...),
    student_id: str = Form(...)
):
    try:
        # 1. Find the master photo for this student
        master_path = os.path.join(FACES_DIR, f"{student_id}.jpg")
        if not os.path.exists(master_path):
            return {"success": False, "message": f"No master record found for {student_id}. Please register first."}

        # 2. Save the temporary capture
        temp_path = f"temp_{student_id}.jpg"
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(image.file, buffer)

        # 3. Perform DeepFace verification
        # enforce_detection=False allows processing even if face detection is difficult
        result = DeepFace.verify(
            img1_path = temp_path,
            img2_path = master_path,
            enforce_detection = True,
            model_name = "VGG-Face",
            distance_metric = "cosine"
        )

        os.remove(temp_path) # Clean up

        if result["verified"]:
            return {
                "success": True,
                "confidence": 1 - result["distance"],
                "message": f"Face verified for {student_id}"
            }
        else:
            return {"success": False, "message": "Face mismatch. Verification failed."}

    except Exception as e:
        print(f"Face Error: {str(e)}")
        return {"success": False, "message": f"AI Processing Error: {str(e)}"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
