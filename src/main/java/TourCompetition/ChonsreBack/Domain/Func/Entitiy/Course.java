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
        farm,
        fishing,
        etc
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long courseId;

    // 코스 제목 (기본 : 지역명)
    @Column(nullable = false)
    private String title;

    // A,B,C 코스
    @Column(length = 10)
    private String courseLabel;

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

    // 숙소 관련 엔티티
    @Column(name = "acc_name", length = 200)
    private String accommodationName;

    @Column(name = "acc_address", length = 300)
    private String accommodationAddress;

    @Lob // 길어질 수 있으니 가급적 TEXT
    @Column(name = "acc_description")
    private String accommodationDescription;
}
