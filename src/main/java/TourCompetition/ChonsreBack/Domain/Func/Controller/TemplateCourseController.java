package TourCompetition.ChonsreBack.Domain.Func.Controller;

import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.TemplateSaveRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.TemplateCourse.TemplateSaveResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.Service.TemplateCourseService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("templates")
public class TemplateCourseController {
    private final TemplateCourseService templateCourseService;
    private final AuthService authService; // kakaoGetUserIdFromTokenInfo(...) 제공

    @PostMapping(value = "/save", consumes = "application/json", produces = "application/json")
    public TemplateSaveResponseDTO saveTemplateCourse(
            @RequestBody @Valid TemplateSaveRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long kakaoId = null;
        if (authorization != null && !authorization.isBlank()) {
            kakaoId = authService.kakaoGetUserIdFromTokenInfo(authorization.replace("Bearer ", ""));
        }
        if (kakaoId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return templateCourseService.saveTemplate(request, kakaoId);
    }
}
