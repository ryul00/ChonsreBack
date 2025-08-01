package TourCompetition.ChonsreBack.Domain.Kakao.Repository;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KakaoUserRepository extends JpaRepository<KakaoUser, Long> {
    Optional<KakaoUser> findByKakaoId(Long kakaoId);
    void deleteByKakaoId(Long kakaoId);
}
