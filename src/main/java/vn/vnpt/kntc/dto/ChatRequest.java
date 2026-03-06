package vn.vnpt.kntc.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String query;
    private Integer userId;
    private String userName;
}
