# DocMind 架构评审报告

## Context
用户希望对项目架构进行全面梳理，评估当前设计是否合理。以下是按优先级分类的发现。

---

## 整体评价

项目分层清晰（Controller → Service → Repository），职责划分基本合理，代码质量在同类项目中属于中上水平。以下按严重程度列出需要关注的问题。

---

## 🔴 高优先级（建议尽快修复）

### 1. 前端 API 地址硬编码
- **文件**: `docmind-web/src/api/index.ts:4`
- **问题**: `API_BASE = 'http://localhost:8080'` 硬编码，无 `.env` 文件，无 Vite 代理。部署到任何非本地环境都会失败。
- **建议**: 使用 `import.meta.env.VITE_API_BASE` 或配置 Vite proxy。

### 2. 前端 401 拦截器绕过 Pinia Store
- **文件**: `docmind-web/src/api/index.ts:28-30`
- **问题**: 401 时直接操作 `localStorage.removeItem()`，不经过 `userStore.logout()`，导致 Pinia 响应式状态与 localStorage 不同步，页面状态可能错乱。同时使用 `window.location.href` 强制刷新而非 `router.push`。
- **建议**: 导入 userStore 调用 `logout()`，用 `router.push('/login')` 替代。

### 3. SSE 连接泄漏
- **文件**: `docmind-web/src/views/ChatView.vue`
- **问题**: `EventSource` 没有 `onUnmounted` 清理。用户在流式输出期间切换页面，SSE 连接会持续存在。
- **建议**: 添加 `onUnmounted(() => eventSource?.close())` 。

### 4. Embedding 生命周期职责分裂
- **文件**: `DocumentProcessingService.java`（创建 embedding）+ `DocumentService.java`（删除 embedding）
- **问题**: 两个 Service 都注入 `EmbeddingStore`，创建和删除分散在不同类中，违反单一职责。
- **建议**: 将 embedding 删除逻辑从 `DocumentService` 移到 `DocumentProcessingService`，或提取独立的 `EmbeddingService`。

### 5. 文档处理失败后记录被删除
- **文件**: `DocumentProcessingService.java:121-141`
- **问题**: `cleanupOnFailure` 同时删除了 embedding、文件和数据库记录。用户无法看到 FAILED 状态，文档直接消失。
- **建议**: 失败时保留数据库记录并标记 FAILED，允许用户查看失败状态并重试。

### 6. `DocumentProcessingService` catch 块重复保存
- **文件**: `DocumentProcessingService.java:70-81`
- **问题**: 第72行 `updateDocumentStatus(documentId, FAILED)` 已经保存了 FAILED 状态，第74-79行又读出来再保存一次，完全冗余。

---

## 🟡 中优先级（建议近期修复）

### 7. 前端 SSE 每个 chunk 触发一次渲染
- **文件**: `docmind-web/src/views/ChatView.vue:40`
- **问题**: `messages.value[assistantIndex].content += event.data` 每个 token 都触发 Vue 重渲染，长回答时性能差。
- **建议**: 使用 `requestAnimationFrame` 或 debounce 批量更新。

### 8. Element Plus 全量引入
- **文件**: `docmind-web/src/main.ts`
- **问题**: `app.use(ElementPlus)` 引入全部组件和 CSS，打包体积大（~800KB+）。
- **建议**: 配置按需自动导入（`unplugin-vue-components` + `unplugin-auto-import`）。

### 9. `InMemoryChatMemoryStore` 不持久化
- **文件**: `QaAssistantManager.java:56`
- **问题**: 对话历史存在内存中，重启丢失，不支持多实例部署。
- **建议**: 生产环境切换为数据库持久化的 ChatMemoryStore。

### 10. 数据库 schema 缺少外键约束
- **文件**: `src/main/resources/schema.sql`
- **问题**: `doc_document.user_id` 存 email 而非 `sys_user.id`，无外键约束。用户邮箱变更会破坏关联；删除用户会产生孤立文档。
- **建议**: `user_id` 改为引用 `sys_user.id` 的外键，或至少添加 `ON DELETE CASCADE`。

