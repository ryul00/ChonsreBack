package TourCompetition.ChonsreBack.Domain.Func.Repository;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendGroupRepository extends JpaRepository<RecommendGroup, Long> {
}