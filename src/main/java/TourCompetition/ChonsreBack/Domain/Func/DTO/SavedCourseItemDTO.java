package TourCompetition.ChonsreBack.Domain.Func.DTO;

import TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse.CourseResponseDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavedCourseItemDTO {
    private Long savedId;          // Saved_Course PK
    private String svdStartDate;   // 저장 시 시작일
    private String svdEndDate;     // 저장 시 종료일
    private CourseResponseDTO course; // 풀 코스 JSON (숙소 + days 포함)
}
