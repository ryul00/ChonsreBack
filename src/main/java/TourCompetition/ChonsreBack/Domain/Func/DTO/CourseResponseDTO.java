package TourCompetition.ChonsreBack.Domain.Func.DTO;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CourseResponseDTO {
    private Long courseId;
    private String title;
    private String description;
    private String region;
    private Course.CourseStyle style;
    private String createdAt;
}
