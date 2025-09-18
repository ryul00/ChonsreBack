package TourCompetition.ChonsreBack.Domain.Func.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SavedCourseListItemDTO {
    private Long savedId;
    private String svdStartDate;
    private String svdEndDate;

    private Long courseId;
    private String courseLabel;
    private String title;

    private String accommodationName;
    private String courseImgUrl;      ;
    private boolean past;       // 과거 여부
    private boolean canReview;  // 과거이면 true
    private boolean canDelete;
    private boolean hasReview;  // 내가 이미 리뷰를 작성했는지
}