package TourCompetition.ChonsreBack.Domain.Func.DTO.Review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCreateRequestDTO {
    @NotBlank
    @Size(max = 300)
    private String content;   // 300자 이내 텍스트만
}
