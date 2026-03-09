package vn.vnpt.kntc.agent.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import vn.vnpt.kntc.config.GeminiClient;
import vn.vnpt.kntc.config.GeminiClient.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class cho mọi Sub-Agent — dùng Google Gemini thay vì OpenAI.
 *
 * ReAct loop: Thought → Action → Observation → Thought → ... → Final Answer
 *
 * Subclass chỉ cần implement:
 * - getAgentId()
 * - getDomain()
 * - buildSystemPrompt()
 * - executeTool(toolName, args)
 */
@Slf4j
public abstract class BaseReActAgent {

    protected final GeminiClient geminiClient;
    protected final ObjectMapper objectMapper;

    private static final int MAX_STEPS = 8;

    protected BaseReActAgent(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    // ── Abstract methods — subclass bắt buộc implement ───────────
    public abstract String getAgentId();

    public abstract String getDomain();

    protected abstract String buildSystemPrompt();

    protected abstract String executeTool(String toolName, JsonNode args);

    /**
     * Chạy ReAct loop cho đến khi có Final Answer hoặc hết steps.
     */
    public AgentResult run(String task, AgentContext context) {
        log.info("▶ {} | task: {}", getAgentId(), task);

        // Gemini dùng roles "user" và "model" xen kẽ, bắt đầu = user
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.user(buildUserMessage(task, context)));

        List<String> thoughtProcess = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();

        for (int step = 1; step <= MAX_STEPS; step++) {
            log.debug("{} step {}/{}", getAgentId(), step, MAX_STEPS);

            String llmOutput = callGemini(messages);
            messages.add(ChatMessage.model(llmOutput));
            thoughtProcess.add(llmOutput);

            // Có Final Answer → dừng
            if (llmOutput.contains("Final Answer:")) {
                String answer = extractAfter(llmOutput, "Final Answer:").trim();
                log.info("✅ {} done after {} steps | tools: {}", getAgentId(), step, toolsUsed);
                return AgentResult.success(getAgentId(), getDomain(),
                        answer, thoughtProcess, toolsUsed);
            }

            // LLM muốn gọi tool
            if (llmOutput.contains("Action:")) {
                String toolName = extractToolName(llmOutput);
                JsonNode toolArgs = extractToolArgs(llmOutput, context.getUserId());

                log.info("{} → tool: {} args: {}", getAgentId(), toolName, toolArgs);
                toolsUsed.add(toolName);

                String observation = executeTool(toolName, toolArgs);
                log.debug("{} Observation: {}", getAgentId(), observation);

                // Observation gửi lại là user message (Gemini cần xen kẽ user/model)
                messages.add(ChatMessage.user("Observation: " + observation));
            }
        }

        log.warn("⚠ {} exceeded MAX_STEPS={}", getAgentId(), MAX_STEPS);
        return AgentResult.failed(getAgentId(), getDomain(),
                "Hệ thống cần thêm thông tin để tra cứu chính xác. Hãy thử đưa ra câu hỏi cụ thể hơn cho tôi nhé.");
    }

    // ─────────────────────────────────────────────────────────────
    private String callGemini(List<ChatMessage> messages) {
        try {
            return geminiClient.chat(buildSystemPrompt(), messages);
        } catch (GeminiClient.GeminiException e) {
            log.error("❌ {} Gemini lỗi: {}", getAgentId(), e.getMessage());
            if (e.getMessage().contains("429"))
                return "Final Answer: Hệ thống bận (rate limit). Thử lại sau 1 phút.";
            if (e.getMessage().contains("API key") || e.getMessage().contains("401") || e.getMessage().contains("403"))
                return "Final Answer: Cấu hình Gemini API key không hợp lệ. Liên hệ admin.";
            return "Final Answer: Lỗi kết nối AI: " + e.getMessage();
        } catch (Exception e) {
            log.error("❌ {} Unexpected: {}", getAgentId(), e.getMessage());
            return "Final Answer: Lỗi hệ thống: " + e.getMessage();
        }
    }

    private String buildUserMessage(String task, AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("userId: ").append(context.getUserId()).append("\n");
        sb.append("userName: ").append(context.getUserName()).append("\n");
        sb.append("Thời gian: ").append(context.getCurrentTime()).append("\n");
        sb.append("Nhiệm vụ: ").append(task);
        if (!context.getPreviousResults().isEmpty()) {
            sb.append("\n\nKết quả agent trước:\n");
            for (AgentResult prev : context.getPreviousResults()) {
                if (prev.isSuccess()) {
                    sb.append("── ").append(prev.getDomain()).append(":\n")
                            .append(prev.getAnswer()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String extractToolName(String output) {
        return output.lines()
                .filter(l -> l.trim().startsWith("Action:"))
                .findFirst()
                .map(l -> l.replace("Action:", "").trim())
                .orElse("unknown");
    }

    private JsonNode extractToolArgs(String output, Integer userId) {
        try {
            String raw = output.lines()
                    .filter(l -> l.trim().startsWith("Action Input:"))
                    .findFirst()
                    .map(l -> l.replace("Action Input:", "").trim())
                    .orElse("{}");

            // Gemini đôi khi wrap trong ```json ... ```
            raw = raw.replaceAll("```json\\s*", "").replaceAll("```", "").trim();

            JsonNode node = objectMapper.readTree(raw);

            // Tự inject userId nếu LLM quên
            if (node.isObject() && !node.has("userId")) {
                ((ObjectNode) node).put("userId", userId);
            }
            return node;
        } catch (Exception e) {
            log.warn("{} parse Action Input failed, fallback userId={}", getAgentId(), userId);
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("userId", userId);
            return fallback;
        }
    }

    protected String extractAfter(String text, String marker) {
        int idx = text.indexOf(marker);
        return idx >= 0 ? text.substring(idx + marker.length()).trim() : text;
    }

    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"Serialization failed\"}";
        }
    }
}