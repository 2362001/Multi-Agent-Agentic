package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import vn.vnpt.kntc.agent.base.AgentContext;
import vn.vnpt.kntc.agent.base.AgentResult;
import vn.vnpt.kntc.model.GqHoSo;
import vn.vnpt.kntc.repository.GqHoSoRepository;
import vn.vnpt.kntc.repository.TnHoSoRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoSoAgentTest {

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.CallResponseSpec responseSpec;
    @Mock private ChatResponse chatResponse;
    @Mock private Generation generation;
    @Mock private GqHoSoRepository gqRepo;
    @Mock private TnHoSoRepository tnRepo;

    private HoSoAgent agent;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        agent = new HoSoAgent(chatClient, objectMapper, gqRepo, tnRepo);
    }

    @Test
    void getAgentId_shouldReturnCorrectId() {
        assertThat(agent.getAgentId()).isEqualTo("HO_SO_AGENT");
    }

    @Test
    void run_whenQueryAboutOverdueHoSo_shouldCallOverdueTool() {
        // Arrange
        GqHoSo mockHoSo = new GqHoSo();
        mockHoSo.setId(1);
        mockHoSo.setGqMaHoSo("HS-001");
        mockHoSo.setNgayPheDuyetGqkntc(LocalDateTime.now().minusDays(2));

        when(gqRepo.findOverdue(any())).thenReturn(List.of(mockHoSo));

        // LLM step 1: quyết định gọi tool
        String llmStep1 = """
            Thought: Người dùng hỏi về hồ sơ quá hạn, cần gọi tool tìm quá hạn
            Action: gq_find_overdue
            Action Input: {"userId": 123}
            """;
        // LLM step 2: có kết quả, trả lời
        String llmStep2 = "Final Answer: Bạn có 1 hồ sơ quá hạn: HS-001";

        when(chatClient.call(any(Prompt.class))).thenReturn(responseSpec);
        when(responseSpec.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(
            new AssistantMessage(llmStep1),
            new AssistantMessage(llmStep2)
        );

        AgentContext context = AgentContext.builder()
            .userId(123).userName("Test User").build();

        // Act
        AgentResult result = agent.run("Hồ sơ quá hạn của tôi?", context);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).contains("HS-001");
        assertThat(result.getToolsUsed()).contains("gq_find_overdue");
        verify(gqRepo).findOverdue(123);
    }

    @Test
    void run_whenNoData_shouldReturnGracefulMessage() {
        when(gqRepo.findOverdue(any())).thenReturn(List.of());

        String llmStep1 = """
            Action: gq_find_overdue
            Action Input: {"userId": 99}
            """;
        String llmStep2 = "Final Answer: Bạn không có hồ sơ quá hạn nào. ✅";

        when(chatClient.call(any(Prompt.class))).thenReturn(responseSpec);
        when(responseSpec.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(
            new AssistantMessage(llmStep1),
            new AssistantMessage(llmStep2)
        );

        AgentContext context = AgentContext.builder()
            .userId(99).userName("Test").build();

        AgentResult result = agent.run("Tôi có hồ sơ quá hạn không?", context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAnswer()).contains("không có");
    }
}
