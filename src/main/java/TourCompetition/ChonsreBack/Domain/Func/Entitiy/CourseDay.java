package TourCompetition.ChonsreBack.Domain.Func.Entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Course_Day") // 각 코스 별 일자
public class CourseDay {
    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dayId;
    private  Integer dayNum; // 1,2,3일차

    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private Course course;

}
