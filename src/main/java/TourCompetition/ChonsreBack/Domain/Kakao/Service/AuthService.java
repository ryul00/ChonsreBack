package TourCompetition.ChonsreBack.Domain.Kakao.Service;

import TourCompetition.ChonsreBack.Domain.Kakao.Entity.KakaoUser;
import TourCompetition.ChonsreBack.Domain.Kakao.Repository.KakaoUserRepository;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthService {
    @Value("${kakao.api.key}")
    private String restApiKey;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    private final KakaoUserRepository kakaoUserRepository;
    // 생성자 주입을 사용하여 의존성 주입
    public AuthService(KakaoUserRepository kakaoUserRepository) {
        this.kakaoUserRepository = kakaoUserRepository;
    }

    RestTemplate restTemplate = new RestTemplate();
    // 코드 발급
    public String kakaoGetAccessViaCode(String code) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", restApiKey); // 카카오 REST API 키
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> responseNode = restTemplate.exchange("https://kauth.kakao.com/oauth/token", HttpMethod.POST, entity, JsonNode.class);
        String accessToken = responseNode.getBody().get("access_token").asText();
        return accessToken;
    }

    // 카카오 사용자 정보 가져오기 및 DB에 저장
    @Transactional
    public KakaoUser kakaoGetUserInfoViaAccessToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> responseNode = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, entity, JsonNode.class
            );

            JsonNode userInfo = responseNode.getBody();

            // 1. 기본 정보 추출
            Long kakaoId = userInfo.get("id").asLong();
            String connectedAtString = userInfo.get("connected_at").asText();
            LocalDateTime connectedAt = LocalDateTime.parse(connectedAtString.substring(0, 19));

            // 2. 프로필 이미지 URL 추출
            String profileImageUrl = null;
            if (userInfo.has("kakao_account") &&
                    userInfo.get("kakao_account").has("profile") &&
                    userInfo.get("kakao_account").get("profile").has("profile_image_url")) {
                profileImageUrl = userInfo.get("kakao_account").get("profile").get("profile_image_url").asText();
            }

            // 3. 기존 사용자 확인 or 생성
            KakaoUser kakaoUser = kakaoUserRepository.findByKakaoId(kakaoId).orElseGet(() -> {
                KakaoUser newUser = new KakaoUser();
                newUser.setKakaoId(kakaoId);
                newUser.setConnectedAt(connectedAt);
                return newUser;
            });

            // 4. 값 설정
            kakaoUser.setConnectedAt(connectedAt);
            kakaoUser.setProfileImgUrl(profileImageUrl); // 여기에 프로필 이미지 저장

            kakaoUserRepository.save(kakaoUser);
            return kakaoUser;

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.", e);
        }
    }


    @Transactional
    public void setNicknameForKakaoUser(Long kakaoId, String nickname) {
        KakaoUser kakaoUser = kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. kakaoId: " + kakaoId));

        kakaoUser.setNickname(nickname);
        kakaoUserRepository.save(kakaoUser); // 생략 가능하지만 명시적 저장
        log.info("닉네임 설정 완료: kakaoId={}, nickname={}", kakaoId, nickname);
    }


    // 로그아웃
    @Transactional
    public String kakaoLogout(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> responseNode = restTemplate.exchange(
                "https://kapi.kakao.com/v1/user/logout",
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        log.info("로그아웃 응답: {}", responseNode.getBody().toPrettyString());

        return responseNode.getBody().toString();
    }

    // 카카오 로그인 해제
    @Transactional
    public String kakaoUnlink(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> responseNode = restTemplate.exchange(
                "https://kapi.kakao.com/v1/user/unlink",
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        // 응답에서 사용자 ID 추출
        Long kakaoId = responseNode.getBody().get("id").asLong();

        // 데이터베이스에서 사용자 삭제
        kakaoUserRepository.deleteByKakaoId(kakaoId);

        log.info("탈퇴 응답: {}", responseNode.getBody().toPrettyString());

        // 반환할 때 메시지를 포함한 전체 JSON을 반환하도록 조정
        return responseNode.getBody().toString(); // JSON 문자열 반환
    }

    @Transactional
    public Long kakaoGetUserIdFromTokenInfo(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/access_token_info";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        log.info("Authorization 헤더 설정 완료: Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            log.info("카카오 API 응답 본문: " + body);

            if (body != null && body.has("id")) {
                Long kakaoUserId = body.get("id").asLong();
                log.info("카카오 사용자 ID: " + kakaoUserId);
                return kakaoUserId;
            } else {
                log.error("토큰 정보 조회 실패 또는 유효하지 않은 토큰입니다. 응답 본문: " + body);
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("카카오 API 클라이언트 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("카카오 API 클라이언트 오류", e);
        } catch (Exception e) {
            log.error("카카오 토큰 정보 조회 실패", e);
            throw new RuntimeException("카카오 토큰 정보 조회에 실패했습니다.", e);
        }
    }

    // 액세스 토큰 정보 확인 -> 추후 기능 구현 시 인증 수단으로 사용
    public Map<String, Object> getTokenInfo(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/access_token_info";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class
            );

            JsonNode body = response.getBody();
            log.info("토큰 정보 확인 응답: {}", body);

            // 응답 데이터 파싱
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("id", body.get("id").asLong());
            tokenInfo.put("expires_in", body.get("expires_in").asInt());
            tokenInfo.put("app_id", body.get("app_id").asInt());

            return tokenInfo;
        } catch (HttpClientErrorException e) {
            log.error("카카오 API 요청 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("토큰 정보 확인에 실패했습니다.", e);
        } catch (Exception e) {
            log.error("토큰 정보 확인 실패", e);
            throw new RuntimeException("알 수 없는 오류로 토큰 정보를 확인할 수 없습니다.", e);
        }
    }

    // 사용자 정보 조회
    @Transactional(readOnly = true)
    public KakaoUser getKakaoUserById(Long kakaoId) {
        return kakaoUserRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));
    }




}
