package TourCompetition.ChonsreBack.Domain.Func.Controller;



import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewCreateRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewUpdateRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Service.ReviewService;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthService authService;

    // 단건 조회 (해당 saved 코스의 리뷰 1개)
    @GetMapping("/saved/{savedId}")
    public ResponseEntity<ReviewResponseDTO> getOne(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        return ResponseEntity.ok(reviewService.getOne(savedId, kakaoId));
    }

    // 생성 (과거 코스만)
    @PostMapping("/saved/{savedId}")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable Long savedId,
            @RequestBody @Valid ReviewCreateRequestDTO request,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        ReviewResponseDTO res = reviewService.create(savedId, kakaoId, request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "review", res
        ));
    }

    // 수정 (과거 코스만)
    @PutMapping("/saved/{savedId}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long savedId,
            @RequestBody @Valid ReviewUpdateRequestDTO request,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        ReviewResponseDTO res = reviewService.update(savedId, kakaoId, request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "review", res
        ));
    }

    // 삭제 (항상 가능 — 요구사항 반영)
    @DeleteMapping("/saved/{savedId}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long savedId,
            @RequestHeader("Authorization") String token
    ) {
        Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(token.replace("Bearer ", ""));
        reviewService.delete(savedId, kakaoId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "savedId", savedId
        ));
    }
}
