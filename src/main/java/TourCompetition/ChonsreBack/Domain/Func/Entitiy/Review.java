package TourCompetition.ChonsreBack.Domain.Func.Entitiy;



import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(
        name = "Review",
        uniqueConstraints = @UniqueConstraint(name = "UK_review_saved", columnNames = "savedId")
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "kakaoId")
    private KakaoUser kakaoUser;

    @OneToOne(optional = false)
    @JoinColumn(name = "savedId", unique = true)
    private SavedCourse savedCourse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "courseId")
    private Course course;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(nullable = false)
    private String createdAt;

    @Column(nullable = false)
    private String updatedAt;
}

