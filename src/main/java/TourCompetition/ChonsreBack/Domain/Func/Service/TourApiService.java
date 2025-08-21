package TourCompetition.ChonsreBack.Domain.Func.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourApiService {

    private final WebClient.Builder webClientBuilder;

    @Value("${tourapi.base-url}")
    private String baseUrl;

    @Value("${tourapi.key-enc}") // Encoding된 서비스키
    private String serviceKeyEnc;

    private WebClient wc() { return webClientBuilder.build(); }

    /** 지역 코드 목록(시/도) */
    public JsonNode getAreaCodeList() {
        String url = baseUrl + "/areaCode2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&numOfRows=100&pageNo=1";
        return wc().get().uri(URI.create(url)).retrieve().bodyToMono(JsonNode.class).block();
    }

    /** 시군구 코드 목록 */
    public JsonNode getSigunguCodeList(int areaCode) {
        String url = baseUrl + "/areaCode2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&areaCode=" + areaCode
                + "&numOfRows=200&pageNo=1";
        return wc().get().uri(URI.create(url)).retrieve().bodyToMono(JsonNode.class).block();
    }

    /** '전라남도' areaCode 찾기 (문자열 매칭, 공백/대소문 무시) */
    public Integer findJeonnamAreaCode() {
        JsonNode root = getAreaCodeList();
        JsonNode items = root.path("response").path("body").path("items").path("item");
        if (items.isArray()) {
            for (JsonNode it : items) {
                String name = it.path("name").asText("");
                if (name.replace(" ", "").contains("전라남도")) {
                    return it.path("code").asInt();
                }
            }
        }
        return null; // 못 찾으면 null
    }

    /** 시군구명으로 sigunguCode 찾기 (예: '순천시', '나주시' 등) */
    public Integer findSigunguCodeByCounty(int areaCode, String countyName) {
        if (countyName == null || countyName.isBlank()) return null;
        JsonNode root = getSigunguCodeList(areaCode);
        JsonNode items = root.path("response").path("body").path("items").path("item");
        String key = countyName.replace(" ", "");
        if (items.isArray()) {
            for (JsonNode it : items) {
                String name = it.path("name").asText("").replace(" ", "");
                if (name.equals(key) || name.contains(key) || key.contains(name)) {
                    return it.path("code").asInt();
                }
            }
        }
        return null;
    }

    /** 지역기반 관광지 조회 (contentTypeId 기본 12=관광지) */
    public List<TourSpot> getTopAttractions(int areaCode, Integer sigunguCode, int limit) {
        String url = baseUrl + "/areaBasedList2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&arrange=P"               // 인기순 정렬(없으면 A)
                + "&contentTypeId=12"        // 관광지
                + "&pageNo=1&numOfRows=" + Math.max(limit, 3)
                + "&areaCode=" + areaCode
                + (sigunguCode != null ? "&sigunguCode=" + sigunguCode : "");

        JsonNode root = wc().get().uri(URI.create(url)).retrieve().bodyToMono(JsonNode.class).block();
        JsonNode items = root.path("response").path("body").path("items").path("item");

        List<TourSpot> list = new java.util.ArrayList<>();
        if (items.isArray()) {
            for (JsonNode it : items) {
                String title = it.path("title").asText("");
                String addr1 = it.path("addr1").asText("");
                String addr2 = it.path("addr2").asText("");
                String addr = addr1 + (addr2.isBlank() ? "" : (" " + addr2));
                if (!title.isBlank()) {
                    list.add(new TourSpot(title, addr));
                }
            }
        }
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    /** 간단 DTO */
    public record TourSpot(String name, String address) {}
}
