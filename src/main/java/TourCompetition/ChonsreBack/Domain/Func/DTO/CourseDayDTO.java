package TourCompetition.ChonsreBack.Domain.Func.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CourseDayDTO {
    int day;
    List<CoursePlaceDTO> places;
}
