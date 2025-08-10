package TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

//단일 템플릿 코스 저장 요청 DTO
@Getter
@Setter
public class TemplateSaveRequestDTO {

//    시작/종료일, 지역 등 메타 입력값 (시작/종료일+지역 필수, 나머지 null 허용)
    @NotNull
    private TemplateGroupInputDTO groupInput;
//    실제 저장할 단일 코스
    @NotNull
    private TemplateCourseInputDTO course;
}
