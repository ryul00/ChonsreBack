package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseEditPlaceDTO {
    @NotBlank
    private String placeName;

    @JsonProperty("description")
    private String description;      // 정상 케이스

    @JsonProperty("Description")
    private void setLegacyDesc(String v) { this.description = v; } // 구버전 호환

    private String address;
    private String imgUrl;
}
