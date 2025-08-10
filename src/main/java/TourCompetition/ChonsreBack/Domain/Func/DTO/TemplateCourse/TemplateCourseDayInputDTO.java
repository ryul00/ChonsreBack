package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TemplateCourseDayInputDTO {
    private Integer day; // 몇 일차인지 (1부터)
    private List<TemplateCoursePlaceInputDTO> places;  // 방문 장소들 (전달 순서=방문 순서)
}

