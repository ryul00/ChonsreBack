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

    /**
     * 그룹 저장 후 DB에 코스도 저장하는 경우
     */
    @Transactional
    public void generateCoursesFromDatasetWithGpt(RecommendGroup group) {
        Map<String, List<CourseDayDTO>> courseMap =
                aiRequestService.getRecommendedCourseStructure(
                        "전라남도",
                        group.getInpStartDate(),
                        group.getInpEndDate(),
                        safeInt(group.getInpAdultCnt()),
                        safeInt(group.getInpChildCnt()),
                        safeInt(group.getInpBabyCnt()),
                        group.getInpStyle() != null ? group.getInpStyle().name() : "etc"
                );

        for (Map.Entry<String, List<CourseDayDTO>> entry : courseMap.entrySet()) {
            String label = entry.getKey(); // A, B, C
            List<CourseDayDTO> dayList = entry.getValue();

            // 첫 장소명으로 county 조회
            String firstPlaceName = null;
            if (!dayList.isEmpty() && dayList.get(0).getPlaces() != null && !dayList.get(0).getPlaces().isEmpty()) {
                firstPlaceName = dayList.get(0).getPlaces().get(0).getPlaceName();
            }
            String countyTitle = aiRequestService.findCountyByPlaceName(firstPlaceName);

            Course course = new Course();
            course.setTitle(countyTitle);                 // 시군을 title로
            course.setCourseLabel(label);
            course.setStyle(toCourseStyle(group.getInpStyle()));
            course.setRegion("전라남도");                  // region은 전남 고정
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

    public List<CourseResponseDTO> generateCoursesWithoutSaving(RecommendGroupRequestDTO request) {
        Map<String, List<CourseDayDTO>> courseMap =
                aiRequestService.getRecommendedCourseStructure(
                        "전라남도",
                        request.getInpStartDate(),
                        request.getInpEndDate(),
                        safeInt(request.getInpAdultCnt()),
                        safeInt(request.getInpChildCnt()),
                        safeInt(request.getInpBabyCnt()),
                        request.getInpStyle() != null ? request.getInpStyle().name() : "etc"
                );

        return courseMap.entrySet().stream().map(entry -> {
            List<CourseDayDTO> days = entry.getValue();

            // 각 place에 address 세팅
            for (CourseDayDTO day : days) {
                if (day.getPlaces() == null) continue;
                for (CoursePlaceDTO p : day.getPlaces()) {
                    String addr = aiRequestService.findAddressByPlaceName(p.getPlaceName());
                    p.setAddress(addr);
                }
            }

            // (선택) title을 시군으로 쓰고 싶다면 county 조회해서 세팅
            String firstPlaceName = (days != null && !days.isEmpty()
                    && days.get(0).getPlaces() != null && !days.get(0).getPlaces().isEmpty())
                    ? days.get(0).getPlaces().get(0).getPlaceName()
                    : null;
            String countyTitle = aiRequestService.findCountyByPlaceName(firstPlaceName); // 헬퍼가 있다면 사용

            CourseResponseDTO dto = new CourseResponseDTO();
            dto.setCourseId(null);
            dto.setTitle(countyTitle != null && !countyTitle.isBlank() ? countyTitle : "전라남도");
            dto.setCourseLabel(entry.getKey());
            dto.setDays(days);
            return dto;
        }).toList();
    }



    /**
     * RecommendGroup 생성 (DB 저장)
     */
    @Transactional
    public RecommendGroup saveRecommendGroup(RecommendGroupRequestDTO request, Long kakaoId) {
        RecommendGroup group = new RecommendGroup();
        group.setInpStartDate(request.getInpStartDate());
        group.setInpEndDate(request.getInpEndDate());
        group.setInpStyle(request.getInpStyle());
        group.setInpAdultCnt(request.getInpAdultCnt());
        group.setInpChildCnt(request.getInpChildCnt());
        group.setInpBabyCnt(request.getInpBabyCnt());
        group.setReqCreatedAt(java.time.LocalDateTime.now().toString());

        if (kakaoId != null) {
            KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                    .orElseThrow(() -> new RuntimeException("사용자 없음"));
            group.setKakaoUser(user);
        }
        return recommendGroupRepository.save(group);
    }

    /**
     * 그룹 ID로 DB에 저장된 추천 코스 조회
     */
    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByGroupId(Long groupId) {
        RecommendGroup group = recommendGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹 ID입니다: " + groupId));

        List<Course> courses = courseRepository.findByRecommendGroup(group);

        return courses.stream().map(course -> {
            CourseResponseDTO dto = new CourseResponseDTO();
            dto.setCourseId(course.getCourseId());
            dto.setTitle(course.getTitle());
            dto.setCourseLabel(course.getCourseLabel());

            List<CourseDay> courseDays = courseDayRepository.findByCourse(course);
            List<CourseDayDTO> dayDTOs = courseDays.stream().map(day -> {
                CourseDayDTO dayDTO = new CourseDayDTO();
                dayDTO.setDay(day.getDayNum());

                List<CoursePlace> places = coursePlaceRepository.findByCourseDay(day);
                List<CoursePlaceDTO> placeDTOs = places.stream().map(place -> {
                    CoursePlaceDTO placeDTO = new CoursePlaceDTO();
                    placeDTO.setPlaceName(place.getPlaceName());
                    placeDTO.setDescription(place.getPlaceDesc());

                    // DB 값 우선, 없으면 엑셀에서 조회
                    String addr = (place.getAddress() != null && !place.getAddress().isBlank())
                            ? place.getAddress()
                            : aiRequestService.findAddressByPlaceName(place.getPlaceName());
                    placeDTO.setAddress(addr);
                    return placeDTO;
                }).toList();


                dayDTO.setPlaces(placeDTOs);
                return dayDTO;
            }).toList();

            dto.setDays(dayDTOs);
            return dto;
        }).toList();
    }


    // 유틸
    private int safeInt(Integer v) { return v == null ? 0 : v; }

    private Course.CourseStyle toCourseStyle(RecommendGroup.InpCourseStyle inp) {
        if (inp == null) return Course.CourseStyle.etc;
        return switch (inp) {
            case farm -> Course.CourseStyle.farm;
            case fishing -> Course.CourseStyle.fishing;
            case etc -> Course.CourseStyle.etc;
        };
    }
}
