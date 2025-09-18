// [NEW FILE] SavedCourseQueryController.java
package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.SavedCourseListItemDTO;
import TourCompetition.ChonsreBack.Domain.Func.Service.SavedCourseQueryService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("recommend")
public class SavedCourseQueryController {

    private final SavedCourseQueryService queryService;
    private final AuthService authService;

    // 미래/진행중 목록
    @GetMapping("/saved/upcoming")
    public ResponseEntity<List<SavedCourseListItemDTO>> listUpcoming(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "today", required = false) String today
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        return ResponseEntity.ok(queryService.listUpcoming(kakaoId, today));
    }

    // 과거 목록
    @GetMapping("/saved/past")
    public ResponseEntity<List<SavedCourseListItemDTO>> listPast(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "today", required = false) String today
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        return ResponseEntity.ok(queryService.listPast(kakaoId, today));
    }

    // 미래/진행중 상세
    @GetMapping("/saved/upcoming/{savedId}")
    public ResponseEntity<Map<String, Object>> getUpcomingDetail(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        CourseResponseDTO course = queryService.getSavedDetail(savedId, kakaoId);
        return ResponseEntity.ok(Map.of("savedId", savedId, "course", course));
    }

    // 과거 상세
    @GetMapping("/saved/past/{savedId}")
    public ResponseEntity<Map<String, Object>> getPastDetail(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        CourseResponseDTO course = queryService.getSavedDetail(savedId, kakaoId);
        return ResponseEntity.ok(Map.of("savedId", savedId, "course", course));
    }


}
