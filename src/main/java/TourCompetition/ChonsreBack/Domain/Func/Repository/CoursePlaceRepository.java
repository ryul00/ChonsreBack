package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CourseDay;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {
    List<CoursePlace> findByCourseDay(CourseDay day);
}
