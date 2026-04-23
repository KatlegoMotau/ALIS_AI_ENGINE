# Import database session creator (used to connect to PostgreSQL)
from database import SessionLocal

# SQLAlchemy helper for writing raw SQL queries safely
from sqlalchemy import text as sql_text

import numpy as np
import json
import os
import traceback
from datetime import datetime   # used for logging

from similarity_engine import cosine_similarity_score

from document_processor import extract_text, segment_clauses           # Function to extract text from documents and split text into clauses
from similarity_engine import generate_embedding, cosine_similarity    # Function added cosine similarity import
from risk_detector import detect_risk                                  # Function to detect risky clauses

# Create FastAPI app
from fastapi import FastAPI
from pydantic import BaseModel

# FIX: ENABLE CORS (CRITICAL)
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Root endpoint (used to check if the server is running)
@app.get("/")
def home():
    return {"message": "ALIS AI Engine Running"}

# Creating a request model
class DocumentRequest(BaseModel):
    file_path: str
    user_identifier: str


# ================= RULE LOADER (NEW FIX) =================
def load_rules(db):
    query = sql_text("""
        SELECT r.keyword, r.requirements, r.risk_level, r.suggestion, l.law_name
        FROM rule r
        JOIN law l ON r.law_id = l.id
    """)

    rows = db.execute(query).fetchall()

    rules = []
    for r in rows:
        rules.append({
            "keyword": r.keyword.lower(),
            "requirement": r.requirements,
            "risk_level": r.risk_level,
            "suggestion": r.suggestion,
            "law": r.law_name
        })

    return rules


# ================= LAW + RULE ANALYSIS ENGINE =================
def analyze_with_laws(clauses, rules):

    text = " ".join(clauses).lower()

    risks = []
    compliance_issues = []
    suggestions = []

    for rule in rules:
        if rule["keyword"] in text:

            compliance_issues.append({
                "law": rule["law"],
                "issue": rule["requirement"]
            })

            if rule["risk_level"].lower() == "high":
                risks.append({
                    "risk": rule["keyword"],
                    "suggestion": rule["suggestion"]
                })

            suggestions.append(rule["suggestion"])

    return risks, compliance_issues, suggestions


# Suggestion Generator
def generate_suggestions(text, similar_docs_content):

    suggestions = []

    # Existing rule-based intelligence (your current logic)
    if "liability" not in text.lower():
        suggestions.append("Add a Liability Clause to protect both parties.")

    if "termination" not in text.lower():
        suggestions.append("Include a clear Termination Clause.")

    if "confidential" not in text.lower():
        suggestions.append("Add a Confidentiality Clause for data protection.")

    if "payment" not in text.lower():
        suggestions.append("Specify Payment Terms and penalties for late payment.")

    # Learn from past contracts
    if similar_docs_content and len(similar_docs_content) > 0:
        suggestions.append("Based on previous contracts in the database:")
        suggestions.append("Review stronger clause structures used in similar agreements.")
        suggestions.append("Consider improving dispute resolution clauses used in past contracts.")
        suggestions.append("Strengthen data protection clauses (common improvement area).")

    return suggestions


# ================= SAFE FILE NAME EXTRACTOR (FIXED) =================
def safe_filename(file_path):
    if not file_path:
        return f"document_{datetime.now().strftime('%Y%m%d%H%M%S')}.docx"

    name = os.path.basename(file_path)

    if not name or name.lower() in ["undefined", "null", ""]:
        return f"document_{datetime.now().strftime('%Y%m%d%H%M%S')}.docx"

    return name


