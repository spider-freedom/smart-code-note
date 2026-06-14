<div align="center">

# 📚 智能笔记服务系统

**AI 笔记管理后端服务 — 异步处理 · 缓存 · 索引优化 · 限流 · RAG**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot)](https://spring.io/)
[![Java](https://img.shields.io/badge/Java-21-ed8b00?logo=openjdk)](https://openjdk.org/)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-4fc08d?logo=vue.js)](https://vuejs.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-6366f1)](https://platform.deepseek.com/)
[![Redis](https://img.shields.io/badge/Cache-Redis-dc382d?logo=redis)](https://redis.io/)
[![Guava](https://img.shields.io/badge/RateLimit-Guava-4285f4?logo=google)](https://github.com/google/guava)
[![RAG](https://img.shields.io/badge/AI-RAG-8b5cf6)](https://en.wikipedia.org/wiki/Retrieval-augmented_generation)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## 🏛️ 后端架构亮点（面试重点）

| 特性 | 描述 | 技术实现 |
|------|------|----------|
| 🔄 **异步 AI 处理** | 笔记上传后异步执行知识点提取+题目生成，上传接口响应 ~30s → ~200ms | 线程池 (`NoteAsyncService`) + 轮询 |
| ⚡ **Redis 缓存** | 用户信息、知识点列表、学习概览三层缓存，热点命中率约 70% | Spring Cache + Cache-Aside + Lettuce |
| 📊 **索引优化** | 5 个联合索引（user_id + create_time 等），列表查询 ~800ms → ~50ms | EXPLAIN + HikariCP |
| 🚦 **限流保护** | AI 接口单用户 10 次/分钟，令牌桶算法 | Guava RateLimiter + AOP |
| 📝 **统一日志** | AOP 拦截所有 Controller，请求耗时 + 慢请求 >3s 告警 | `WebLogAspect` |
| 🔢 **批量操作** | 批量删除笔记/知识点，单条 SQL IN 查询 | MyBatis-Plus `deleteBatchIds` |
| ⚙️ **工程规范** | 连接池调优 + 优雅停机 + Actuator 健康检查 | HikariCP + `server.shutdown=graceful` |
| 🧠 **自研 RAG** | 中文分块 → Embeddings 向量化 → 余弦相似度检索 → 上下文注入 | `ChunkingService` + `RetrievalService` |

---

## 📸 项目截图

### 工作台（Dashboard）

![工作台](docs/screenshots/02-dashboard.png)

*学习数据总览：今日任务、ECharts 统计图表、薄弱环节分析、AI 智能建议*

### 笔记上传

![笔记列表](docs/screenshots/03-notes.png)

*上传 `.md` / `.txt` 笔记，后台异步 AI 解析生成知识点和题目，上传即返回不阻塞*

### 知识点管理

![知识点列表](docs/screenshots/04-knowledge.png)

*结构化展示 AI 提取的知识点，支持编辑难度、掌握程度和复习时间*

### 题目管理

![题目管理](docs/screenshots/05-questions.png)

*AI 自动生成单选/多选/判断/主观题，按类型筛选，错题自动归集*

### 登录注册

![登录页](docs/screenshots/01-login.png)

*账号密码登录 + JWT 无状态认证*

### 个人中心

![个人中心](docs/screenshots/06-profile.png)

*个人信息编辑、头像上传、密码修改*

---

## ✨ 业务功能

| 模块 | 功能 |
|------|------|
| 🤖 **AI 笔记解析** | 上传笔记 → AI 提取 3-5 个知识点 → 每个知识点生成 5 道练习题（单选/多选/判断/主观） |
| 💬 **AI 编程助手** | 浮动聊天窗口，多轮对话 + Markdown 渲染，基于 RAG 检索笔记知识回答 |
| 📝 **在线练习** | 按笔记/知识点/题型筛选，实时判分，AI 生成答题反馈 |
| 🧠 **间隔复习** | 遗忘曲线驱动，三档评分（已掌握/模糊/遗忘）动态调整复习计划 |
| ❌ **错题本** | 错题自动归集，支持重练和标记已掌握 |
| 📊 **数据看板** | ECharts 图表 + 学习统计 + 薄弱环节 AI 分析 |

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.5 · Java 21 · MyBatis-Plus 3.5 · Maven |
| **数据库** | MySQL 8.0 + H2（测试）· HikariCP 连接池 |
| **缓存** | Redis 7.x（Lettuce 客户端）· Spring Cache |
| **AI** | DeepSeek API（Chat + Embeddings）· 自研 RAG 引擎 |
| **通信** | REST API · SSE 流式 · WebSocket 双向 |
| **认证** | JWT（Auth0 java-jwt）· HMAC256 · 拦截器 |
| **限流** | Guava RateLimiter · 令牌桶算法 · AOP 注解 |
| **监控** | Actuator · AOP 日志 · 慢请求告警 |
| **前端** | Vue 3 · TypeScript · Vite · Element Plus · Pinia · ECharts |
| **测试** | JUnit 5（9 后端集成测试）· Vitest（24 前端用例） |

---

## 📐 系统架构

```
┌──────────────────────────────────────────────────────────┐
│                    Vue 3 Web 前端 (Vite + TS)              │
│  Dashboard │ 笔记管理 │ 知识点 │ 题目 │ 练习 │ 复习 │ 聊天 │
└──────────────────────┬───────────────────────────────────┘
                       │  REST / SSE / WebSocket
                       ▼
┌──────────────────────────────────────────────────────────┐
│              Spring Boot 3.5 (Java 21)                     │
│                                                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ 用户模块  │ │ 笔记模块  │ │ 知识模块  │ │  题目模块    │ │
│  │ (JWT)    │ │ (异步)   │ │ (AI 生成) │ │  (AI 生成)   │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ 练习模块  │ │ 复习模块  │ │ 报告模块  │ │  聊天模块    │ │
│  │ (判分)   │ │ (遗忘曲线)│ │ (ECharts) │ │ (RAG+WS)    │ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────────┘ │
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │  异步处理层: NoteAsyncService (3 线程池)            │    │
│  │  缓存层: Redis (Cache-Aside, 5min/30min TTL)       │    │
│  │  限流层: RateLimitAspect (Guava Token Bucket)      │    │
│  │  日志层: WebLogAspect (统一请求日志 + 慢请求告警)   │    │
│  │  RAG 引擎: Chunking → Embedding → Retrieval        │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
│       MyBatis-Plus  ←→  MySQL 8.0  ←→  Redis 7.x        │
└──────────────────────────────────────────────────────────┘
```

---

## 🚀 快速启动

### 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| Java | 21+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | Web 前端 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 7.x | 缓存（可选 — 不配置则缓存功能不可用） |
| DeepSeek API Key | — | [获取](https://platform.deepseek.com/api_keys) |

### 步骤

```bash
# 1. 克隆
git clone https://github.com/spider-freedom/smart-code-note.git
cd smart-code-note

# 2. 配置
cp .env.example .env
# 编辑 .env 填入 DEEPSEEK_API_KEY / DB_URL / DB_USERNAME / DB_PASSWORD / REDIS_HOST

# 3. 数据库
# CREATE DATABASE smart_code_note DEFAULT CHARACTER SET utf8mb4;
# 应用启动后 MyBatis-Plus 自动建表

# 4. 后端
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. 前端（新终端）
cd frontend
npm install && npm run dev
```

- 后端：**http://localhost:8080** · 健康检查：`GET /actuator/health`
- 前端：**http://localhost:5173** · Vite 自动代理 `/api` → `localhost:8080`

---

## 📋 微信小程序模块

> ⚠️ **注意：** `miniapp/` 目录下的 uni-app 微信小程序仅完成了基础代码框架（页面结构 + API 封装 + 组件搭建），**尚未与后端 API 完成联调**，目前无法独立运行。该模块不作为简历亮点，仅作为跨端技术探索保留在仓库中。

---

## 🚢 部署

```bash
# 前端构建
cd frontend && npm run build

# 后端打包
cd backend && mvn clean package -DskipTests

# 运行
java -Dspring.profiles.active=prod \
     -DDEEPSEEK_API_KEY=sk-xxxxxxxx \
     -jar target/smart-code-note-0.0.1-SNAPSHOT.jar
```

详见 [部署检查清单](#) — Nginx 反向代理 · HTTPS · CORS · SSE 长连接 · WebSocket 代理。

---

## 📄 License

MIT © 2024 [spider-freedom](https://github.com/spider-freedom)

---

<div align="center">
  <sub>Built with Spring Boot, Vue 3, Redis, and DeepSeek AI</sub>
</div>
