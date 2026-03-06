package vn.vnpt.kntc.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.AgentContext;
import vn.vnpt.kntc.agent.base.AgentResult;
import vn.vnpt.kntc.agent.base.BaseReActAgent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * OrchestratorAgent — Bộ não điều phối toàn bộ Multi-Agent system.
 *
 * Trách nhiệm:
 * 1. Phân tích câu hỏi → lên kế hoạch (agent nào, thứ tự nào)
 * 2. Thực thi song song hoặc tuần tự theo dependency
 * 3. Giao kết quả cho SynthesisAgent
 */
@Slf4j
@Component
public class OrchestratorAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Map<String, BaseReActAgent> agentRegistry;
    private final SynthesisAgent synthesisAgent;

    @Value("${agent.parallel-timeout-sec:30}")
    private int parallelTimeoutSec;

    private static final String ORCHESTRATOR_PROMPT = """
        Bạn là Orchestrator điều phối hệ thống AI xử lý nghiệp vụ KNTC.
        
        === AGENTS CÓ SẴN ===
        - HO_SO_AGENT    : hồ sơ giải quyết KNTC (GQ_HOSO), đơn thư tiếp nhận (TN_HOSO)
        - LICH_AGENT     : lịch họp, lịch làm việc
        - THONG_KE_AGENT : thống kê, báo cáo, số liệu
        - THONG_BAO_AGENT: thông báo hệ thống, nhắc nhở
        
        === NHIỆM VỤ ===
        Phân tích câu hỏi và trả về JSON hợp lệ (không markdown, không giải thích):
        {
            "understanding": "Tóm tắt bạn hiểu câu hỏi là gì",
            "tasks": [
                {
                    "agentId": "HO_SO_AGENT",
                    "task": "Mô tả nhiệm vụ cụ thể bằng tiếng Việt",
                    "canParallel": true,
                    "dependsOn": [],
                    "priority": 1
                }
            ],
            "synthesisInstruction": "Hướng dẫn cách tổng hợp câu trả lời cuối"
        }
        
        === QUY TẮC ROUTING ===
        Câu hỏi 1 domain → 1 task
        "hồ sơ gấp/quá hạn"     → HO_SO_AGENT
        "lịch họp/làm việc"      → LICH_AGENT
        "thống kê/báo cáo"       → THONG_KE_AGENT
        "thông báo/nhắc nhở"     → THONG_BAO_AGENT
        
        Câu hỏi đa domain → nhiều tasks, canParallel=true
        "hôm nay có gì"          → HO_SO + LICH + THONG_BAO (song song)
        "tóm tắt công việc"      → HO_SO + LICH + THONG_BAO (song song)
        
        Câu hỏi phụ thuộc → dependsOn
        "thống kê rồi nhắc nhở"  → THONG_KE trước, THONG_BAO dependsOn THONG_KE
        
        === LƯU Ý ===
        - Chỉ dùng đúng agentId đã liệt kê ở trên
        - task phải mô tả cụ thể, kèm userId nếu biết
        - synthesisInstruction hướng dẫn cách trình bày kết quả cuối
        """;

    public OrchestratorAgent(ChatClient chatClient,
                              ObjectMapper objectMapper,
                              List<BaseReActAgent> agents,
                              SynthesisAgent synthesisAgent) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.synthesisAgent = synthesisAgent;
        // Tự động đăng ký tất cả agents vào registry
        this.agentRegistry = agents.stream()
            .collect(Collectors.toMap(
                BaseReActAgent::getAgentId,
                a -> a
            ));
        log.info("AgentRegistry: {}", agentRegistry.keySet());
    }

    /**
     * Entry point chính — nhận query và userId, trả về câu trả lời
     */
    public String run(String query, Integer userId, String userName) {
        log.info("══════════════════════════════════════");
        log.info("ORCHESTRATOR | user={} | query={}", userId, query);
        log.info("══════════════════════════════════════");

        // Bước 1: Lên kế hoạch
        OrchestrationPlan plan = buildPlan(query, userId);
        log.info("Kế hoạch: {} | tasks={}",
            plan.getUnderstanding(), plan.getTasks().size());

        // Bước 2: Thực thi theo topology (xử lý dependency)
        List<AgentResult> results = executeWithTopology(plan, userId, userName);

        // Bước 3: Tổng hợp
        String finalAnswer = synthesisAgent.synthesize(
            query, results, plan.getSynthesisInstruction()
        );

        log.info("══════════════════════════════════════");
        log.info("ORCHESTRATOR hoàn thành | {} kết quả", results.size());
        return finalAnswer;
    }

    // ── Private: Build Plan ───────────────────────────────────────

    private OrchestrationPlan buildPlan(String query, Integer userId) {
        try {
            String llmResponse = chatClient.call(new Prompt(List.of(
                new SystemMessage(ORCHESTRATOR_PROMPT),
                new UserMessage(
                    "userId: " + userId + "\n" +
                    "Thời gian: " + LocalDateTime.now() + "\n" +
                    "Câu hỏi: " + query
                )
            ))).getResult().getOutput().getContent();

            // Làm sạch JSON (loại bỏ markdown nếu LLM thêm vào)
            String json = llmResponse.trim()
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

            return objectMapper.readValue(json, OrchestrationPlan.class);

        } catch (Exception e) {
            log.error("Lỗi build plan, dùng fallback HO_SO_AGENT", e);
            return fallbackPlan(query);
        }
    }

    // ── Private: Execute with Topology ───────────────────────────

    private List<AgentResult> executeWithTopology(
            OrchestrationPlan plan, Integer userId, String userName) {

        Map<String, AgentResult> resultMap = new LinkedHashMap<>();

        // Topo sort: nhóm tasks theo wave (wave = tasks có thể chạy cùng lúc)
        List<List<OrchestrationPlan.AgentTask>> waves =
            topologicalSort(plan.getTasks());

        for (int i = 0; i < waves.size(); i++) {
            List<OrchestrationPlan.AgentTask> wave = waves.get(i);
            log.info("Wave {}/{}: agents={}", i + 1, waves.size(),
                wave.stream().map(OrchestrationPlan.AgentTask::getAgentId).toList());

            List<AgentResult> waveResults = executeWave(wave, userId, userName, resultMap);
            waveResults.forEach(r -> resultMap.put(r.getAgentId(), r));
        }

        return new ArrayList<>(resultMap.values());
    }

    private List<AgentResult> executeWave(
            List<OrchestrationPlan.AgentTask> wave,
            Integer userId, String userName,
            Map<String, AgentResult> previousResultMap) {

        if (wave.size() == 1) {
            // Chỉ 1 task → chạy trực tiếp
            return List.of(runTask(wave.get(0), userId, userName,
                new ArrayList<>(previousResultMap.values())));
        }

        // Nhiều tasks → chạy song song
        ExecutorService executor = Executors.newFixedThreadPool(wave.size());
        List<Future<AgentResult>> futures = wave.stream()
            .map(task -> executor.submit(() ->
                runTask(task, userId, userName,
                    new ArrayList<>(previousResultMap.values()))
            ))
            .toList();

        List<AgentResult> results = new ArrayList<>();
        for (Future<AgentResult> future : futures) {
            try {
                results.add(future.get(parallelTimeoutSec, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                log.error("Task timeout sau {}s", parallelTimeoutSec);
                results.add(AgentResult.failed("UNKNOWN", "UNKNOWN", "Timeout"));
            } catch (Exception e) {
                log.error("Task thất bại", e);
                results.add(AgentResult.failed("UNKNOWN", "UNKNOWN", e.getMessage()));
            }
        }
        executor.shutdown();
        return results;
    }

    private AgentResult runTask(OrchestrationPlan.AgentTask task,
                                 Integer userId, String userName,
                                 List<AgentResult> previousResults) {
        BaseReActAgent agent = agentRegistry.get(task.getAgentId());
        if (agent == null) {
            log.error("Agent không tồn tại: {}", task.getAgentId());
            return AgentResult.failed(task.getAgentId(), "UNKNOWN",
                "Agent không tồn tại: " + task.getAgentId());
        }

        AgentContext context = AgentContext.builder()
            .userId(userId)
            .userName(userName)
            .previousResults(previousResults)
            .build();

        return agent.run(task.getTask(), context);
    }

    // ── Private: Topological Sort ─────────────────────────────────

    /**
     * Chia tasks thành các waves.
     * Tasks trong cùng wave không phụ thuộc nhau → chạy song song.
     * Tasks ở wave sau phụ thuộc wave trước → chạy tuần tự.
     */
    private List<List<OrchestrationPlan.AgentTask>> topologicalSort(
            List<OrchestrationPlan.AgentTask> tasks) {

        List<List<OrchestrationPlan.AgentTask>> waves = new ArrayList<>();
        Set<String> completed = new HashSet<>();
        List<OrchestrationPlan.AgentTask> remaining = new ArrayList<>(tasks);

        while (!remaining.isEmpty()) {
            // Lấy tasks có thể chạy (dependencies đã hoàn thành)
            List<OrchestrationPlan.AgentTask> wave = remaining.stream()
                .filter(t -> completed.containsAll(t.getDependsOn()))
                .toList();

            if (wave.isEmpty()) {
                log.warn("Circular dependency detected, chạy tất cả còn lại");
                waves.add(remaining);
                break;
            }

            waves.add(wave);
            wave.forEach(t -> completed.add(t.getAgentId()));
            remaining.removeAll(wave);
        }

        return waves;
    }

    // ── Fallback Plan ─────────────────────────────────────────────

    private OrchestrationPlan fallbackPlan(String query) {
        return OrchestrationPlan.builder()
            .understanding("Câu hỏi về hồ sơ KNTC")
            .tasks(List.of(
                OrchestrationPlan.AgentTask.builder()
                    .agentId("HO_SO_AGENT")
                    .task(query)
                    .canParallel(true)
                    .dependsOn(new ArrayList<>())
                    .priority(1)
                    .build()
            ))
            .synthesisInstruction("Trả lời trực tiếp câu hỏi về hồ sơ")
            .build();
    }
}