# ================= MAIN ANALYZE ENDPOINT =================
@app.post("/analyze_document")
def analyze_document(request: DocumentRequest):

    db = SessionLocal()

    try:
        file_path = request.file_path
        user_identifier = request.user_identifier

        print("===================================")
        print("REQUEST RECEIVED")
        print("FILE PATH:", file_path)
        print("USER:", user_identifier)
        print("===================================")

        # GET USER ID (SAFE QUOTING FIX)
        user_query = sql_text("""
            SELECT id FROM app_user WHERE user_identifier = :uid
        """)

        user_result = db.execute(user_query, {"uid": user_identifier}).fetchone()

        if not user_result:
            return {"error": "User not found"}

        user_id = user_result[0]

        # ================= FILE VALIDATION =================
        if not file_path or not os.path.exists(file_path):
            return {"error": f"File not found: {file_path}"}

        # ================= EXTRACT TEXT (SAFE) =================
        try:
            text = extract_text(file_path)
        except Exception as e:
            print(traceback.format_exc())
            return {"error": f"Document read failed: {str(e)}"}

        if not text or len(text.strip()) == 0:
            return {"error": "Document is empty or could not be read"}

        print("DOCUMENT TEXT LENGTH:", len(text))

        # ================= SPLIT CLAUSES =================
        clauses = segment_clauses(text)

        # ================= EMBEDDING (SAFE) =================
        try:
            embedding = generate_embedding(text)
        except Exception as e:
            print(traceback.format_exc())
            return {"error": f"Embedding failed: {str(e)}"}

        # ================= LOAD EXISTING DOCUMENTS =================
        get_embeddings = sql_text("""
            SELECT id, embedding, content
            FROM public.document
        """)

        results = db.execute(get_embeddings).fetchall()

        max_similarity = 0
        most_similar_doc_id = None
        similar_docs = []

        for row in results:

            try:
                stored_embedding = row.embedding

                if isinstance(stored_embedding, str):
                    stored_embedding = json.loads(stored_embedding)

                similarity = cosine_similarity_score(embedding, stored_embedding)

                if similarity > max_similarity:
                    max_similarity = similarity
                    most_similar_doc_id = row.id

                similar_docs.append({
                    "id": row.id,
                    "similarity": similarity,
                    "content": row.content
                })

            except Exception:
                continue

        similar_docs = sorted(similar_docs, key=lambda x: x["similarity"], reverse=True)
        top_docs = similar_docs[:3]

        similarity_percentage = float(max_similarity * 100)

        # ================= DUPLICATE CHECK =================
        if max_similarity > 0.85:

            db.execute(sql_text("""
                INSERT INTO log (user_id, action)
                VALUES (:uid, :action)
            """), {
                "uid": user_id,
                "action": f"Duplicate document detected ({similarity_percentage:.2f}%)"
            })

            db.commit()

            return {
                "message": "Document already exists",
                "similarity_percentage": round(similarity_percentage, 2),
                "existing_document_id": most_similar_doc_id,
                "risks": [],
                "compliance_issues": [],
                "suggestions": [],
                "clauses_found": len(clauses)
            }

        # ================= RULES =================
        similar_docs_content = [doc["content"] for doc in top_docs if doc["content"]]
        rules = load_rules(db)

        rule_risks, rule_compliance, rule_suggestions = analyze_with_laws(clauses, rules)

        ai_risks = detect_risk(clauses)

        risks = ai_risks + rule_risks
        compliance = rule_compliance
        suggestions = generate_suggestions(text, similar_docs_content) + rule_suggestions

        # ================= SAVE DOCUMENT =================
        file_name = safe_filename(file_path)

        insert_doc = sql_text("""
            INSERT INTO public.document (user_id, file_name, content, embedding)
            VALUES (:uid, :file_name, :content, :embedding)
            RETURNING id;
        """)

        result = db.execute(insert_doc, {
            "uid": user_id,
            "file_name": file_name,
            "content": text,
            "embedding": json.dumps(embedding.tolist())
        })

        document_id = result.fetchone()[0]
        db.commit()

        # ================= LOG =================
        db.execute(sql_text("""
            INSERT INTO log (user_id, document_id, action)
            VALUES (:uid, :doc_id, :action)
        """), {
            "uid": user_id,
            "doc_id": document_id,
            "action": f"Document analyzed ID {document_id}"
        })

        db.commit()

        # ================= FINAL RESPONSE =================
        return {
            "document_id": document_id,
            "clauses_found": len(clauses),
            "risks": risks,
            "compliance_issues": compliance,
            "similarity_percentage": round(similarity_percentage, 2),
            "suggestions": suggestions
        }

    except Exception as e:
        db.rollback()
        print("FULL ERROR TRACEBACK:")
        print(traceback.format_exc())
        return {
            "error": str(e),
            "trace": traceback.format_exc()
        }

    finally:
        db.close()


# ================= DOCUMENTS ENDPOINT (FIXED ROUTE SUPPORT) =================
@app.get("/api/document/all")
@app.get("/api/documents/all")   # FIX: supports frontend mismatch
def get_documents():

    db = SessionLocal()
    try:
        result = db.execute(sql_text("""
            SELECT id, file_name
            FROM public.document
            ORDER BY id DESC
        """)).fetchall()

        return [
            {
                "id": r.id,
                "file_name": r.file_name or "Unnamed document"
            }
            for r in result
        ]

    except Exception as e:
        print("DOCUMENT ERROR:", e)
        return []

    finally:
        db.close()


# ================= LOGS ENDPOINT (FIXED + SAFE) =================
@app.get("/api/log")
def get_logs():

    db = SessionLocal()
    try:
        result = db.execute(sql_text("""
            SELECT id, user_id, document_id, action, timestamp
            FROM public.log
            ORDER BY id DESC
        """)).fetchall()

        return [
            {
                "id": r.id,
                "user_id": r.user_id,
                "document_id": r.document_id,
                "action": r.action,
                "timestamp": r.timestamp.strftime("%Y-%m-%d %H:%M:%S") if r.timestamp else None
            }
            for r in result
        ]

    except Exception as e:
        print("LOG ERROR:", e)
        return []

    finally:
        db.close()