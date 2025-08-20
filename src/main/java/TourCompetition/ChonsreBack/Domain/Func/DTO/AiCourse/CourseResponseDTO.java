package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseResponseDTO {
    Long courseId;
    String courseLabel;
    String title;
    List<CourseDayDTO> days;
}
