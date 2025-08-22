package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.AccommodationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.apache.poi.util.LocaleID.OM;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiService {

    private final WebClient.Builder webClientBuilder;

    private static final ObjectMapper OM = new ObjectMapper();

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

    /** 관광지 상세 설명(overview) 조회 */
    public String getPlaceDescription(Long contentId) {
        String url = baseUrl + "/detailCommon2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&contentId=" + contentId
                + "&overviewYN=Y"
                + "&defaultYN=Y";

        try {
            JsonNode root = wc().get().uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isArray() && items.size() > 0) {
                return items.get(0).path("overview").asText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /** 지역기반 관광지 조회 (contentTypeId 기본 12=관광지) */
    public List<TourSpot> getTopAttractions(int areaCode, Integer sigunguCode, int limit) {
        String url = baseUrl + "/areaBasedList2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&arrange=P"
                + "&contentTypeId=12"
                + "&pageNo=1&numOfRows=" + Math.max(limit, 3)
                + "&areaCode=" + areaCode
                + (sigunguCode != null ? "&sigunguCode=" + sigunguCode : "");

        JsonNode root = wc().get().uri(URI.create(url)).retrieve().bodyToMono(JsonNode.class).block();
        JsonNode items = root.path("response").path("body").path("items").path("item");

        List<TourSpot> list = new java.util.ArrayList<>();
        if (items.isArray()) {
            for (JsonNode it : items) {
                Long contentId = it.path("contentid").asLong();
                String title = it.path("title").asText("");
                String addr1 = it.path("addr1").asText("");
                String addr2 = it.path("addr2").asText("");
                String addr = addr1 + (addr2.isBlank() ? "" : (" " + addr2));

                if (!title.isBlank()) {
                    list.add(new TourSpot(contentId, title, addr));
                }
            }
        }
        return list.size() > limit ? list.subList(0, limit) : list;
    }
    // TourApiService.java (추가 또는 대체)
//    public AccommodationDTO getOneAccommodation(int areaCode, Integer sigunguCode) {
//        String url = baseUrl + "/areaBasedList2"
//                + "?serviceKey=" + serviceKeyEnc
//                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
//                + "&contentTypeId=32"   // 숙박
//                + "&arrange=P"          // 인기/조회수 우선
//                + "&numOfRows=1&pageNo=1"
//                + "&areaCode=" + areaCode
//                + (sigunguCode != null ? "&sigunguCode=" + sigunguCode : "");
//
//        try {
//            JsonNode root = wc().get().uri(URI.create(url))
//                    .retrieve()
//                    .bodyToMono(JsonNode.class)
//                    .block();
//
//            JsonNode items = root.path("response").path("body").path("items").path("item");
//            if (items.isArray() && items.size() > 0) {
//                JsonNode it = items.get(0);
//
//                Long contentId = it.path("contentid").asLong();
//                String title = it.path("title").asText("");
//                String addr1 = it.path("addr1").asText("");
//                String addr2 = it.path("addr2").asText("");
//                String address = addr1 + (addr2.isBlank() ? "" : " " + addr2);
//
//                // 설명은 detailCommon2.overview 사용
//                String overview = getPlaceDescription(contentId); // 없으면 "" 반환
//
//                AccommodationDTO dto = new AccommodationDTO();
//                dto.setName(title);
//                dto.setAddress(address);
//                dto.setDescription(overview != null ? overview : "");
//                return dto;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public AccommodationDTO getOneAccommodationByCountyName(String countyName) {
        Integer jeonnam = findJeonnamAreaCode();
        if (jeonnam == null) return null;
        Integer sigungu = findSigunguCodeByCounty(jeonnam, countyName);
        return getOneAccommodation(jeonnam, sigungu);
    }

    // 장소 별 대표 이미지 추출
    public String getFirstPlaceImageUrl(Long contentId) {
        if (contentId == null) return null;

        String url = baseUrl + "/detailImage2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&contentId=" + contentId
                + "&imageYN=Y"
                + "&numOfRows=1&pageNo=1";

        try {
            JsonNode root = wc().get().uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isArray() && items.size() > 0) {
                JsonNode it = items.get(0);
                String origin = it.path("originimgurl").asText("");
                String small  = it.path("smallimageurl").asText("");
                if (!origin.isBlank()) return origin;
                if (!small.isBlank())  return small;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 숙소 랜덤 선택
// TourApiService.java (추가/교체)

    private static class StayPage {
        final int totalCount;
        final List<JsonNode> items;
        StayPage(int totalCount, List<JsonNode> items) {
            this.totalCount = totalCount;
            this.items = items;
        }
    }

    private void logHeader(String tag, String url, JsonNode root, boolean dumpRawIfNotOk) {
        try {
            JsonNode header = root.path("response").path("header");
            String code = header.path("resultCode").asText("MISSING");
            String msg  = header.path("resultMsg").asText("MISSING");
            log.info("[{}] resultCode={} resultMsg={}", tag, code, msg);
            if (!"0000".equals(code) && dumpRawIfNotOk) {
                String raw = OM.writeValueAsString(root);
                if (raw.length() > 2000) raw = raw.substring(0, 2000) + "...(truncated)";
                log.warn("[{}] RAW url={} body={}", tag, url, raw);
            }
        } catch (Exception e) {
            log.error("[{}] header log error: {}", tag, e.toString(), e);
        }
    }

    private StayPage fetchStayPage(int areaCode, Integer sigunguCode, int numRows, int pageNo) {
        String url = baseUrl + "/searchStay2"
                + "?serviceKey=" + serviceKeyEnc
                + "&MobileOS=ETC&MobileApp=AppTest&_type=json"
                + "&arrange=P"              // 인기/조회
                + "&numOfRows=" + Math.max(numRows, 10)
                + "&pageNo=" + Math.max(pageNo, 1)
                + (areaCode > 0 ? "&areaCode=" + areaCode : "")
                + (sigunguCode != null ? "&sigunguCode=" + sigunguCode : "");

        log.info("[STAY-PAGE] url={}", url);
        try {
            JsonNode root = wc().get().uri(URI.create(url))
                    .retrieve().bodyToMono(JsonNode.class).block();

            logHeader("STAY-PAGE", url, root, true);

            int total = root.path("response").path("body").path("totalCount").asInt(0);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            List<JsonNode> list = new ArrayList<>();
            if (items != null && !items.isNull()) {
                if (items.isArray()) items.forEach(list::add);
                else if (items.isObject()) list.add(items);
            }
            log.info("[STAY-PAGE] totalCount={} size={}", total, list.size());

            return new StayPage(total, list);
        } catch (Exception e) {
            log.error("[STAY-PAGE] Exception: {}", e.toString(), e);
            return new StayPage(0, List.of());
        }
    }

    private static int ceilDiv(int a, int b) { return (a + b - 1) / b; }

    /** 숙소 1개(이름/주소/설명) — searchStay2 기반 랜덤 선택 */
    public AccommodationDTO getOneAccommodationRandomized(int areaCode, Integer sigunguCode, long seed, int pool) {
        log.info("[ACC-RAND-STAY] areaCode={} sigunguCode={} seed={} pool={}", areaCode, sigunguCode, seed, pool);

        int rows = Math.max(pool, 10);

        // 1) 우선 시군구 단위
        StayPage page = fetchStayPage(areaCode, sigunguCode, rows, 1);

        // 2) 시군구가 비면 도(광역)로 폴백
        if (page.items.isEmpty() && sigunguCode != null) {
            log.warn("[ACC-RAND-STAY] empty at sigungu={}, fallback to area only", sigunguCode);
            page = fetchStayPage(areaCode, null, rows, 1);
        }

        // 3) 페이지 다양화 (총건수 기반)
        if (page.items.isEmpty() && page.totalCount > 0) {
            int maxPage = Math.max(1, Math.min(5, ceilDiv(page.totalCount, rows))); // 과호출 방지
            int pagePick = Math.floorMod((int) seed, maxPage) + 1;
            log.info("[ACC-RAND-STAY] try other page={}/{}", pagePick, maxPage);
            page = fetchStayPage(areaCode, sigunguCode, rows, pagePick);
            if (page.items.isEmpty() && sigunguCode != null) {
                page = fetchStayPage(areaCode, null, rows, pagePick);
            }
        }

        if (page.items.isEmpty()) {
            log.error("[ACC-RAND-STAY] NO CANDIDATES (searchStay2) -> return null");
            return null;
        }

        // 4) 셔플 후 첫 번째 선택 (편향 제거)
        java.util.Random rnd = new java.util.Random(seed ^ 0x9E3779B97F4A7C15L);
        java.util.Collections.shuffle(page.items, rnd);
        JsonNode it = page.items.get(0);

        Long contentId = it.path("contentid").asLong(0);
        String title = it.path("title").asText("");
        String addr1 = it.path("addr1").asText("");
        String addr2 = it.path("addr2").asText("");
        String address = addr1 + (addr2.isBlank() ? "" : " " + addr2);

        String overview = getPlaceDescription(contentId); // detailCommon2 재사용

        AccommodationDTO dto = new AccommodationDTO();
        dto.setName(title);
        dto.setAddress(address);
        dto.setDescription(overview != null ? overview : "");
        log.info("[ACC-RAND-STAY] pick contentId={} title='{}'", contentId, title);
        return dto;
    }

    /** 숙소 1개(첫 번째) — searchStay2 기반 고정 버전(원하면 사용) */
    public AccommodationDTO getOneAccommodation(int areaCode, Integer sigunguCode) {
        StayPage page = fetchStayPage(areaCode, sigunguCode, 1, 1);
        if (page.items.isEmpty() && sigunguCode != null) {
            page = fetchStayPage(areaCode, null, 1, 1);
        }
        if (page.items.isEmpty()) return null;

        JsonNode it = page.items.get(0);
        Long contentId = it.path("contentid").asLong(0);
        String title = it.path("title").asText("");
        String addr1 = it.path("addr1").asText("");
        String addr2 = it.path("addr2").asText("");
        String address = addr1 + (addr2.isBlank() ? "" : " " + addr2);

        String overview = getPlaceDescription(contentId);

        AccommodationDTO dto = new AccommodationDTO();
        dto.setName(title);
        dto.setAddress(address);
        dto.setDescription(overview != null ? overview : "");
        return dto;
    }





    /** DTO 확장 (contentId 포함) */
    public record TourSpot(Long contentId, String name, String address) {}

}
