package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.CourseDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseDayRepository extends JpaRepository<CourseDay, Long> {
    List<CourseDay> findByCourse(Course course);
}
