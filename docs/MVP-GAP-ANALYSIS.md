# MindVault MVP 缺口清单

> 基于对仓库源码的逐文件核对（非 README 口径）。结论先行：**AIGC 主链路已闭环可跑，但鉴权未接入、4 个前端必需接口缺失、若干必现 bug 未修，当前不具备上线条件。**

审计范围：`common / auth / user / aigc / diary / business` 六个模块全部 Java 源码、MyBatis XML、`application*.yml`、`sql/`。
审计结论不代表运行期验证——本清单未启动应用实测。

---

## 0. 总览

| 等级 | 含义 | 条数 |
| :--- | :--- | :--- |
| **P0** | 阻断上线：安全或登录态根本不可用 | 3 |
| **P1** | 功能缺失：前端无法完成 MVP 业务 | 6 |
| **P1** | 逻辑缺陷：能跑但结果错 / 必现异常 | 10 |
| **P2** | 工程化与运维：可上线但会出事 | 7 |

---

## 1. P0 — 阻断级

### 1.1 鉴权链路完全未接入 ⚠️最严重

**证据** — `auth/src/main/java/com/yanglizi/config/MvcConfig.java:36-39`

```java
registry.addInterceptor(authenticationInterceptor)
        .addPathPatterns("/hi");        // 只拦截了占位接口
```

**连锁后果**

1. `AuthenticationInterceptor` 对业务接口从不执行 → `UserContext` 恒为 `null`。
2. 所有业务接口的 `userId` 都是**裸请求参数**，例如：
   - `ChatController.chat(..., Integer userId)`
   - `ChatController.getSession(..., Integer userId)`
   - `DiaryController` 全部 7 个方法
   - `SessionController` 全部 5 个方法

   → 任意人带上 `?userId=1` 即可读写他人的会话与日记，**无越权防护**。
3. `@CurrentUser` 注解只在 `LogoutController.java:5` 被 **import 但未使用**；`CurrentUserArgumentResolver` 虽在 `MvcConfig:43` 注册，全项目零处生效。
4. `@RequireLevel` 全项目仅 `business/src/main/java/com/yanglizi/business/controller/hi.java:12` 使用一次，等级体系未进任何真实接口。
5. 拦截器自身逻辑对缺失 token 直接 `return true`（`AuthenticationInterceptor.java:31-34`），即使接上也需要白名单配合。

**修复方案**

```java
// MvcConfig
registry.addInterceptor(authenticationInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/login/**", "/register/**", "/captcha/**",
            "/token/**", "/logout", "/error");
```

所有 Controller 的 `Integer userId` → `@CurrentUser Integer userId`。

**验收**：不带 token 访问 `/diary` 返回 401；带 A 用户 token 传 `?userId=B` 无法读到 B 的数据。

---

### 1.2 刷新令牌是一次性的，且 `revokeToken` 删错 key

**问题 A — 刷新即失效**：`RefreshTokenServiceImpl.java:41-49`

```java
String script = "local val = redis.call('GET', KEYS[1]) " +
                "redis.call('DEL', KEYS[1]) " +
                "return val";
```

`GET` 后立刻 `DEL`。前端并发刷新（或多标签页、或网络重试）时，第二个请求拿到 `null` → 抛 `LoginException` → **用户被强制登出**。

应改为：刷新令牌允许多次读取（或设置宽限期），只在新令牌签发时才作废旧令牌；或采用"同一 refreshToken 只允许签发一次新令牌 + 幂等返回"的策略。

**问题 B — 撤销无效**：`RefreshTokenServiceImpl.java:77-80`

```java
public void revokeToken(String refreshToken) {
    redisTemplate.delete(refreshToken);     // 漏了 REFRESH_TOKEN_KEY 前缀
}
```

真实 key 是 `REFRESH_TOKEN_KEY + refreshToken`。因此 `POST /token/revoke` **什么都没删，令牌仍然有效**。

对比 `LogoutServiceImpl.java:34-45` 是写对的（带前缀 + Lua 双删），两者行为不一致。

---

### 1.3 Access Token 仅 600 秒，且缺少 401 续期契约

**证据**

