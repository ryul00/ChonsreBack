package TourCompetition.ChonsreBack.Domain.Kakao.Entity;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Review;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.SavedCourse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private String profileImgUrl;

    private LocalDateTime connectedAt;

    public KakaoUser(Long kakaoId, String nickname, LocalDateTime connectedAt) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.connectedAt = connectedAt;
        this.profileImgUrl = profileImgUrl;
    }

    @OneToMany(mappedBy = "kakaoUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<SavedCourse> savedCourses = new ArrayList<>();

    @OneToMany(mappedBy = "kakaoUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "kakaoUser", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<RecommendGroup> recommendGroups = new ArrayList<>();
}
