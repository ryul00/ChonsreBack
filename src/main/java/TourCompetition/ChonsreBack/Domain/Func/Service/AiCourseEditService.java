package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseEditDayDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseEditPlaceDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseEditRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.TemplateSaveResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCourseEditService {
    private final CourseRepository courseRepository;
    private final RecommendGroupRepository recommendGroupRepository;
    private final CourseDayRepository courseDayRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final KakaoUserRepository kakaoUserRepository;

    // 생성된 코스 선택 및 편집
    @Transactional
    public void replaceCourse(Long courseId, CourseEditRequestDTO req, Long kakaoId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스가 존재하지 않습니다."));

        // 소유권 체크: 코스 → 그룹 → 카카오사용자
        RecommendGroup group = course.getRecommendGroup();
        if (group == null || group.getKakaoUser() == null
                || !group.getKakaoUser().getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 1) 기존 일차/장소 삭제
        List<CourseDay> oldDays = courseDayRepository.findByCourse(course);
        for (CourseDay d : oldDays) {
            List<CoursePlace> places = coursePlaceRepository.findByCourseDay(d);
            if (!places.isEmpty()) coursePlaceRepository.deleteAll(places);
        }
        if (!oldDays.isEmpty()) courseDayRepository.deleteAll(oldDays);

        // 2) 타이틀 수정
        course.setTitle(req.getTitle());
        courseRepository.save(course);

        // 3) 새 일차/장소 생성
        for (CourseEditDayDTO d : req.getDays()) {
            CourseDay day = new CourseDay();
            day.setDayNum(d.getDay());
            day.setCourse(course);
            courseDayRepository.save(day);

            int order = 1;
            for (CourseEditPlaceDTO p : d.getPlaces()) {
                CoursePlace place = new CoursePlace();
                place.setOrderNum(order++);
                place.setPlaceName(p.getPlaceName());
                place.setPlaceDesc(p.getDescription());
                place.setCourseDay(day);
                coursePlaceRepository.save(place);
            }
        }
    }

    // 코스 편집 완료 후 저장 (savedCourse)
    @Transactional
    public TemplateSaveResponseDTO createSavedCourse(Long courseId, Long kakaoId) {
        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("코스가 존재하지 않습니다."));

        RecommendGroup group = course.getRecommendGroup();
        if (group == null || group.getKakaoUser() == null
                || !group.getKakaoUser().getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // SavedCourse 생성 (그룹의 여행일자 사용)
        SavedCourse saved = new SavedCourse();
        saved.setKakaoUser(user);
        saved.setCourse(course); // 편집된 그 코스를 바로 연결
        saved.setSvdStartDate(group.getInpStartDate());
        saved.setSvdEndDate(group.getInpEndDate());
        savedCourseRepository.save(saved);

        TemplateSaveResponseDTO res = new TemplateSaveResponseDTO();
        res.setSavedId(saved.getSavedId());
        res.setCourseId(course.getCourseId());
        res.setGroupId(group.getGroupId());
        return res;
    }
}
