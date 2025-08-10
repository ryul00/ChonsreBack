package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseDayDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CoursePlaceDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CourseDay;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CoursePlace;
import TourCompetition.ChonsreBack.Domain.Func.Repository.CourseDayRepository;
import TourCompetition.ChonsreBack.Domain.Func.Repository.CoursePlaceRepository;
import org.springframework.transaction.annotation.Transactional;
import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.RecommendGroupRequestDTO;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final RecommendGroupRepository recommendGroupRepository;
    private final CourseRepository courseRepository;
    private final KakaoUserRepository kakaoUserRepository;
    private final AiRequestService aiRequestService;
    private final CourseDayRepository courseDayRepository;
    private final CoursePlaceRepository coursePlaceRepository;

    @Transactional
    public void generateCoursesFromGpt(RecommendGroup group) {
        String region = group.getInpRegion();
        String startDate = group.getInpStartDate();
        String endDate = group.getInpEndDate();
        int adult = group.getInpAdultCnt();
        int child = group.getInpChildCnt();
        int baby = group.getInpBabyCnt();
        String style = group.getInpStyle().name();

        Map<String, List<CourseDayDTO>> gptCourseMap = aiRequestService.getRecommendedCourseStructure(
                region, startDate, endDate, adult, child, baby, style
        );

        for (Map.Entry<String, List<CourseDayDTO>> entry : gptCourseMap.entrySet()) {
            String courseTitle = entry.getKey(); // A, B, C
            List<CourseDayDTO> dayList = entry.getValue();

            Course course = new Course();
            course.setTitle(courseTitle);
            course.setStyle(Course.CourseStyle.valueOf(style));
            course.setRegion(region);
            course.setCreatedAt(LocalDateTime.now().toString());
            course.setRecommendGroup(group);
            course.setTemplate(false);

            courseRepository.save(course);

            for (CourseDayDTO dayDto : dayList) {
                CourseDay courseDay = new CourseDay();
                courseDay.setDayNum(dayDto.getDay());
                courseDay.setCourse(course);
                courseDayRepository.save(courseDay);

                int order = 1;
                for (CoursePlaceDTO placeDto : dayDto.getPlaces()) {
                    CoursePlace place = new CoursePlace();
                    place.setOrderNum(order++);
                    place.setPlaceName(placeDto.getPlaceName());
                    place.setPlaceDesc(placeDto.getDescription());
                    place.setCourseDay(courseDay);
                    coursePlaceRepository.save(place);
                }
            }
        }
    }

    // 그룹 저장용 메서드
    @Transactional
    public RecommendGroup saveRecommendGroup(RecommendGroupRequestDTO request, Long kakaoId) {
        RecommendGroup group = new RecommendGroup();
        group.setInpStartDate(request.getInpStartDate());
        group.setInpEndDate(request.getInpEndDate());
        group.setInpRegion(request.getInpRegion());
        group.setInpStyle(request.getInpStyle());
        group.setInpAdultCnt(request.getInpAdultCnt());
        group.setInpChildCnt(request.getInpChildCnt());
        group.setInpBabyCnt(request.getInpBabyCnt());
        group.setReqCreatedAt(LocalDateTime.now().toString());

        if (kakaoId != null) {
            KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
            group.setKakaoUser(user);
        }

        return recommendGroupRepository.save(group);
    }


    public List<CourseResponseDTO> generateCoursesWithoutSaving(RecommendGroupRequestDTO request) {
        Map<String, List<CourseDayDTO>> gptCourseMap = aiRequestService.getRecommendedCourseStructure(
                request.getInpRegion(),
                request.getInpStartDate(),
                request.getInpEndDate(),
                request.getInpAdultCnt(),
                request.getInpChildCnt(),
                request.getInpBabyCnt(),
                request.getInpStyle().name()
        );

        return gptCourseMap.entrySet().stream().map(entry -> {
            CourseResponseDTO courseDTO = new CourseResponseDTO();
            courseDTO.setCourseId(null); // 비회원은 저장되지 않으므로 0L 또는 null
            courseDTO.setTitle(entry.getKey()); // 예: A, B, C
            courseDTO.setDays(entry.getValue()); // 이미 List<CourseDayDTO>


            return courseDTO;
        }).toList();
    }




    // 추천된 코스 조회
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByGroupId(Long groupId) {
        RecommendGroup group = recommendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹 ID입니다: " + groupId));

        List<Course> courses = courseRepository.findByRecommendGroup(group);

        return courses.stream().map(course -> {
            CourseResponseDTO courseDTO = new CourseResponseDTO();
            courseDTO.setCourseId(course.getCourseId());
            courseDTO.setTitle(course.getTitle());

            // 코스의 일차들을 가져옴
            List<CourseDay> courseDays = courseDayRepository.findByCourse(course);
            List<CourseDayDTO> dayDTOs = courseDays.stream().map(day -> {
                CourseDayDTO dayDTO = new CourseDayDTO();
                dayDTO.setDay(day.getDayNum());

                // 각 일차의 장소들 가져오기
                List<CoursePlace> places = coursePlaceRepository.findByCourseDay(day);
                List<CoursePlaceDTO> placeDTOs = places.stream().map(place -> {
                    CoursePlaceDTO placeDTO = new CoursePlaceDTO();
                    placeDTO.setPlaceName(place.getPlaceName());
                    placeDTO.setDescription(place.getPlaceDesc());
                    return placeDTO;
                }).toList();

                dayDTO.setPlaces(placeDTOs);
                return dayDTO;
            }).toList();

            courseDTO.setDays(dayDTOs);
            return courseDTO;
        }).toList();
    }




}
