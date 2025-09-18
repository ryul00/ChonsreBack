package TourCompetition.ChonsreBack.Domain.Kakao.Controller;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/auth")
@Slf4j
@RestController
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 프론트와 연결 시 컨트롤러 수정해서 프론트에서 코드를 보내고 그걸 받아서 토큰 발급 진행해야함
    @GetMapping("/kakaoLogin")
    public ResponseEntity<Map<String, Object>> kakaoLogin(@RequestParam("code") String code) {
        try {
            // 1단계: 전달받은 인가 코드를 사용하여 Kakao에서 액세스 토큰 발급
            String accessToken = authService.kakaoGetAccessViaCode(code);

            // 2단계: 액세스 토큰을 사용하여 Kakao 사용자 정보 조회 및 서재 생성
            KakaoUser kakaoUser = authService.kakaoGetUserInfoViaAccessToken(accessToken);

            // JSON 형태의 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("kakaoId", kakaoUser.getKakaoId());
            response.put("nickname", kakaoUser.getNickname());
            response.put("connectedAt", kakaoUser.getConnectedAt());
            response.put("profileImg", kakaoUser.getProfileImgUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 실패 시에도 Map<String, Object> 사용
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "인증 실패");
            errorResponse.put("message", "유효한 code가 아닙니다.");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 닉네임 변경 (기존)
    @PostMapping("/SetNickname")
    public ResponseEntity<?> setNickname(@RequestHeader("Authorization") String token,
                                         @RequestBody Map<String, String> requestBody) {
        try {
            String accessToken = token.replace("Bearer ", "");
            Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(accessToken);
            String nickname = requestBody.get("nickname");

            authService.setNicknameForKakaoUser(kakaoId, nickname);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "닉네임이 설정(신규/변경) 완료.");
            response.put("nickname", nickname);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("닉네임 설정 실패", e);
            return ResponseEntity.status(400).body("닉네임 설정 중 오류가 발생했습니다.");
        }
    }

    // [NEW] 프로필 이미지 변경
    @PostMapping("/SetProfileImg")
    public ResponseEntity<?> setProfileImg(@RequestHeader("Authorization") String token,
                                           @RequestBody Map<String, String> requestBody) {
        try {
            String accessToken = token.replace("Bearer ", "");
            Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(accessToken);
            String profileImgUrl = requestBody.get("profileImgUrl");

            authService.setProfileImgForKakaoUser(kakaoId, profileImgUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "프로필 이미지가 설정(신규/변경) 완료.");
            response.put("profileImgUrl", profileImgUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프로필 이미지 변경 실패", e);
            return ResponseEntity.status(400).body("프로필 이미지 설정 중 오류가 발생했습니다.");
        }
    }


    @GetMapping("/logout")
    public ResponseEntity<String> kakaoLogout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // "Bearer ..." 형태에서 실제 토큰만 추출
        String token = authorizationHeader.replace("Bearer ", "").trim();

        String result = authService.kakaoLogout(token);

        log.info("카카오 로그아웃 완료: {}", result);

        return ResponseEntity.ok("카카오 로그아웃 성공");
    }




    // 카카오 회원 탈퇴
    @GetMapping("/unlink")
    public ResponseEntity<Map<String, Object>> kakaoUnlink(@RequestHeader("Authorization") String token) {
        try {
            String accessToken = token.replace("Bearer ", "");
            // ✅ 컨트롤러에서는 서비스 호출만
            String response = authService.kakaoUnlink(accessToken);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", "success");
            responseBody.put("message", "회원 탈퇴가 성공적으로 처리되었습니다.");
            responseBody.put("kakaoResponse", response);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("탈퇴 실패: ", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "회원 탈퇴 중 오류가 발생했습니다.");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyNickname(@RequestHeader("Authorization") String token) {
        try {
            String accessToken = token.replace("Bearer ", "");
            Long kakaoId = authService.kakaoGetUserIdFromTokenInfo(accessToken);

            KakaoUser kakaoUser = authService.getKakaoUserById(kakaoId);

            Map<String, Object> response = new HashMap<>();
            response.put("nickname", kakaoUser.getNickname());
            response.put("profileImgUrl", kakaoUser.getProfileImgUrl());


            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("닉네임 조회 실패", e);
            return ResponseEntity.status(401).body("사용자 정보를 조회할 수 없습니다.");
        }
    }



}
