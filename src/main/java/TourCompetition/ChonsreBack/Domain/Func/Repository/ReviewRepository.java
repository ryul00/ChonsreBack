package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Review;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findBySavedCourse(SavedCourse savedCourse);
    boolean existsBySavedCourse(SavedCourse savedCourse);
    void deleteBySavedCourse(SavedCourse savedCourse);
}