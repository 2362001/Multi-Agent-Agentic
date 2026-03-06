package vn.vnpt.kntc.agent.base;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context truyền vào mỗi Agent khi chạy.
 * Chứa thông tin user + kết quả từ các agent trước (nếu có).
 */
@Data
@Builder
public class AgentContext {

    private Integer userId;
    private String userName;

    @Builder.Default
    private LocalDateTime currentTime = LocalDateTime.now();

    // Kết quả từ các agent đã chạy trước (cho sequential tasks)
    @Builder.Default
    private List<AgentResult> previousResults = new ArrayList<>();

    // Extra params tuỳ chỉnh cho từng task
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    public Object getExtra(String key) {
        return extra.getOrDefault(key, null);
    }
}
