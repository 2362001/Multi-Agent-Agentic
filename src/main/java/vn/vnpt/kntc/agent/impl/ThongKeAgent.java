package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public String getAgentId() {
        return "THONG_KE_AGENT";
    }

    @Override
    public String getDomain() {
        return "Thống kê";
    }

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
                   Dùng khi: "tháng này tiếp nhận bao nhiêu đơn", "tháng 3 năm 2026 bao nhiêu đơn"

                4. tk_gq_theo_nam(userId, year)
                   Số hồ sơ GQ được phân công trong NĂM cụ thể
                   Dùng khi: "năm 2025 có bao nhiêu hồ sơ GQ", "công việc năm nay"

                5. tk_tn_theo_nam(userId, year)
                   Số đơn thư TN tiếp nhận trong NĂM cụ thể
                   Dùng khi: "năm 2025 tiếp nhận bao nhiêu đơn", "tổng đơn thư năm nay"

                6. tk_qua_han(userId)
                   Tổng số hồ sơ quá hạn GQKNTC
                   Dùng khi: "bao nhiêu hồ sơ quá hạn", "tỷ lệ quá hạn"

                7. tk_tong_hop(userId)
                   Tổng hợp toàn bộ: tổng hồ sơ, đang xử lý, quá hạn, hôm nay
                   Dùng khi: "tóm tắt", "tổng quan", "dashboard", "báo cáo tổng"

                === QUY TẮC PHẢN HỒI ===
                - Dựa VÀO ĐÚNG kết quả (Observation) trả về từ tools. ⚠️ TUYỆT ĐỐI KHÔNG ĐƯỢC BỊA THÊM DỮ LIỆU.
                - Nếu kết quả là số lượng (VD: count=0), trả lời chính xác số lượng đó. Tuyệt đối KHÔNG TỰ SÁNG TÁC danh sách hồ sơ, tên người, bảng biểu nếu tool không trả về thông tin chi tiết.
                - Nếu cần tính phần trăm, hãy sử dụng tính toán toán học chính xác từ con số trả về.

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
        int currentYear = LocalDateTime.now().getYear();

        return switch (toolName) {

            case "tk_theo_trang_thai" ->
                toJson(gqRepo.countGroupByStatus(userId));

            case "tk_gq_theo_thang" -> {
                int month = args.path("month").asInt(currentMonth);
                int year = args.path("year").asInt(currentYear);
                long count = ((Number) gqRepo.countByMonth(userId, month, year)).longValue();
                yield String.format(
                        "{\"count\":%d,\"month\":%d,\"year\":%d,\"loai\":\"GQ_HOSO\"}",
                        count, month, year);
            }

            case "tk_tn_theo_thang" -> {
                int month = args.path("month").asInt(currentMonth);
                int year = args.path("year").asInt(currentYear);
                long count = ((Number) tnRepo.countByMonth(userId, month, year)).longValue();
                yield String.format(
                        "{\"count\":%d,\"month\":%d,\"year\":%d,\"loai\":\"TN_HOSO\"}",
                        count, month, year);
            }

            case "tk_gq_theo_nam" -> {
                int year = args.path("year").asInt(currentYear);
                long count = ((Number) gqRepo.countByYear(userId, year)).longValue();
                yield String.format(
                        "{\"count\":%d,\"year\":%d,\"loai\":\"GQ_HOSO\"}",
                        count, year);
            }

            case "tk_tn_theo_nam" -> {
                int year = args.path("year").asInt(currentYear);
                long count = ((Number) tnRepo.countByYear(userId, year)).longValue();
                yield String.format(
                        "{\"count\":%d,\"year\":%d,\"loai\":\"TN_HOSO\"}",
                        count, year);
            }

            case "tk_qua_han" -> {
                long overdue = ((Number) gqRepo.countOverdue(userId)).longValue();
                long all = ((Number) gqRepo.countAll(userId)).longValue();
                double pct = all > 0 ? (overdue * 100.0 / all) : 0;
                yield String.format(
                        "{\"qua_han\":%d,\"tong\":%d,\"ty_le\":\"%.1f%%\"}",
                        overdue, all, pct);
            }

            case "tk_tong_hop" -> {
                long all = ((Number) gqRepo.countAll(userId)).longValue();
                long pending = ((Number) gqRepo.countPending(userId)).longValue();
                long overdue = ((Number) gqRepo.countOverdue(userId)).longValue();
                long today = ((Number) gqRepo.countAssignedToday(userId)).longValue();
                long tnToday = ((Number) tnRepo.countToday(userId)).longValue();
                yield String.format(
                        "{\"tong_gq\":%d,\"dang_xu_ly\":%d,\"qua_han\":%d," +
                                "\"gq_hom_nay\":%d,\"tn_hom_nay\":%d}",
                        all, pending, overdue, today, tnToday);
            }

            default -> "{\"error\": \"Tool không tồn tại: " + toolName + "\"}";
        };
    }
}
