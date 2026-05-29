# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Backend (requires .env file - copy from .env.example)
./mvnw spring-boot:run

# Backend tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AuthServiceTest

# Frontend (separate project in docmind-web/)
cd docmind-web && npm install && npm run dev

# Frontend build
cd docmind-web && npm run build
```

## Architecture

DocMind is a RAG (Retrieval-Augmented Generation) document Q&A system with Spring Boot backend and Vue 3 frontend.

### Backend Stack
- **Spring Boot 4.0.4** with Spring Data JDBC (not JPA/Hibernate), Spring Security, Lombok
- **LangChain4j 1.12.2** for RAG pipeline — AiServices, ContentRetriever, EmbeddingStore, ChatMemory
- **Apache Tika** for document parsing (PDF/Word/Excel/TXT/Markdown/CSV)
- **PgVector** for vector storage in PostgreSQL
- **Java 25**, Maven build

### Frontend Stack
- **Vue 3 + TypeScript + Vite 8** in `docmind-web/`
- Element Plus UI, Pinia state management, Vue Router
- Vite dev server proxies API calls to backend at `:8080`

### Key Architectural Patterns

- **Layered**: Controller → Service → Repository with DTOs separating API from entity layer. Constructor injection throughout.
- **Unified API response**: All endpoints return `ApiResponse<T>` (`{success, message, data}`).
- **Async document processing**: Upload returns immediately; parsing + embedding happens asynchronously via `@Async` with custom thread pool. Documents track status: PENDING → PROCESSING → COMPLETED/FAILED.
- **Per-user AI assistant isolation**: `QaAssistantManager` creates separate LangChain4j `AiServices` instances per user with Caffeine cache (max 200, 30min TTL). Each instance has its own ChatMemory and ContentRetriever filtered to the user's docs + public docs.
- **SSE streaming**: Backend uses `SseEmitter`, frontend uses `EventSource` with JWT as query parameter (EventSource cannot set custom headers).
- **Dual database profiles**: `pgsql` (production, with PgVector) and `h2` (dev, file-based at `./data/docmind`). Active profile set in `application.properties`.
- **Stateless JWT auth**: BCrypt passwords, HMAC-SHA JWT tokens via `jjwt 0.12.6`.

### Configuration

All custom properties use `docmind.*` namespace in `application.properties`. Secrets come from `.env` file (copy from `.env.example`). Key env vars: `DB_URL`, `DB_PASSWORD`, `JWT_SECRET`, `DASHSCOPE_API_KEY`.

Database schema is in `src/main/resources/schema.sql` — tables use `sys_user`, `doc_document` prefixes. Document entity uses pre-generated UUIDs and implements `Persistable<String>`.

### AI/RAG Pipeline

Configured in `AiConfig.java`:
- **Chat model**: OpenAI-compatible endpoint (defaults to Qwen via DashScope)
- **Embedding**: Provider-based (dashscope/zhipu/openai-compatible), configurable dimensions
- **RAG parameters**: chunk-size (500), chunk-overlap (100), max-results (5), min-score (0.6) — all configurable via env vars

### Test Structure

JUnit 5 + Mockito + AssertJ. Tests use `@Nested` classes for organization. Integration test (`DocMindApplicationTests`) is `@Disabled` (requires env vars). Tests are in `src/test/java/com/zm/docmind/service/`.
