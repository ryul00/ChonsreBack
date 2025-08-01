package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.RecommendGroupRequestDTO;
import org.springframework.transaction.annotation.Transactional;
import TourCompetition.ChonsreBack.Domain.Func.DTO.RecommendGroupRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import TourCompetition.ChonsreBack.Domain.Func.Repository.CourseRepository;
import TourCompetition.ChonsreBack.Domain.Func.Repository.RecommendGroupRepository;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final RecommendGroupRepository recommendGroupRepository;
    private final CourseRepository courseRepository;
    private final KakaoUserRepository kakaoUserRepository;

    // 사용자 정보를 받아 추천 그룹 ABC 생성
    @Transactional
    public Long createRecommendGroup(RecommendGroupRequestDTO request, Long kakaoId) {
        RecommendGroup group = new RecommendGroup();
        group.setInpStartDate(request.getInpStartDate());
        group.setInpEndDate(request.getInpEndDate());
        group.setInpPeopleCnt(request.getInpPeopleCnt());
        group.setInpRegion(request.getInpRegion());
        group.setInpStyle(request.getInpStyle());
        group.setReqCreatedAt(LocalDateTime.now().toString());

        if (kakaoId != null) {
            KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
            group.setKakaoUser(user);
        }

        RecommendGroup savedGroup = recommendGroupRepository.save(group);

        // 더미 코스 A, B, C 생성
        for (int i = 0; i < 3; i++) {
            Course course = new Course();
            course.setTitle(group.getInpRegion() + " 코스 " + (char)('A' + i));
            course.setDescription("추천된 더미 코스입니다.");
            course.setRegion(group.getInpRegion());
            course.setStyle(Course.CourseStyle.valueOf(group.getInpStyle().name())); // Enum 매핑
            course.setCreatedAt(LocalDateTime.now().toString());
            course.setRecommendGroup(savedGroup);

            courseRepository.save(course);
        }

        return savedGroup.getGroupId();
    }

    // 추천 코스 그룹 조회 로직
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByGroupId(Long groupId) {
        RecommendGroup group = recommendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹 ID입니다: " + groupId));

        List<Course> courses = courseRepository.findByRecommendGroup(group);

        return courses.stream()
                .map(course -> {
                    CourseResponseDTO dto = new CourseResponseDTO();
                    dto.setCourseId(course.getCourseId());
                    dto.setTitle(course.getTitle());
                    dto.setDescription(course.getDescription());
                    dto.setRegion(course.getRegion());
                    dto.setStyle(course.getStyle());
                    dto.setCreatedAt(course.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }



}
