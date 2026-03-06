package vn.vnpt.kntc.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.AgentResult;

import java.util.List;

/**
 * Synthesis Agent — Tổng hợp kết quả từ nhiều Sub-Agent
 * thành một câu trả lời hoàn chỉnh, tự nhiên cho người dùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynthesisAgent {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
        Bạn là AI tổng hợp thông tin cho cán bộ trong hệ thống KNTC (Khiếu nại - Tố cáo).
        
        Nhiệm vụ: Tổng hợp kết quả từ nhiều AI Agent thành MỘT câu trả lời hoàn chỉnh.
        
        Nguyên tắc tổng hợp:
        - Ưu tiên thông tin quan trọng/khẩn cấp lên đầu
        - Câu trả lời mạch lạc, tự nhiên, không lặp lại
        - Dùng emoji và định dạng rõ ràng để dễ đọc
        - Kết thúc bằng gợi ý hành động cụ thể nếu phù hợp
        - Luôn trả lời bằng tiếng Việt
        - Nếu không có dữ liệu → thông báo rõ ràng, không bịa
        """;

    public String synthesize(String originalQuery,
                              List<AgentResult> results,
                              String instruction) {
        log.info("SynthesisAgent tổng hợp {} kết quả", results.size());

        // Nếu chỉ có 1 agent → trả thẳng kết quả, không cần tổng hợp
        if (results.size() == 1 && results.get(0).isSuccess()) {
            return results.get(0).getAnswer();
        }

        StringBuilder context = new StringBuilder();
        context.append("Câu hỏi gốc của người dùng: ").append(originalQuery).append("\n\n");
        context.append("Hướng dẫn tổng hợp: ").append(instruction).append("\n\n");
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
            return chatClient.call(new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(context.toString())
            ))).getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("SynthesisAgent lỗi LLM", e);
            // Fallback: ghép các câu trả lời lại
            return buildFallbackResponse(results);
        }
    }

    private String buildFallbackResponse(List<AgentResult> results) {
        StringBuilder sb = new StringBuilder();
        for (AgentResult r : results) {
            if (r.isSuccess()) {
                sb.append("【").append(r.getDomain()).append("】\n");
                sb.append(r.getAnswer()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
