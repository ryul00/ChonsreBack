// TemplateCoursePlaceInputDTO.java
package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateCoursePlaceInputDTO {
    @NotBlank            // 장소명은 필수 권장
    private String placeName;

    private String description;  // 설명은 선택
    private String address;      // 선택(없으면 서버에서 보강 시도)
    private String imgUrl;       // 선택
}
