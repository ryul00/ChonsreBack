package TourCompetition.ChonsreBack.Domain.Func.Service;

import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.*;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TemplateCourseService {

    private final RecommendGroupRepository recommendGroupRepository;
    private final KakaoUserRepository kakaoUserRepository;
    private final CourseRepository courseRepository;
    private final CourseDayRepository courseDayRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final SavedCourseRepository savedCourseRepository;

    // [ADDED] 주소 보강(빈 주소일 때 엑셀/사전 데이터로 보강하려는 경우)
    private final AiRequestService aiRequestService;

    @Transactional
    public TemplateSaveResponseDTO saveTemplate(TemplateSaveRequestDTO req, Long kakaoId) {

        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        TemplateGroupInputDTO gi = req.getGroupInput();
        TemplateCourseInputDTO ci = req.getCourse();

        // 1) 그룹 생성
        RecommendGroup group = new RecommendGroup();
        group.setInpStartDate(gi.getInpStartDate());
        group.setInpEndDate(gi.getInpEndDate());

        // [REMOVED] RecommendGroup에 inpRegion 필드가 없다면 주석/삭제하세요
        // group.setInpRegion(gi.getInpRegion());

        group.setInpAdultCnt(gi.getInpAdultCnt() != null ? gi.getInpAdultCnt() : 0);
        group.setInpChildCnt(gi.getInpChildCnt() != null ? gi.getInpChildCnt() : 0);
        group.setInpBabyCnt(gi.getInpBabyCnt() != null ? gi.getInpBabyCnt() : 0);

        // [CHANGED] 스타일 매핑: AI 생성 로직과 동일(Enum: farm/fishing/etc)
        if (gi.getInpStyle() != null && !gi.getInpStyle().isBlank()) {
            try { group.setInpStyle(RecommendGroup.InpCourseStyle.valueOf(gi.getInpStyle().toLowerCase())); }
            catch (IllegalArgumentException ignore) { group.setInpStyle(null); }
        }
        group.setReqCreatedAt(LocalDateTime.now().toString());
        group.setKakaoUser(user);
        recommendGroupRepository.save(group);

        // 2) 코스 생성
        Course course = new Course();
        course.setTitle(ci.getTitle());

        // [CHANGED] region은 Group이 아니라 Course에만 세팅 (엔티티에 맞춤)
        String region = (gi.getInpRegion() != null && !gi.getInpRegion().isBlank()) ? gi.getInpRegion() : "전라남도";
        course.setRegion(region);

        course.setCreatedAt(LocalDateTime.now().toString());
        course.setRecommendGroup(group);
        course.setTemplate(true);
        course.setStyle(mapToCourseStyle(gi.getInpStyle())); // farm/fishing/etc
        courseRepository.save(course);

        // [ADDED] 숙소 1개 세팅 (프론트에서 accommodation 내려줄 경우)
        // TemplateCourseInputDTO에 getAccommodation() {name,address,description,imgUrl} 가 있다고 가정
        if (ci.getAccommodation() != null) {
            var acc = ci.getAccommodation();
            course.setAccommodationName(nz(acc.getName()));
            course.setAccommodationAddress(nz(acc.getAddress()));
            course.setAccommodationDescription(nz(acc.getDescription()));
            course.setAccommodationImgUrl(nz(acc.getImgUrl()));
        }
        courseRepository.save(course);

        // 3) 일차/장소
        for (TemplateCourseDayInputDTO dayInput : ci.getDays()) {
            CourseDay day = new CourseDay();
            day.setDayNum(dayInput.getDay());
            day.setCourse(course);
            courseDayRepository.save(day);

            int order = 1;
            if (dayInput.getPlaces() != null) {
                for (TemplateCoursePlaceInputDTO p : dayInput.getPlaces()) {
                    CoursePlace place = new CoursePlace();
                    place.setOrderNum(order++);
                    place.setPlaceName(nz(p.getPlaceName()));
                    place.setPlaceDesc(nz(p.getDescription()));

                    // [ADDED] 주소 저장(없으면 보강 시도)
                    String addr = nz(p.getAddress()); // TemplateCoursePlaceInputDTO에 address 필드 추가 가정
                    if (addr.isBlank() && p.getPlaceName() != null && !p.getPlaceName().isBlank()) {
                        try { addr = nz(aiRequestService.findAddressByPlaceName(p.getPlaceName())); }
                        catch (Exception ignore) { /* 실패해도 진행 */ }
                    }
                    place.setAddress(addr);

                    // [ADDED] 대표 이미지 저장
                    place.setImgUrl(nz(p.getImgUrl())); // TemplateCoursePlaceInputDTO에 imgUrl 필드 추가 가정

                    place.setCourseDay(day);
                    coursePlaceRepository.save(place);
                }
            }
        }

        // 4) 코스 저장(내 저장 목록에 연결)
        SavedCourse saved = new SavedCourse();
        saved.setKakaoUser(user);
        saved.setCourse(course);
        saved.setSvdStartDate(gi.getInpStartDate());
        saved.setSvdEndDate(gi.getInpEndDate());
        savedCourseRepository.save(saved);

        TemplateSaveResponseDTO res = new TemplateSaveResponseDTO();
        res.setGroupId(group.getGroupId());
        res.setCourseId(course.getCourseId());
        res.setSavedId(saved.getSavedId());
        return res;
    }

    // [ADDED] AI 생성 코스와 동일한 스타일 매핑
    private Course.CourseStyle mapToCourseStyle(String styleStr) {
        if (styleStr == null || styleStr.isBlank()) return Course.CourseStyle.etc;
        String s = styleStr.trim().toLowerCase();
        return switch (s) {
            case "farm" -> Course.CourseStyle.farm;
            case "fishing" -> Course.CourseStyle.fishing;
            default -> Course.CourseStyle.etc;
        };
    }

    private String nz(String s) { return s == null ? "" : s; }
}
