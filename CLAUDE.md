# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Run single test class
./mvnw test -Dtest=DocMindApplicationTests

# Package
./mvnw package -DskipTests

# Run application
./mvnw spring-boot:run
```

Frontend (from `docmind-web/` directory):
```bash
cd docmind-web
npm install
npm run dev          # Dev server
npm run build        # Production build (vue-tsc + vite build)
```

## Architecture

DocMind is an AI-powered document Q&A system using RAG (Retrieval-Augmented Generation).

### Package Structure

```
com.zm.docmind
├── config/          — AiConfig (LLM/embedding/retriever beans), CorsConfig
├── controller/      — QaController (/qa), DocumentController (/documents)
├── service/         — QaAssistant (interface), QaAssistantManager, DocumentService
├── entity/          — Document (Lombok @Data/@Builder)
└── dto/             — DocumentUploadResponse
```

### RAG Pipeline

```
User Question → ContentRetriever (vector similarity, top 3, minScore 0.6) → Retrieved Context + Question → ChatModel → Response
```

### Core Components

| Component | Purpose |
|-----------|---------|
| `AiConfig` | Bean configuration: EmbeddingModel (Qwen via DashScope), EmbeddingStore (in-memory), ContentRetriever, ChatModel (MiniMax via OpenAI-compatible API), seed knowledge base |
| `QaAssistant` | Interface with `@SystemMessage`; LangChain4j AiServices auto-generates implementation. Has `answer()` and `stream()` (returns `TokenStream`) |
| `QaAssistantManager` | Factory managing user-isolated QaAssistant instances with `ConcurrentHashMap` cache, each with independent `MessageWindowChatMemory` (10 messages) |
| `DocumentService` | Upload, storage, parsing (TXT/MD only), paragraph-based chunking (500 chars max), embedding into vector store. Uses in-memory `ConcurrentHashMap` for document metadata |

### REST API

**QaController** (`/qa`):
- `GET /qa?userId=&question=` — Sync Q&A
- `GET /qa/stream?userId=&question=` — SSE streaming Q&A (sends `[DONE]` on completion, 60s timeout)
- `DELETE /qa/history/del/{userId}` — Clear user chat history

**DocumentController** (`/documents`):
- `GET /documents` — List all documents
- `GET /documents/{id}` — Get document by ID
- `POST /documents` — Upload (multipart: `file` + `userId`), 10MB limit, TXT/MD only
- `DELETE /documents/{id}` — Delete document

### User Isolation

`QaAssistantManager` creates per-user `QaAssistant` instances with independent `ChatMemory`. Each user's conversation history is isolated via `userId` parameter (defaults to `"default"`).

### Frontend

Located at `docmind-web/` (Vue 3 + TypeScript + Element Plus + Pinia). Axios calls backend at `http://localhost:8080`. SSE streaming uses native `EventSource`.

**Pages:** `LoginView` (userId-based login) → `ChatView` (SSE streaming chat) + `DocumentsView` (upload/delete documents)

## Tech Stack

- Spring Boot 4.0.4 + Java 25
- LangChain4j 1.12.2 (OpenAI integration for chat, community DashScope for embeddings)
- Lombok (`@Slf4j`, `@Data`, `@Builder`)
- InMemoryEmbeddingStore + InMemoryChatMemoryStore (both volatile — data lost on restart)

## Configuration

Key properties in `src/main/resources/application.properties`:
- `openai.model` / `openai.api-key` / `openai.base-url` — MiniMax chat model via OpenAI-compatible API
- `dashscope.api_key` — Qwen embedding model (DashScope)
- `docmind.storage.path` — Local directory for uploaded document files
