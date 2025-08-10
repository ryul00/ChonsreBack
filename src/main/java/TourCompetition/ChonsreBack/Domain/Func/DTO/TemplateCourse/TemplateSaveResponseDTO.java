package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateSaveResponseDTO {
    private Long groupId;
    private Long courseId;  // 생성된 단일 코스 ID
    private Long savedId;   // 생성된 SavedCourse ID (마이페이지용)
}
