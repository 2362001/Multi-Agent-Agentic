package vn.vnpt.kntc.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.AgentResult;
import vn.vnpt.kntc.config.GeminiClient;
import vn.vnpt.kntc.config.GeminiClient.ChatMessage;

import java.util.List;

/**
 * SynthesisAgent — Tổng hợp kết quả từ nhiều Sub-Agent
 * thành một câu trả lời hoàn chỉnh bằng Gemini.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynthesisAgent {

    private final GeminiClient geminiClient;

    private static final String SYSTEM_PROMPT = """
            Bạn là AI tổng hợp thông tin cho cán bộ trong hệ thống KNTC (Khiếu nại - Tố cáo).

            Nhiệm vụ: Tổng hợp kết quả từ nhiều AI Agent thành MỘT câu trả lời hoàn chỉnh.

            Nguyên tắc:
            - Ưu tiên thông tin quan trọng/khẩn cấp lên đầu
            - Câu trả lời mạch lạc, tự nhiên, không lặp lại
            - Dùng emoji và định dạng rõ ràng để dễ đọc
            - Kết thúc bằng gợi ý hành động cụ thể nếu phù hợp
            - Luôn trả lời bằng tiếng Việt
            - ⚠️ TUYỆT ĐỐI KHÔNG BỊA RA DỮ LIỆU. Nếu KẾT QUẢ TỪ CÁC AGENT không có dữ liệu hoặc lỗi, bạn phải thông báo rõ ràng là không tìm thấy, không được tự ý tạo bảng, tạo danh sách hoặc số liệu giả.
            """;

    public String synthesize(String originalQuery,
            List<AgentResult> results,
            String instruction) {
        log.info("SynthesisAgent tổng hợp {} kết quả", results.size());

        // Chỉ 1 agent → trả thẳng, không cần tổng hợp
        if (results.size() == 1 && results.get(0).isSuccess()) {
            return results.get(0).getAnswer();
        }

        StringBuilder context = new StringBuilder();
        context.append("Câu hỏi gốc: ").append(originalQuery).append("\n\n");
        context.append("Hướng dẫn: ").append(instruction).append("\n\n");
        context.append("=== KẾT QUẢ TỪ CÁC AGENT ===\n\n");

        for (AgentResult result : results) {
            context.append("── ").append(result.getDomain()).append(" ──\n");
            if (result.isSuccess()) {
                context.append(result.getAnswer());
            } else {
                context.append("(Không có dữ liệu: ").append(result.getErrorMessage()).append(")");
            }
            context.append("\n\n");
        }

        try {
            return geminiClient.chat(
                    SYSTEM_PROMPT,
                    List.of(ChatMessage.user(context.toString())));
        } catch (GeminiClient.GeminiException e) {
            log.error("SynthesisAgent Gemini lỗi: {}", e.getMessage());
            return buildFallbackResponse(results);
        } catch (Exception e) {
            log.error("SynthesisAgent unexpected: {}", e.getMessage());
            return buildFallbackResponse(results);
        }
    }

    private String buildFallbackResponse(List<AgentResult> results) {
        StringBuilder sb = new StringBuilder();
        for (AgentResult r : results) {
            if (r.isSuccess()) {
                sb.append("【").append(r.getDomain()).append("】\n")
                        .append(r.getAnswer()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}