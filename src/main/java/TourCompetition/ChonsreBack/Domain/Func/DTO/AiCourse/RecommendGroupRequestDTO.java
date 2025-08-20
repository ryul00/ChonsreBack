package TourCompetition.ChonsreBack.Domain.Func.DTO.AiCourse;

import TourCompetition.ChonsreBack.Domain.Func.Entitiy.RecommendGroup;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendGroupRequestDTO {
    private String inpStartDate;
    private String inpEndDate;
    private Integer inpAdultCnt;
    private Integer inpChildCnt;
    private Integer inpBabyCnt;
//    private String inpRegion;
    private RecommendGroup.InpCourseStyle inpStyle;
    private Boolean isTemplate = false;   // true면 새 그룹 생성 + 코스 isTemplate=true
}
