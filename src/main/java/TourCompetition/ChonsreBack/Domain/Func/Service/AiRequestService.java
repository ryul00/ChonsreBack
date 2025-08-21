package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseDayDTO;
//import TourCompetition.ChonsreBack.Domain.Func.DTO.GptCourseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Locale;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Service
@RequiredArgsConstructor
public class AiRequestService {
    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    // === XLSX 설정 (application.yml) ===
    @Value("${dataset.jeonnam.path}")
    private String datasetPath; // e.g. datasets/jeonnam_rural.xlsx (classpath 기준)

    @Value("${dataset.region-fixed:전라남도}")
    private String regionFixed;  // 시도 고정

    @Value("${dataset.columns.category}")
    private String colCategory;  // "구분"
    @Value("${dataset.columns.county}")
    private String colCounty;    // "시군"
    @Value("${dataset.columns.place}")
    private String colPlace;     // "장소명"
    @Value("${dataset.columns.address:}")
    private String colAddress;   // "주소" (선택)
    @Value("${dataset.columns.desc:}")
    private String colDesc;      // "체험" (선택)

    private static class PlaceRow {
        String region;   // 전라남도(고정)
        String county;   // 시군
        String category; // 구분 (농촌/어촌/기타 등)
        String place;    // 장소명
        String address;  // 주소
        String desc;     // 체험

        boolean matchesStyle(String style) {
            String s = StringUtils.defaultString(category).toLowerCase(Locale.ROOT);
            String st = StringUtils.defaultString(style).toLowerCase(Locale.ROOT);
            if (st.contains("farm"))    return s.contains("농");
            if (st.contains("fishing")) return s.contains("어") || s.contains("바다");
            return true; // etc
        }
    }

    // 시군구 엑셀 값 가져오기
    public String findCountyByPlaceName(String placeName) {
        if (placeName == null) return "전라남도";
        if (cachedRows == null || cachedRows.isEmpty()) {
            loadDatasetOnce();
        }
        for (PlaceRow r : cachedRows) {
            if (placeName.equals(r.place)) {
                return r.county != null ? r.county : "전라남도";
            }
        }
        return "전라남도"; // 매칭 실패 시 기본값
    }

    public String findAddressByPlaceName(String placeName) {
        if (placeName == null) return "";
        if (cachedRows == null || cachedRows.isEmpty()) {
            loadDatasetOnce();
        }
        for (var r : cachedRows) {
            if (placeName.equals(r.place)) {
                return StringUtils.defaultString(r.address);
            }
        }
        return "";
    }

    private List<PlaceRow> cachedRows;

    @PostConstruct
    public void loadDatasetOnce() {
        try (InputStream is = resource(datasetPath)) {
            if (is == null) throw new IllegalStateException("데이터셋을 찾을 수 없습니다: " + datasetPath);
            cachedRows = readXlsx(is);
        } catch (Exception e) {
            throw new RuntimeException("엑셀 데이터셋 로딩 실패: " + datasetPath, e);
        }
        if (cachedRows == null || cachedRows.isEmpty()) {
            throw new RuntimeException("전라남도 데이터가 비어 있습니다. 컬럼 매핑/파일 확인 필요");
        }
    }

