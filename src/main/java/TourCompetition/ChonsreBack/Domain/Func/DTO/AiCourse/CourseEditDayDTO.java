package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseEditDayDTO {
//    1,2,3일차
    @NotNull
    private Integer day;

    // @Valid : 객체 검증 어노테이션
    @NotNull
    @Valid
    private List<CourseEditPlaceDTO> places;

}
