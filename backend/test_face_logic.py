import json
import numpy as np
from main import SessionLocal, Student

def test_logic():
    db = SessionLocal()
    # Pick a student with an embedding
    student = db.query(Student).filter(Student.face_embedding != None).first()

    if not student:
        print("No students with embeddings found in DB.")
        return

    print(f"Testing logic for Student: {student.full_name} ({student.registration_number})")

    # 1. Load stored embedding
    stored_emb = np.array(json.loads(student.face_embedding.decode('utf-8')))

    # 2. Simulate "current" scan being the same as stored (Perfect Match)
    current_emb = stored_emb

    dot_product = np.dot(current_emb, stored_emb)
    norm_current = np.linalg.norm(current_emb)
    norm_stored = np.linalg.norm(stored_emb)
    cosine_similarity = dot_product / (norm_current * norm_stored)
    dist = 1 - cosine_similarity

    print(f"Perfect Match Distance: {dist:.6f}")
    if dist < 0.40:
        print("SUCCESS: Perfect match identified correctly.")
    else:
        print("FAILURE: Perfect match failed threshold!")

    # 3. Simulate a Slight Variation (Add noise)
    noise = np.random.normal(0, 0.01, stored_emb.shape)
    varied_emb = stored_emb + noise

    dot_product = np.dot(varied_emb, stored_emb)
    norm_varied = np.linalg.norm(varied_emb)
    cosine_similarity = dot_product / (norm_varied * norm_stored)
    dist = 1 - cosine_similarity

    print(f"Slight Variation Distance: {dist:.6f}")
    if dist < 0.40:
        print("SUCCESS: Slight variation identified correctly.")
    else:
        print("FAILURE: Slight variation failed threshold!")

    # 4. Simulate Mismatch (Random Vector)
    random_emb = np.random.rand(*stored_emb.shape)

    dot_product = np.dot(random_emb, stored_emb)
    norm_random = np.linalg.norm(random_emb)
    cosine_similarity = dot_product / (norm_random * norm_stored)
    dist = 1 - cosine_similarity

    print(f"Mismatch Distance: {dist:.6f}")
    if dist >= 0.40:
        print("SUCCESS: Mismatch identified correctly.")
    else:
        print("FAILURE: Mismatch incorrectly passed as match!")

    db.close()

if __name__ == "__main__":
    test_logic()
