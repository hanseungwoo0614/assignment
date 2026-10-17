
# 오늘의 학습 기록 — JWT 인증 게시판

웹서비스설계 개인 과제입니다. 누구나 학습 기록을 읽을 수 있고, 로그인한 사용자만 글을 작성하며, 본인이 쓴 글만 수정·삭제할 수 있는 게시판입니다.

## 작성자와 서비스 소개

- 학번 / 이름: [학번] / [이름]
- 서비스 목적과 대상 사용자: 공부한 내용을 짧게 기록하고 다른 학생의 학습 과정을 함께 읽는 게시판입니다. 대상은 같은 수업을 듣는 학생들입니다. 기록은 로그인 없이 누구나 읽을 수 있지만, 작성은 로그인한 사용자만 할 수 있고 수정·삭제는 작성자 본인만 할 수 있습니다. 로그인은 JWT로 처리하며 토큰은 브라우저 탭의 메모리에만 보관합니다.
- 선택한 기술: React + Spring Boot
- 주요 기능: 공개 목록·상세 조회, 로그인과 JWT 발급, 내 정보 조회, 로그인 사용자 글 작성, 본인 글 수정·삭제, 토큰 없음·변조·만료 처리, 로그아웃

## 보고서와 테스트 증거

- [구현 보고서 PDF](docs/report.pdf)
- [테스트 증거](docs/evidence/)

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── tools/
│   └── check_api.py        # 제공된 공통 API 자동 검사 도구
├── frontend-react/         # React 프론트 (Vite)
│   ├── package.json, package-lock.json, vite.config.js, index.html
│   └── src/
│       ├── api.js          # FE-01~04: 로그인, 토큰 첨부, 오류 처리, canEdit
│       ├── main.jsx        # 화면 (서비스 안내 문구만 수정)
│       └── style.css
├── backend-spring/         # Spring Boot 백엔드
│   ├── pom.xml, mvnw, mvnw.cmd, .mvn/, env.example.txt
│   └── src/main/
│       ├── java/edu/classroom/board/
│       │   ├── AuthService.java      # BE-01~03: JWT 발급·검증, 작성자 권한
│       │   ├── BoardController.java  # API 라우트 (제공 코드)
│       │   ├── PostStore.java        # SQLite 저장소 (제공 코드)
│       │   ├── Users.java            # 테스트 계정·비밀번호 검증 (제공 코드)
│       │   └── Errors.java, ApiError.java, DatabaseConfig.java, BoardApplication.java
│       └── resources/application.properties, schema.sql
└── docs/
    ├── report.pdf
    └── evidence/
```

제공 템플릿의 폴더 이름(`frontend-react`, `backend-spring`)을 그대로 사용했습니다. 선택하지 않은 템플릿(`frontend-vue`, `backend-express`, `backend-fastapi`)은 저장소에 포함하지 않았습니다.

## 실행 환경

| 항목 | 사용 버전 |
|---|---|
| 운영체제 | Windows 11 |
| 프론트 런타임 Node.js | v24.21.0 |
| 백엔드 런타임 | JDK 25.0.4.1 (Eclipse Temurin) |
| 프레임워크 | Spring Boot 3.5.16, JJWT 0.13.0, React 19.3.0, Vite 8.3.4 |
| 데이터 저장 | SQLite (sqlite-jdbc 3.51.3.0) |

## 설치와 실행

아래 명령은 Windows PowerShell 기준입니다. 터미널 두 개를 사용합니다. 터미널 A는 백엔드, 터미널 B는 프론트입니다.

### 1 저장소 복제

```powershell
git clone https://github.com/hanseungwoo0614/assignment.git
cd assignment
```

### 2 백엔드 준비와 실행 (터미널 A)

JWT 비밀 키를 새로 생성합니다. 출력된 64자리 16진수를 아래 `JWT_SECRET`에 넣습니다.

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
```

```powershell
cd backend-spring
$env:JWT_SECRET='위에서_생성한_64자리_16진수'
$env:TOKEN_TTL='90'
.\mvnw.cmd package
java -jar target/jwt-board-1.0.0.jar
```

macOS/Linux는 `export JWT_SECRET=...`, `export TOKEN_TTL=90`, `chmod +x mvnw`, `./mvnw package`를 사용합니다.

