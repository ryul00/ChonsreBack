package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.SavedCourse;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;

import java.util.Optional;



import TourCompetition.ChonsreBack.Domain.Func.Entitiy.SavedCourse;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    // 한 유저가 동일 코스를 중복 저장했는지 체크/조회
    Optional<SavedCourse> findByKakaoUserAndCourse(KakaoUser user, Course course);

    // 마이페이지 리스트 조회용(필요시)
    List<SavedCourse> findByKakaoUserOrderBySavedIdDesc(KakaoUser user);

    // 존재 여부만 빠르게 확인하고 싶을 때
    boolean existsByKakaoUserAndCourse(KakaoUser user, Course course);

    // 코스 삭제시 연쇄 정리(선택)
    long deleteByCourse(Course course);
}

