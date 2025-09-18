package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseEditRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CoursePlaceDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.Service.AiCourseEditService;
import TourCompetition.ChonsreBack.Domain.Func.Service.TourApiService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("recommend")
public class RecommendCourseEditController {
    private final AiCourseEditService courseEditService;
    private final AuthService authService;
    private final TourApiService tourApiService;


    // 선택한 코스 편집
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<Map<String, Object>> replaceCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid CourseEditRequestDTO request,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        courseEditService.replaceCourse(courseId, request, kakaoId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "courseId", courseId,
                "message", "코스가 성공적으로 수정되었습니다."
        ));
    }

    // RecommendCourseEditController.java (엔드포인트 추가)
    @GetMapping("/places/search")
    public ResponseEntity<Map<String, Object>> searchPlaces(
            @RequestParam("keyword") String q,
            @RequestParam(value = "county", required = false) String county,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));

        // 서비스가 null을 반환하더라도 방어
        List<CoursePlaceDTO> results = tourApiService.searchAttractionsInJeonnam(q, county, limit);
        if (results == null) results = List.of();

        // ★ Map.of(...)는 null 불가 → 가변 Map 사용
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("query", q);
        // county가 null일 수 있으므로, 원하면 빈문자/생략 중 택1
        // 1) 그대로 노출(serialize 시 null 출력 허용)
        body.put("county", county);
        // 2) null 대신 빈 문자열로 치환하고 싶으면 아래로 교체
        // body.put("county", county == null ? "" : county);

        body.put("count", results.size());
        body.put("results", results);

        return ResponseEntity.ok(body);
    }



    // 편집 완료한 코스 저장
    @PostMapping("/courses/{courseId}/save")
    public ResponseEntity<Map<String, Object>> saveEditedCourse(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        var res = courseEditService.createSavedCourse(courseId, kakaoId);
        return ResponseEntity.ok(Map.of(
                "savedId", res.getSavedId(),
                "courseId", res.getCourseId(),
                "groupId", res.getGroupId()
        ));
    }

    // 저장한 코스 상세 조회
    @GetMapping("/saved/{savedId}")
    public ResponseEntity<Map<String, Object>> getSavedCourse(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        CourseResponseDTO course = courseEditService.getSavedCourseDetail(savedId, kakaoId);

        // 여기서는 코스 JSON에 집중하되, savedId만 같이 넣어둠
        return ResponseEntity.ok(Map.of(
                "savedId", savedId,
                "course", course
        ));
    }

    // 사용자가 저장한 코스 전체 조회
    @GetMapping("/saved")
    public ResponseEntity<Map<String, Object>> listSavedCourses(
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        var items = courseEditService.listSavedCourses(kakaoId);
        return ResponseEntity.ok(Map.of(
                "count", items.size(),
                "items", items
        ));
    }

    @DeleteMapping("/saved/{savedId}")
    public ResponseEntity<Map<String, Object>> deleteSavedCourse(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        courseEditService.deleteSavedCourse(savedId, kakaoId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "deletedId", savedId,
                "message", "저장된 코스가 삭제되었습니다."
        ));
    }



}
