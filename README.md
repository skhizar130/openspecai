

# OpenSpecAI

AI-assisted OpenAPI specification generation, editing, and validation.

---

## Overview

OpenSpecAI allows you to:

- Generate OpenAPI specs from natural language
- Modify existing specs using instructions
- Generate unified diffs for updates
- Validate specs using Spectral
- Auto-fix validation issues using AI
- Store semantic chunks in ChromaDB

---


### Install Spectral CLI

Spectral must be installed globally for validation to work when running the backend outside Docker.

```bash
npm install -g @stoplight/spectral-cli
```

Verify installation:

```bash
spectral --version
```
---

## Configuration

Create a `.env` file in /openspecai/backend folder with:

```bash
OPENAI_API_KEY=your_api_key_here
CHROMA_URL=http://localhost:8000/
CHROMA_COLLECTION=api-specs
```
Note: Configure your IDE launch to include /openspecai/backend/.env

---

## Running the Application

### 1️⃣ Start ChromaDB (Docker)

From the project root directory:

```bash
docker compose up -d
```

This starts **only ChromaDB**.

ChromaDB will be available at:

```
http://localhost:8000
```

---

### 2️⃣ Start the Spring Boot Backend

Navigate to the backend directory:

```bash
cd backend
```

Run the application:

```bash
mvn spring-boot:run
```

The backend will start at:

```
http://localhost:8080
```

---

## API Endpoints

### Generate Spec

**POST** `/api/specs`

```json
{
  "instruction": "Create a Payments API with create, get and refund endpoints"
}
```

Response: Complete OpenAPI specification (YAML)

---

### Get Spec

**GET** `/api/specs/{id}`

Response: Complete OpenAPI specification

---

### Modify Spec

**POST** `/api/specs/{id}`

```json
{
  "instruction": "Add idempotency to POST /payments"
}
```

Response: Unified diff

---

### Accept Modification

**GET** `/api/specs/{id}/accept`

Response: Updated spec

---

### Validate

**GET** `/api/specs/validate/{id}?fix=false`

Response: Spectral output (JSON)

---

### Validate + Auto Fix

**GET** `/api/specs/validate/{id}?fix=true`

Response: Updated OpenAPI specification

---

### Test SwaggerHub connection

**GET** `/api/swaggerhub/connect`

Response: { Status: Connected }

---

### Publish spec to SwaggerHub

**GET** `/api/spec/{id}/publish`

Response: Published successfully

---

### Preview SwaggerHub UI

**GET** `/api/spec/{id}/preview`

Response: Link to SwaggerHub UI

---

## License

MIT