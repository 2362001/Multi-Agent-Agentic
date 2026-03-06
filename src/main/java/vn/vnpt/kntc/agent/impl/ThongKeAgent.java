package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.config.GeminiClient;
import vn.vnpt.kntc.repository.GqHoSoRepository;
import vn.vnpt.kntc.repository.TnHoSoRepository;

import java.time.LocalDateTime;

/**
 * ThongKeAgent — Chuyên xử lý câu hỏi thống kê, báo cáo số liệu KNTC.
 * Nguồn dữ liệu: GQ_HOSO + TN_HOSO (tổng hợp)
 */
@Slf4j
@Component
public class ThongKeAgent extends BaseReActAgent {

    private final GqHoSoRepository gqRepo;
    private final TnHoSoRepository tnRepo;

    public ThongKeAgent(GeminiClient chatClient, ObjectMapper objectMapper,
                        GqHoSoRepository gqRepo, TnHoSoRepository tnRepo) {
        super(chatClient, objectMapper);
        this.gqRepo = gqRepo;
        this.tnRepo = tnRepo;
    }

    @Override public String getAgentId() { return "THONG_KE_AGENT"; }
    @Override public String getDomain()  { return "Thống kê"; }

    @Override
    protected String buildSystemPrompt() {
        return """
            Bạn là AI chuyên xử lý câu hỏi thống kê và báo cáo về hồ sơ KNTC.
            
            === TOOLS CÓ SẴN ===
            
            1. tk_theo_trang_thai(userId)
               Thống kê số hồ sơ GQ theo từng trạng thái (0,1,2,3,4...)
               Dùng khi: "thống kê theo trạng thái", "phân loại hồ sơ"
            
            2. tk_gq_theo_thang(userId, month, year)
               Số hồ sơ GQ được phân công trong tháng cụ thể
               Dùng khi: "tháng này/trước/... có bao nhiêu hồ sơ"
            
            3. tk_tn_theo_thang(userId, month, year)
               Số đơn thư TN tiếp nhận trong tháng
               Dùng khi: "tháng này tiếp nhận bao nhiêu đơn"
            
            4. tk_qua_han(userId)
               Tổng số hồ sơ quá hạn GQKNTC
               Dùng khi: "bao nhiêu hồ sơ quá hạn", "tỷ lệ quá hạn"
            
            5. tk_tong_hop(userId)
               Tổng hợp toàn bộ: tổng hồ sơ, đang xử lý, quá hạn, hôm nay
               Dùng khi: "tóm tắt", "tổng quan", "dashboard", "báo cáo tổng"
            
            === FORMAT ===
            Thought: <phân tích>
            Action: <tên tool>
            Action Input: {"userId": <id>, "month": 3, "year": 2026}
            
            Final Answer: <báo cáo số liệu rõ ràng, có phần trăm nếu phù hợp>
            
            Lưu ý: Với câu hỏi phức tạp có thể gọi nhiều tool.
            Ví dụ "báo cáo đầy đủ" → gọi tk_tong_hop + tk_theo_trang_thai.
            """;
    }

    @Override
    protected String executeTool(String toolName, JsonNode args) {
        Integer userId = args.path("userId").asInt();
        int currentMonth = LocalDateTime.now().getMonthValue();
        int currentYear  = LocalDateTime.now().getYear();

        return switch (toolName) {

            case "tk_theo_trang_thai" ->
                toJson(gqRepo.countGroupByStatus(userId));

            case "tk_gq_theo_thang" -> {
                int month = args.path("month").asInt(currentMonth);
                int year  = args.path("year").asInt(currentYear);
                long count = gqRepo.countByMonth(userId, month, year);
                yield String.format(
                    "{\"count\":%d,\"month\":%d,\"year\":%d,\"loai\":\"GQ_HOSO\"}",
                    count, month, year
                );
            }

            case "tk_tn_theo_thang" -> {
                int month = args.path("month").asInt(currentMonth);
                int year  = args.path("year").asInt(currentYear);
                long count = tnRepo.countByMonth(userId, month, year);
                yield String.format(
                    "{\"count\":%d,\"month\":%d,\"year\":%d,\"loai\":\"TN_HOSO\"}",
                    count, month, year
                );
            }

            case "tk_qua_han" -> {
                long overdue = gqRepo.countOverdue(userId);
                long all     = gqRepo.countAll(userId);
                double pct   = all > 0 ? (overdue * 100.0 / all) : 0;
                yield String.format(
                    "{\"qua_han\":%d,\"tong\":%d,\"ty_le\":\"%.1f%%\"}",
                    overdue, all, pct
                );
            }

            case "tk_tong_hop" -> {
                long all     = gqRepo.countAll(userId);
                long pending = gqRepo.countPending(userId);
                long overdue = gqRepo.countOverdue(userId);
                long today   = gqRepo.countAssignedToday(userId);
                long tnToday = tnRepo.countToday(userId);
                yield String.format(
                    "{\"tong_gq\":%d,\"dang_xu_ly\":%d,\"qua_han\":%d," +
                    "\"gq_hom_nay\":%d,\"tn_hom_nay\":%d}",
                    all, pending, overdue, today, tnToday
                );
            }

            default -> "{\"error\": \"Tool không tồn tại: " + toolName + "\"}";
        };
    }
}
