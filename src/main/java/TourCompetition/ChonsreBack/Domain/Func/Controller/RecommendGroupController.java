package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.RecommendGroupRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import TourCompetition.ChonsreBack.Domain.Func.Repository.RecommendGroupRepository;
import TourCompetition.ChonsreBack.Domain.Func.Service.AiRequestService;
import TourCompetition.ChonsreBack.Domain.Func.Service.RecommendService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor // 생성자 자동 추가
@RequestMapping("/recommend")
public class RecommendGroupController {
    private final RecommendService recommendService;
    private final AuthService authService;
    private final AiRequestService aiRequestService;
    private final RecommendGroupRepository recommendGroupRepository;




    // 1차:지역 추천
//    @PostMapping("/region-first")
//    public ResponseEntity<?> recommendRegionFirst(@RequestBody RecommendGroupRequestDTO request,
//                                                  @RequestHeader(value = "Authorization", required = false) String token) {
//        Long kakaoId = null;
//        if (token != null) {
//            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
//        }
//
//        // kakaoId를 활용한 사용자 기반 로직이 있다면 이후에 확장 가능
//        String recommendedCity = aiRequestService.getRecommendedCityByProvince(request.getInpRegion());
//        return ResponseEntity.ok(Map.of("recommendedRegion", recommendedCity));
//    }

    // 1차 코스 추천 (사용자 인증 X)
    @PostMapping("/region-first")
    public ResponseEntity<?> recommendRegionFirst(@RequestBody RecommendGroupRequestDTO request) {
        String recommendedCity = aiRequestService.getRecommendedCityByProvince(request.getInpRegion());
        return ResponseEntity.ok(Map.of("recommendedRegion", recommendedCity));
    }


    // 2차: 코스 추천(회원,비회원 분기 처리 )
    @PostMapping("/group")
    public ResponseEntity<?> createRecommendGroup(
            @RequestBody RecommendGroupRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Long kakaoId = null;
        if (token != null) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        }

        // 회원: 그룹 저장 + 코스 생성 + groupId 반환
        if (kakaoId != null) {
            RecommendGroup group = recommendService.saveRecommendGroup(request, kakaoId);
            recommendService.generateCoursesFromGpt(group);

            return ResponseEntity.ok(Map.of(
                    "groupId", group.getGroupId()
                    // 필요 시 코스도 함께 반환 가능
                    // "courses", recommendService.getCoursesByGroupId(group.getGroupId())
            ));
        }


        //  비회원: 코스만 즉시 생성해서 CourseResponseDTO 형태로 반환
        List<CourseResponseDTO> courseDTOs = recommendService.generateCoursesWithoutSaving(request);

        return ResponseEntity.ok(Map.of(
                "courses", courseDTOs
        ));
    }



    // 3차 코스 조회
    @GetMapping("/group/{groupId}/courses")
    public ResponseEntity<?> getCoursesByGroupId(
            @PathVariable Long groupId,
            @RequestHeader(value = "Authorization", required = true) String token
    ) {
        // kakaoId 추출
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));

        // groupId에 해당하는 그룹 조회 및 소유자 체크
        RecommendGroup group = recommendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        if (group.getKakaoUser() == null || !group.getKakaoUser().getKakaoId().equals(kakaoId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
        }

        // 본인 소유가 맞으면 코스 반환
        List<CourseResponseDTO> responseDTOs = recommendService.getCoursesByGroupId(groupId);
        return ResponseEntity.ok(responseDTOs);
    }



}
