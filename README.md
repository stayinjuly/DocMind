# DocMind

DocMind 是一款基于 RAG（检索增强生成）技术的 AI 文档问答系统，支持多用户隔离、文档管理和流式问答。

## 功能特点

- **文档管理** - 上传、解析、删除文档，支持 TXT / Markdown / PDF / Word 格式
- **智能问答** - 基于向量语义检索 + LLM 生成，提供上下文相关的准确回答
- **流式输出** - 支持 SSE 流式问答，实时返回生成内容
- **用户隔离** - JWT 认证，每个用户只能检索自己的文档和公共文档
- **对话记忆** - 每用户独立对话历史，支持清除
- **多数据库** - 支持 PostgreSQL（生产）和 H2（开发）两种 Profile

## 技术栈

### 后端
| 组件 | 技术 |
|---|---|
| 框架 | Spring Boot 4.0 |
| AI 框架 | LangChain4j 1.12 |
| LLM | MiniMax-M2.7（OpenAI 兼容接口） |
| Embedding | 通义千问 text-embedding-v4（1024 维） |
| 向量存储 | PostgreSQL + pgvector |
| 文档解析 | Apache Tika |
| 认证 | Spring Security + JWT |
| 数据库 | PostgreSQL / H2 |

### 前端
| 组件 | 技术 |
|---|---|
| 框架 | Vue 3 + TypeScript |
| UI | Element Plus |
| 构建 | Vite |
| 状态管理 | Pinia |
| 路由 | Vue Router |

## 项目结构

```
DocMind/
├── src/main/java/com/zm/docmind/
│   ├── config/             # 配置（AI、Security、全局异常处理）
│   ├── controller/         # REST API（文档、问答、认证）
│   ├── dto/                # 数据传输对象
│   ├── entity/             # 实体类
│   ├── repository/         # 数据访问层
│   ├── security/           # JWT 过滤器
│   └── service/            # 业务逻辑（文档处理、RAG、认证）
├── src/main/resources/
│   ├── application.properties          # 主配置
│   ├── application-pgsql.properties    # PostgreSQL Profile
│   ├── application-h2.properties       # H2 Profile
│   └── schema.sql                      # 数据库表结构
└── docmind-web/            # Vue 3 前端
```

## API 接口

### 认证
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/login` | 用户登录 |

### 文档
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/documents` | 获取当前用户的文档列表 |
| GET | `/documents/public` | 获取公共文档列表 |
| GET | `/documents/{id}` | 获取文档详情 |
| POST | `/documents` | 上传文档（multipart/form-data） |
| DELETE | `/documents/{id}` | 删除文档 |

### 问答
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/qa?question=xxx` | 普通问答 |
| GET | `/qa/stream?question=xxx` | 流式问答（SSE） |
| DELETE | `/qa/history` | 清除对话历史 |

## 数据库设计

### doc_document（文档元数据）
由 Spring Data JDBC 管理，存储文档基本信息。

### doc_embedding（向量存储）
由 PgVector 自动创建和管理，存储文档分块的向量嵌入，通过 `documentId` 与 `doc_document` 关联。

## 快速开始

### 环境要求
- Java 25
- Maven
- PostgreSQL（需安装 pgvector 扩展）

### 配置

修改 `application.properties` 中的相关配置：

```properties
# 数据库 Profile: pgsql（生产）或 h2（开发）
spring.profiles.active=pgsql

# LLM 配置
openai.api-key=your-api-key
openai.base-url=https://api.minimaxi.com/v1
openai.model=MiniMax-M2.7

# Embedding 配置
dashscope.api_key=your-dashscope-key
dashscope.embedding-model=text-embedding-v4

# 文档存储路径
docmind.storage.path=/path/to/your/docs
```

### 运行

```bash
# 后端
mvn spring-boot:run

# 前端
cd docmind-web
npm install
npm run dev
```
