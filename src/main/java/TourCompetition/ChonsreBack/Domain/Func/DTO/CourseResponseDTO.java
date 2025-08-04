package TourCompetition.ChonsreBack.Domain.Func.DTO;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.Course;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CourseResponseDTO {
    Long courseId;
    String title;
//    String region;
//    String style;
    List<CourseDayDTO> days;

}
