package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.*;
import TourCompetition.ChonsreBack.Domain.Func.DTO.SavedCourseItemDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.TemplateSaveResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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

    private final AiRequestService aiRequestService;

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
        List<CourseDay> oldDays = courseDayRepository.findByCourseOrderByDayNumAsc(course);
        for (CourseDay d : oldDays) {
            List<CoursePlace> places = coursePlaceRepository.findByCourseDayOrderByOrderNumAsc(d);
            if (!places.isEmpty()) coursePlaceRepository.deleteAll(places);
        }
        if (!oldDays.isEmpty()) courseDayRepository.deleteAll(oldDays);

        // 2) 타이틀 수정
        course.setTitle(req.getTitle());

        // [ADDED] 요청에 숙소가 오면 코스의 숙소 정보 갱신, 없으면 비움
        if (req.getAccommodation() != null) {
            var acc = req.getAccommodation();
            course.setAccommodationName(nz(acc.getName()));
            course.setAccommodationAddress(nz(acc.getAddress()));
            course.setAccommodationDescription(nz(acc.getDescription()));
            course.setAccommodationImgUrl(nz(acc.getImgUrl()));
        } else {
            course.setAccommodationName(null);
            course.setAccommodationAddress(null);
            course.setAccommodationDescription(null);
            course.setAccommodationImgUrl(null);
        }

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

                // [ADDED] 주소 보강: 요청에 주소가 없으면 엑셀 기반 주소 탐색 시도
                String addr = p.getAddress();
                if (addr == null || addr.isBlank()) {
                    try {
                        if (p.getPlaceName() != null && !p.getPlaceName().isBlank()) {
                            addr = aiRequestService.findAddressByPlaceName(p.getPlaceName());
                        }
                    } catch (Exception ignore) { /* 주소 보강 실패해도 진행 */ }
                }
                place.setAddress(addr);

                // [ADDED] 대표 이미지 저장(요청에서 온 값 우선)
                place.setImgUrl(p.getImgUrl());
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

    @Transactional(readOnly = true)
    public CourseResponseDTO getSavedCourseDetail(Long savedId, Long kakaoId) {
        // 1) SavedCourse 조회 + 권한 체크
        SavedCourse saved = savedCourseRepository.findById(savedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 코스를 찾을 수 없습니다."));

        KakaoUser owner = saved.getKakaoUser();
        if (owner == null || !owner.getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        Course course = saved.getCourse();
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "코스 데이터가 손상되었습니다.");
        }

        // 2) 코스 메타 + 숙소 매핑
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setCourseId(course.getCourseId());
        dto.setCourseLabel(course.getCourseLabel());
        dto.setTitle(course.getTitle());

        // 숙소 (코스 엔티티에 저장된 값 그대로)
        AccommodationDTO acc = new AccommodationDTO();
        acc.setName(course.getAccommodationName());
        acc.setAddress(course.getAccommodationAddress());
        acc.setDescription(course.getAccommodationDescription());
        acc.setImgUrl(course.getAccommodationImgUrl());
        dto.setAccommodation(acc);

        // 3) 일차/장소 구성
        List<CourseDay> days = courseDayRepository.findByCourseOrderByDayNumAsc(course);
        List<CourseDayDTO> dayDTOs = new ArrayList<>();

        for (CourseDay day : days) {
            CourseDayDTO dDto = new CourseDayDTO();
            dDto.setDay(day.getDayNum());

            List<CoursePlace> places = coursePlaceRepository.findByCourseDayOrderByOrderNumAsc(day);
            List<CoursePlaceDTO> pDtos = new ArrayList<>();

            for (CoursePlace p : places) {
                CoursePlaceDTO cp = new CoursePlaceDTO();
                cp.setPlaceName(p.getPlaceName());
                cp.setDescription(p.getPlaceDesc());

                // 주소: 엔티티 우선, 없으면 엑셀 기반 보강(선택)
                String addr = p.getAddress();
                if ((addr == null || addr.isBlank()) && p.getPlaceName() != null) {
                    try { addr = aiRequestService.findAddressByPlaceName(p.getPlaceName()); }
                    catch (Exception ignore) {}
                }
                cp.setAddress(addr);

                // 이미지: 엔티티에 저장된 값 그대로
                cp.setImgUrl(p.getImgUrl());

                pDtos.add(cp);
            }
            dDto.setPlaces(pDtos);
            dayDTOs.add(dDto);
        }
        dto.setDays(dayDTOs);

        return dto;
    }
    // [ADDED] 공통 빌더: Course -> CourseResponseDTO
    private CourseResponseDTO toCourseResponseDTO(Course course) {
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setCourseId(course.getCourseId());
        dto.setCourseLabel(course.getCourseLabel());
        dto.setTitle(course.getTitle());

        // 숙소
        AccommodationDTO acc = new AccommodationDTO();
        acc.setName(course.getAccommodationName());
        acc.setAddress(course.getAccommodationAddress());
        acc.setDescription(course.getAccommodationDescription());
        acc.setImgUrl(course.getAccommodationImgUrl());
        dto.setAccommodation(acc);

        // days/places
        List<CourseDay> days = courseDayRepository.findByCourseOrderByDayNumAsc(course);
        List<CourseDayDTO> dayDTOs = new ArrayList<>();
        for (CourseDay day : days) {
            CourseDayDTO dDto = new CourseDayDTO();
            dDto.setDay(day.getDayNum());

            List<CoursePlace> places = coursePlaceRepository.findByCourseDayOrderByOrderNumAsc(day);
            List<CoursePlaceDTO> pDtos = new ArrayList<>();
            for (CoursePlace p : places) {
                CoursePlaceDTO cp = new CoursePlaceDTO();
                cp.setPlaceName(p.getPlaceName());
                cp.setDescription(p.getPlaceDesc());

                String addr = p.getAddress();
                if ((addr == null || addr.isBlank()) && p.getPlaceName() != null) {
                    try { addr = aiRequestService.findAddressByPlaceName(p.getPlaceName()); }
                    catch (Exception ignore) {}
                }
                cp.setAddress(addr);
                cp.setImgUrl(p.getImgUrl());
                pDtos.add(cp);
            }
            dDto.setPlaces(pDtos);
            dayDTOs.add(dDto);
        }
        dto.setDays(dayDTOs);
        return dto;
    }

    // [ADDED] 사용자 저장 코스 전체 조회
    @Transactional(readOnly = true)
    public List<SavedCourseItemDTO> listSavedCourses(Long kakaoId) {
        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        List<SavedCourse> savedList = savedCourseRepository.findByKakaoUserOrderBySavedIdDesc(user);

        List<SavedCourseItemDTO> out = new ArrayList<>();
        for (SavedCourse saved : savedList) {
            Course course = saved.getCourse();
            if (course == null) continue; // 방어

            SavedCourseItemDTO item = new SavedCourseItemDTO();
            item.setSavedId(saved.getSavedId());
            item.setSvdStartDate(saved.getSvdStartDate());
            item.setSvdEndDate(saved.getSvdEndDate());
            item.setCourse(toCourseResponseDTO(course));
            out.add(item);
        }
        return out;
    }

    @Transactional
    public void deleteSavedCourse(Long savedId, Long kakaoId) {
        SavedCourse saved = savedCourseRepository.findById(savedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 코스를 찾을 수 없습니다."));

        KakaoUser owner = saved.getKakaoUser();
        if (owner == null || !owner.getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        savedCourseRepository.delete(saved);
    }


    private String nz(String s) { return s == null ? "" : s; }
}