- `auth/src/main/resources/application.yml:88` — `expireTime: 600s`
- `AuthenticationInterceptor.java:56` — 失效时 `throw new TokenException("refresh_token")`
- `GlobalExceptionHandler.java` — **没有 `@ExceptionHandler(TokenException.class)`**

**后果**：`TokenException` 落到兜底的 `Exception` 分支，返回 `{"code":500,"message":"服务器繁忙，请稍后重试"}`。前端拿不到任何"该刷新令牌"的可识别信号，**自动续期无法实现**，用户每 10 分钟被登出一次。

**修复**：新增专用 handler，返回 401 + 明确业务码（如 `code: 401, message: "refresh_token"`），前端据此调 `/token/refresh`。

---

## 2. P1 — 缺失接口

| # | 缺失接口 | 必要性证据 | 落点 |
| :--- | :--- | :--- | :--- |
| 1 | `PUT /diary/{id}` 编辑日记 | `confirmDiary` 要求前端回传完整 `content`/`emotions`/`diaryDate`，但没有"取回草稿→编辑"的独立接口；草稿接口已被注释（`DiaryController.java:86-106`） | `DiaryController` |
| 2 | `DELETE /diary/{id}` | `deleted_at` 字段存在，所有 SQL 都有 `deleted_at is null` / `is null` 条件，但**全项目无任何代码写入该字段** → 删除功能完全缺失 | `DiaryController` + 软删 SQL |
| 3 | `GET /emotion` 心情字典 | `EmotionMapper.selectEmotion`（`where ai_gen = 0`）与 `EmotionService.getEmotionTags()` 均已实现，但**无 Controller 暴露**。前端无法渲染心情选择器，也无法得知哪些 tag 是标准 tag（直接影响 `custom:` 前缀逻辑） | 新增 `EmotionController` |
| 4 | `GET /user/me` / `PUT /user/me` | 注册可建用户、登录返回 `UserLoginVo`，但**无任何查询/修改自身资料的接口**；`UserService` 只有 `register` 和 `getUserLevelCode` | 新增 `UserController` |
| 5 | 会话删除需清理 Redis | `SessionServiceImpl.deleteSession` 只删了 `chat_sessions` + `diary_conversations`，未删 `chat:memory:{sid}_{uid}`（`RedisChatMemoryRepository`）与 `chat:state:{sid}`（`IntentClassifier`） | `SessionServiceImpl` |
| 6 | 历史消息顺序错误 | `ChatMapper.xml:65` 是 `order by round desc`，`ChatServiceImpl.getChatHistory` 直接返回，前端拿到倒序 | `ChatServiceImpl` |

**补充**：`POST /chat/test`（`ChatController.java:27`）是公开的调试接口，直连大模型并硬编码 system prompt，上线前必须删除或加开关。

---

## 3. P1 — 逻辑缺陷

### 3.1 向量检索被隐式锁死在"开心日记" ⚠️影响效果最大

`HybridSearchService.java:109-116`

```java
List<Integer> validIds = emotionFilterIds != null ?
        emotionFilterIds :
        diaryMapper.selectCurrentDiaryIdsByEmotionScore(userId, EMOTION_SCORE);   // = 5
```

用户**未传** `highEmotion` 时走 else 分支，仍按 `emotion_score >= 5` 取 ID 并拼进 `filterExpression`。

**后果**：普通检索永远搜不到情绪分 ≤4 的历史日记，即"找难过的回忆"这类查询必然无结果。

**修复**：else 分支应为"不加过滤"，仅在 `highEmotion=true` 时才构造过滤。

---

### 3.2 全文召回先取 3 条再内存过滤

`HybridSearchService.java:86-104` 中 `searchByFullText` 的 SQL **不含情绪过滤条件**，情绪过滤是在拿回 Top-3 之后用 `stream().filter()` 做的 → 可能出现"过滤后一条不剩"，召回率不稳定。

另外 `filterExpression` 用 `||` 拼 OR（`HybridSearchService.java:119-121`），ID 数量大时会生成超长表达式。

---

### 3.3 `handleEmotions` 边遍历边修改列表

`DiaryServiceImpl.java:169-176`

```java
for (String emotion : emotions) {
    if (!emotionTags.contains(emotion)) {
        emotions.remove(emotion);          // ConcurrentModificationException
        emotion = "custom:" + emotion;
        emotions.add(emotion);
    }
}
```

