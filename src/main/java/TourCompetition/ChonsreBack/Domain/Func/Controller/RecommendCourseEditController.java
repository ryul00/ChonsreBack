package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseEditRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Service.AiCourseEditService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("recommend")
public class RecommendCourseEditController {
    private final AiCourseEditService courseEditService;
    private final AuthService authService;


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


}
