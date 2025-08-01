package TourCompetition.ChonsreBack.Domain.Func.Entitiy;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Saved_Course")
public class SavedCourse {
    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long savedId;
    private String svdStartDate;
    private String svdEndDate;

    @ManyToOne
    @JoinColumn(name = "kakaoId", nullable = false)
    private KakaoUser kakaoUser;

    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private Course course;
}
