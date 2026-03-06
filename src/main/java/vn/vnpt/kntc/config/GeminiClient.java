package vn.vnpt.kntc.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GeminiClient — Gọi Google Gemini API trực tiếp qua REST (OkHttp).
 *
 * Hỗ trợ 2 model:
 *   - gemini-2.0-flash  (nhanh, rẻ — khuyên dùng)
 *   - gemini-1.5-pro    (thông minh hơn, chậm hơn)
 *
 * API key: https://aistudio.google.com/app/apikey (FREE tier có sẵn)
 *
 * Cách dùng:
 *   export GEMINI_API_KEY=AIzaSy...
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public GeminiClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model  = model;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Gọi Gemini với danh sách tin nhắn (system + user + assistant xen kẽ).
     *
     * @param systemPrompt  Prompt hệ thống (role của agent)
     * @param messages      Lịch sử hội thoại: [{"role":"user/model","text":"..."}]
     * @return              Text response từ Gemini
     */
    public String chat(String systemPrompt, List<ChatMessage> messages) {
        String url = BASE_URL + model + ":generateContent?key=" + apiKey;

        ObjectNode body = objectMapper.createObjectNode();

        // System instruction
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sysNode = body.putObject("system_instruction");
            ArrayNode sysParts = sysNode.putArray("parts");
            sysParts.addObject().put("text", systemPrompt);
        }

        // Conversation contents
        ArrayNode contents = body.putArray("contents");
        for (ChatMessage msg : messages) {
            ObjectNode contentNode = contents.addObject();
            contentNode.put("role", msg.role()); // "user" or "model"
            ArrayNode parts = contentNode.putArray("parts");
            parts.addObject().put("text", msg.text());
        }

        // Generation config
        ObjectNode config = body.putObject("generationConfig");
        config.put("temperature", 0.0);
        config.put("maxOutputTokens", 2048);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            log.debug("Gemini request → model: {}, messages: {}", model, messages.size());

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody,
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("Gemini API error {}: {}", response.code(), responseBody);
                    handleHttpError(response.code(), responseBody);
                }

                return parseResponse(responseBody);
            }

        } catch (GeminiException e) {
            throw e;
        } catch (IOException e) {
            log.error("Gemini network error: {}", e.getMessage());
            throw new GeminiException("Network error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Gemini unexpected error: {}", e.getMessage());
            throw new GeminiException("Unexpected error: " + e.getMessage());
        }
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Kiểm tra lỗi từ API
        if (root.has("error")) {
            String errMsg = root.path("error").path("message").asText("Unknown error");
            throw new GeminiException("Gemini API error: " + errMsg);
        }

        // Parse response text
        JsonNode text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text");

        if (text.isMissingNode()) {
            // Kiểm tra finishReason để thông báo rõ
            String finishReason = root
                    .path("candidates").get(0)
                    .path("finishReason").asText("");
            if ("SAFETY".equals(finishReason)) {
                return "Final Answer: Nội dung bị chặn bởi safety filter. Vui lòng thử lại.";
            }
            throw new GeminiException("Không parse được response: " + responseBody);
        }

        return text.asText();
    }

    private void handleHttpError(int code, String body) {
        switch (code) {
            case 400 -> throw new GeminiException("Request không hợp lệ (400): " + body);
            case 401, 403 -> throw new GeminiException(
                    "API key không hợp lệ hoặc không có quyền ("+code+"). " +
                            "Kiểm tra GEMINI_API_KEY tại https://aistudio.google.com/app/apikey");
            case 429 -> throw new GeminiException(
                    "Vượt quá rate limit (429). " +
                            "Gemini Free: 15 requests/min. Chờ 1 phút rồi thử lại.");
            case 500, 503 -> throw new GeminiException(
                    "Gemini server lỗi (" + code + "). Thử lại sau.");
            default -> throw new GeminiException(
                    "HTTP " + code + ": " + body);
        }
    }

    // ── Record đơn giản đại diện 1 tin nhắn ──────────────────────
    public record ChatMessage(String role, String text) {
        /** Tạo tin nhắn từ user */
        public static ChatMessage user(String text) {
            return new ChatMessage("user", text);
        }
        /** Tạo tin nhắn từ model (assistant) */
        public static ChatMessage model(String text) {
            return new ChatMessage("model", text);
        }
    }

    // ── Custom exception ──────────────────────────────────────────
    public static class GeminiException extends RuntimeException {
        public GeminiException(String message) { super(message); }
    }
}