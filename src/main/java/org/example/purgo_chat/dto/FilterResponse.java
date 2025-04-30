// 필터링 응답용 DTO
package org.example.purgo_chat.dto;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterResponse {
    private boolean isAbusive;
    private String rewrittenText;

    // FastAPI 응답을 파싱하는 팩토리 메서드
    public static FilterResponse fromApiResponse(Map<String, Object> apiResponse) {
        boolean isAbusive = false;
        String rewrittenText = null;

        if (apiResponse != null) {
            // final_decision 확인
            Object decision = apiResponse.get("final_decision");
            isAbusive = decision != null && decision.toString().equals("1");

            // rewritten_text 확인
            Map<String, Object> resultInner = (Map<String, Object>) apiResponse.get("result");
            if (resultInner != null) {
                rewrittenText = (String) resultInner.get("rewritten_text");
            }
        }

        return FilterResponse.builder()
                .isAbusive(isAbusive)
                .rewrittenText(rewrittenText)
                .build();
    }
}