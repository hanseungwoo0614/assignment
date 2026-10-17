# JWT 게시판 공통 API 명세

이 문서는 최종 완성본의 계약입니다. 시작 코드의 데모 인증과 501 응답은 이 계약의 인증 요구사항을 아직 충족하지 않습니다. 모든 조합은 동일한 JSON 요청·응답을 사용합니다. 백엔드 기본 주소는 http://127.0.0.1:8000 입니다.

## 공통 규칙

- 요청 JSON은 `Content-Type: application/json`으로 보냅니다. FastAPI도 로그인에 폼 데이터 대신 JSON을 사용합니다.
- 인증 요청은 `Authorization: Bearer <JWT>`를 사용합니다.
- 날짜는 UTC ISO 8601 문자열, ID는 양의 정수입니다. 목록은 id 내림차순 배열입니다.
- 글 등록과 수정은 title, content 두 필드만 허용합니다. 둘 다 필요합니다. 수정은 PUT입니다.
- 제목은 공백 정리 후 1~100자, 내용은 1~5000자입니다. 공백만 입력할 수 없습니다.
- authorId, userId, id 등 추가 요청 필드는 400으로 거부합니다.
- 에러는 `{ "code": "오류코드", "message": "설명" }`입니다. message 문구는 자유롭게 정할 수 있습니다.
- 본 명세에서 지정한 경로를 평가합니다. 프레임워크 기본 문서 경로·미등록 경로의 응답 형태는 평가 대상이 아닙니다.

## 응답 코드가 일치해야 완료입니다

**HTTP 상태와 JSON code를 함께 확인합니다.** 성공은 200·201·204, 실패는 400·401·403·404를 아래 명세대로 사용하세요. HTTP 200 본문에 오류 코드만 넣는 구현은 실패 응답으로 인정하지 않습니다. 201의 Location, 보호 API 401의 WWW-Authenticate, 204의 빈 본문도 계약에 포함됩니다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 정상 응답 |
|---|---|---|---|
| GET | /api/health | 없음 | **200** 상태 확인 |
| POST | /api/auth/login | 없음 | **200** 토큰과 사용자 |
| GET | /api/me | 필요 | **200** 내 정보 |
| GET | /api/posts | 없음 | **200** 게시글 배열 |
| GET | /api/posts/{id} | 없음 | **200** 게시글 |
| POST | /api/posts | 필요 | **201** 새 게시글 |
| PUT | /api/posts/{id} | 필요, 본인 | **200** 수정된 게시글 |
| DELETE | /api/posts/{id} | 필요, 본인 | **204** 본문 없음 |

## 로그인

요청 필드는 email과 password이며 문자열입니다. 이메일은 1~254자, 비밀번호는 1~128자입니다. 비밀번호 원문을 임의로 trim해서 비교하지 않습니다.

```json
{"email":"alice@class.test","password":"Classroom!2026"}
```

응답 예시는 다음과 같습니다. 아래 토큰은 실제 토큰이 아닌 자리표시자입니다.

```json
{
  "accessToken":"<발급된 JWT>",
  "expiresIn":90,
  "user":{"id":1,"name":"Alice","email":"alice@class.test"}
}
```

잘못된 계정·비밀번호는 401 INVALID_CREDENTIALS, 잘못된 입력 형식은 400 BAD_INPUT입니다. `/api/me`는 user와 같은 형태의 객체만 반환합니다. Alice는 id 1, Bob은 id 2입니다.

## 게시글

등록과 수정 요청 예시입니다. 작성자는 요청 본문에 넣지 않습니다.

```json
{"title":"오늘의 학습 기록","content":"JWT 검증 흐름을 정리했습니다."}
```

게시글 응답은 다음 필드를 포함합니다.

```json
{
  "id":1,
  "title":"오늘의 학습 기록",
  "content":"JWT 검증 흐름을 정리했습니다.",
  "authorId":1,
  "authorName":"Alice",
  "createdAt":"2026-10-10T00:00:00.000Z",
  "updatedAt":"2026-10-10T00:00:00.000Z"
}
```

등록 응답은 201과 `Location: /api/posts/{id}`를 반환합니다. 수정해도 작성자와 생성 시각은 바뀌지 않습니다. 삭제 성공은 204이며 JSON 본문을 보내지 않습니다. 빈 목록은 `[]`입니다.

## JWT 설정과 검증

| 설정 | 값 또는 의미 |
|---|---|
| 알고리즘 | HS256으로 고정 |
| JWT_SECRET | 32바이트 난수를 표현한 64자리 16진수 문자열. 사용 시 bytes로 변환 |
| JWT_ISSUER | webservice-class |
| JWT_AUDIENCE | board-api |
| TOKEN_TTL | 기본 90초. 테스트에서는 3초 등으로 조정 가능 |
| sub | 계정의 양의 정수 ID를 표현한 문자열 |
| exp | 발급 시각 이후의 만료 시각. UTC Unix timestamp 초 |
| iss / aud | 위의 발급자와 사용 대상 |

서버 시작 또는 인증 설정 시 예시 키·빈 키·형식 오류를 거부하도록 구현하세요. 최종 서버는 유효한 키 설정 후 실행합니다. 한 서비스의 프론트·백엔드 조합만 평가하므로 서로 다른 백엔드 사이의 토큰 공유는 요구하지 않습니다.

