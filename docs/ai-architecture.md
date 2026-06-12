# AI 基础架构设计

## 设计目标

第一版 AI 模块不依赖真实 API Key，不接入外部大模型服务，通过 `MockLlmClient` 保证本地测试和启动可用。

设计原则：

- AI 接口统一放在 `/ai/**`。
- 不修改旧 HMDP 接口。
- 大模型不能直接访问数据库、Redis 或执行 SQL。
- 所有业务数据必须由后端 Service 显式读取、整理和传入 Prompt。
- LLM 失败时返回降级文案，不让接口直接 500。

## 包结构

```text
com.hmdp.ai
├── client
│   ├── LlmClient
│   └── impl/MockLlmClient
├── controller/AiLlmController
├── dto/LlmRequest, LlmResponse
├── log/AiCallLog, AiCallLogger
├── prompt/PromptTemplateService
└── service/LlmService
```

## 已提供接口

```text
POST /ai/llm/chat
```

请求体：

```json
{
  "scene": "merchant-copywriting",
  "prompt": "请生成一段商家文案",
  "systemPrompt": "...",
  "userPrompt": "...",
  "model": "mock-local-llm",
  "temperature": 0.7,
  "maxTokens": 800
}
```

响应仍使用原项目 `Result`：

```json
{
  "success": true,
  "data": {
    "content": "【Mock】...",
    "success": true,
    "model": "mock-local-llm",
    "latencyMs": 0,
    "estimatedTokens": 100
  }
}
```

## Prompt 模板

模板位于 `src/main/resources/prompts/`：

- `merchant-copywriting.txt`
- `review-summary.txt`
- `shop-rag-qa.txt`
- `recommend-agent.txt`

## 降级策略

`LlmService` 捕获 LLM 调用异常，返回固定兜底文案：

```text
AI 服务暂时不可用，请稍后重试。
```

## 后续扩展

后续可新增真实模型实现，例如：

- OpenAI
- DeepSeek
- 通义千问
- 智谱

扩展时只新增 `LlmClient` 实现和配置，不修改业务 Service。
