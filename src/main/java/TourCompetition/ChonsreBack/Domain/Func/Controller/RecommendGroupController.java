package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.RecommendGroupRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import TourCompetition.ChonsreBack.Domain.Func.Service.AiRequestService;
import TourCompetition.ChonsreBack.Domain.Func.Service.RecommendService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommend")
public class RecommendGroupController {
    private final RecommendService recommendService;
    private final AuthService authService;
    private final AiRequestService aiRequestService;

    public RecommendGroupController(RecommendService recommendService, AuthService authService, AiRequestService aiRequestService) {
        this.recommendService = recommendService;
        this.authService = authService;
        this.aiRequestService = aiRequestService;
    }

    // 1차:지역 추천
    @PostMapping("/region-first")
    public ResponseEntity<?> recommendRegionFirst(@RequestBody RecommendGroupRequestDTO request,
                                                  @RequestHeader(value = "Authorization", required = false) String token) {
        Long kakaoId = null;
        if (token != null) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        }

        // kakaoId를 활용한 사용자 기반 로직이 있다면 이후에 확장 가능
        String recommendedCity = aiRequestService.getRecommendedCityByProvince(request.getInpRegion());
        return ResponseEntity.ok(Map.of("recommendedRegion", recommendedCity));
    }

    // 2차: 코스 추천
    @PostMapping("/group")
    public ResponseEntity<?> createRecommendGroup(
            @RequestBody RecommendGroupRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Long kakaoId = null;
        if (token != null) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        }

        // 1. 그룹 저장
        RecommendGroup group = recommendService.saveRecommendGroup(request, kakaoId);

        // 2. GPT 코스 생성
        recommendService.generateCoursesFromGpt(group);

        return ResponseEntity.ok(Map.of("groupId", group.getGroupId()));
    }



    @GetMapping("/group/{groupId}/courses")
    public ResponseEntity<?> getCoursesByGroupId(@PathVariable Long groupId) {
        List<CourseResponseDTO> responseDTOs = recommendService.getCoursesByGroupId(groupId);
        return ResponseEntity.ok(responseDTOs);
    }


}
