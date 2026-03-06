package vn.vnpt.kntc.agent.base;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Kết quả sau khi một Agent hoàn thành ReAct loop.
 */
@Data
@Builder
public class AgentResult {

    private String agentId;
    private String domain;
    private boolean success;

    // Câu trả lời cuối (Final Answer từ LLM)
    private String answer;

    // Toàn bộ quá trình suy nghĩ (Thought/Action/Observation)
    private List<String> thoughtProcess;

    // Các tool đã được gọi
    private List<String> toolsUsed;

    // Error message nếu thất bại
    private String errorMessage;

    public static AgentResult success(String agentId, String domain,
                                       String answer,
                                       List<String> thoughtProcess,
                                       List<String> toolsUsed) {
        return AgentResult.builder()
            .agentId(agentId).domain(domain)
            .success(true).answer(answer)
            .thoughtProcess(thoughtProcess).toolsUsed(toolsUsed)
            .build();
    }

    public static AgentResult failed(String agentId, String domain, String error) {
        return AgentResult.builder()
            .agentId(agentId).domain(domain)
            .success(false).errorMessage(error)
            .answer("Không thể xử lý yêu cầu: " + error)
            .build();
    }
}