검증은 허용 알고리즘, 서명, exp 존재와 만료, iss, aud, sub 형식과 실제 계정 존재를 포함합니다. 검증된 사용자 ID만 요청 처리에 넘깁니다. 토큰에 비밀번호나 비밀번호 해시를 넣지 않습니다.

## 실패 응답

| 상황 | HTTP | code |
|---|---:|---|
| 계정 정보 불일치 | **401** | INVALID_CREDENTIALS |
| 토큰 없음 또는 Bearer 헤더 형식 오류 | **401** | TOKEN_REQUIRED |
| 변조·서명 오류·잘못된 클레임·존재하지 않는 사용자 | **401** | INVALID_TOKEN |
| 만료된 정상 서명의 토큰 | **401** | TOKEN_EXPIRED |
| 인증했지만 다른 사람의 글을 수정·삭제 | **403** | FORBIDDEN |
| 유효한 ID이지만 글이 없음 | **404** | NOT_FOUND |
| 입력 형식·길이·추가 필드·잘못된 ID | **400** | BAD_INPUT |

보호 API의 401에는 `WWW-Authenticate: Bearer`를 포함합니다. 정상 형식의 보호 요청은 인증 → 글 존재 확인 → 작성자 권한 → 입력 검증 → 변경 순서로 처리하세요. 요청 JSON 자체가 깨진 경우 프레임워크가 먼저 400을 반환할 수 있습니다.

## 요청 조건에 따른 판단 예시

| 요청 조건 | 실제 HTTP | JSON code |
|---|---|---|
| 토큰 없이 정상 ID의 없는 글 수정 | **401** | TOKEN_REQUIRED |
| 정상 토큰으로 같은 없는 글 수정 | **404** | NOT_FOUND |
| Bob의 정상 토큰으로 Alice 글 수정 | **403** | FORBIDDEN |
| Bob의 만료된 정상 서명 토큰으로 Alice 글 수정 | **401** | TOKEN_EXPIRED |
| Alice가 자기 글에 공백 제목을 제출 | **400** | BAD_INPUT |
| Bob이 Alice 글에 공백 제목을 제출 | **403** | FORBIDDEN |
| GET /api/posts/abc | **400** | BAD_INPUT |

위의 보호 요청은 올바른 ID 형식과 정상 JSON을 전제로 합니다. 요청 JSON이 깨지거나 ID 형식 자체가 틀리면 400으로 거부할 수 있습니다.

예를 들어 Bob이 Alice의 글 수정을 시도한 실패 응답은 다음과 같습니다. message 문구는 달라도 되지만 HTTP 상태와 code는 맞아야 합니다.

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{"code":"FORBIDDEN","message":"본인이 작성한 글만 수정할 수 있습니다."}
```

만료된 정상 서명의 토큰으로 보호 API에 요청한 경우입니다.

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
WWW-Authenticate: Bearer

{"code":"TOKEN_EXPIRED","message":"로그인 시간이 만료되었습니다. 다시 로그인하세요."}
```

삭제 성공은 **204 No Content**이며 본문은 비어 있어야 합니다. 프론트는 response.json()을 호출하지 않습니다. 등록 성공은 **201 Created**, 그 밖의 지정된 조회·로그인·수정 성공은 **200 OK**입니다.

## 프레임워크별 주의

- FastAPI: 본 과제의 지정 입력 오류는 400 BAD_INPUT입니다. 기본 422 응답을 그대로 사용하지 않도록 예외 처리를 맞추세요.
- Express: 에러 본문만 JSON으로 보내고 HTTP 상태를 200으로 남기지 마세요. 상태 지정과 JSON 전송을 함께 확인합니다.
- Spring: 인증 실패가 HTML 로그인 화면이나 302 리다이렉트가 되지 않게 지정 API를 구성합니다. Security를 추가했어도 동일한 JSON 계약을 따릅니다.
- 501 NOT_IMPLEMENTED는 시작 코드의 미완성 발급 표시입니다. 최종 정상 로그인에는 남기지 않습니다. 예상 가능한 입력·인증·권한 실패를 500 INTERNAL_ERROR로 대체하지 않습니다. 예상하지 못한 내부 오류의 상세 스택이나 키는 응답에 노출하지 않습니다.

## 응답과 화면의 연결

401이면 토큰과 사용자 상태를 지우고 재로그인을 안내합니다. 403은 유효한 인증 뒤 권한 부족이므로 자동 로그아웃하지 않습니다. 400은 입력을 고치게 하고, 404는 글 없음 안내와 목록 갱신으로 처리합니다. 실패 응답 뒤에는 DB의 글과 작성자·시간이 그대로인지 재조회해 확인합니다.

## 참고 자료

- Vite 실행과 개발 서버: https://vite.dev/guide/
- Express 설치: https://expressjs.com/en/starter/installing/
- Node JWT 라이브러리: https://github.com/auth0/node-jsonwebtoken
- FastAPI JWT 학습 자료: https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/ (본 과제의 로그인 입력은 JSON)
- PyJWT 사용법: https://pyjwt.readthedocs.io/en/stable/usage.html
- Spring Boot 3.5 실행 환경: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- JJWT 사용법: https://github.com/jwtk/jjwt
