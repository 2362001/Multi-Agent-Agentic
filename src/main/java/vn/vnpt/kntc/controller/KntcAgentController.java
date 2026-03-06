package vn.vnpt.kntc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.vnpt.kntc.dto.ChatRequest;
import vn.vnpt.kntc.dto.ChatResponse;
import vn.vnpt.kntc.service.KntcAgentService;

import java.util.List;
import java.util.Map;

/**
 * REST API cho hệ thống KNTC Multi-Agent.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kntc-agent")
@RequiredArgsConstructor
public class KntcAgentController {

    private final KntcAgentService agentService;

    /**
     * Endpoint chat chính.
     *
     * POST /api/v1/kntc-agent/chat
     * {
     *   "query": "Hôm nay tôi có hồ sơ gấp và lịch họp gì không?",
     *   "userId": 123,
     *   "userName": "Nguyễn Văn A"
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("POST /chat | userId={} | query={}",
            request.getUserId(), request.getQuery());

        ChatResponse response = agentService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "KNTC Multi-Agent AI",
            "agents", List.of(
                "HO_SO_AGENT", "LICH_AGENT",
                "THONG_KE_AGENT", "THONG_BAO_AGENT"
            )
        ));
    }
}
