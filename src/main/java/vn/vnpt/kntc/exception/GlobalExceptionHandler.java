package vn.vnpt.kntc.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Xử lý lỗi toàn cục — trả về JSON thay vì HTML error page.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<Map<String, Object>> handleAgentError(AgentException ex) {
        log.error("Agent error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống: " + ex.getMessage()));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        return Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(), "error", status.getReasonPhrase(), "message", message);
    }
}