`confirmDiary` 与 `submitNewVersionDiary` 都会走到这里，**用户一旦传自定义心情就 500**。应收集后重建列表。

---

### 3.4 `custom:` 前缀会被重复叠加

`handleEmotions` 把非字典 tag 换成 `custom:xxx`，但 `custom:xxx` 本身也不在 `emotionTags` 中 → **重复确认时变成 `custom:custom:xxx`**。

---

### 3.5 定版无乐观锁，全量覆盖

`confirmDiary` 直接 `diaryMapper.update`；`version` 字段存在但未参与并发控制（`redisTemplate.increment` 只用于生成计数）。多标签页同时确认会互相覆盖。

---

### 3.6 摘要轮次比较可能 NPE

`AsyncDiaryServiceImpl.java:56`

```java
if (chatSession.getRawFactsLastRound() < chatSession.getLastRound()) {
```

`raw_facts_last_round` 为 `Integer`，新会话从未生成过摘要时为 `null` → **拆箱 NPE**。应先判空。

---

### 3.7 摘要补偿任务只有注释，没有实现

`AsyncChatServiceImpl.java:64-67`

```java
/*
    定时任务做补偿:
    select * from chat_sessions where raw_facts_pending_round is not null;
*/
```

乐观锁失败后该轮摘要永久缺失，且**无任何重试机制**，直接导致生成的日记丢失这段事实。

同时 `selectConversationByRoundRange`（`chatMapper.xml:55-57`）是 `round > #{diff}` 全量捞，**无 limit**，长会话会一次拉回数百轮喂给摘要模型。

---

### 3.8 失败态无操作入口

`GenStatusContants` 定义了 `PENDING=1 / GENERATING=2 / SUCCESS=3 / FAILED=4 / CANCELLED=5 / MANUAL=6`，`AsyncDiaryServiceImpl` 会写 `status=4`，但**没有重试接口、没有取消接口**。`/diary/queue` 能查到失败任务，前端却无法处理。

---

### 3.9 工具调用的 `userId` 由大模型填写

`DiarySearchTool.java:25-30`

```java
@Tool(description = "搜索用户的历史日记...")
public List<String> searchDiaries(
        @ToolParam(description = "用户心情低落的主要原因") String query,
        @ToolParam(description = "用户ID") Integer userId,          // ← 模型填
        @ToolParam(description = "是否只搜索高情绪分数日记...") Boolean highEmotion
)
```

`userId` 由 LLM 生成，模型填错或被提示注入即可**检索到他人日记**。应改为后端从 `UserContext` 注入，绝不出现在工具签名中。

---

### 3.10 其他必现问题

| 位置 | 问题 |
| :--- | :--- |
| `AiConfig.java:66-69` | `new RedisChatMemoryRepository()` 手动 new，其 `@Autowired private RedisTemplate` **字段注入不会生效** → `redisTemplate` 为 null，`findByConversationId` / `saveAll` 直接 NPE |
| `LoginServiceImpl.java:49-54` | 校验用 `Objects.equals(phone, "")`，`null` 可穿过；`accountLogin` 完全不校验，`Encrypt.sha256(null)` NPE |
| `LoginServiceImpl.java:105-113` | `checkPhotoCaptcha` 先 `delete` 再比对，输错一次就得重新取图；`null` 判断在 delete 之后，顺序应反转 |
| `DiaryServiceImpl.java:90-95` | `getDiaryListByVersionGroup` 参数为空时 `return null`，前端需特判 |
| `SessionServiceImpl.java:34-37` | 复用空会话时**不重置** `chat:state`，上一轮的 `GATHERING` 状态可能残留 |

---

## 4. P2 — 工程化与运维

