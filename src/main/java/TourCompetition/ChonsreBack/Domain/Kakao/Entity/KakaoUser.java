package TourCompetition.ChonsreBack.Domain.Kakao.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kakao_user")
public class KakaoUser {

    @Id
    @Column(nullable = false, unique = true)
    private Long kakaoId;

    @Column(nullable = true, unique = true)
    private String nickname;

    private String profileimgUrl;

    private LocalDateTime connectedAt;

    public KakaoUser(Long kakaoId, String nickname, LocalDateTime connectedAt, String profileimgUrl) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.connectedAt = connectedAt;
        this.profileimgUrl = profileimgUrl;
    }
}
