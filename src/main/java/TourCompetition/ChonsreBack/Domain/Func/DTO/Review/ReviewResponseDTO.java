package TourCompetition.ChonsreBack.Domain.Func.DTO.Review;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewResponseDTO {
    private Long reviewId;
    private Long savedId;
    private Long courseId;
    private String courseTitle;
    private String content;
    private String createdAt;
    private String updatedAt;
}