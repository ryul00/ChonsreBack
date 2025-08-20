package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseEditPlaceDTO {
    @NotBlank
    private String placeName;
    private String Description;
}
