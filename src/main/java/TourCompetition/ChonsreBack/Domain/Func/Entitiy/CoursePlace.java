package TourCompetition.ChonsreBack.Domain.Func.Entitiy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Course_Place") // 각 일자별 포함된 장소
public class CoursePlace {
    @Id
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long placeId;
    private  Integer orderNum; // 일정 장소 순서
    private String placeName;
    private String placeDesc; // 장소별 설명
    private String ImgUrl; // 장소 이미지

    @ManyToOne
    @JoinColumn(name = "dayId", nullable = false)
    private CourseDay courseDay;

}
