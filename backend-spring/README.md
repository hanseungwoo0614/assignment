# Spring Boot 백엔드 시작 코드

JDK 17 이상을 설치하고 java -version으로 확인하세요. 이 폴더에서 Maven Wrapper를 실행합니다. 첫 실행에는 Maven과 의존성을 다운로드하므로 인터넷 연결이 필요합니다.

macOS / Linux:

```sh
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

주소: http://127.0.0.1:8000/api/health . 최초 설치에 시간이 걸릴 수 있습니다. Maven이 이미 설치되어 있다면 `mvn spring-boot:run`도 가능합니다.

JWT 설정은 `src/main/resources/application.properties`에 연결되어 있습니다. Spring은 `.env`를 자동으로 읽지 않습니다. `env.example.txt`를 보고 IDE Run Configuration의 환경변수 또는 터미널 환경변수로 설정하세요.

macOS / Linux 예시:

```sh
export JWT_SECRET='본인이_생성한_64자리_16진수'
export TOKEN_TTL=90
./mvnw spring-boot:run
```

Windows PowerShell 예시:

```powershell
$env:JWT_SECRET='본인이_생성한_64자리_16진수'
$env:TOKEN_TTL='90'
.\mvnw.cmd spring-boot:run
```

프론트용 Node.js가 설치되어 있으므로 `node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"`로 키를 만들 수 있습니다. 실제 키는 제출하지 않습니다. `AuthService.java`에서 설정을 주입하고 키를 16진수에서 바이트로 변환하여 JJWT를 사용하세요. 예시 키는 최종 서버에서 거부해야 합니다.

`AuthService.java`의 BE-01~03이 학생 구현 범위입니다. BoardController는 라우트, PostStore는 SQLite 쿼리, DatabaseConfig는 DB 폴더 생성과 연결 설정, Users는 PBKDF2 비밀번호 검증을 제공합니다. Spring Security 전체 설정은 필수가 아닙니다. 추가해도 동일 API 규격을 유지하세요.

DB는 `data/board.sqlite`입니다. 초기화하려면 서버 종료 후 해당 파일을 삭제하고 재실행하세요. 기존 실습 글이 지워집니다. SQLite JDBC를 사용하며 폴더·파일·posts 테이블은 자동 생성됩니다. 환경변수 DB_FILE로 SQLite 파일 경로를 바꿀 수 있습니다. 별도 DB 서버 설치는 필요하지 않습니다.

```sh
./mvnw package
```

빌드 후에는 같은 백엔드 폴더에서 다음 명령으로 로컬 배포본을 실행할 수 있습니다. 먼저 기존 서버를 Ctrl+C로 종료하고 JWT 환경변수를 설정한 터미널을 사용하세요.

```sh
java -jar target/jwt-board-1.0.0.jar
```

Windows에서는 패키징 명령을 `.\mvnw.cmd package`로 실행하며, 위 java 명령은 동일합니다. 백엔드 주소와 파일 DB 경로도 동일합니다. ZIP을 푼 macOS/Linux 환경에서 실행 권한 오류가 나면 `chmod +x mvnw`를 한 번 실행하세요.

패키징은 소스 컴파일과 실행 파일 생성을 확인합니다. 과제의 최종 API 검사는 상위 tools/check_api.py와 수동 시나리오로 별도 수행합니다.
