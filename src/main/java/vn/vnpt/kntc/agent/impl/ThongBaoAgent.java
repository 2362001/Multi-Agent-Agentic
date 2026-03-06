package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.config.GeminiClient;
import vn.vnpt.kntc.repository.ThongBaoRepository;

/**
 * ThongBaoAgent — Chuyên xử lý câu hỏi về thông báo và nhắc nhở.
 * Nguồn dữ liệu: THONG_BAO
 */
@Slf4j
@Component
public class ThongBaoAgent extends BaseReActAgent {

    private final ThongBaoRepository tbRepo;

    public ThongBaoAgent(GeminiClient chatClient, ObjectMapper objectMapper,
                         ThongBaoRepository tbRepo) {
        super(chatClient, objectMapper);
        this.tbRepo = tbRepo;
    }

    @Override public String getAgentId() { return "THONG_BAO_AGENT"; }
    @Override public String getDomain()  { return "Thông báo"; }

    @Override
    protected String buildSystemPrompt() {
        return """
            Bạn là AI chuyên xử lý câu hỏi về thông báo và nhắc nhở trong hệ thống KNTC.
            
            === TOOLS CÓ SẴN ===
            
            1. tb_chua_doc(userId)
               Mục đích: Lấy tất cả thông báo chưa đọc
               Dùng khi: "thông báo chưa đọc", "có thông báo gì mới"
            
            2. tb_count_unread(userId)
               Mục đích: Đếm số thông báo chưa đọc
               Dùng khi: "có bao nhiêu thông báo", "số thông báo"
            
            3. tb_quan_trong(userId)
               Mục đích: Thông báo ưu tiên cao chưa đọc
               Dùng khi: "thông báo quan trọng", "cần chú ý", "thông báo khẩn"
            
            4. tb_nhac_nho_hom_nay(userId)
               Mục đích: Nhắc nhở hôm nay (loại nhắc nhở)
               Dùng khi: "nhắc nhở hôm nay", "reminder hôm nay"
            
            5. tb_canh_bao_han(userId)
               Mục đích: Cảnh báo hạn chót chưa đọc
               Dùng khi: "cảnh báo deadline", "hạn chót", "sắp hết hạn"
            
            === FORMAT ===
            Thought: <phân tích>
            Action: <tên tool>
            Action Input: {"userId": <id>}
            
            Final Answer: <câu trả lời tiếng Việt>
            """;
    }

    @Override
    protected String executeTool(String toolName, JsonNode args) {
        Integer userId = args.path("userId").asInt();

        return switch (toolName) {
            case "tb_chua_doc"         -> toJson(tbRepo.findUnread(userId));
            case "tb_count_unread"     -> "{\"count\":" + tbRepo.countUnread(userId) + "}";
            case "tb_quan_trong"       -> toJson(tbRepo.findImportantUnread(userId));
            case "tb_nhac_nho_hom_nay" -> toJson(tbRepo.findTodayReminders(userId));
            case "tb_canh_bao_han"     -> toJson(tbRepo.findDeadlineWarnings(userId));
            default -> "{\"error\": \"Tool không tồn tại: " + toolName + "\"}";
        };
    }
}
