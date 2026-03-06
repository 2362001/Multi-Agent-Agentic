package vn.vnpt.kntc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.vnpt.kntc.dto.ChatRequest;
import vn.vnpt.kntc.dto.ChatResponse;
import vn.vnpt.kntc.workflow.OrchestratorAgent;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KntcAgentService {

    private final OrchestratorAgent orchestrator;

    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        log.info("Chat | userId={} | query={}", request.getUserId(), request.getQuery());

        String answer = orchestrator.run(
            request.getQuery(),
            request.getUserId(),
            request.getUserName()
        );

        long elapsed = System.currentTimeMillis() - start;
        log.info("Chat hoàn thành trong {}ms", elapsed);

        return ChatResponse.builder()
            .answer(answer)
            .userId(request.getUserId())
            .query(request.getQuery())
            .processingTimeMs(elapsed)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
