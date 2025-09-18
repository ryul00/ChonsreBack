package TourCompetition.ChonsreBack.Domain.Func.Service;


import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewCreateRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewResponseDTO;
import TourCompetition.ChonsreBack.Domain.Func.DTO.Review.ReviewUpdateRequestDTO;
import TourCompetition.ChonsreBack.Domain.Func.Entitiy.*;
import TourCompetition.ChonsreBack.Domain.Func.Repository.*;
import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final SavedCourseRepository savedCourseRepository;
    private final ReviewRepository reviewRepository;
    private final KakaoUserRepository kakaoUserRepository;

    @Transactional(readOnly = true)
    public ReviewResponseDTO getOne(Long savedId, Long kakaoId) {
        SavedCourse saved = getOwnedSavedCourse(savedId, kakaoId);
        Review review = reviewRepository.findBySavedCourse(saved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 없습니다."));
        return toDto(review);
    }

    @Transactional
    public ReviewResponseDTO create(Long savedId, Long kakaoId, ReviewCreateRequestDTO req) {
        SavedCourse saved = getOwnedSavedCourse(savedId, kakaoId);

        // 과거 코스만 허용: endDate < today
        if (!isPast(saved.getSvdEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 종료 후에만 리뷰를 작성할 수 있습니다.");
        }

        if (reviewRepository.existsBySavedCourse(saved)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "리뷰가 이미 존재합니다.");
        }

        KakaoUser user = saved.getKakaoUser();
        Review rv = new Review();
        rv.setKakaoUser(user);
        rv.setSavedCourse(saved);
        rv.setCourse(saved.getCourse());
        rv.setContent(req.getContent());
        rv.setCreatedAt(now());
        rv.setUpdatedAt(now());
        reviewRepository.save(rv);

        return toDto(rv);
    }

    @Transactional
    public ReviewResponseDTO update(Long savedId, Long kakaoId, ReviewUpdateRequestDTO req) {
        SavedCourse saved = getOwnedSavedCourse(savedId, kakaoId);

        // 과거 코스만 허용(요구사항 유지)
        if (!isPast(saved.getSvdEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 종료 후에만 리뷰를 수정할 수 있습니다.");
        }

        Review rv = reviewRepository.findBySavedCourse(saved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 없습니다."));
        rv.setContent(req.getContent());
        rv.setUpdatedAt(now());
        reviewRepository.save(rv);

        return toDto(rv);
    }

    @Transactional
    public void delete(Long savedId, Long kakaoId) {
        SavedCourse saved = getOwnedSavedCourse(savedId, kakaoId);
        Review rv = reviewRepository.findBySavedCourse(saved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 없습니다."));
        reviewRepository.delete(rv);
    }

    // --- helpers ---
    private SavedCourse getOwnedSavedCourse(Long savedId, Long kakaoId) {
        SavedCourse saved = savedCourseRepository.findById(savedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 코스를 찾을 수 없습니다."));
        if (saved.getKakaoUser() == null || !saved.getKakaoUser().getKakaoId().equals(kakaoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
        return saved;
    }

    private boolean isPast(String endDateStr) {
        // svdEndDate 형식: "yyyy-MM-dd" 가정
        LocalDate today = LocalDate.now();
        LocalDate end   = LocalDate.parse(endDateStr);
        return end.isBefore(today);
    }

    private String now() { return LocalDateTime.now().toString(); }

    private ReviewResponseDTO toDto(Review r) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setReviewId(r.getReviewId());
        dto.setSavedId(r.getSavedCourse().getSavedId());
        dto.setCourseId(r.getCourse().getCourseId());
        dto.setCourseTitle(r.getCourse().getTitle());
        dto.setContent(r.getContent());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