- 환경설정 예시 파일: `backend-spring/env.example.txt` (Spring은 `.env`를 자동으로 읽지 않으므로 터미널 환경변수 또는 IDE Run Configuration으로 설정합니다)
- JWT_SECRET: 직접 생성한 64자리 16진수(32바이트)만 허용합니다. 비어 있거나, 형식이 틀리거나, 예시 값(`REPLACE_WITH_RANDOM_64_HEX_CHARACTERS`)이면 서버가 시작되지 않습니다. 실제 키는 저장소와 보고서에 기록하지 않습니다.
- TOKEN_TTL: 기본 90초입니다. 만료 테스트 때는 `$env:TOKEN_TTL='3'`으로 바꾸고 서버를 다시 실행합니다.
- 백엔드 주소: http://127.0.0.1:8000
- 정상 실행 확인: 브라우저에서 http://127.0.0.1:8000/api/health 를 열어 `"status":"ok"`를 확인합니다.

### 3 프론트 빌드와 로컬 실행 (터미널 B)

백엔드를 켜 둔 상태에서 실행합니다.

```powershell
cd frontend-react
npm ci
npm run build
npm run preview
```

- 최종 확인 주소: http://127.0.0.1:4173
- 개발 중 실행: `npm run dev` → http://127.0.0.1:5173
- API 연결 방식: 프론트는 `/api`로 요청하고, Vite의 `preview.proxy`와 `server.proxy`가 http://127.0.0.1:8000 백엔드로 전달합니다. 백엔드 포트를 바꾸면 `frontend-react/vite.config.js`의 두 proxy 값을 함께 수정합니다.
- 종료: 프론트와 백엔드를 실행한 각 터미널에서 Ctrl+C
- 코드 변경 후 재실행: 프론트는 `npm run build` 후 `npm run preview`를 다시 실행하고, 백엔드는 `.\mvnw.cmd package` 후 `java -jar ...`를 다시 실행합니다.

### 4 테스트 계정과 DB

| 사용자 | 이메일 | 실습용 비밀번호 |
|---|---|---|
| Alice (id 1) | alice@class.test | Classroom!2026 |
| Bob (id 2) | bob@class.test | Classroom!2026 |

- SQLite 파일 위치: `backend-spring/data/board.sqlite`. 서버 시작 시 폴더·파일·`posts` 테이블이 자동으로 생성됩니다. 환경변수 `DB_FILE`로 경로를 바꿀 수 있습니다.
- DB 초기화: 백엔드를 종료한 뒤 `backend-spring/data/board.sqlite`를 삭제하고 다시 실행합니다.
- 서버 재시작 후 데이터 유지 확인 결과: 아래 "테스트 방법과 결과 요약" 참고

위 계정은 과제 전용 테스트 계정입니다.

## 구현 완료 범위

| 기능 | 구현 여부 | 주요 파일 또는 함수 |
|---|---|---|
| 공개 목록과 상세 | 구현 | `BoardController.list`, `get` (제공 코드) |
| 로그인과 JWT 발급 | 구현 | `AuthService` 생성자, `AuthService.issueToken` / `api.js login` |
| JWT 검증과 내 정보 조회 | 구현 | `AuthService.currentUserId`, `bearerToken` / `BoardController.me` |
| 글 작성과 작성자 저장 | 구현 | `BoardController.create` + `currentUserId` / `api.js authorizationHeaders` |
| 본인 글 수정·삭제 | 구현 | `AuthService.assertOwner` / `api.js canEdit` |
| 토큰 만료와 로그아웃 | 구현 | `currentUserId`의 TOKEN_EXPIRED 처리 / `api.js request`의 401 처리, `clearSession` |
| 입력 오류와 권한 오류 처리 | 구현 | `BoardController.fields`, `Errors` (제공 코드) / `api.js request` |

각 기능의 동작 검증 결과는 아래 "테스트 방법과 결과 요약"에 기록합니다.

알려진 문제와 미완성 기능:
- 404 응답 시 안내 문구는 표시하지만, 상세 화면을 자동으로 닫거나 목록을 자동 갱신하지는 않습니다. (`main.jsx`는 안내 문구 외에는 제공 코드를 유지)

## 테스트 방법과 결과 요약

- 자동 검사: 저장소 루트에서 백엔드 실행 중에 제공 도구 `tools/check_api.py`를 실행합니다.
  - `py tools/check_api.py --mode final` (TOKEN_TTL=90)
  - `py tools/check_api.py --mode final --expiry` (백엔드를 TOKEN_TTL=3으로 재시작한 뒤)
