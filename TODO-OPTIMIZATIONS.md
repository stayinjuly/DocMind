# DocMind 待优化事项

> 已完成第一批修复（关键 Bug + 安全加固），以下为剩余待处理项。

## 一、资源管理与稳定性

### ~~1. 自定义异步线程池~~ ✅ 已完成
- **文件**: `DocumentProcessingService.java`
- **问题**: `@Async` 使用默认 `SimpleAsyncTaskExecutor`，无上限线程创建，高并发上传会耗尽资源
- **方案**: 注册自定义 `ThreadPoolTaskExecutor` Bean，设置 corePoolSize=2, maxPoolSize=5, queueCapacity=50

### ~~2. QaAssistantManager 缓存无上限~~ ✅ 已完成
- **文件**: `QaAssistantManager.java:39-40`
- **问题**: `ConcurrentHashMap` 无容量限制，大量用户涌入可能 OOM
- **方案**: 用 Caffeine 替代，设置 `maximumSize(200)` 和 `expireAfterAccess(30min)`

### ~~3. Tika 解析器每次调用重新创建~~ ✅ 已完成
- **文件**: `DocumentParserService.java:21`
- **问题**: `ApacheTikaDocumentParser` 是重量级对象，每次 `parseDocument` 都 new 一个
- **方案**: 提升为类字段，构造时初始化一次

## 二、代码质量

### 4. 提取文档所有权检查逻辑
- **文件**: `DocumentController.java:62-76, 78-92`
- **问题**: `deleteDocument` 和 `getDocument` 有重复的所有权检查代码
- **方案**: 在 `DocumentService` 中新增 `getDocumentForUser(id, userId)` 方法

### 5. Document 状态字段用枚举替代字符串
- **文件**: `DocumentService.java`, `DocumentProcessingService.java` 多处
- **问题**: "PENDING"/"PROCESSING"/"COMPLETED"/"FAILED" 散布为魔法字符串
- **方案**: 创建 `DocumentStatus` 枚举

### 6. 嵌入批量处理分段提交
- **文件**: `DocumentProcessingService.java:embedDocument`
- **问题**: 大文档可能产生上千 segment，单次 `embedAll` 可能超时或触发 API 限流
- **方案**: 分批处理，每批 50-100 个 segment

### 7. 分页参数边界校验
- **文件**: `DocumentController.java:32-33`
- **问题**: `size` 无上限，客户端可传 `size=999999`
- **方案**: 加 `@Max(100)` 校验或 Service 层 clamp

### 8. JwtService 异常捕获范围过宽
- **文件**: `JwtService.java:53-60`
- **问题**: `isTokenValid` 捕获 `Exception`，包括 `NullPointerException` 等编程错误
- **方案**: 缩窄为 `JwtException`

## 三、基础设施补全

### 9. 日志配置
- **问题**: 无 `logback-spring.xml`，无文件输出、无滚动策略、无分级日志
- **方案**: 添加 `logback-spring.xml`，配置文件 appender + 滚动策略

### 10. 健康检查端点
- **问题**: 缺少 `spring-boot-starter-actuator`，无健康检查和监控端点
- **方案**: 添加 actuator 依赖

### 11. 数据库迁移工具
- **问题**: 使用 `schema.sql` + `always` 模式，无版本管理
- **方案**: 引入 Flyway 或 Liquibase

### 12. API 文档
- **问题**: 无 Swagger/OpenAPI，API 文档依赖手写 README
- **方案**: 添加 `springdoc-openapi`

### 13. 接口限流
- **问题**: `/auth/**` 和 `/qa/**` 无限流，易被暴力攻击和 API 滥用
- **方案**: 引入 Bucket4j 或 Resilience4j

### 14. HikariCP 连接池参数补全
- **文件**: `application-pgsql.properties`
- **问题**: 缺少 `leak-detection-threshold`、`connection-timeout` 等关键参数
- **方案**: 补充推荐配置

### 15. 容器化
- **问题**: 无 Dockerfile / docker-compose.yml
- **方案**: 创建容器化部署配置

### 16. 单元测试
- **问题**: 仅有 context load 测试，无任何业务逻辑测试
- **方案**: 为 `AuthService`、`JwtService`、`DocumentService`、`QaAssistantManager` 编写单元测试
