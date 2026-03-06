package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.repository.LichHopRepository;

import java.time.LocalDateTime;

/**
 * LichAgent — Chuyên xử lý câu hỏi về lịch họp, lịch làm việc.
 * Nguồn dữ liệu: LICH_HOP
 */
@Slf4j
@Component
public class LichAgent extends BaseReActAgent {

    private final LichHopRepository lichRepo;

    public LichAgent(ChatClient chatClient, ObjectMapper objectMapper,
                     LichHopRepository lichRepo) {
        super(chatClient, objectMapper);
        this.lichRepo = lichRepo;
    }

    @Override public String getAgentId() { return "LICH_AGENT"; }
    @Override public String getDomain()  { return "Lịch làm việc"; }

    @Override
    protected String buildSystemPrompt() {
        return """
            Bạn là AI chuyên xử lý câu hỏi về lịch họp và lịch làm việc KNTC.
            
            === TOOLS CÓ SẴN ===
            
            1. lich_hop_hom_nay(userId)
               Mục đích: Lấy tất cả cuộc họp/lịch làm việc HÔM NAY
               Dùng khi: "hôm nay có họp gì", "lịch hôm nay", "cuộc họp hôm nay"
            
            2. lich_sap_toi(userId, days)
               Mục đích: Lịch trong N ngày tới (mặc định 7 ngày)
               Dùng khi: "tuần này", "sắp tới", "lịch tới", "lịch tuần"
            
            3. lich_count_tuan(userId)
               Mục đích: Đếm số cuộc họp trong tuần này
               Dùng khi: "tuần này có bao nhiêu cuộc họp"
            
            4. lich_count_hom_nay(userId)
               Mục đích: Đếm số cuộc họp hôm nay
               Dùng khi: "hôm nay có bao nhiêu lịch"
            
            === FORMAT ===
            Thought: <phân tích câu hỏi>
            Action: <tên tool>
            Action Input: {"userId": <id>}
            
            Final Answer: <câu trả lời tiếng Việt, có giờ cụ thể>
            
            Lưu ý: Nếu không có lịch → thông báo rõ ràng, không bịa.
            """;
    }

    @Override
    protected String executeTool(String toolName, JsonNode args) {
        Integer userId = args.path("userId").asInt();

        return switch (toolName) {
            case "lich_hop_hom_nay" ->
                toJson(lichRepo.findTodayMeetings(userId));

            case "lich_sap_toi" -> {
                int days = args.path("days").asInt(7);
                yield toJson(lichRepo.findUpcoming(
                    userId, LocalDateTime.now().plusDays(days)
                ));
            }

            case "lich_count_tuan" ->
                "{\"count\":" + lichRepo.countThisWeek(userId) + "}";

            case "lich_count_hom_nay" ->
                "{\"count\":" + lichRepo.countToday(userId) + "}";

            default -> "{\"error\": \"Tool không tồn tại: " + toolName + "\"}";
        };
    }
}
