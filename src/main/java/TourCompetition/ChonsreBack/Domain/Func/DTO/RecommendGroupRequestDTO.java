package TourCompetition.ChonsreBack.Domain.Func.DTO;

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
    private String inpRegion;
    private RecommendGroup.InpCourseStyle inpStyle;
}
