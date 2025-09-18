// TemplateCourseDayInputDTO.java
package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;  // ← [FIX] import 추가

@Getter
@Setter
public class TemplateCourseDayInputDTO {
    @NotNull
    private Integer day; // 몇 일차인지 (1부터)

    @NotNull
    private List<TemplateCoursePlaceInputDTO> places;  // 방문 장소들 (전달 순서=방문 순서)
}
