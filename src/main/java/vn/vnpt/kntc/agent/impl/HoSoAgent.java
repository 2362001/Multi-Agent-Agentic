package vn.vnpt.kntc.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Component;
import vn.vnpt.kntc.agent.base.BaseReActAgent;
import vn.vnpt.kntc.repository.GqHoSoRepository;
import vn.vnpt.kntc.repository.TnHoSoRepository;

import java.time.LocalDateTime;

/**
 * HoSoAgent — Chuyên xử lý câu hỏi về hồ sơ KNTC.
 * Nguồn dữ liệu: GQ_HOSO (giải quyết) + TN_HOSO (tiếp nhận)
 */
@Slf4j
@Component
public class HoSoAgent extends BaseReActAgent {

    private final GqHoSoRepository gqRepo;
    private final TnHoSoRepository tnRepo;

    public HoSoAgent(ChatClient chatClient, ObjectMapper objectMapper,
                     GqHoSoRepository gqRepo, TnHoSoRepository tnRepo) {
        super(chatClient, objectMapper);
        this.gqRepo = gqRepo;
        this.tnRepo = tnRepo;
    }

    @Override public String getAgentId() { return "HO_SO_AGENT"; }
    @Override public String getDomain()  { return "Hồ sơ KNTC"; }

    @Override
    protected String buildSystemPrompt() {
        return """
            Bạn là AI chuyên xử lý câu hỏi về hồ sơ KNTC (Khiếu nại - Tố cáo).
            Dữ liệu từ 2 bảng: GQ_HOSO (giải quyết) và TN_HOSO (tiếp nhận đơn thư).
            
            === TOOLS CÓ SẴN ===
            
            1. gq_find_assigned_today(userId)
               Mục đích: Lấy hồ sơ GQ được phân công cho user HÔM NAY
               Dùng khi: "hôm nay có hồ sơ gì", "hồ sơ mới phân công"
            
            2. gq_find_overdue(userId)
               Mục đích: Hồ sơ GQ đã quá hạn ngày phê duyệt GQKNTC
               Dùng khi: "hồ sơ quá hạn", "hồ sơ trễ deadline"
            
            3. gq_find_due_soon(userId, hours)
               Mục đích: Hồ sơ GQ sắp đến hạn trong N giờ (mặc định 24h)
               Dùng khi: "hồ sơ gấp", "sắp đến hạn", "cần xử lý gấp"
            
            4. gq_find_overdue_ktrdK(userId)
               Mục đích: Hồ sơ quá hạn kiểm tra điều kiện thụ lý
               Dùng khi: hỏi về kiểm tra điều kiện
            
            5. gq_find_pending_signature(userId)
               Mục đích: Hồ sơ đang chờ trình ký
               Dùng khi: "hồ sơ chờ ký", "cần trình ký"
            
            6. gq_count(userId, filter)
               Mục đích: Đếm hồ sơ. filter = all | pending | overdue | today
               Dùng khi: "có bao nhiêu hồ sơ", "số lượng hồ sơ"
            
            7. gq_search(userId, keyword)
               Mục đích: Tìm hồ sơ theo mã hoặc nội dung
               Dùng khi: user đề cập tên cụ thể, mã hồ sơ
            
            8. gq_find_need_xac_minh(userId)
               Mục đích: Hồ sơ cần thành lập tổ xác minh
               Dùng khi: hỏi về tổ xác minh
            
            9. tn_find_pending(userId)
               Mục đích: Đơn thư TN chờ phân công xử lý
               Dùng khi: "đơn thư chờ xử lý", "đơn chưa phân công"
            
            10. tn_count_today(userId)
                Mục đích: Số đơn thư TN tiếp nhận hôm nay
                Dùng khi: "hôm nay tiếp nhận bao nhiêu đơn"
            
            === QUY TẮC ===
            - Câu hỏi về "gấp/khẩn" → dùng gq_find_overdue + gq_find_due_soon
            - Câu hỏi về "hôm nay"   → dùng gq_find_assigned_today
            - Câu hỏi về số lượng    → dùng gq_count
            - Câu hỏi chung chung    → dùng cả GQ lẫn TN
            - Có thể gọi nhiều tools nếu cần
            
            === FORMAT ===
            Thought: <phân tích câu hỏi, quyết định tool nào>
            Action: <tên tool>
            Action Input: {"userId": <id>, "param": "value"}
            
            (Sau khi có Observation, tiếp tục Thought/Action nếu cần)
            
            Final Answer: <câu trả lời đầy đủ bằng tiếng Việt, có mã hồ sơ cụ thể>
            """;
    }

    @Override
    protected String executeTool(String toolName, JsonNode args) {
        Integer userId = args.path("userId").asInt();

        return switch (toolName) {
            case "gq_find_assigned_today" ->
                toJson(gqRepo.findAssignedToday(userId));

            case "gq_find_overdue" ->
                toJson(gqRepo.findOverdue(userId));

            case "gq_find_due_soon" -> {
                int hours = args.path("hours").asInt(24);
                yield toJson(gqRepo.findDueSoon(
                    userId, LocalDateTime.now().plusHours(hours)
                ));
            }

            case "gq_find_overdue_ktrdk" ->
                toJson(gqRepo.findOverdueKtrDk(userId));

            case "gq_find_pending_signature" ->
                toJson(gqRepo.findPendingSignature(userId));

            case "gq_count" -> {
                String filter = args.path("filter").asText("all");
                long count = switch (filter) {
                    case "pending"  -> gqRepo.countPending(userId);
                    case "overdue"  -> gqRepo.countOverdue(userId);
                    case "today"    -> gqRepo.countAssignedToday(userId);
                    default         -> gqRepo.countAll(userId);
                };
                yield "{\"count\":" + count + ", \"filter\":\"" + filter + "\"}";
            }

            case "gq_search" -> {
                String keyword = args.path("keyword").asText();
                yield toJson(gqRepo.searchByKeyword(userId, keyword));
            }

            case "gq_find_need_xac_minh" ->
                toJson(gqRepo.findNeedToXacMinh(userId));

            case "tn_find_pending" ->
                toJson(tnRepo.findPendingByUser(userId));

            case "tn_count_today" ->
                "{\"count\":" + tnRepo.countToday(userId) + "}";

            default -> "{\"error\": \"Tool không tồn tại: " + toolName + "\"}";
        };
    }
}
