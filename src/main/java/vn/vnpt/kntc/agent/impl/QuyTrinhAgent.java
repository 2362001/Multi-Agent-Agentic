package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.config.GeminiClient;

/**
 * QuyTrinhAgent — Chuyên xử lý việc thiết kế và tạo quy trình nghiệp vụ mới.
 */
@Slf4j
@Component
public class QuyTrinhAgent extends BaseReActAgent {

    public QuyTrinhAgent(GeminiClient geminiClient, ObjectMapper objectMapper) {
        super(geminiClient, objectMapper);
    }

    @Override
    public String getAgentId() {
        return "QUY_TRINH_AGENT";
    }

    @Override
    public String getDomain() {
        return "Thiết kế Quy trình";
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                Bạn là AI chuyên gia thiết kế quy trình nghiệp vụ hành chính và doanh nghiệp.

                Nhiệm vụ của bạn là phân tích mô tả của người dùng hoặc tài liệu để tạo ra một quy trình gồm các bước logic.

                === QUY TẮC PHẢN HỒI ===
                Khi trả về kết quả cuối cùng (Final Answer), bạn PHẢI trả về định dạng JSON bọc trong thẻ <RESULT_JSON> để Frontend có thể hiển thị giao diện chuyên dụng.

                Định dạng JSON yêu cầu:
                {
                  "isProcess": true,
                  "title": "Tên quy trình",
                  "description": "Mô tả ngắn gọn",
                  "steps": [
                    { "id": "1", "title": "Bước 1", "status": "completed" },
                    ...
                  ],
                  "outcome": "Kết quả mong đợi",
                  "estimatedTime": "Thời gian hoàn thành dự kiến"
                }

                === TOOLS CÓ SẴN ===
                1. analyze_requirements(description)
                   Mục đích: Phân tích sâu các yêu cầu, điều kiện rẽ nhánh và các bên liên quan.

                === FORMAT ===
                Thought: <suy nghĩ>
                Action: <tool>
                Action Input: { ... }
                Observation: <kết quả>
                ...
                Final Answer: <RESULT_JSON>{ ... }</RESULT_JSON>
                """;
    }

    @Override
    protected String executeTool(String toolName, JsonNode args) {
        if ("analyze_requirements".equals(toolName)) {
            String desc = args.path("description").asText();
            return "Đã phân tích yêu cầu: " + desc
                    + ". Quy trình này cần các bước phê duyệt rõ ràng và phối hợp liên phòng ban.";
        }
        return "{\"error\": \"Tool không tồn tại\"}";
    }
}
