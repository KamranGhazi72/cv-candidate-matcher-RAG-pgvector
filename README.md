# 🚀 AI-Powered CV Candidate Matcher & Screener

An enterprise-grade, production-ready candidate screening and global ranking platform built with **Java 21**, **Spring Boot 3.3.4**, and **Spring AI**. 

The platform automates HR candidate evaluation by transforming unstructured resume PDFs into structured vector embeddings, performing high-dimensional semantic search with **PostgreSQL pgvector (HNSW Indexing)**, and executing multi-criteria candidate evaluation via local LLMs (**Ollama / Llama 3.2**).

---

## 📌 Executive Summary & Problem Statement

Traditional Applicant Tracking Systems (ATS) rely on rigid keyword matching, often rejecting highly qualified candidates due to subtle phrasing differences. Conversely, manual resume screening by HR teams is slow, costly, and difficult to scale across hundreds of applicants.

**This solution bridges the gap:**
1. **Automated RAG Pipeline:** Converts candidate PDFs into semantic vector chunks stored in PostgreSQL.
2. **Global Similarity Search:** Leverages Cosine Distance vector indexing ($O(\log N)$ with HNSW) to instantly locate top candidate matches across job postings.
3. **Structured LLM Evaluation:** Uses local LLMs to generate multi-dimensional feedback (Match Score, Summary, Matching Skills, Missing Skills, HR Recommendations) without relying on external cloud APIs.
4. **Dual-Tier Analytics:** Separates high-latency AI computations from fast HR dashboard read queries by persisting structured evaluation results in a relational `JSONB` store.

---

## 🏗 System Architecture & Design Patterns

The project strictly enforces **Hexagonal Architecture (Clean Architecture)** principles to decouple core domain logic from framework-specific infrastructure adapters, vector databases, and LLM implementations.

```
                    ┌─────────────────────────────────────────────────────────┐
                    │               REST Controller Adapters                  │
                    │      (/api/v1/jobs , /api/v1/candidates)               │
                    └────────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                    ┌─────────────────────────────────────────────────────────┐
                    │               Application Service Layer                 │
                    │               (Domain Orchestration)                    │
                    └───────────┬─────────────────────────────────┬───────────┘
                                │                                 │
                                ▼                                 ▼
┌───────────────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│              Outbound Ports                   │ │                Outbound Ports                 │
│      (VectorStorePort, LlmEvaluationPort)     │ │        (SpringData Repositories)              │
└───────────────────────┬───────────────────────┘ └───────────────────────┬───────────────────────┘
                        │                                                 │
                        ▼                                                 ▼
┌───────────────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│            Infrastructure Adapters            │ │            Infrastructure Persistence         │
│   - SpringAiLlmAdapter (Ollama / Llama 3.2)   │ │   - PostgreSQL (pgvector / HNSW Index)        │
│   - PdfDocumentParser (TokenTextSplitter)     │ │   - JSONB Feedback Storage                    │
│   - EmailService (JavaMailSender)             │ │   - Flyway Schema Migrations                  │
└───────────────────────────────────────────────┘ └───────────────────────────────────────────────┘
```

### Key Architectural Highlights:
- **Clean Separation of Concerns:** Domain models (`JobPosting`, `EvaluationResult`, `ResumeChunk`) are immutable Java Records with zero framework dependencies.
- **Defensive LLM Integration:** Employs customized Jackson mapping, JSON repair sanitizers, and numerical fallback mechanisms to safely convert non-deterministic LLM text responses into strongly typed DTOs.
- **HNSW Vector Indexing:** Employs Hierarchical Navigable Small World (HNSW) graphs in PostgreSQL for sub-millisecond similarity search queries over candidate embeddings.

---

## 🛠 Tech Stack

| Layer | Technologies & Tools |
| :--- | :--- |
| **Language & Runtime** | Java 21 (LTS) |
| **Core Framework** | Spring Boot 3.3.4, Spring AI |
| **AI / LLM Engine** | Ollama (`llama3.2:1b` for evaluation, `nomic-embed-text` for embeddings) |
| **Database & Vector Store** | PostgreSQL 16 + `pgvector` extension |
| **Data Migration** | Flyway DB Migration |
| **Data Persistence & Mapping** | Spring Data JPA, Jackson (`jsonb` support via `@JdbcTypeCode`) |
| **Document Processing** | Apache Tika / Spring AI Document Parsers |
| **Infrastructure & Build** | Docker Compose, Apache Maven |

---

## 🔄 Core Processing Workflow

```
[Candidate Resume PDF] ──► [PdfDocumentParser] ──► [TokenTextSplitter] ──► [Ollama Embedding] ──► [PostgreSQL pgvector]
                                                                                                          │
                                                                                                          ▼
[HR Job Posting] ───────► [Vector Search Query] ◄──────────────────────────────────────────────────────────┘
                                │
                                ▼
                       [Top-K Candidate Chunks]
                                │
                                ▼
                   [Llama 3.2 LLM Evaluation]
                                │
                                ▼
             [Defensive JSON Sanitizer & Jackson Parser]
                                │
                                ▼
                   [PostgreSQL DB] + [HR Email]
```

1. **Resume Ingestion & Chunking:** PDF resumes are uploaded via multi-part HTTP request, parsed into text, split into overlapping chunks via `TokenTextSplitter`, and converted into 768-dimensional embeddings using `nomic-embed-text`.
2. **Semantic Similarity Search:** When evaluating a job posting, job requirements are embedded into a single search vector. The vector engine queries PostgreSQL using Cosine Distance (`<=>`) to fetch the top matching candidate chunks.
3. **Structured Evaluation:** Contextual chunks are injected into a specialized `PromptTemplate`. Ollama evaluates the candidate and outputs structured evaluation metrics.
4. **Resilient JSON Parsing:** Raw LLM outputs pass through `sanitizeAndRepairJson()` and custom `ObjectMapper` configurations to fix unclosed JSON brackets, single-string arrays, or decimal-normalized match scores.
5. **Relational Persistence:** Evaluation summaries are persisted in the `candidate_evaluations` table with structured skill lists serialized as PostgreSQL `jsonb` columns. If missing, missing `CandidateEntity` base records are automatically provisioned to satisfy foreign key constraints.

