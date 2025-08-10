package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateCourseInputDTO {
    private String title;                  // 코스 제목
    private List<TemplateCourseDayInputDTO> days; // 일차 목록
}
