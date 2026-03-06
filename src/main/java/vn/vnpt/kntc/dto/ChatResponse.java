package vn.vnpt.kntc.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private Integer userId;
    private String query;
    private long processingTimeMs;
    private LocalDateTime timestamp;
}
