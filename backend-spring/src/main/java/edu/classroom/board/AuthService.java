package edu.classroom.board;

import io.jsonwebtoken.Claims; // 수정 (BE-02-3)
import io.jsonwebtoken.ExpiredJwtException; // 수정 (BE-02-3)
import io.jsonwebtoken.Jws; // 수정 (BE-02-3)
import io.jsonwebtoken.JwtException; // 수정 (BE-02-3)
import io.jsonwebtoken.Jwts; // 수정 (BE-01-3)
import io.jsonwebtoken.security.Keys; // 수정 (BE-01-2)
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant; // 수정 (BE-01-3)
import java.util.Date; // 수정 (BE-01-3)
import java.util.HexFormat; // 수정 (BE-01-2)
import javax.crypto.SecretKey; // 수정 (BE-01-2)
import org.springframework.beans.factory.annotation.Value; // 수정 (BE-01-2)
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  // 제공 설정 파일에 들어 있는 예시 키. 이 값이면 거부한다.
  private static final String EXAMPLE_SECRET = "REPLACE_WITH_RANDOM_64_HEX_CHARACTERS"; // 수정 (BE-01-2)

  private final SecretKey key; // 수정 (BE-01-2) 서명용 키
  private final String issuer; // 수정 (BE-01-3) iss: webservice-class
  private final String audience; // 수정 (BE-01-3) aud: board-api
  private final long ttlSeconds; // 수정 (BE-01-3) 토큰 유효 시간(초)
  private final Users users; // 수정 (BE-02-4) 계정 확인용

  // 서버가 켜질 때 한 번 실행된다. 여기서 예외가 나면 서버가 시작되지 않는다.
  public AuthService( // 수정 (BE-01-2) 생성자 추가
                      @Value("${app.jwt.secret}") String secret, // 수정 (BE-01-2)
                      @Value("${app.jwt.issuer}") String issuer, // 수정 (BE-01-3)
                      @Value("${app.jwt.audience}") String audience, // 수정 (BE-01-3)
                      @Value("${app.jwt.ttl}") long ttlSeconds, // 수정 (BE-01-3)
                      Users users) { // 수정 (BE-02-4)
    // 빈 값이거나 예시 키면 거부
    if (secret == null || secret.isBlank() || secret.equals(EXAMPLE_SECRET)) { // 수정 (BE-01-2)
      throw new IllegalStateException("JWT_SECRET 환경변수에 직접 생성한 64자리 16진수 키를 설정하세요."); // 수정 (BE-01-2)
    } // 수정 (BE-01-2)
    // 64자리 16진수가 아니면 거부
    if (!secret.matches("[0-9a-fA-F]{64}")) { // 수정 (BE-01-2)
      throw new IllegalStateException("JWT_SECRET은 64자리 16진수(32바이트)여야 합니다."); // 수정 (BE-01-2)
    } // 수정 (BE-01-2)
    // 0초 이하 토큰은 의미가 없으니 거부
    if (ttlSeconds <= 0) { // 수정 (BE-01-3)
      throw new IllegalStateException("TOKEN_TTL은 1 이상이어야 합니다."); // 수정 (BE-01-3)
    } // 수정 (BE-01-3)
    // 64자리 16진수 문자열 → 32바이트 → HS256 서명용 키
    this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret)); // 수정 (BE-01-2)
    this.issuer = issuer; // 수정 (BE-01-3)
    this.audience = audience; // 수정 (BE-01-3)
    this.ttlSeconds = ttlSeconds; // 수정 (BE-01-3)
    this.users = users; // 수정 (BE-02-4)
  } // 수정 (BE-01-2)

  // BE-01: 비밀번호 검사를 통과한 사용자에게만 호출된다 (BoardController.login 참고).
  public String issueToken(Users.PublicUser user) {
    // [삭제] throw new ApiError(501, "NOT_IMPLEMENTED", "JWT 발급을 구현하세요. BE-01");
    Instant now = Instant.now(); // 수정 (BE-01-3) 지금 시각
    return Jwts.builder() // 수정 (BE-01-3) 토큰 만들기 시작
            .subject(String.valueOf(user.id())) // 수정 (BE-01-3) sub: 1 → "1" (문자열)
            .issuer(issuer) // 수정 (BE-01-3) iss
            .audience().add(audience).and() // 수정 (BE-01-3) aud
            .issuedAt(Date.from(now)) // 수정 (BE-01-3) iat: 발급 시각
            .expiration(Date.from(now.plusSeconds(ttlSeconds))) // 수정 (BE-01-3) exp: 지금 + TTL
            .signWith(key, Jwts.SIG.HS256) // 수정 (BE-01-3) HS256 서명. 비밀번호·해시는 넣지 않음
            .compact(); // 수정 (BE-01-3) "헤더.내용.서명" 문자열로 완성
  }

  // BE-02: 요청마다 Authorization 헤더의 JWT를 검증하고, 검증된 사용자 ID만 돌려준다.
  public long currentUserId(HttpServletRequest request) {
    // [삭제] return 1L;  (데모 사용자 고정 반환)

    // 헤더에서 토큰만 꺼낸다. 형식이 틀리면 bearerToken 안에서 401 TOKEN_REQUIRED.
    String token = bearerToken(request.getHeader("Authorization")); // 수정 (BE-02-1, 2)

    // (1) 라이브러리로 서명·iss·aud·만료를 한 번에 검증
    Jws<Claims> jws; // 수정 (BE-02-3)
    try { // 수정 (BE-02-3)
      jws = Jwts.parser() // 수정 (BE-02-3)
              .verifyWith(key) // 수정 (BE-02-3) 우리 키로 서명 검증 (decode가 아니라 verify)
              .requireIssuer(issuer) // 수정 (BE-02-3) iss 불일치·누락이면 실패
              .requireAudience(audience) // 수정 (BE-02-3) aud 불일치·누락이면 실패
              .build() // 수정 (BE-02-3)
              .parseSignedClaims(token); // 수정 (BE-02-3) 서명 없는 토큰은 여기서 거부
    } catch (ExpiredJwtException e) { // 수정 (BE-02-3) 서명은 정상인데 만료된 토큰
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인 시간이 만료되었습니다. 다시 로그인하세요."); // 수정 (BE-02-3)
    } catch (JwtException | IllegalArgumentException e) { // 수정 (BE-02-3) 변조·형식·클레임 오류 전부
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다."); // 수정 (BE-02-3)
    } // 수정 (BE-02-3)

    // (2) 허용 알고리즘을 HS256 하나로 고정
    if (!"HS256".equals(jws.getHeader().getAlgorithm())) { // 수정 (BE-02-3)
      throw new ApiError(401, "INVALID_TOKEN", "허용되지 않은 서명 알고리즘입니다."); // 수정 (BE-02-3)
    } // 수정 (BE-02-3)

    Claims claims = jws.getPayload(); // 수정 (BE-02-3)

    // (3) exp 존재 확인. JJWT는 exp가 없으면 만료 검사를 건너뛰므로 직접 확인한다.
    if (claims.getExpiration() == null) { // 수정 (BE-02-3)
      throw new ApiError(401, "INVALID_TOKEN", "만료 시각이 없는 토큰입니다."); // 수정 (BE-02-3)
    } // 수정 (BE-02-3)

    // (4) sub 형식 확인. "1", "2" 같은 양의 정수 문자열이어야 한다.
    String sub = claims.getSubject(); // 수정 (BE-02-3)
    if (sub == null || !sub.matches("[1-9][0-9]{0,15}")) { // 수정 (BE-02-3)
      throw new ApiError(401, "INVALID_TOKEN", "토큰의 사용자 정보가 올바르지 않습니다."); // 수정 (BE-02-3)
    } // 수정 (BE-02-3)

    // (5) sub → 숫자 ID로 바꾸고, 실제 계정이 있는지 확인
    long userId = Long.parseLong(sub); // 수정 (BE-02-4)
    if (users.find(userId) == null) { // 수정 (BE-02-4)
      throw new ApiError(401, "INVALID_TOKEN", "사용자를 확인할 수 없습니다."); // 수정 (BE-02-4)
    } // 수정 (BE-02-4)

    // 모든 검사를 통과한 ID만 돌려준다 → /me, 글 작성·수정·삭제에서 사용
    return userId; // 수정 (BE-02-4)
  }

  /** "Bearer <토큰>" 형식에서 토큰만 꺼낸다. 헤더가 없거나 형식이 다르면 401 TOKEN_REQUIRED. */
  private String bearerToken(String header) { // 수정 (BE-02-2) 새 함수
    if (header == null) { // 수정 (BE-02-1)
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다."); // 수정 (BE-02-1)
    } // 수정 (BE-02-1)
    String[] parts = header.trim().split(" +"); // 수정 (BE-02-2) 공백 기준으로 자르기
    if (parts.length != 2 || !parts[0].equalsIgnoreCase("Bearer")) { // 수정 (BE-02-2)
      throw new ApiError(401, "TOKEN_REQUIRED", "Authorization: Bearer <토큰> 형식으로 보내세요."); // 수정 (BE-02-2)
    } // 수정 (BE-02-2)
    return parts[1]; // 수정 (BE-02-2) 토큰 부분만 반환
  } // 수정 (BE-02-2)

  // BE-03: 저장된 글의 작성자(authorId)와 현재 로그인 사용자(userId)를 비교한다.
  public void assertOwner(long userId, long authorId) {
    if (userId != authorId) { // 수정 (BE-03)
      throw new ApiError(403, "FORBIDDEN", "본인이 작성한 글만 수정·삭제할 수 있습니다."); // 수정 (BE-03)
    } // 수정 (BE-03)
  }
}