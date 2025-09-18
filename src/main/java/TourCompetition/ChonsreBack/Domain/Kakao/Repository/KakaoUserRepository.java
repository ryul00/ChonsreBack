package TourCompetition.ChonsreBack.Domain.Kakao.Repository;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KakaoUserRepository extends JpaRepository<KakaoUser, Long> {
    Optional<KakaoUser> findByKakaoId(Long kakaoId);
    void deleteByKakaoId(Long kakaoId);

    // 닉네임 중복 검사
    boolean existsByNickname(String nickname);

    // 닉네임 중복 검사 (자기 자신 제외)
    boolean existsByNicknameAndKakaoIdNot(String nickname, Long kakaoId);
}
