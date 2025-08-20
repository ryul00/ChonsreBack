package TourCompetition.ChonsreBack.Domain.Func.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class TourApiService {

    private final WebClient.Builder webClientBuilder;

    @Value("${tourapi.base-url}")
    private String baseUrl;

    @Value("${tourapi.key-enc}") //  Encoding 키 사용 (%2B, %3D%3D 포함된 버전)
    private String serviceKeyEnc;

    public Mono<JsonNode> getAreaBasedList(
            Integer areaCode, Integer sigunguCode,
            Integer contentTypeId, Integer pageNo, Integer numOfRows
    ) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl)
                .append("/areaBasedList2")
                .append("?serviceKey=").append(serviceKeyEnc) // Encoding 키
                .append("&MobileOS=ETC")
                .append("&MobileApp=AppTest")
                .append("&_type=json")
//                .append("&listYN=Y")
                .append("&arrange=A")
                .append("&pageNo=").append(pageNo == null ? 1 : pageNo)
                .append("&numOfRows=").append(numOfRows == null ? 10 : numOfRows);

        if (areaCode != null) urlBuilder.append("&areaCode=").append(areaCode);
        if (sigunguCode != null) urlBuilder.append("&sigunguCode=").append(sigunguCode);
        if (contentTypeId != null) urlBuilder.append("&contentTypeId=").append(contentTypeId);

        String url = urlBuilder.toString();
        System.out.println("▶ 호출 URL: " + url); // 디버그용

        return webClientBuilder.build()
                .get()
                .uri(URI.create(url)) // 인코딩 방지
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
