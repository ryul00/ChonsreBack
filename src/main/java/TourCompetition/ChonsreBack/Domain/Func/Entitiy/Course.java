package TourCompetition.ChonsreBack.Domain.Func.Entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Course")
public class Course {
    public enum CourseStyle {
        가족여행,
        힐링여행,
        우정여행,
        데이트,
        뚜벅이,
        그외
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long courseId;

    @Column(nullable = false)
    private String title;
//    private  String description;
    private String region;

    private String createdAt;

    @Column(nullable = false)
    private boolean isTemplate = false;   // 추천(false) vs 템플릿(true)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStyle style;  // 여행 스타일

    @ManyToOne
    @JoinColumn(name = "groupId", nullable = false)
    private RecommendGroup recommendGroup;

}
