package vn.vnpt.kntc.agent.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class cho mọi Sub-Agent trong hệ thống KNTC.
 *
 * Implement ReAct pattern:
 *   Thought → Action → Observation → Thought → ... → Final Answer
 *
 * Subclass chỉ cần implement:
 *   - getAgentId()
 *   - getDomain()
 *   - buildSystemPrompt()
 *   - executeTool(toolName, args)
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseReActAgent {

    protected final ChatClient chatClient;
    protected final ObjectMapper objectMapper;

    private static final int MAX_STEPS = 8;

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

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildSystemPrompt()));
        messages.add(new UserMessage(buildUserMessage(task, context)));

        List<String> thoughtProcess = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();

        for (int step = 1; step <= MAX_STEPS; step++) {
            log.debug("{} step {}", getAgentId(), step);

            // ── LLM quyết định làm gì tiếp theo ─────────────────
            String llmOutput = callLLM(messages);
            messages.add(new AssistantMessage(llmOutput));
            thoughtProcess.add(llmOutput);

            // ── Có Final Answer → dừng ───────────────────────────
            if (llmOutput.contains("Final Answer:")) {
                String answer = extractAfter(llmOutput, "Final Answer:").trim();
                log.info("✅ {} hoàn thành sau {} bước | tools: {}",
                    getAgentId(), step, toolsUsed);
                return AgentResult.success(
                    getAgentId(), getDomain(),
                    answer, thoughtProcess, toolsUsed
                );
            }

            // ── LLM muốn gọi tool ────────────────────────────────
            if (llmOutput.contains("Action:")) {
                String toolName = extractToolName(llmOutput);
                JsonNode toolArgs = extractToolArgs(llmOutput);

                log.info("{} → tool: {} | args: {}", getAgentId(), toolName, toolArgs);
                toolsUsed.add(toolName);

                String observation = executeTool(toolName, toolArgs);
                log.debug("Observation: {}", observation);

                messages.add(new UserMessage("Observation: " + observation));

            } else {
                // LLM không có action cụ thể → có thể đang suy nghĩ
                log.debug("{} thinking... no action yet", getAgentId());
            }
        }

        log.warn("⚠ {} vượt quá MAX_STEPS={}", getAgentId(), MAX_STEPS);
        return AgentResult.failed(getAgentId(), getDomain(),
            "Vượt quá " + MAX_STEPS + " bước xử lý");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String callLLM(List<Message> messages) {
        try {
            return chatClient.call(new Prompt(messages))
                .getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("{} LLM call failed", getAgentId(), e);
            return "Final Answer: Lỗi kết nối AI: " + e.getMessage();
        }
    }

    private String buildUserMessage(String task, AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("userId: ").append(context.getUserId()).append("\n");
        sb.append("userName: ").append(context.getUserName()).append("\n");
        sb.append("Thời gian hiện tại: ").append(context.getCurrentTime()).append("\n");
        sb.append("Nhiệm vụ: ").append(task);

        if (!context.getPreviousResults().isEmpty()) {
            sb.append("\n\nKết quả từ các agent đã chạy:\n");
            for (AgentResult prev : context.getPreviousResults()) {
                if (prev.isSuccess()) {
                    sb.append("── ").append(prev.getDomain()).append(":\n");
                    sb.append(prev.getAnswer()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String extractToolName(String llmOutput) {
        try {
            String actionLine = llmOutput.lines()
                .filter(l -> l.trim().startsWith("Action:"))
                .findFirst().orElse("");
            return actionLine.replace("Action:", "").trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private JsonNode extractToolArgs(String llmOutput) {
        try {
            String inputLine = llmOutput.lines()
                .filter(l -> l.trim().startsWith("Action Input:"))
                .findFirst().orElse("");
            String json = inputLine.replace("Action Input:", "").trim();
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Không parse được Action Input, dùng empty node");
            return objectMapper.createObjectNode();
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
            return "{\"error\": \"Serialization failed\"}";
        }
    }
}