### 11. PgVector 无近似索引
- **问题**: 未创建 HNSW 或 IVFFlat 索引，向量查询走暴力搜索，数据量超过 ~10 万条后性能会显著下降。
- **建议**: 数据量增长后添加 HNSW 索引。

### 12. `user_id` 存储邮箱而非主键 ID
- **文件**: `DocumentService.java` 中 `userId` 来自 JWT 的 subject（即 email）
- **问题**: 邮箱变更场景下文档归属关系会断裂。
- **建议**: JWT 中使用用户 ID 作为 subject，`doc_document.user_id` 也改为引用 `sys_user.id`。

### 13. 文档错误提示信息遗漏文件类型
- **文件**: `DocumentService.java:135`
- **问题**: 错误消息说"仅支持 TXT、Markdown、PDF 和 Word 文件"，但实际 `SUPPORTED_TYPES` 包含 `xlsx`、`xls`、`csv`。

### 14. Tika Parser 共享实例的线程安全
- **文件**: `DocumentParserService.java:20`
- **问题**: `ApacheTikaDocumentParser` 作为单例字段，在异步线程池中被并发调用。
- **建议**: 改为每次调用创建新实例，或验证当前版本是线程安全的。

---

## 🟢 低优先级（可后续优化）

### 15. 无文档处理重试机制
- Embedding API 瞬态故障会导致永久 FAILED，无重试队列。

### 16. 无管理后台/管理员角色
- 所有用户均为 `ROLE_USER`，无法统一管理。

### 17. JWT 无刷新/吊销机制
- Token 泄露后无法在过期前失效。

### 18. 缺少测试
- `DocumentProcessingService`（核心管道）、`DocumentParserService`、所有 Controller、Security 配置均无测试覆盖。

### 19. 前端小问题
- `components/` 目录为空，`ChatView` 可提取子组件
- `index.html` 标题仍是默认的 `docmind-web`
- 无 404 路由
- 聊天消息用数组 index 做 `:key`，应使用唯一 ID

### 20. 前端 `qaApi.ask` 使用 GET 方法
- **文件**: `docmind-web/src/api/index.ts:65`
- **问题**: 前端仍用 GET 调用 `/qa`，但后端已改为 POST。当前前端只用了 SSE 流式，普通问答接口未使用，但代码不一致。

---

## 架构合理性总结

| 方面 | 评价 |
|---|---|
| 分层架构 | ✅ 清晰规范 |
| 职责分离 | ⚠️ embedding 生命周期分裂需合并 |
| 安全设计 | ✅ JWT + Spring Security 基本完善，query param 传 token 是 SSE 的已知妥协 |
| 数据模型 | ⚠️ 缺少外键、user_id 存 email |
| 配置管理 | ✅ .env + profiles 设计良好 |
| 异步处理 | ✅ @Async 线程池合理，但需重试和错误展示 |
| 前端工程化 | ⚠️ API 硬编码、全量引入、SSE 泄漏需修复 |
| 测试覆盖 | ❌ 核心管道零测试 |

## 实施建议

建议按以下顺序修复，每项都是独立的，可以分批进行：

1. **第一批**（前端阻断性问题）：#1 API 地址配置化 + #2 401 拦截器修复 + #3 SSE 清理
2. **第二批**（后端逻辑问题）：#4 合并 embedding 职责 + #5 保留 FAILED 记录 + #6 删除冗余代码 + #13 修正错误消息
3. **第三批**（性能和扩展性）：#7 SSE 批量渲染 + #8 Element Plus 按需引入 + #9 持久化对话 + #14 Tika 线程安全
4. **第四批**（数据模型和长期优化）：#10-12 数据库优化 + #11 向量索引 + #15-20 其他
