package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateCoursePlaceInputDTO {
    private String placeName;
    private String description;
    // 필요 시 확장 가능:
    // private Double lat; 좌표
    // private Double lng;
    // private String imageUrl; 사진
}