- 수동 검사: PowerShell `Invoke-WebRequest`로 직접 요청을 보내고, 브라우저 개발자 도구 Network 탭에서 상태 코드·요청 헤더·응답을 확인했습니다. Bob 계정으로 Alice 글을 수정·삭제하는 요청은 화면 버튼과 관계없이 PowerShell로 직접 보냈습니다.
- 확인한 응답 기준: 200·201(Location 헤더)·204(빈 본문) / 400 BAD_INPUT / 401 INVALID_CREDENTIALS·TOKEN_REQUIRED·INVALID_TOKEN·TOKEN_EXPIRED(WWW-Authenticate: Bearer) / 403 FORBIDDEN / 404 NOT_FOUND
- 전체 T01~T17 결과와 증거: 보고서 4절, `docs/evidence/`

| 검사 | 결과 |
|---|---|
| `check_api.py --mode final` | 미확인 |
| `check_api.py --mode final --expiry` | 미확인 |
| 백엔드 재시작 후 데이터 유지 (T15) | 미확인 |
| 빌드본(4173) 로그인·글쓰기·API 연결 | 미확인 |
| 새 폴더 clone 후 README대로 실행 | 미확인 |

## 핵심 구현 설명

1. **로그인과 발급**: 프론트 `api.js`의 `login`이 email·password를 `POST /api/auth/login`으로 보냅니다. 서버는 제공된 `Users.verify`로 비밀번호를 확인한 사용자에게만 `AuthService.issueToken`으로 HS256 JWT(sub·iss·aud·iat·exp)를 발급합니다.
2. **토큰 보관과 전송**: 프론트는 받은 토큰과 사용자 정보를 모듈 변수(`session`)에만 저장합니다. `authorizationHeaders`가 토큰이 있을 때만 `Authorization: Bearer <JWT>`를 붙입니다. 새로고침하면 로그인 상태가 해제됩니다.
3. **서버 검증**: 보호 API(`/me`, 글 작성·수정·삭제)는 `AuthService.currentUserId`를 호출합니다. 헤더 형식 확인 → JJWT로 서명·iss·aud·만료 검증 → 알고리즘 HS256 고정 → exp 존재, sub 형식, 실제 계정 존재를 확인한 뒤 사용자 ID만 돌려줍니다.
4. **작성자 권한**: 글 작성 시 작성자는 검증된 사용자 ID로 서버가 정합니다. 수정·삭제는 `AuthService.assertOwner`가 저장된 글의 authorId와 현재 사용자 ID를 비교해 다르면 403 FORBIDDEN을 반환하며, DB 변경 전에 실행됩니다. 프론트의 `canEdit`은 버튼 표시용입니다.

## AI 활용 요약과 참고 자료

- AI 사용 여부: 사용
- 도구: Claude (Anthropic), 모델 설정값 claude-opus-5-5 (실제 응답 모델은 확인 불가)
- 사용 목적: 과제 요구사항 해석, JWT 개념 학습, 코드 초안 작성, 테스트 명령 작성, README 초안 작성
- 반영 위치:
  - `backend-spring/.../AuthService.java` 생성자, `issueToken`, `currentUserId`, `bearerToken`, `assertOwner` — AI 초안을 단계별(BE-01→BE-02→BE-03)로 설명받으며 그대로 반영
  - `frontend-react/src/api.js` `authorizationHeaders`, `request` 오류 처리, `login`, `canEdit` — AI 초안 적용
  - `frontend-react/src/main.jsx` 서비스 안내 문구 — AI 제안 문구 적용
  - 이 README 초안 — AI 작성 후 본인이 실제 결과로 보완
- 검증: PowerShell 요청과 브라우저 Network로 직접 확인, `tools/check_api.py` 실행 (결과는 위 표)
- 상세 활용 기록: 보고서 5절
- 참고 자료:
  - 제공 과제 문서 `docs/01_과제공지`, `02_API명세`, `05_테스트시나리오`
  - JJWT 사용법: https://github.com/jwtk/jjwt
  - Spring Boot 3.5 실행 환경: https://docs.spring.io/spring-boot/3.5/system-requirements.html
  - Vite 가이드: https://vite.dev/guide/

## 제출 확인

- [ ] README의 학번·이름과 "미확인" 결과를 실제 내용으로 교체했다.
- [ ] 새 폴더에 clone하여 README만으로 설치·환경설정·빌드·로컬 실행했다.
- [ ] 4173번 빌드 화면에서 API 연결과 전체 필수 기능을 확인했다.
- [ ] GitHub에서 보고서와 증거 링크가 열린다.
- [ ] 교수자가 저장소를 열람·복제할 수 있다.
- [ ] 비밀 키·실제 토큰·DB·node_modules·target을 커밋하지 않았다.
- [ ] 최종 push 후 저장소 URL과 제출 기준 커밋 SHA를 LMS에 제출했다.
