package org.example.purgo_chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purgo_chat.dto.FilterResponse;
import org.example.purgo_chat.entity.ChatRoom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadwordFilterService {

    private final ChatService chatService;
    private final RestTemplate purgoRestTemplate;

    @Value("${proxy.server.url}")
    private String gatewayUrl;

    public FilterResponse filterMessage(String text, ChatRoom chatRoom, String sender) {
        try {
            log.info("📤 FastAPI로 전송할 텍스트 (채팅): {}", text);

            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = purgoRestTemplate.postForEntity(gatewayUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                log.info("📦 FastAPI 응답 전체: {}", result);

                FilterResponse filterResponse = FilterResponse.fromApiResponse(result);
                log.info("욕설 여부: {}", filterResponse.isAbusive());
                log.info("대체 문장: {}", filterResponse.getRewrittenText());

                // 욕설이 감지되면 카운트 증가
                if (filterResponse.isAbusive()) {
                    chatService.incrementBadwordCount(chatRoom);
                }

                return filterResponse;
            }
        } catch (Exception e) {
            log.error("❌ 욕설 분석 실패: {}", e.getMessage(), e);
        }

        // 필터링 실패 시 원본 메시지 유지
        return FilterResponse.builder()
                .isAbusive(false)
                .rewrittenText(text)
                .build();
    }
}