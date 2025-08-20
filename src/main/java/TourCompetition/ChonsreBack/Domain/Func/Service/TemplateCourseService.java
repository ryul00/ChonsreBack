//package TourCompetition.ChonsreBack.Domain.Func.Service;
//
//import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.*;
//import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
//import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
//import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
//import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//public class TemplateCourseService {
//
//    private final RecommendGroupRepository recommendGroupRepository;
//    private final KakaoUserRepository kakaoUserRepository;
//    private final CourseRepository courseRepository;
//    private final CourseDayRepository courseDayRepository;
//    private final CoursePlaceRepository coursePlaceRepository;
//    private final SavedCourseRepository savedCourseRepository;
//
//    @Transactional
//    public TemplateSaveResponseDTO saveTemplate(TemplateSaveRequestDTO req, Long kakaoId) {
//
//        KakaoUser user = kakaoUserRepository.findByKakaoId(kakaoId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));
//
//        TemplateGroupInputDTO gi = req.getGroupInput();
//        TemplateCourseInputDTO ci = req.getCourse();
//
//        // 1) 그룹 생성
//        RecommendGroup group = new RecommendGroup();
//        group.setInpStartDate(gi.getInpStartDate());
//        group.setInpEndDate(gi.getInpEndDate());
//        group.setInpRegion(gi.getInpRegion());
//        group.setInpAdultCnt(gi.getInpAdultCnt() != null ? gi.getInpAdultCnt() : 0);
//        group.setInpChildCnt(gi.getInpChildCnt() != null ? gi.getInpChildCnt() : 0);
//        group.setInpBabyCnt(gi.getInpBabyCnt() != null ? gi.getInpBabyCnt() : 0);
//        if (gi.getInpStyle() != null && !gi.getInpStyle().isBlank()) {
//            try { group.setInpStyle(RecommendGroup.InpCourseStyle.valueOf(gi.getInpStyle().toUpperCase())); }
//            catch (IllegalArgumentException ignore) { group.setInpStyle(null); }
//        }
//        group.setReqCreatedAt(LocalDateTime.now().toString());
//        group.setKakaoUser(user);
//        recommendGroupRepository.save(group);
//
//        // 2) 코스 생성
//        Course course = new Course();
//        course.setTitle(ci.getTitle());
//        course.setRegion(group.getInpRegion());
//        course.setCreatedAt(LocalDateTime.now().toString());
//        course.setRecommendGroup(group);
//        course.setTemplate(true);
//        course.setStyle(mapToCourseStyle(gi.getInpStyle()));
//        courseRepository.save(course);
//
//        // 3) 일차/장소
//        for (TemplateCourseDayInputDTO dayInput : ci.getDays()) {
//            CourseDay day = new CourseDay();
//            day.setDayNum(dayInput.getDay());
//            day.setCourse(course);
//            courseDayRepository.save(day);
//
//            int order = 1;
//            if (dayInput.getPlaces() != null) {
//                for (TemplateCoursePlaceInputDTO p : dayInput.getPlaces()) {
//                    CoursePlace place = new CoursePlace();
//                    place.setOrderNum(order++);
//                    place.setPlaceName(p.getPlaceName());
//                    place.setPlaceDesc(p.getDescription());
//                    place.setCourseDay(day);
//                    coursePlaceRepository.save(place);
//                }
//            }
//        }
//
//        // 4) 코스 저장
//        SavedCourse saved = new SavedCourse();
//        saved.setKakaoUser(user);
//        saved.setCourse(course);
//        saved.setSvdStartDate(gi.getInpStartDate());
//        saved.setSvdEndDate(gi.getInpEndDate());
//        savedCourseRepository.save(saved);
//
//        TemplateSaveResponseDTO res = new TemplateSaveResponseDTO();
//        res.setGroupId(group.getGroupId());
//        res.setCourseId(course.getCourseId());
//        res.setSavedId(saved.getSavedId());
//        return res;
//    }
//
////    private Course.CourseStyle mapToCourseStyle(String styleStr) {
////        if (styleStr == null || styleStr.isBlank()) return Course.CourseStyle.그외;
////        switch (styleStr.trim().toUpperCase()) {
////            case "FAMILY":
////            case "가족여행":
////                return Course.CourseStyle.가족여행;
////            case "HEALING":
////            case "NATURE":
////            case "힐링여행":
////                return Course.CourseStyle.힐링여행;
////            case "FRIENDS":
////            case "우정여행":
////                return Course.CourseStyle.우정여행;
////            case "DATE":
////            case "데이트":
////                return Course.CourseStyle.데이트;
////            case "WALK":
////            case "뚜벅이":
////                return Course.CourseStyle.뚜벅이;
////            default:
////                return Course.CourseStyle.그외;
////        }
////    }
//}