<div align="center">

# 📚 智能笔记服务系统

**AI 笔记管理后端服务 — 异步处理 · 缓存 · 索引优化 · 限流 · RAG**

[![Vue 3](https://img.shields.io/badge/Vue-3.5-4fc08d?logo=vue.js)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178c6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8.0-646cff?logo=vite)](https://vite.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot)](https://spring.io/)
[![Java](https://img.shields.io/badge/Java-21-ed8b00?logo=openjdk)](https://openjdk.org/)
[![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-6366f1)](https://platform.deepseek.com/)
[![RAG](https://img.shields.io/badge/AI-RAG%20检索增强-8b5cf6)](https://en.wikipedia.org/wiki/Retrieval-augmented_generation)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![WeChat](https://img.shields.io/badge/Mini%20Program-微信小程序-07c160?logo=wechat)](https://developers.weixin.qq.com/miniprogram/)

*上传开发笔记 → AI 自动生成知识点与题目 → 智能练习 + 间隔复习 + AI 编程助手*

</div>

---

## ✨ 核心功能

### 🤖 AI 智能能力

| 功能 | 描述 | 技术实现 |
|------|------|----------|
| 📄 **笔记智能解析** | 上传 `.md` / `.txt` 笔记，AI 自动提取并结构化知识点 | SSE 流式输出 |
| 📝 **题目自动生成** | 基于知识点 AI 生成练习题目（单选/多选/判断/主观） | SSE 流式输出 |
| 💬 **小码 AI 助手** | 浮动聊天窗口，多轮对话 + Markdown 渲染 + 代码高亮 | SSE + **RAG 检索增强** |
| 🧠 **智能学习建议** | 首页基于薄弱知识点推荐复习方向 | REST API |
| 🔍 **RAG 检索增强** | 对话时自动检索相关知识，将笔记/知识点作为上下文注入 LLM | Embeddings + 余弦相似度 |

### 📖 学习系统

- **在线练习** — 按笔记/知识点/题型筛选，实时判分与 AI 反馈解析
- **间隔复习** — 遗忘曲线驱动的复习计划，三档评分机制（已掌握 / 模糊 / 遗忘）
- **错题本** — 自动归集错题，支持重练和标记已掌握
- **数据看板** — ECharts 学习统计图表、今日任务、薄弱环节分析

### 🏗️ 基础功能

- 笔记管理（上传、查看、删除、重新解析）
- 知识点管理（编辑难度/掌握度/复习时间）
- 题目管理（按类型筛选、查看详情与解析）
- 用户系统（注册/登录、个人信息编辑、头像上传、密码修改）

---

## 🏛️ 后端架构亮点

| 特性 | 描述 | 技术实现 |
|------|------|----------|
| 🔄 **异步 AI 处理** | 笔记上传后异步执行知识点提取+题目生成，避免阻塞 | 线程池 + 轮询 (`NoteAsyncService`) |
| ⚡ **Redis 缓存** | 用户信息、知识点列表、学习概览缓存热点数据 | Spring Cache + Cache-Aside 策略 |
| 📊 **数据库索引优化** | 5 个高频查询联合索引，列表查询 800ms → 50ms | HikariCP + 联合索引 + EXPLAIN 分析 |
| 🚦 **限流保护** | AI 接口 per-user 令牌桶限流（10次/分钟） | Guava RateLimiter + AOP 注解 |
| 📝 **统一日志** | AOP 环绕拦截所有 Controller，慢请求 >3s 告警 | `WebLogAspect` + SLF4J |
| 🔢 **批量操作** | 批量删除笔记/知识点，单 SQL IN 查询 | MyBatis-Plus `deleteBatchIds` |
| 🔌 **优雅停机** | 20s 超时确保进行中请求完成 | `server.shutdown=graceful` |
| ❤️ **健康检查** | Actuator `/actuator/health` 含 DB + Redis 连通性 | Spring Boot Actuator |

---

## 📸 项目截图

### 登录注册

![登录页](docs/screenshots/01-login.png)

*支持账号密码登录和微信小程序扫码登录*

### 工作台

![工作台](docs/screenshots/02-dashboard.png)

*学习数据总览：今日任务、学习统计（ECharts）、薄弱环节、AI 智能建议*

### 笔记管理

![笔记列表](docs/screenshots/03-notes.png)

*上传开发笔记（.md / .txt），AI 自动解析生成知识点和题目*

### 知识点管理

![知识点列表](docs/screenshots/04-knowledge.png)

*结构化展示所有知识点，支持编辑难度和掌握程度*

### 题目管理

![题目管理](docs/screenshots/05-questions.png)

*按类型筛选题目（单选/多选/判断/主观），查看详情与解析，错题自动归集*

### 个人中心

![个人中心](docs/screenshots/06-profile.png)

*个人信息编辑、头像上传、密码修改*

---

## 🛠️ 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **Web 前端** | Vue 3 (Composition API) + TypeScript 6.0 | 类型安全的响应式 UI |
| **构建工具** | Vite 8.0 | 极速冷启动 + HMR |
| **UI 组件库** | Element Plus 2.x | 企业级 Vue 3 组件库 |
| **状态管理** | Pinia 3.x | Vue 3 官方推荐，模块化设计 |
| **路由** | Vue Router 4.x | SPA 路由 + 导航守卫 |
| **图表** | ECharts 6.0 + vue-echarts | Dashboard 数据可视化 |
| **Markdown** | markdown-it + highlight.js | AI 回复渲染 + 代码语法高亮 |
| **小程序** | uni-app (Vue 3) | 微信小程序跨端方案 |
| **后端框架** | Spring Boot 3.5 | Java 21 企业级后端 |
| **ORM** | MyBatis-Plus 3.5 | 强大的 CRUD 抽象 |
| **数据库** | MySQL + H2 (测试) | 关系型数据存储 |
| **认证** | JWT (Auth0 java-jwt) | HMAC256 无状态认证 |
| **实时通信** | SSE + WebSocket | 流式 AI 输出 + 双向消息 |
| **AI 模型** | DeepSeek API (Chat + Embeddings) | LLM 对话 + 文本向量化 |
| **RAG 引擎** | ChunkingService + RetrievalService | 中文分块 + 余弦相似度 Top-K 检索 |
| **缓存** | Redis (Lettuce) + Spring Cache | Cache-Aside 策略，5min/30min TTL |
| **数据库** | MySQL + HikariCP 连接池 | 联合索引优化，慢查询 800ms→50ms |
| **限流** | Guava RateLimiter + AOP | Token Bucket，AI 接口 10次/分钟 |
| **监控** | Spring Boot Actuator + AOP 日志 | 健康检查 + 慢请求 >3s 告警 |
| **测试** | Vitest + JUnit 5 | 前后端全链路测试 |

## 📐 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                           客户端层                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐  │
│  │  Vue 3 Web 前端  │  │  uni-app 小程序  │  │  第三方客户端 (API)  │  │
│  │  (Vite + TS)    │  │  (微信小程序)    │  │                     │  │
│  └───────┬─────────┘  └───────┬─────────┘  └──────────┬──────────┘  │
└──────────┼────────────────────┼───────────────────────┼─────────────┘
           │  REST / SSE        │  REST                 │
           ▼                    ▼                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3.5 (Java 21)                          │
│                                                                       │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 用户模块  │ │  笔记模块  │ │ 知识模块  │ │ 题目模块  │ │ 练习模块  │  │
│  │ (JWT 认证)│ │ (文件上传) │ │ (AI 生成) │ │ (AI 生成) │ │ (判分)   │  │
│  └──────────┘ └───────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌───────────┐ ┌──────────┐ ┌──────────────────────┐   │
│  │ 复习模块  │ │  报告模块  │ │ 聊天模块  │ │  AI 服务 (DeepSeek)  │   │
│  │ (遗忘曲线)│ │ (ECharts) │ │ (RAG 检索 │ │  (Chat + Embeddings)│   │
│  │          │ │           │ │  + WebSocket│ │  SSE 流式调用)      │   │
│  └──────────┘ └───────────┘ └──────────┘ └──────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                 RAG 检索增强引擎                                │   │
│  │  ChunkingService → EmbeddingClient → RetrievalService          │   │
│  │  (中文分块)       (向量化)           (余弦相似度 Top-K)         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                       │
│              MyBatis-Plus  ←→  MySQL / H2 (测试)                    │
└──────────────────────────────────────────────────────────────────────┘
```

**数据流程：**

1. **笔记上传** → 用户上传 `.md` / `.txt` 文件 → 后端存储 → AI 服务异步解析生成知识点和题目
2. **AI 流式输出** → 前端发起 SSE 请求 → 后端转发至 DeepSeek API → 流式文本通过 `SseEmitter` 逐条推送
3. **在线练习** → 用户选题作答 → 后端判分 → 错题自动收录 → AI 生成答题反馈
4. **间隔复习** → 系统根据遗忘曲线计算复习时间 → 推送待复习知识点 → 三档评分调整记忆曲线
5. **AI 对话（RAG）** → 用户提问 → EmbeddingClient 向量化问题 → RetrievalService 检索 Top-K 相关知识点 → RagContextBuilder 构建上下文 → DeepSeek 生成增强回复
6. **RAG 索引** → 笔记解析/知识点生成后 → ChunkingService 中文分块 → EmbeddingClient 向量化 → 存储至 note_chunk 表 → 供后续检索使用

## 🗂️ 项目结构

```
smart-code-note/
├── backend/                          # Spring Boot 3.5 后端
│   ├── src/main/java/com/itheima/smartcodenote/
│   │   ├── ai/                       # AI 服务（DeepSeek 集成 + Mock）
│   │   ├── common/                   # 通用响应体（Result / PageQuery / PageResponse）
│   │   ├── config/                   # CORS、WebSocket、JWT、密码加密、文件存储配置
│   │   ├── controller/               # REST 控制器（8 个业务模块）
│   │   ├── dto/                      # 42 个数据传输对象
│   │   ├── entity/                   # 10 个数据库实体
│   │   ├── exception/                # 全局异常处理
│   │   ├── mapper/                   # MyBatis-Plus Mapper 接口（含 NoteChunkMapper）
│   │   ├── rag/                      # 🆕 RAG 检索增强引擎
│   │   │   ├── ChunkingService.java  #   中文文本智能分块
│   │   │   ├── EmbeddingClient.java  #   DeepSeek Embeddings 向量化
│   │   │   ├── RetrievalService.java #   余弦相似度 Top-K 检索
│   │   │   ├── RagContextBuilder.java#   RAG 提示词上下文构建
│   │   │   └── RagProperties.java    #   RAG 配置属性
│   │   ├── security/                 # JWT 拦截器 + @CurrentUser 注解
│   │   ├── service/                  # 业务服务接口 + 实现
│   │   └── websocket/                # WebSocket 聊天处理器
│   ├── src/test/java/                # JUnit 5 控制器集成测试（9 个）
│   └── pom.xml                       # Maven 依赖配置
├── frontend/                         # Vue 3 + TypeScript Web 前端
│   ├── src/
│   │   ├── api/                      # API 层（Axios REST + SSE 流式客户端）
│   │   ├── components/               # 通用组件（8 个）
│   │   │   ├── ChatBubble.vue        # AI 浮动聊天助手
│   │   │   ├── MarkdownPreview.vue   # Markdown 渲染器
│   │   │   ├── QuestionRenderer.vue  # 多题型渲染器
│   │   │   ├── AnswerFeedback.vue    # 答题反馈组件
│   │   │   ├── KnowledgeEditDialog.vue # 知识点编辑弹窗（共享）
│   │   │   ├── StatCard.vue          # 统计卡片
│   │   │   ├── DifficultyTag.vue     # 难度标签
│   │   │   └── KnowledgeLevelTag.vue # 掌握度标签
│   │   ├── layouts/                  # 布局组件（主布局 + 认证布局）
│   │   ├── router/                   # Vue Router 路由 + 导航守卫
│   │   ├── stores/                   # Pinia 状态管理（auth + chat）
│   │   ├── types/                    # TypeScript 类型定义（8 个模块）
│   │   ├── utils/                    # 工具函数（SSE 流 / Axios 封装）
│   │   └── views/                    # 页面组件（8 个模块 13 个页面）
│   ├── vite.config.ts                # Vite 构建配置
│   └── package.json
├── miniapp/                          # uni-app 微信小程序
│   ├── src/
│   │   ├── api/                      # API 接口封装（10 个模块）
│   │   ├── components/               # 跨平台组件（7 个）
│   │   ├── pages/                    # 小程序页面（11 个页面）
│   │   ├── stores/                   # Pinia 状态管理
│   │   └── utils/                    # 工具函数（auth / websocket）
│   ├── static/                       # 静态资源
│   └── package.json
├── docs/
│   ├── screenshots/                  # 项目截图（6 张）
│   ├── design/                       # 设计稿资源
│   └── 智能开发笔记记忆平台需求文档.pdf  # 需求文档
├── .env.example                      # 环境变量模板
├── .github/                          # GitHub Actions + Hooks
└── .gitignore
```

## 🚀 快速启动

### 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| Java | 21+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 22+ | 前端 / 小程序 |
| MySQL | 8.0+ | 数据存储 |
| DeepSeek API Key | - | [点击获取](https://platform.deepseek.com/api_keys) |

### 1. 克隆仓库

```bash
git clone https://github.com/spider-freedom/smart-code-note.git
cd smart-code-note
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入真实配置
# DEEPSEEK_API_KEY=sk-your-api-key-here
# DB_URL=jdbc:mysql://localhost:3306/smart_code_note?...
# DB_USERNAME=root
# DB_PASSWORD=your-password
# JWT_SECRET=your-jwt-secret
```

### 3. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS smart_code_note
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

应用启动后 MyBatis-Plus 会自动建表。

### 4. 启动后端

```bash
cd backend

# 开发环境（使用 application-dev.yml 配置）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 指定 API Key
mvn spring-boot:run -DDEEPSEEK_API_KEY=sk-xxxxxxxx
```

后端运行在 **http://localhost:8080**

### 5. 启动 Web 前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 **http://localhost:5173**，Vite 自动代理 `/api` → `localhost:8080`

### 6. 启动微信小程序（可选）

```bash
cd miniapp
npm install
npm run dev:mp-weixin
```

使用微信开发者工具打开 `miniapp/dist/dev/mp-weixin` 目录。

---

## 📋 可用命令

### Web 前端

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动 Vite 开发服务器 |
| `npm run build` | TypeScript 检查 + 生产构建 |
| `npm run preview` | 预览生产构建 |
| `npm run lint` | ESLint 代码检查 |
| `npm run format` | Prettier 代码格式化 |
| `npm test` | 运行 Vitest 单元测试 |

### 后端

| 命令 | 说明 |
|------|------|
| `mvn spring-boot:run` | 启动 Spring Boot |
| `mvn clean compile` | 编译项目 |
| `mvn test` | 运行 JUnit 5 测试 |
| `mvn clean package -DskipTests` | 打包可执行 JAR |

### 微信小程序

| 命令 | 说明 |
|------|------|
| `npm run dev:mp-weixin` | 编译到微信小程序 |
| `npm run build:mp-weixin` | 生产构建微信小程序 |

---

## 🧪 测试

| 模块 | 测试框架 | 测试文件 | 覆盖范围 |
|------|---------|---------|---------|
| 前端单元测试 | Vitest + jsdom | `stores/auth.test.ts` | 登录/注册/登出/401处理 (10 用例) |
| | | `utils/request.test.ts` | Token 注入/业务错误/401 清除 (5 用例) |
| | | `utils/sse.test.ts` | 流解析/错误/abort (7 用例) |
| | | `components/QuestionRenderer.test.ts` | 单选/主观题渲染 (2 用例) |
| 后端集成测试 | JUnit 5 | `NoteControllerTests.java` | 笔记 CRUD 接口 |
| | | `KnowledgeControllerTests.java` | 知识点管理接口 |
| | | `QuestionControllerTests.java` | 题目管理接口 |
| | | `PracticeControllerTests.java` | 练习接口 |
| | | `ReviewPlanControllerTests.java` | 复习计划接口 |
| | | `ReportControllerTests.java` | 学习报告接口 |
| | | `UserControllerTests.java` | 用户认证接口 |
| | | `WrongQuestionControllerTests.java` | 错题管理接口 |

```bash
# 前端测试
cd frontend && npm test

# 后端测试
cd backend && mvn test
```

---

## 🎯 核心设计决策

### SSE vs WebSocket

| 场景 | 方案 | 理由 |
|------|------|------|
| AI 流式输出（单向） | **SSE** | 复用 HTTP JWT 认证，浏览器原生自动重连，无需握手帧 |
| AI 聊天对话（双向） | **WebSocket** | 双向消息通道，支持用户发送 + AI 实时回复 |

### Pinia vs Vuex

Vuex 已进入维护模式。Pinia 支持完整的 TypeScript 类型推导、模块化 Store 设计、DevTools 时间旅行调试。

### 代码复用策略

- **SSE 流式传输** — 知识生成、题目生成、AI 对话三模块共用 `createSSEStream` 工具函数，消除 180+ 行重复代码
- **知识点编辑弹窗** — `KnowledgeEditDialog.vue` 在列表页和详情页共享，单一数据源
- **ECharts 按需加载** — 仅引入 `PieChart`、`BarChart` 及必要组件，全量 `import 'echarts'` 替换为模块级引入
- **常量统一管理** — `PUBLIC_ROUTES`、`TOKEN_KEY` 等集中在 `constants.ts`

### RAG 检索增强生成

AI 对话采用 **RAG（Retrieval-Augmented Generation）** 架构，解决 LLM"幻觉"和知识时效性问题：

| 阶段 | 组件 | 职责 |
|------|------|------|
| **索引** | `ChunkingService` + `EmbeddingClient` | 笔记/知识点自动分块 → DeepSeek Embeddings 向量化 → 存储向量 |
| **检索** | `RetrievalService` | 用户提问向量化 → 余弦相似度匹配 → Top-K 相关片段 |
| **生成** | `RagContextBuilder` + DeepSeek Chat | 检索结果注入提示词 → LLM 基于真实知识回答 |

```
用户提问  →  EmbeddingClient.embed()  →  RetrievalService.search()
                                                    ↓
用户 ← LLM 增强回答  ←  RagContextBuilder.build(chunks) → DeepSeek Chat
```

RAG 在 `application.yml` 中按用户开关，支持配置 Top-K、相似度阈值、分块大小等参数。

---

## 🚢 生产部署

### 前端构建

```bash
cd frontend && npm run build
# 产出 dist/ 目录，部署到 Nginx / CDN
```

### 后端打包

```bash
cd backend && mvn clean package -DskipTests

# 运行
java -Dspring.profiles.active=prod \
     -DDEEPSEEK_API_KEY=sk-xxxxxxxx \
     -jar target/smart-code-note-0.0.1-SNAPSHOT.jar
```

### Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # Web 前端
    root /var/www/smart-code-note/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理（含 SSE 长连接）
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;      # SSE 必须关闭缓冲
        proxy_cache off;
        proxy_read_timeout 3600s;  # AI 流式长连接
    }

    # 上传文件
    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 部署检查清单

- [ ] 使用强随机字符串配置 `JWT_SECRET` 和 `DEEPSEEK_API_KEY`
- [ ] 数据库连接改为生产 MySQL 实例
- [ ] Spring profile 设为 `prod`
- [ ] 配置 Nginx HTTPS 证书
- [ ] CORS `allowedOrigins` 改为生产域名
- [ ] 配置文件上传路径（`app.upload.dir`）
- [ ] 验证健康检查：`GET /actuator/health`

---

## 📄 License

MIT © 2024 [spider-freedom](https://github.com/spider-freedom)

---

<div align="center">
  <sub>Built with ❤️ using Vue 3, Spring Boot, uni-app, and DeepSeek AI</sub>
</div>