---

## 💾 Database Schema Overview

```sql
-- Job Postings Table
CREATE TABLE job_postings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    min_years_experience INT DEFAULT 0,
    required_education VARCHAR(255),
    required_skills TEXT[],
    description TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Candidates Table
CREATE TABLE candidates (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50),
    years_of_experience INT DEFAULT 0,
    education_level VARCHAR(100),
    skills TEXT[],
    resume_file_path VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Candidate Evaluations Table (HR Analytics & Dashboard Store)
CREATE TABLE candidate_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_posting_id UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    overall_match_score NUMERIC NOT NULL,
    evaluation_summary TEXT NOT NULL,
    structured_feedback JSONB NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🤖 AI Engineering: Handling LLM Non-Determinism & Edge Cases

Integrations with local LLMs (like `llama3.2:1b`) present probabilistic behavior challenges. This project implements production-grade defensive patterns to ensure zero-downtime execution:

### 1. `MismatchedInputException` Fix (Single Value Array Handling)
Local models occasionally return single string values instead of JSON arrays (e.g., `"recommendations": "Conduct interview"` instead of `["Conduct interview"]`).
- **Solution:** Configured `objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)`.

### 2. Java Record Construction via Jackson
Java Records require explicit parameter name discovery when instantiated reflectively.
- **Solution:** Enabled `objectMapper.findAndRegisterModules()` to automatically bind Jackson to Java 21 Record constructors.

### 3. JSON EOF & Truncation Recovery
To handle truncated LLM JSON outputs caused by token limits (`num-predict`), custom JSON repair logic cleans markdown code fences (` ```json `), locates brace limits, and auto-applies closing brackets (`]}`).

### 4. Score Normalization Guardrail
Models occasionally report scores in range `0.0 - 1.0` despite explicit prompts for `0.0 - 100.0`.
- **Solution:** Integrated automatic backend scaling: if $0.0 < \text{score} \le 1.0$, it is dynamically multiplied by $100.0$.

---

## ⚡ API Endpoints Reference

### Job Postings

#### 1. Create Job Posting
- **`POST /api/v1/jobs`**
```json
{
  "title": "Senior Java Backend Engineer",
  "department": "Engineering",
  "minYearsExperience": 4,
  "requiredEducation": "Master's Degree",
  "requiredSkills": ["Java 21", "Spring Boot", "PostgreSQL", "pgvector", "Docker"],
  "description": "Seeking a Senior Engineer experienced in Spring Boot 3, Clean Architecture, and vector store integration."
}
```

#### 2. Get All Job Postings
- **`GET /api/v1/jobs`**

#### 3. Global Top Candidate Ranking (AI Evaluation Trigger)
- **`GET /api/v1/jobs/{jobId}/top-candidates?limit=3`**
- **Response Example:**
```json
[
  {
    "candidateId": "123e4567-e89b-12d3-a456-426614174006",
    "rank": 1,
    "evaluation": {
      "candidateId": "123e4567-e89b-12d3-a456-426614174006",
      "jobPostingId": "099b4e8c-447a-4666-b1c0-2ecd77645021",
      "overallMatchScore": 85.0,
      "summary": "High alignment with Java 21, Spring Boot, and vector databases. Missing experience with Kubernetes.",
      "matchingSkills": ["Java 21", "Spring Boot", "PostgreSQL", "pgvector"],
      "missingSkills": ["Kubernetes"],
      "recommendations": ["Conduct technical interview focus on distributed systems."]
    }
  }
]
```

#### 4. Get Saved Job Evaluations (HR Read Dashboard)
- **`GET /api/v1/jobs/{jobId}/evaluations`**
- *Instant fetch from PostgreSQL relational store without triggering LLM inference.*

---

### Candidates

#### 1. Upload & Index Candidate Resume
- **`POST /api/v1/candidates/{candidateId}/resume`**
- **Content-Type:** `multipart/form-data`
- **Body:** `file` (PDF file upload)

#### 2. Deep-Dive Candidate Evaluation
- **`POST /api/v1/candidates/{candidateId}/evaluate?hrEmail=hr@company.com`**
- Performs single-candidate RAG analysis and sends an email summary report to HR.

#### 3. Candidate Evaluation History
- **`GET /api/v1/candidates/{candidateId}/evaluations`**

---

## 🛠 Local Setup & Running Instructions

### Prerequisites
- **JDK 21** installed
- **Docker & Docker Compose** installed
- **Ollama CLI** installed locally

### Step 1: Clone the Repository
```bash
git clone https://github.com/YourUsername/cv-candidate-matcher.git
cd cv-candidate-matcher
```

### Step 2: Start PostgreSQL with pgvector
```bash
docker-compose up -d
```

### Step 3: Pull Ollama Models
```bash
ollama pull llama3.2:1b
ollama pull nomic-embed-text
```

### Step 4: Configure `application.yaml`
Ensure `src/main/resources/application.yaml` points to your running services:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/matcher_db
    username: matcher_user
    password: matcher_password
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2:1b
          num-ctx: 4096
          num-predict: 1024
          temperature: 0.2
      embedding:
        options:
          model: nomic-embed-text
```

### Step 5: Build and Run
```bash
./mvnw clean spring-boot:run
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.