    private InputStream resource(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    private List<PlaceRow> readXlsx(InputStream is) throws Exception {
        List<PlaceRow> rows = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new IllegalStateException("엑셀 시트가 없습니다.");

            Map<String, Integer> idx = new HashMap<>();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            for (Cell c : header) {
                c.setCellType(CellType.STRING);
                idx.put(StringUtils.trim(c.getStringCellValue()), c.getColumnIndex());
            }
            int iCategory = requiredIndex(idx, colCategory);
            int iCounty   = requiredIndex(idx, colCounty);
            int iPlace    = requiredIndex(idx, colPlace);
            int iAddress  = idx.getOrDefault(colAddress, -1);
            int iDesc     = idx.getOrDefault(colDesc, -1);

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                PlaceRow pr = new PlaceRow();
                pr.region   = regionFixed;
                pr.category = getStr(row.getCell(iCategory));
                pr.county   = getStr(row.getCell(iCounty));
                pr.place    = getStr(row.getCell(iPlace));
                pr.address  = (iAddress >= 0) ? getStr(row.getCell(iAddress)) : "";
                pr.desc     = (iDesc >= 0) ? getStr(row.getCell(iDesc)) : "";
                if (StringUtils.isNotBlank(pr.place)) rows.add(pr);
            }
        }
        return rows;
    }

    private int requiredIndex(Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null) throw new IllegalStateException("컬럼을 찾을 수 없습니다: " + key);
        return i;
    }

    private String getStr(Cell c) {
        if (c == null) return "";
        c.setCellType(CellType.STRING);
        return StringUtils.trimToEmpty(c.getStringCellValue());
    }



    public Map<String, List<CourseDayDTO>> getRecommendedCourseStructure(
            String region, String startDate, String endDate,
            int adultCnt, int childCnt, int babyCnt,
            String style
    ) {
        // 0) 엑셀 데이터 보장
        if (cachedRows == null || cachedRows.isEmpty()) {
            loadDatasetOnce();
        }

        // 1) 스타일로 후보 필터링 (엑셀 기반)
        List<PlaceRow> candidates = new ArrayList<>();
        for (PlaceRow r : cachedRows) {
            if (r.matchesStyle(style)) {
                candidates.add(r);
            }
        }
        if (candidates.size() < 3) {
            throw new IllegalStateException("선택한 스타일의 전남 후보가 3곳 미만입니다. 엑셀 데이터 보강 필요.");
        }

        // 2) 후보 수 제한(토큰 절약) + 셔플
        java.util.Collections.shuffle(candidates);
        int candidateCount = Math.min(25, candidates.size()); // 필요시 15~30 사이로 조절
        candidates = candidates.subList(0, candidateCount);

        // 3) 후보 JSON 직렬화 (GPT에 '이 중에서만 고르라'고 강제)
        StringBuilder candJson = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            PlaceRow p = candidates.get(i);
            String descPref = StringUtils.firstNonBlank(p.desc, p.address, p.region + " " + p.county);
            candJson.append(String.format(
                    java.util.Locale.ROOT,
                    "{\"idx\":%d,\"placeName\":\"%s\",\"county\":\"%s\",\"address\":\"%s\",\"desc\":\"%s\"}%s",
                    i,
                    escape(p.place),
                    escape(p.county),
                    escape(p.address),
                    escape(descPref),
                    (i < candidates.size() - 1) ? "," : ""
            ));
        }
        candJson.append("]");

        // 4) 프롬프트: 후보 안에서만 선택하도록 엄격하게
        String prompt = String.format(java.util.Locale.ROOT, """
        너는 전라남도 '촌캉스' 체험 코스 편성 전문가야.
        반드시 아래 제공된 후보 목록 **안에서만** A/B/C 각 1곳씩 선택해 Day1에 배치해.
        후보에 없는 새로운 장소명을 만들면 안 된다.

        컨텍스트(참고용):
        - 지역: %s
        - 여행 기간: %s ~ %s
        - 인원: 성인 %d, 어린이 %d, 신생아 %d
        - 요청 스타일: %s

        후보 목록(JSON 배열):
        %s

        요구사항:
        - A/B/C 각 코스의 Day1에 places 1개만 포함.
        - 가능하면 서로 다른 '시군'을 고르되, 품질을 우선.
        - description은 후보의 desc를 우선 사용. 없으면 address, 그래도 없으면 "전라남도 시군" 형태로 간단히.
        - 오직 JSON만 반환 (```로 감싸지 마).

        반환 스키마 예시:
        {
          "A": [{ "day": 1, "places": [ { "placeName": "…", "description": "…" } ] }],
          "B": [{ "day": 1, "places": [ { "placeName": "…", "description": "…" } ] }],
          "C": [{ "day": 1, "places": [ { "placeName": "…", "description": "…" } ] }]
        }
        """,
                nullTo(region), nullTo(startDate), nullTo(endDate),
                adultCnt, childCnt, babyCnt,
                nullTo(style),
                candJson
        );

        // 5) OpenAI 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);                // 주의: application.yml의 키 이름과 일치해야 함
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 여행 코스 편성 전문가다. 반드시 JSON만 출력한다."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", request, Map.class);

        // 6) 응답 파싱
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("choices")) {
            // GPT 실패 시 백업
            return fallbackThree(candidates);
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices.isEmpty() || !choices.get(0).containsKey("message")) {
            return fallbackThree(candidates);
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = String.valueOf(message.get("content"));

        if (content.startsWith("```")) {
            content = content.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```", "")
                    .trim();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, List<CourseDayDTO>> parsed =
                    mapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            // 최소 검증
            if (!parsed.containsKey("A") || !parsed.containsKey("B") || !parsed.containsKey("C")) {
                return fallbackThree(candidates);
            }
            return parsed;
        } catch (Exception e) {
            // GPT JSON 이상 시 백업
            return fallbackThree(candidates);
        }
    }
    private String escape(String s) { return s == null ? "" : s.replace("\"", "\\\""); }
    private String nullTo(String s) { return s == null ? "" : s; }

    private Map<String, List<CourseDayDTO>> fallbackThree(List<PlaceRow> candidates) {
        java.util.Collections.shuffle(candidates);
        List<PlaceRow> picks = candidates.subList(0, 3);
        String[] labels = {"A", "B", "C"};
        Map<String, List<CourseDayDTO>> result = new java.util.LinkedHashMap<>();

        for (int i = 0; i < 3; i++) {
            PlaceRow pr = picks.get(i);
            String desc = StringUtils.firstNonBlank(pr.desc, pr.address, pr.region + " " + pr.county);

            // DTO 구성
            TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CoursePlaceDTO place =
                    new TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CoursePlaceDTO();
            place.setPlaceName(pr.place);
            place.setDescription(desc);

            CourseDayDTO day1 = new CourseDayDTO();
            day1.setDay(1);
            day1.setPlaces(java.util.Collections.singletonList(place));

            result.put(labels[i], java.util.Collections.singletonList(day1));
        }
        return result;
    }



}
