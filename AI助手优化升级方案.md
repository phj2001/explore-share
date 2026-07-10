# AI 探索助手优化升级方案

基于对现有代码（`smart-campus-backend/src/main/java/com/smartcampus/assistant/`）的走查，本方案针对当前实现的四个主要短板给出具体、可落地的升级路径，按优先级排列。

## 现状基线

当前已具备：Spring AI + DashScope(Qwen) 真实 LLM 调用、Function Calling（4 个工具）、pgvector 语义检索 RAG、语义缓存、限流、护栏（`AssistantGuard`）、LLM-as-judge 评测。缺口集中在：功能默认关闭、无多轮记忆、伪流式、无个性化。数据库中 `POIFavorite`、`POICheckIn`、`POIReview`、`UserRouteFavorite` 等个性化原始数据其实已经存在，只是助手从未读取。

---

## P0：启用与可用性（工作量：0.5 人日）

1. 在 `.env` 中配置真实 `DASHSCOPE_API_KEY`，将 `ASSISTANT_ENABLED` 置为 `true`。
2. 补一条最小冒烟测试：部署后自动调用 `/api/assistant/chat` 一次固定 query，失败则告警——避免密钥失效或欠费导致功能静默不可用（`@ConditionalOnProperty` 关闭时前端会拿到 404，需要前端做兜底文案而不是空白报错）。
3. 前端 `AssistantChat.vue` 增加"助手未启用/暂时不可用"的降级提示状态，而不是让用户以为是网络问题。

验收：真实用户可在生产环境发起对话并得到回复。

---

## P1：多轮对话记忆（工作量：2-3 人日）

当前 `ChatRequest` 无 `conversationId`/历史字段，每轮请求都是独立的，用户说"再远一点的"这类指代性追问会失败。

**实现方案**：
- `ChatRequest` 新增可选 `conversationId`（前端首次对话时生成 UUID，后续复用）。
- 新建 `assistant_conversation_turn` 表（或直接用 Redis List，TTL 30 分钟即可，没必要长期持久化闲聊记录）：存 `conversationId`、`role`、`content`、`createdAt`。
- `AssistantChatService.stream()` 组装 prompt 时，取最近 N 轮（建议 N=6，防止 token 膨胀）历史一并传入 `chatClient.prompt().messages(...)`。
- **语义缓存需要跟着调整**：现有 `AssistantSemanticCache` 是按单条 query 做语义匹配，多轮场景下同一句话在不同上下文里含义不同（如"这个"），必须让缓存 key 感知是否有上文——建议规则是**有历史的多轮请求一律跳过语义缓存**，只对首轮/无上下文请求做缓存，避免语义漂移导致答非所问。
- 限流器 `AssistantRateLimiter` 按 conversationId 或 userId 计数即可，无需改动。

验收：连续追问"附近有安静的咖啡馆吗" → "远一点的呢" 能正确延续上下文。

---

## P2：个性化推荐（工作量：3-4 人日）

方案原文设计了 `userContext` 工具但代码里未实现；数据库里 `POIFavorite`/`POICheckIn`/`POIReview` 数据已经现成可用。

**实现方案**：
1. 新增 `ExplorerTools.getUserPreferences()` 工具（`@Tool`），复用现有 `POIFavoriteService`/`POICheckInService`，返回用户最近收藏/签到过的地点类别分布（如"用户常去：咖啡馆、图书馆"），供模型作为推荐依据，而不是直接拼进 system prompt（避免每次都消耗 token，且并非每次提问都需要个性化）。
2. `AssistantController` 已经能拿到 `userId`（`authentication.getPrincipal()`），但从未透传给 `chatService.stream()`——需要把 `userId` 一路传到 `ExplorerTools`，工具内部按需查询。
3. System prompt 增加一条规则：仅当用户请求宽泛（如"推荐点地方"）且未指定明确偏好时，才主动调用 `getUserPreferences` 辅助排序；用户已明确说明需求时不必调用，避免过度个性化打扰。
4. 隐私考虑：工具返回的偏好摘要不应包含具体时间戳/地址等敏感细节，只给类别聚合结果。

验收：对同一句"帮我推荐个地方"，长期收藏咖啡馆的用户和长期收藏运动场所的用户应得到不同倾向的推荐。

---

## P3：真流式（工作量：1-2 人日，优先级可视体验诉求而定）

现状：`.call()` 阻塞拿到完整回复后按 30 字符/块做"假流式"回放（代码注释里说明是绕过 DashScope 工具调用 + 流式的兼容性 bug）。

**建议**：
- 先在 Spring AI / DashScope 侧确认该兼容性问题是否已在新版本修复（Spring AI 迭代较快，`spring-ai-starter-model-openai` 或 DashScope 的 OpenAI 兼容模式近期版本可能已解决工具调用 + `.stream()` 并用报错的问题），如已修复直接切回真流式，改动量很小。
- 若仍未修复，可退而求其次：工具调用阶段（`recommendNearbyPlaces` 等）保持非流式获取工具结果，但最终自然语言生成这一步单独用 `.stream()`，即"两阶段"：先执行 tool calls 拿到结果，再流式生成最终回答文本。这样至少最耗时的文本生成阶段是真流式。
- 该项对用户体验的实际提升有限（现有分块回放已经"看起来"是流式的），优先级低于 P1/P2，可放在两者之后处理。

---

## P4：其他增强（按需，非阻塞）

- **可观测性**：现有 Micrometer 指标（请求数/缓存命中率/延迟）之外，建议加一条"工具调用失败率"指标，便于发现高德路线 API 或 POI 检索的隐性故障。
- **成本控制**：`AssistantEvaluationService` 的评测建议纳入 CI，每次 prompt/工具描述改动后自动跑一次基线分，防止改坏。
- **安全**：`AssistantGuard` 目前做注入/超长过滤，建议补充对工具返回内容的长度截断（防止单个地点描述异常长导致 prompt 膨胀）。
- **扩展工具**：可考虑新增"按用户当前对话意图跨会话推荐路线组合"（如"安排一个周末半日游"），复用 `planRoute` + `recommendNearbyPlaces` 多步调用，这个 LLM 本身已具备自主编排的能力，不需要额外开发编排逻辑，主要是补充 system prompt 引导。

---

## 优先级与排期建议

| 优先级 | 事项 | 工作量 | 前置依赖 |
|---|---|---|---|
| P0 | 启用配置 + 降级提示 | 0.5 天 | 无 |
| P1 | 多轮对话记忆 | 2-3 天 | P0 |
| P2 | 个性化推荐 | 3-4 天 | P0，可与 P1 并行 |
| P3 | 真流式 | 1-2 天 | 无强依赖，可最后做 |
| P4 | 可观测性/安全/扩展工具 | 按需 | 无 |

建议顺序：P0 → (P1 与 P2 并行) → P3 → P4，总计约 7-10 人日可完成主要短板闭环。
