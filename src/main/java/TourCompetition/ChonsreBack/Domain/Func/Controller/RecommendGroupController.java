package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.RecommendGroupRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
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

    public RecommendGroupController(RecommendService recommendService, AuthService authService) {
        this.recommendService = recommendService;
        this.authService = authService;
    }
    @PostMapping("/group")
    public ResponseEntity<?> createRecommendGroup(@RequestBody RecommendGroupRequestDTO request,
                                                  @RequestHeader(value = "Authorization", required = false) String token) {
        Long kakaoId = null;
        if (token != null) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        }

        Long groupId = recommendService.createRecommendGroup(request, kakaoId);

        return ResponseEntity.ok(Map.of("groupId", groupId));
    }


    @GetMapping("/group/{groupId}/courses")
    public ResponseEntity<?> getCoursesByGroupId(@PathVariable Long groupId) {
        List<CourseResponseDTO> courses = recommendService.getCoursesByGroupId(groupId);
        return ResponseEntity.ok(courses);
    }


}
