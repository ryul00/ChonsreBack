package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.*;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CourseDay;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CoursePlace;
import TourCompetition.ChonsreBack.Domain.Func.Repository.CourseDayRepository;
import TourCompetition.ChonsreBack.Domain.Func.Repository.CoursePlaceRepository;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final RecommendGroupRepository recommendGroupRepository;
    private final CourseRepository courseRepository;
    private final KakaoUserRepository kakaoUserRepository;
    private final AiRequestService aiRequestService;
    private final CourseDayRepository courseDayRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final TourApiService tourApiService;


    /**
     * 그룹 저장 후 DB에 코스도 저장하는 경우
     * Day1: 엑셀 대표 1곳
     * Day2~N: TourAPI 같은 시군에서 3곳씩
     */

    /**
     * 그룹 저장 후 DB에 코스도 저장하는 경우
     */
    @Transactional
    public void generateCoursesFromDatasetWithGpt(RecommendGroup group) {
        Map<String, List<CourseDayDTO>> courseMap = aiRequestService.getRecommendedCourseStructure(
                "전라남도",
                group.getInpStartDate(),
                group.getInpEndDate(),
                safeInt(group.getInpAdultCnt()),
                safeInt(group.getInpChildCnt()),
                safeInt(group.getInpBabyCnt()),
                group.getInpStyle() != null ? group.getInpStyle().name() : "etc"
        );

        int totalDays = computeTripDays(group.getInpStartDate(), group.getInpEndDate());

        Integer jeonnam = tourApiService.findJeonnamAreaCode();



        for (Map.Entry<String, List<CourseDayDTO>> entry : courseMap.entrySet()) {
            String label = entry.getKey();
            List<CourseDayDTO> dayList = entry.getValue(); // GPT/엑셀 Day1 포함

            // Day1 엑셀 대표 장소 주소 보강
            CoursePlaceDTO excelPlace = null;
            if (!dayList.isEmpty() && dayList.get(0).getPlaces()!=null && !dayList.get(0).getPlaces().isEmpty()) {
                excelPlace = dayList.get(0).getPlaces().get(0);
                String addr = aiRequestService.findAddressByPlaceName(excelPlace.getPlaceName());
                excelPlace.setAddress(addr);
            }

            // county 타이틀
            String countyTitle = aiRequestService.findCountyByPlaceName(excelPlace != null ? excelPlace.getPlaceName() : null);
            if (countyTitle == null || countyTitle.isBlank()) countyTitle = "전라남도";

            // --- TourAPI 호출: Day1 추가 1개 + Day2~N 3개씩 ---
            if (jeonnam != null) {
                Integer sigungu = tourApiService.findSigunguCodeByCounty(jeonnam, countyTitle);

                // 총 필요 개수: Day1 보강 1개 + Day2~N 3개씩
                int extraForDay1 = 1;
                int needCount = (totalDays > 1 ? 3 * (totalDays - 1) : 0) + extraForDay1;

                // 중복(엑셀 대표 장소명) 제거를 위해 set 준비
                Set<String> usedNames = new HashSet<>();
                if (excelPlace != null) usedNames.add(excelPlace.getPlaceName());

                // 관광지 목록 가져오기 (여유분 포함)
                List<TourApiService.TourSpot> fetched = tourApiService.getTopAttractions(jeonnam, sigungu, Math.max(needCount * 2, 6));
                // 이름 기준 중복 제거 + needCount로 컷
                List<TourApiService.TourSpot> dedup = fetched.stream()
                        .filter(s -> usedNames.add(s.name()))
                        .limit(needCount)
                        .toList();

                // 1) Day1에 1개 추가 (+ description)
                if (!dayList.isEmpty() && !dedup.isEmpty()) {
                    CourseDayDTO day1 = dayList.get(0);
                    if (day1.getPlaces() == null) day1.setPlaces(new ArrayList<>());

                    TourApiService.TourSpot addToDay1 = dedup.get(0);
                    CoursePlaceDTO add = new CoursePlaceDTO();
                    add.setPlaceName(addToDay1.name());
                    add.setAddress(addToDay1.address());
                    // 상세설명
                    String desc = tourApiService.getPlaceDescription(addToDay1.contentId());
                    add.setDescription((desc != null && !desc.isBlank()) ? desc : "관광지");
                    add.setImgUrl(tourApiService.getFirstPlaceImageUrl(addToDay1.contentId()));
                    day1.getPlaces().add(add);
                }

                // 2) 남은 것들로 Day2~N 채우기(3개씩)
                if (totalDays > 1) {
                    List<TourApiService.TourSpot> rest = dedup.size() > 1 ? dedup.subList(1, dedup.size()) : Collections.emptyList();
                    List<List<TourApiService.TourSpot>> chunks = chunkBy3(rest, totalDays - 1);

                    int dNum = 2;
                    for (List<TourApiService.TourSpot> chunk : chunks) {
                        CourseDayDTO d = new CourseDayDTO();
                        d.setDay(dNum++);
                        List<CoursePlaceDTO> ps = new ArrayList<>();
                        for (TourApiService.TourSpot s : chunk) {
                            CoursePlaceDTO p = new CoursePlaceDTO();
                            p.setPlaceName(s.name());
                            p.setAddress(s.address());
                            String desc = tourApiService.getPlaceDescription(s.contentId());
                            p.setDescription((desc != null && !desc.isBlank()) ? desc : "관광지");
                            p.setImgUrl(tourApiService.getFirstPlaceImageUrl(s.contentId()));
                            ps.add(p);
                        }
                        d.setPlaces(ps);
                        dayList.add(d);
                        if (dNum > totalDays) break;
                    }
                }
            }

            // countyTitle / label / ReqCreatedAt 기반 씨드
            long accSeed = Integer.toUnsignedLong(Objects.hash(countyTitle, label, group.getReqCreatedAt(), "ACC"));
            AccommodationDTO acc = null;
            if (jeonnam != null) {
                Integer sigungu = tourApiService.findSigunguCodeByCounty(jeonnam, countyTitle);
                acc = tourApiService.getOneAccommodationRandomized(jeonnam, sigungu, accSeed, 20);
            }


            // === 저장 ===
            Course course = new Course();
            course.setTitle(countyTitle);
            course.setCourseLabel(label);
            course.setStyle(toCourseStyle(group.getInpStyle()));
            course.setRegion("전라남도");
            course.setCreatedAt(LocalDateTime.now().toString());
            course.setRecommendGroup(group);
            course.setTemplate(false);


            // 숙소정보 저장
            if (acc != null) {
                course.setAccommodationName(acc.getName());
                course.setAccommodationAddress(acc.getAddress());
                course.setAccommodationDescription(acc.getDescription());
            }

            courseRepository.save(course);



            for (CourseDayDTO dayDto : dayList) {
                CourseDay dayEnt = new CourseDay();
                dayEnt.setDayNum(dayDto.getDay());
                dayEnt.setCourse(course);
                courseDayRepository.save(dayEnt);

                int order = 1;
                if (dayDto.getPlaces() != null) {
                    for (CoursePlaceDTO placeDto : dayDto.getPlaces()) {
                        CoursePlace place = new CoursePlace();
                        place.setOrderNum(order++);
                        place.setPlaceName(placeDto.getPlaceName());
                        place.setPlaceDesc(placeDto.getDescription());
                        // 엔티티에 address 컬럼 존재 시
                        place.setAddress(placeDto.getAddress());
                        place.setImgUrl(placeDto.getImgUrl());
                        place.setCourseDay(dayEnt);
                        coursePlaceRepository.save(place);
                    }
                }
            }
        }
    }


    /**
     * 비회원 등 → DB 저장 안 하고 코스만 미리보기
     * Day1: 엑셀 대표 1곳
     * Day2~N: TourAPI 같은 시군 3곳씩
     */
    public List<CourseResponseDTO> generateCoursesWithoutSaving(RecommendGroupRequestDTO request) {
        Map<String, List<CourseDayDTO>> courseMap = aiRequestService.getRecommendedCourseStructure(
                "전라남도",
                request.getInpStartDate(),
                request.getInpEndDate(),
                safeInt(request.getInpAdultCnt()),
                safeInt(request.getInpChildCnt()),
                safeInt(request.getInpBabyCnt()),
                request.getInpStyle() != null ? request.getInpStyle().name() : "etc"
        );

        int totalDays = computeTripDays(request.getInpStartDate(), request.getInpEndDate());
        Integer jeonnam = tourApiService.findJeonnamAreaCode();


        return courseMap.entrySet().stream().map(entry -> {
            String label = entry.getKey();
            List<CourseDayDTO> originalDays = entry.getValue();

            // Day1: 엑셀 대표 1곳 + 주소 보강
            CourseDayDTO day1 = new CourseDayDTO();
            day1.setDay(1);
            List<CoursePlaceDTO> day1Places = new ArrayList<>();
            CoursePlaceDTO excelPlace = null;
            if (!originalDays.isEmpty()
                    && originalDays.get(0).getPlaces() != null
                    && !originalDays.get(0).getPlaces().isEmpty()) {
                excelPlace = originalDays.get(0).getPlaces().get(0);
                excelPlace.setAddress(aiRequestService.findAddressByPlaceName(excelPlace.getPlaceName()));
                day1Places.add(excelPlace);
            }
            day1.setPlaces(day1Places);

            String county = aiRequestService.findCountyByPlaceName(excelPlace != null ? excelPlace.getPlaceName() : null);
            if (county == null || county.isBlank()) county = "전라남도";

            List<CourseDayDTO> finalDays = new ArrayList<>();
            finalDays.add(day1);

            // TourAPI: Day1 보강 1 + Day2~N 3개/일
            if (jeonnam != null) {
                Integer sigungu = tourApiService.findSigunguCodeByCounty(jeonnam, county);

                int extraForDay1 = 1;
                int needCount = (totalDays > 1 ? 3 * (totalDays - 1) : 0) + extraForDay1;

                Set<String> usedNames = new HashSet<>();
                if (excelPlace != null) usedNames.add(excelPlace.getPlaceName());

                List<TourApiService.TourSpot> fetched = tourApiService.getTopAttractions(jeonnam, sigungu, Math.max(needCount * 2, 6));
                List<TourApiService.TourSpot> dedup = fetched.stream()
                        .filter(s -> usedNames.add(s.name()))
                        .limit(needCount)
                        .toList();

                // Day1에 1개 추가(+overview)
                if (!dedup.isEmpty()) {
                    TourApiService.TourSpot addToDay1 = dedup.get(0);
                    CoursePlaceDTO p = new CoursePlaceDTO();
                    p.setPlaceName(addToDay1.name());
                    p.setAddress(addToDay1.address());
                    String desc = tourApiService.getPlaceDescription(addToDay1.contentId());
                    p.setDescription((desc != null && !desc.isBlank()) ? desc : "관광지");
                    p.setImgUrl(tourApiService.getFirstPlaceImageUrl(addToDay1.contentId()));
                    day1.getPlaces().add(p);
                }

                // Day2~N
                if (totalDays > 1) {
                    List<TourApiService.TourSpot> rest = dedup.size() > 1 ? dedup.subList(1, dedup.size()) : Collections.emptyList();
                    List<List<TourApiService.TourSpot>> chunks = chunkBy3(rest, totalDays - 1);
                    int dNum = 2;
                    for (List<TourApiService.TourSpot> chunk : chunks) {
                        CourseDayDTO d = new CourseDayDTO();
                        d.setDay(dNum++);
                        List<CoursePlaceDTO> ps = new ArrayList<>();
                        for (TourApiService.TourSpot s : chunk) {
                            CoursePlaceDTO cp = new CoursePlaceDTO();
                            cp.setPlaceName(s.name());
                            cp.setAddress(s.address());
                            String desc = tourApiService.getPlaceDescription(s.contentId());
                            cp.setDescription((desc != null && !desc.isBlank()) ? desc : "관광지");
                            cp.setImgUrl(tourApiService.getFirstPlaceImageUrl(s.contentId()));
                            ps.add(cp);
                        }
                        d.setPlaces(ps);
                        finalDays.add(d);
                        if (dNum > totalDays) break;
                    }
                }
            }

            // 숙소 불러오기
            long accSeed = Integer.toUnsignedLong(Objects.hash(
                    county, label, request.getInpStartDate(), request.getInpEndDate(), LocalDate.now().toString(), "ACC"
            ));
            AccommodationDTO acc = null;
            if (jeonnam != null) {
                Integer sigungu = tourApiService.findSigunguCodeByCounty(jeonnam, county);
                acc = tourApiService.getOneAccommodationRandomized(jeonnam, sigungu, accSeed, 5);
            }

            CourseResponseDTO dto = new CourseResponseDTO();
            dto.setCourseId(null);
            dto.setTitle(county);
            dto.setCourseLabel(label);
            dto.setDays(finalDays);
            dto.setAccommodation(acc);
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
                    placeDTO.setImgUrl(place.getImgUrl());
                    return placeDTO;
                }).toList();

                dayDTO.setPlaces(placeDTOs);
                return dayDTO;
            }).toList();

            dto.setDays(dayDTOs);

            //숙소 조회 (엔티티 → DTO)
            AccommodationDTO accDto = null;
            if (course.getAccommodationName() != null ||
                    course.getAccommodationAddress() != null ||
                    course.getAccommodationDescription() != null) {
                accDto = new AccommodationDTO();
                accDto.setName(course.getAccommodationName());
                accDto.setAddress(course.getAccommodationAddress());
                accDto.setDescription(
                        course.getAccommodationDescription() != null ? course.getAccommodationDescription() : ""
                );
            }
            dto.setAccommodation(accDto);

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

    // 여행일수 계산 (종료-시작+1), 실패 시 1일
    private int computeTripDays(String start, String end) {
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            long days = ChronoUnit.DAYS.between(s, e) + 1;
            return (int) Math.max(1, days);
        } catch (Exception ignore) {
            return 1;
        }
    }

    // TourAPI 결과를 day별 3개씩 슬라이스 (요청 일수만큼)
    private List<List<TourApiService.TourSpot>> chunkBy3(List<TourApiService.TourSpot> list, int dayCount) {
        List<List<TourApiService.TourSpot>> chunks = new ArrayList<>();
        int need = dayCount * 3;
        List<TourApiService.TourSpot> safe = (list.size() > need) ? list.subList(0, need) : list;
        for (int i = 0; i < safe.size(); i += 3) {
            chunks.add(new ArrayList<>(safe.subList(i, Math.min(i + 3, safe.size()))));
        }
        // 부족할 때는 빈 day도 가능(로직 유지), 필요하면 보강 로직 추가
        return chunks;
    }


}