| # | 问题 | 证据 / 影响 |
| :--- | :--- | :--- |
| 1 | **无 `application-prod.yml`** | 只有 `application-local.yml`，且 DB 明文密码 `123456` |
| 2 | **连接池未配置** | `application.yml` 无 `hikari` 配置；`@Async` 用默认 `SimpleAsyncTaskExecutor`（**无上限**）。SSE 长连接 + 异步摘要/生成会打满连接池 |
| 3 | **SSE 无心跳** | `ChatServiceImpl.getAiAnswer` 的 `Flux` 无 keep-alive，经网关/浏览器存在静默断连风险 |
| 4 | **异常未被区分** | `GlobalExceptionHandler` 只处理 `BizException` / `DataIntegrityViolationException` / `Exception`。`AiException`、`DiaryException`、`LevelException`、`CaptchaException`、`TokenException` **全部落到兜底** "服务器繁忙"，前端无法区分"无效会话"与真实故障 |
| 5 | **零测试** | 仅 `AigcApplicationTests` 空壳 |
| 6 | **DDL 缺失** | `sql/` 只有 `auth.sql`（`api_level_config` / `levels` / `users` 三张表）。`diaries` / `chat_sessions` / `diary_conversations` / `intent_keyword_config` / `emotion` 的建表语句**完全缺失**，新环境无法启动 |
| 7 | **残留文件** | `aigc/src/main/java/aaa.java` |

---

## 5. 建议补齐顺序

| 阶段 | 内容 | 理由 |
| :--- | :--- | :--- |
| **① 立即** | 1.1 鉴权接入 + `@CurrentUser` 改造；1.2 `revokeToken` 前缀 + 刷新策略；1.3 `TokenException` handler | 改动面小、收益最大。当前登录态是坏的，越权是敞开的 |
| **② 然后** | 3.10 的 `RedisChatMemoryRepository` 注入、3.3 `handleEmotions`、3.1 向量默认过滤、3.4 `custom:` 前缀 | 三条必现 bug + 一条严重影响检索效果 |
| **③ 接着** | 第 2 节 4 个缺失接口（`GET /emotion`、`GET\|PUT /user/me`、`DELETE /diary/{id}`、`PUT /diary/{id}`） | 补齐后前端才能完成 MVP 业务闭环 |
| **④ 最后** | 第 4 节 DDL + prod 配置 + 线程池 + 异常分类 | 上线前必须 |
| **⑤ P2** | 失败重试/取消、`@Scheduled` 摘要补偿、用户画像 | README 承诺但代码完全未实现 |

---

## 6. README 与实现的偏差（供参考）

`aigc/README.md` 中以下内容在代码中**无对应实现**，评估进度时不应计入已完成：

- Spring Cloud Gateway 接入层（JWT 鉴权 / 限流 / 路由转发）
- Dify Agent 工具编排、MinIO/OSS 对象存储、ELK 监控
- L3 长期记忆（PGVector 向量记忆 + 1536 维检索链路）与用户画像定时任务
- 两阶段日记生成（事实提取 + 文学润色分离）—— 实际为**一次调用**直接产出 `content/emotions/emotionScore`（`AsyncDiaryServiceImpl.java:86-90`）
- 关键词"数据库 + Redis 热更新"—— **有 `refreshByType`/`refreshAll` 但没有暴露任何管理接口**，且 Redis key TTL 24h，改库后最多 24 小时才生效

**指标类数字同样需重新核对**：README 称"BM25 与向量各召回 Top-10 → RRF → Top-3"，代码实际是**各召回 3 条 → RRF(k=60) → Top-5**（`HybridSearchService.java:28-32`）；"规则命中率 80-85%"、"RAG 命中率 91%"等无任何监控埋点支撑。

---

## 附：已实现且闭环的部分（无需改动）

为便于评估工作量，以下是核对后确认**功能完整**的部分：

- 会话创建/复用空会话、开场白写入、会话列表/详情/改标题/删除
- 对话 SSE 全链路：落库（`UPDATE ... RETURNING` 原子取轮次）→ 意图分层识别（规则 + Redis 关键词 + LLM 兜底 + 短消息上下文推理）→ 分流 → 流式输出 → 回答落库
- 停止对话（`chat:session:` 标记 + `takeWhile`）
- 增量摘要（窗口 = `maxMsg/2` = 5 轮，`raw_facts_pending_round` 乐观锁）
- 日记生成 → `status=3/4` → `afterCommit` 异步向量化
- 版本组管理（`version_group` + Redis `INCR` 版本号 + `closeCurrent` + `is_current`）
- 混合检索：ParadeDB 全文 + PGVector 向量 + RRF 融合
- 注册/登录/登出/刷新/图形验证码/短信验证码（短信为日志桩）
- 等级权限 AOP（`@RequireLevel` + Redis 动态配置）
