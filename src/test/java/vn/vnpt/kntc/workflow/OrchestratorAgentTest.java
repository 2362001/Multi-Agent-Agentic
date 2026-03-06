package vn.vnpt.kntc.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.agent.base.AgentContext;
import vn.vnpt.kntc.agent.base.AgentResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestratorAgentTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatResponse chatResponse;
    @Mock
    private Generation generation;
    @Mock
    private SynthesisAgent synthesisAgent;

    @Test
    void run_singleDomainQuery_shouldRouteToOneAgent() throws Exception {
        // Giả lập một Sub-Agent
        BaseReActAgent mockHoSoAgent = mock(BaseReActAgent.class);
        when(mockHoSoAgent.getAgentId()).thenReturn("HO_SO_AGENT");
        when(mockHoSoAgent.run(any(), any())).thenReturn(
                AgentResult.success("HO_SO_AGENT", "Hồ sơ",
                        "Bạn có 2 hồ sơ gấp", List.of(), List.of()));

        // Orchestrator plan JSON
        String planJson = """
                {
                    "understanding": "Câu hỏi về hồ sơ gấp",
                    "tasks": [
                        {
                            "agentId": "HO_SO_AGENT",
                            "task": "Tìm hồ sơ gấp của user 123",
                            "canParallel": true,
                            "dependsOn": [],
                            "priority": 1
                        }
                    ],
                    "synthesisInstruction": "Trả lời trực tiếp"
                }
                """;

        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(planJson));
        when(synthesisAgent.synthesize(any(), any(), any()))
                .thenReturn("Bạn có 2 hồ sơ gấp");

        ObjectMapper objectMapper = new ObjectMapper();
        OrchestratorAgent orchestrator = new OrchestratorAgent(
                chatClient, objectMapper,
                List.of(mockHoSoAgent),
                synthesisAgent);

        String result = orchestrator.run("Hồ sơ gấp của tôi?", 123, "Test User");

        assertThat(result).isNotBlank();
        verify(mockHoSoAgent).run(any(), any());
    }

    @Test
    void run_multiDomainQuery_shouldRouteToMultipleAgents() throws Exception {
        BaseReActAgent mockHoSoAgent = mock(BaseReActAgent.class);
        BaseReActAgent mockLichAgent = mock(BaseReActAgent.class);

        when(mockHoSoAgent.getAgentId()).thenReturn("HO_SO_AGENT");
        when(mockLichAgent.getAgentId()).thenReturn("LICH_AGENT");

        when(mockHoSoAgent.run(any(), any())).thenReturn(
                AgentResult.success("HO_SO_AGENT", "Hồ sơ", "2 hồ sơ gấp",
                        List.of(), List.of()));
        when(mockLichAgent.run(any(), any())).thenReturn(
                AgentResult.success("LICH_AGENT", "Lịch", "1 cuộc họp lúc 9h",
                        List.of(), List.of()));

        String planJson = """
                {
                    "understanding": "Câu hỏi về lịch và hồ sơ hôm nay",
                    "tasks": [
                        {"agentId":"HO_SO_AGENT","task":"Hồ sơ hôm nay","canParallel":true,"dependsOn":[],"priority":1},
                        {"agentId":"LICH_AGENT","task":"Lịch hôm nay","canParallel":true,"dependsOn":[],"priority":1}
                    ],
                    "synthesisInstruction": "Tổng hợp kế hoạch ngày"
                }
                """;

        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(planJson));
        when(synthesisAgent.synthesize(any(), any(), any()))
                .thenReturn("Hôm nay bạn bận: 2 hồ sơ + 1 cuộc họp");

        ObjectMapper objectMapper = new ObjectMapper();
        OrchestratorAgent orchestrator = new OrchestratorAgent(
                chatClient, objectMapper,
                List.of(mockHoSoAgent, mockLichAgent),
                synthesisAgent);

        String result = orchestrator.run(
                "Hôm nay tôi có hồ sơ gấp và lịch họp gì?", 123, "Test");

        assertThat(result).isNotBlank();
        verify(mockHoSoAgent).run(any(), any());
        verify(mockLichAgent).run(any(), any());
    }
}
