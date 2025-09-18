package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseEditRequestDTO {
    @NotBlank
    private String title;
    @NotNull
    private List<CourseEditDayDTO> days;

    private AccommodationEditDTO accommodation;
}
