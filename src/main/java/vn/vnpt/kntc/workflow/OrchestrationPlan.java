package vn.vnpt.kntc.workflow;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Kế hoạch thực thi do Orchestrator tạo ra.
 * Chứa danh sách tasks và cách tổng hợp kết quả.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestrationPlan {

    // LLM hiểu câu hỏi là gì
    private String understanding;

    // Danh sách tasks cần thực thi
    @Builder.Default
    private List<AgentTask> tasks = new ArrayList<>();

    // Hướng dẫn cho Synthesis Agent
    private String synthesisInstruction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentTask {
        private String agentId;          // "HO_SO_AGENT", "LICH_AGENT", ...
        private String task;             // Mô tả cụ thể nhiệm vụ cho agent
        private boolean canParallel;     // true = chạy song song được
        @Builder.Default
        private List<String> dependsOn = new ArrayList<>(); // agentId cần chờ
        private int priority;            // 1 = cao nhất
    }
}
