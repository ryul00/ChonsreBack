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
    @Column(nullable = false, unique = true)
    private  Long courseId;

    private String title;
//    private  String description;
    private String region;
    private String createdAt;

    @Enumerated(EnumType.STRING)
    private CourseStyle style;  // 여행 스타일

    @ManyToOne
    @JoinColumn(name = "groupId", nullable = false)
    private RecommendGroup recommendGroup;

}
