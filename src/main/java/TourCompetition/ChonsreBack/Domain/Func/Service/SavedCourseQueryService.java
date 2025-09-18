// [UPDATED FILE] SavedCourseQueryService.java
package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.*;
import TourCompetition.ChonsreBack.Domain.Func.DTO.SavedCourseListItemDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SavedCourseQueryService {

    private final SavedCourseRepository savedCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseDayRepository courseDayRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final KakaoUserRepository kakaoUserRepository;
//    private final SavedCourseReviewRepository savedCourseReviewRepository;
    private final AiRequestService aiRequestService; // 주소 보강에 사용
    private final ReviewRepository reviewRepository;

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDate parseLD(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.isBlank()) return null;
        try { return LocalDate.parse(yyyyMmDd.trim(), DF); }
        catch (Exception ignore) { return null; }
    }

    private SavedCourseListItemDTO toListItem(SavedCourse saved, KakaoUser user, LocalDate today) {
        Course c = saved.getCourse();

        SavedCourseListItemDTO dto = new SavedCourseListItemDTO();
        dto.setSavedId(saved.getSavedId());
        dto.setSvdStartDate(saved.getSvdStartDate());
        dto.setSvdEndDate(saved.getSvdEndDate());

        if (c != null) {
            dto.setCourseId(c.getCourseId());
            dto.setCourseLabel(c.getCourseLabel());
            dto.setTitle(c.getTitle());
            dto.setAccommodationName(c.getAccommodationName()); // 숙소 이름은 메타정보라 남겨도 됨

            // [CHANGED] 숙소 이미지는 배제, 관광지 이미지만 대표로
            String courseImg = null;
            List<CourseDay> days = courseDayRepository.findByCourseOrderByDayNumAsc(c);
            for (CourseDay day : days) {
                List<CoursePlace> places = coursePlaceRepository.findByCourseDayOrderByOrderNumAsc(day);
                for (CoursePlace p : places) {
                    if (p.getImgUrl() != null && !p.getImgUrl().isBlank()) {
                        courseImg = p.getImgUrl();
                        break; // 첫 번째 유효한 관광지 이미지 찾으면 종료
                    }
                }
                if (courseImg != null) break;
            }
            dto.setCourseImgUrl(courseImg); // 관광지 이미지 없으면 null
        }

        LocalDate end = parseLD(saved.getSvdEndDate());
        boolean isPast = (end != null) && end.isBefore(today);
        dto.setPast(isPast);
        dto.setCanReview(isPast);
        dto.setCanDelete(true);

        boolean hasReview = reviewRepository.existsBySavedCourse(saved);
        dto.setHasReview(hasReview);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<SavedCourseListItemDTO> listUpcoming(Long kakaoId, String todayOverride) {
        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        LocalDate today = (todayOverride != null && !todayOverride.isBlank())
                ? parseLD(todayOverride)
                : LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (today == null) today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<SavedCourse> rows = savedCourseRepository.findByKakaoUserOrderBySavedIdDesc(user);
        List<SavedCourseListItemDTO> out = new ArrayList<>();
        for (SavedCourse s : rows) {
            LocalDate end = parseLD(s.getSvdEndDate());
            if (end == null || end.isBefore(today)) continue;
            out.add(toListItem(s, user, today));
        }
        out.sort(Comparator.comparing(i -> {
            LocalDate d = parseLD(i.getSvdStartDate());
            return d != null ? d : LocalDate.MAX;
        }));
        return out;
    }

    @Transactional(readOnly = true)
    public List<SavedCourseListItemDTO> listPast(Long kakaoId, String todayOverride) {
        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        LocalDate today = (todayOverride != null && !todayOverride.isBlank())
                ? parseLD(todayOverride)
                : LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (today == null) today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<SavedCourse> rows = savedCourseRepository.findByKakaoUserOrderBySavedIdDesc(user);
        List<SavedCourseListItemDTO> out = new ArrayList<>();
        for (SavedCourse s : rows) {
            LocalDate end = parseLD(s.getSvdEndDate());
            if (end != null && end.isBefore(today)) out.add(toListItem(s, user, today));
        }
        out.sort(Comparator.comparing((SavedCourseListItemDTO i) -> {
            LocalDate d = parseLD(i.getSvdEndDate());
            return d != null ? d : LocalDate.MIN;
        }).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getSavedDetail(Long savedId, Long kakaoId) {
        SavedCourse saved = savedCourseRepository.findById(savedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 코스를 찾을 수 없습니다."));

        KakaoUser owner = saved.getKakaoUser();
        if (owner == null || !owner.getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
        Course course = saved.getCourse();
        if (course == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "코스 데이터가 손상되었습니다.");

        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setCourseId(course.getCourseId());
        dto.setCourseLabel(course.getCourseLabel());
        dto.setTitle(course.getTitle());

        AccommodationDTO acc = new AccommodationDTO();
        acc.setName(course.getAccommodationName());
        acc.setAddress(course.getAccommodationAddress());
        acc.setDescription(course.getAccommodationDescription());
        acc.setImgUrl(course.getAccommodationImgUrl());
        dto.setAccommodation(acc);

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

    @Transactional
    public void deleteSaved(Long savedId, Long kakaoId) {
        SavedCourse saved = savedCourseRepository.findById(savedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 코스를 찾을 수 없습니다."));
        if (saved.getKakaoUser() == null || !saved.getKakaoUser().getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // [CHANGED] 과거 코스 삭제 제한 로직 제거
        // (이전: endDate < today 이면 CONFLICT 예외)

        savedCourseRepository.delete(saved);
    }
}
