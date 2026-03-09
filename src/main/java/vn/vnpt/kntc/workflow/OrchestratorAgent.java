package vn.vnpt.kntc.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.AgentContext;
import vn.vnpt.kntc.agent.base.AgentResult;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.config.GeminiClient;
import vn.vnpt.kntc.config.GeminiClient.ChatMessage;

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

    // ✅ Thay ChatClient → GeminiClient
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final Map<String, BaseReActAgent> agentRegistry;
    private final SynthesisAgent synthesisAgent;

    @Value("${agent.parallel-timeout-sec:30}")
    private int parallelTimeoutSec;

    private static final String ORCHESTRATOR_PROMPT = """
            Bạn là Orchestrator điều phối hệ thống AI xử lý nghiệp vụ KNTC.

            === AGENTS CÓ SẴN ===
            - HO_SO_AGENT    : hồ sơ giải quyết KNTC (GQ_HOSO), đơn thư tiếp nhận (TN_HOSO)
            - THONG_KE_AGENT : thống kê, báo cáo, số liệu
            - QUY_TRINH_AGENT: tạo quy trình mới, thiết kế workflow, Onboard nhân sự, mua sắm...

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
            "thống kê/báo cáo"       → THONG_KE_AGENT
            "tạo quy trình/vẽ luồng" → QUY_TRINH_AGENT

            Câu hỏi đa domain → nhiều tasks, canParallel=true
            "hôm nay có gì"          → HO_SO + THONG_KE (song song)
            "tóm tắt công việc"      → HO_SO + THONG_KE (song song)

            === LƯU Ý ===
            - Chỉ dùng đúng agentId đã liệt kê ở trên
            - task phải mô tả cụ thể, kèm userId nếu biết
            - synthesisInstruction hướng dẫn cách trình bày kết quả cuối
            - Trả về JSON thuần túy, KHÔNG bọc trong ```json``` hay bất kỳ markdown nào
            """;

    // ✅ Thay ChatClient → GeminiClient trong constructor
    public OrchestratorAgent(GeminiClient geminiClient,
            ObjectMapper objectMapper,
            List<BaseReActAgent> agents,
            SynthesisAgent synthesisAgent) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.synthesisAgent = synthesisAgent;
        // Tự động đăng ký tất cả agents vào registry
        this.agentRegistry = agents.stream()
                .collect(Collectors.toMap(BaseReActAgent::getAgentId, a -> a));
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

        // Bước 2: Thực thi theo topology
        List<AgentResult> results = executeWithTopology(plan, userId, userName);

        // Bước 3: Tổng hợp
        String finalAnswer = synthesisAgent.synthesize(
                query, results, plan.getSynthesisInstruction());

        log.info("══════════════════════════════════════");
        log.info("ORCHESTRATOR hoàn thành | {} kết quả", results.size());
        return finalAnswer;
    }

    // ── Private: Build Plan ───────────────────────────────────────

    private OrchestrationPlan buildPlan(String query, Integer userId) {
        try {
            String llmResponse = geminiClient.chat(
                    ORCHESTRATOR_PROMPT,
                    List.of(ChatMessage.user(
                            "userId: " + userId + "\n" +
                                    "Thời gian: " + LocalDateTime.now() + "\n" +
                                    "Câu hỏi: " + query)));

            // Làm sạch JSON (Gemini đôi khi vẫn wrap trong ```json```)
            String json = llmResponse.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return objectMapper.readValue(json, OrchestrationPlan.class);

        } catch (Exception e) {
            log.error("Lỗi build plan, dùng fallback HO_SO_AGENT: {}", e.getMessage());
            return fallbackPlan(query);
        }
    }

    // ── Private: Execute with Topology ───────────────────────────

    private List<AgentResult> executeWithTopology(
            OrchestrationPlan plan, Integer userId, String userName) {

        Map<String, AgentResult> resultMap = new LinkedHashMap<>();

        List<List<OrchestrationPlan.AgentTask>> waves = topologicalSort(plan.getTasks());

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
            return List.of(runTask(wave.get(0), userId, userName,
                    new ArrayList<>(previousResultMap.values())));
        }

        // Nhiều tasks → chạy song song
        ExecutorService executor = Executors.newFixedThreadPool(wave.size());
        List<Future<AgentResult>> futures = wave.stream()
                .map(task -> executor.submit(() -> runTask(task, userId, userName,
                        new ArrayList<>(previousResultMap.values()))))
                .toList();

        List<AgentResult> results = new ArrayList<>();
        for (Future<AgentResult> future : futures) {
            try {
                results.add(future.get(parallelTimeoutSec, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                log.error("Task timeout sau {}s", parallelTimeoutSec);
                results.add(AgentResult.failed("UNKNOWN", "UNKNOWN", "Timeout"));
            } catch (Exception e) {
                log.error("Task thất bại: {}", e.getMessage());
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

    private List<List<OrchestrationPlan.AgentTask>> topologicalSort(
            List<OrchestrationPlan.AgentTask> tasks) {

        List<List<OrchestrationPlan.AgentTask>> waves = new ArrayList<>();
        Set<String> completed = new HashSet<>();
        List<OrchestrationPlan.AgentTask> remaining = new ArrayList<>(tasks);

        while (!remaining.isEmpty()) {
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
                                .build()))
                .synthesisInstruction("Trả lời trực tiếp câu hỏi về hồ sơ")
                .build();
    }
}