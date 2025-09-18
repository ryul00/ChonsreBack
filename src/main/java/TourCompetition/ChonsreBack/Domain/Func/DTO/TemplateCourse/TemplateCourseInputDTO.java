// TemplateCourseInputDTO.java (참고: 그대로 OK)
package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TemplateCourseInputDTO {
    private String title;
    private List<TemplateCourseDayInputDTO> days;

    // 숙소 1개
    private TemplateAccommodationInputDTO accommodation;
}
