package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByRecommendGroup(RecommendGroup group); // 코스 조회 쿼리문
}
