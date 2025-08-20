package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.RecommendGroupRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import TourCompetition.ChonsreBack.Domain.Func.Repository.RecommendGroupRepository;
import TourCompetition.ChonsreBack.Domain.Func.Service.RecommendService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recommend")
public class RecommendGroupController {

    private final RecommendService recommendService;
    private final AuthService authService;
    private final RecommendGroupRepository recommendGroupRepository;

    // (전남 고정으로 지역 추천은 불필요. 필요시 복구)
    // @PostMapping("/region-first") ...

    /**
     * 2차: 코스 추천 (회원/비회원 분기)
     * - 회원: 그룹 저장 + 코스 저장 → groupId 반환
     * - 비회원: 저장 없이 코스 미리보기 반환
     */
    @PostMapping("/group")
    public ResponseEntity<?> createRecommendGroup(
            @RequestBody RecommendGroupRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Long kakaoId = null;
        if (token != null && !token.isBlank()) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        }

        // 회원 플로우: 그룹 저장 후 코스 생성/저장
        if (kakaoId != null) {
            RecommendGroup group = recommendService.saveRecommendGroup(request, kakaoId);
            // 엑셀 후보 → GPT 선택 → DB 저장
            recommendService.generateCoursesFromDatasetWithGpt(group);

            return ResponseEntity.ok(Map.of(
                    "groupId", group.getGroupId()
            ));
        }

        // 비회원 플로우: 저장 없이 미리보기
        List<CourseResponseDTO> courseDTOs = recommendService.generateCoursesWithoutSaving(request);
        return ResponseEntity.ok(Map.of("courses", courseDTOs));
    }

    /**
     * 3차: 저장된 추천 코스 조회 (본인만)
     */
    @GetMapping("/group/{groupId}/courses")
    public ResponseEntity<?> getCoursesByGroupId(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));

        RecommendGroup group = recommendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        if (group.getKakaoUser() == null || !group.getKakaoUser().getKakaoId().equals(kakaoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }

        List<CourseResponseDTO> responseDTOs = recommendService.getCoursesByGroupId(groupId);
        return ResponseEntity.ok(responseDTOs);
    }
}
