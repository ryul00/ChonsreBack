package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.CourseDayDTO;
//import TourCompetition.ChonsreBack.Domain.Func.DTO.GptCourseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiRequestService {
    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public String getRecommendedCityByProvince(String province) {
        String prompt = String.format("""
        촌캉스는 '촌(시골)'과 '바캉스(휴가)'의 합성어로, 시골에서 휴가를 보내는 것을 의미해.
        너는 촌캉스에 특화된 여행 전문가야
        아래 도(%s)에 속한 시/군 중, 촌캉스 분위기에 가장 적합한 지역을 한 곳 추천해줘.
        결과는 도와 시/군을 모두 응답해줘 (ex.경기도 양평군)

        조건:
        - 자연, 전통, 여유로운 분위기를 느낄 수 있어야 해.
        - 결과는 절대로 ``` 으로 감싸지 말고, JSON 배열만 순수하게 반환해.
        
        {"recommendedRegion": "양주시"}
        """, province);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 대한민국 촌캉스 여행 전문가야. 반드시 JSON 형식으로 응답해."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        String content = message.get("content").toString();

        // GPT가 ```json 블럭으로 감쌀 경우 제거
        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```", "")
                    .trim();
        }

        try {
            List<Map<String, Object>> parsed = new ObjectMapper().readValue(content, new TypeReference<>() {});
            return parsed.get(0).get("recommendedRegion").toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 지역 추천 응답 파싱 실패", e);
        }


    }

    public Map<String, List<CourseDayDTO>> getRecommendedCourseStructure(
            String region, String startDate, String endDate,
            int adultCnt, int childCnt, int babyCnt,
            String style) {

        String prompt = String.format("""
            촌캉스는 '촌(시골)'과 '바캉스(휴가)'의 합성어로, 시골에서 휴가를 보내는 것을 의미해.
            너는 촌캉스에 특화된 여행 전문가야.
        
            조건:
            - 지역: %s
            - 여행 기간: %s ~ %s
            - 인원: 
                성인: %d명
                어린이: %d명
                신생아: %d명
            - 여행 스타일: %s
            다음 조건에 따라 여행 코스를 추천해줘:
            - 코스는 총 3개 (A, B, C)로 구성해.
            - 각 코스는 여행 기간 동안의 일차(day)를 기준으로 구성하고,
            - 각 일차에는 3~4개 장소(식당, 숙소 등)를 포함해.
            - 각 장소는 장소명과 간단한 설명을 포함해야 해.
            - 응답 JSON에는 지역, 스타일, 코스 ID 등의 메타데이터는 포함하지 마.
            - 아래처럼 JSON 형식으로 응답해줘:
                {
                          "A": [
                            { "day": 1, "places": [ { "placeName": "...", "description": "..." }, ... ] },
                            ...
                          ],
                          "B": [...],
                          "C": [...]
                        }}
             꼭 JSON 배열만 반환해줘. ``` 로 감싸지 마.
            ...
            """, region, startDate, endDate, adultCnt, childCnt, babyCnt, style);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 코스 데이터를 JSON으로 정확하게 반환해야 하는 여행 전문가야."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("https://api.openai.com/v1/chat/completions", request, Map.class);

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        String content = message.get("content").toString();

        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```", "")
                    .trim();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(content, new TypeReference<Map<String, List<CourseDayDTO>>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GPT 응답 파싱 실패", e);
        }
    }



}
