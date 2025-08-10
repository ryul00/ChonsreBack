package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateGroupInputDTO {
    private String inpStartDate;   // 필수
    private String inpEndDate;     // 필수
    private String inpRegion;      // 필수

    // 이하 선택값(null 허용)
    private Integer inpAdultCnt;
    private Integer inpChildCnt;
    private Integer inpBabyCnt;
    private String  inpStyle;
}
