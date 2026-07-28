# 인증/보안 도메인 감사 리포트 (개발자 A)

- 일자: 2026-07-28
- 대상 도메인: `com.ssa.lms.auth.*`, `com.ssa.lms.user.*`, `com.ssa.lms.config.SecurityConfig`
- 브랜치: `feat/a-auth-audit`
- 실증 방식: 코드 리딩 + MockMvc 통합 테스트(`AuthSecurityAuditTest`, 20건) + **실기동 HTTP 요청**(`bootRun :8086`, curl)
- 전체 테스트: `./gradlew test` → **173건 통과 / 실패 0**

> 원칙: "코드가 그렇게 보인다"는 증거로 인정하지 않는다. 각 항목은 실제 요청/DB 조회로 실증했다.
> 재현 근거는 `src/test/java/com/ssa/lms/auth/AuthSecurityAuditTest.java` 에 재사용 가능하게 남겼다.

## 요약 (분류)

| # | 항목 | 결과 |
|---|------|------|
| 1 | 인증 기본 흐름(역할 리다이렉트/실패 메시지/상태별 차단/로그아웃) | ✅ (정지·탈퇴 메시지 오분류 1건 ❌→수정) |
| 2 | 세션 고정 방어 / CSRF / HttpOnly | ✅ |
| 2 | 동시접속 방지(A-9) | ⚠️ 미구현(설계) |
| 3 | 권한 경계 매트릭스(역할×구역) | ✅ |
| 4 | IDOR(수강신청 취소·이수증·진도·출결) | ✅ (진도 API 수강검증 부재 ⚠️ 1건) |
| 5 | 시험 응시 본인인증 | ✅ |
| 6 | 비밀번호(bcrypt)/개인정보(AES) 취급 | ✅ |
| 7 | 가입 검증(중복/동의/입력) | ✅ |

**수정한 버그(❌→fixed)**: 1건 — 정지/탈퇴 계정 로그인 시 "승인 대기(?pending)" 오분류 (`auth.*` 국소 수정, 커밋 분리).
**설계 판단 필요(⚠️)**: 3건 — 진도 API 수강검증, 동시접속 방지, h2-console permitAll 프로필 하드닝.
**공동 파일(SecurityConfig) 수정**: 없음. (권한 매트릭스는 현행 설정으로 요건 충족 — 변경 불필요)

---

## 1. 인증 기본 흐름

### 1-1. 역할별 홈 리다이렉트 — ✅
`RoleBasedAuthenticationSuccessHandler` 가 권한 넓은 순(ADMIN>INSTRUCTOR>TRAINEE)으로 분기.

- MockMvc(`AuthFlowTest`): admin→`/admin`, instructor1→`/instructor`, trainee1→`/trainee` 리다이렉트 확인.
- 실기동: `POST /login`(trainee1/1234) → `HTTP 302 Location: /trainee` 확인.

### 1-2. 로그인 실패 & 계정 존재여부 비구분 — ✅
`hideUserNotFoundExceptions`(기본 true)로 없는 계정은 `BadCredentialsException` 으로 변환되어 틀린 비밀번호와 동일 응답.

실기동 결과:
```
없는계정(ghost999)     -> /login?error
틀린비번(trainee1/bad) -> /login?error   ← 동일 (열거 불가)
```

### 1-3. PENDING(가입 직후) 로그인 차단 — ✅
`LoginUser.isEnabled()` 이 PENDING 을 비활성 처리 → `DisabledException` → `LoginFailureHandler` → `?pending`.
승인 전에는 로그인 자체가 통과되지 않는다.

실기동: `POST /login`(trainee_pending/1234) → `/login?pending`.

### 1-4. 비활성(SUSPENDED/WITHDRAWN) 로그인 차단 — ✅ (메시지 오분류 ❌→**수정**)

**차단 자체는 정상**: 정지/탈퇴 계정도 로그인 통과 불가.

**발견한 버그(❌)**: 수정 전 `LoginUser.isEnabled() = (status == ACTIVE)` 였기 때문에 정지(SUSPENDED)·탈퇴(WITHDRAWN) 계정도 PENDING 과 동일하게 `DisabledException` 을 던져 **"승인 대기(?pending)"** 안내로 라우팅됐다.
- 정지된 회원에게 "승인 대기 중"이라 안내하는 것은 명백한 오분류이며,
- 등록만 되고 정지된 계정의 상태를 로그인 화면이 은연중 노출한다.

**수정(국소·1파일, `com.ssa.lms.auth.LoginUser` — A 소유)**:
```java
// PENDING 만 비활성 → ?pending (승인 대기 안내)
@Override public boolean isEnabled()          { return status != UserStatus.PENDING; }
// 정지/탈퇴는 "잠김" → Spring Security 가 먼저 검사 → LockedException → ?error(일반 실패)
@Override public boolean isAccountNonLocked()  { return status != SUSPENDED && status != WITHDRAWN; }
```
`LoginFailureHandler` 는 `DisabledException`→`?pending`, 그 외(Locked/Bad)→`?error` 로 이미 분기하므로 **핸들러/템플릿 변경 없이** 해결. SecurityConfig(공동 파일) 미수정.

실증(MockMvc `LoginStatus`): SUSPENDED 계정 → `/login?error`, PENDING → `/login?pending` 로 분기 고정.

### 1-5. 로그아웃 세션 무효화 — ✅
`POST /logout` 후 `SecurityContextLogoutHandler` 가 세션 무효화.

실기동:
```
로그인 후 /trainee/my-course        => HTTP 200
logout                              => HTTP 302 loc=/login?logout
로그아웃 후 같은세션 /trainee/my-course => HTTP 302 loc=/login   ← 재사용 세션 접근 불가
```
MockMvc(`로그아웃하면_세션이_무효화된다`)로도 동일 실증.

---

## 2. 세션 / CSRF / 쿠키

### 2-1. 세션 고정(session fixation) 방어 — ✅
Spring Security 기본 `changeSessionId` 전략. 로그인 전후 JSESSIONID 교체를 실기동으로 실측:
```
PRE-LOGIN  JSESSIONID = EABDD4A173DB963495A9E72454DBDC37
POST-LOGIN JSESSIONID = 4F6152F482A331AEC18534B1299F3672   ← 교체됨
```

### 2-2. CSRF 토큰 없는 POST 거부 — ✅
- 실기동: `POST /login`(토큰 없음) → **HTTP 403**.
- MockMvc(`Csrf`): 토큰 없는 `POST /login`, `POST /signup/trainee`, `POST /trainee/contents/{id}/progress`(REST) 모두 **403**. 가입은 사용자도 생성되지 않음.
- 로그인 폼에는 CSRF 토큰(hidden `_csrf`, 96자)이 정상 렌더됨.

### 2-3. 쿠키 HttpOnly — ✅
실기동 응답 헤더:
```
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
```
`HttpOnly` 부여 확인. (참고: `Secure` 플래그는 HTTP 로컬이라 미부여 — **운영 HTTPS 에서는 `server.servlet.session.cookie.secure=true` 권장**. ⚠️ 하드닝 항목)

### 2-4. 동시접속 방지(A-9) — ⚠️ 미구현(설계 결정)
`SecurityConfig` 에 `sessionManagement().maximumSessions(...)` 설정 없음 → 동일 계정 다중 로그인 허용. 사전 인지대로 **미구현 확인**. 구현은 본 감사 범위 밖(설계/운영 결정 필요: 세션 저장소, 강제 로그아웃 UX).

---

## 3. 권한 경계 매트릭스 (역할 × 구역) — ✅

실기동(curl, 각 역할 실제 로그인 세션) HTTP 상태코드:

| URL | 미인증 | 훈련생 | 강사 | 판정 |
|---|---|---|---|---|
| /admin | 302→login | 403 | 403 | ✅ |
| /admin/user | 302 | 403 | 403 | ✅ |
| /admin/courses | 302 | 403 | 403 | ✅ |
| /admin/attendance | 302 | 403 | 403 | ✅ |
| /admin/completion-management | 302 | 403 | 403 | ✅ |
| /admin/evaluation | 302 | 403 | 404\* | ✅ (강사 인가 통과) |
| /admin/notice | 302 | 403 | 200 | ✅ (강사 허용 영역) |
| /instructor | 302 | 403 | 200 | ✅ |
| /instructor/courses | 302 | 403 | 200 | ✅ |
| /instructor/proctor | 302 | 403 | 404\* | ✅ (강사 인가 통과) |
| /trainee | 302 | 200 | 403 | ✅ |
| /trainee/my-course | 302 | 200 | 403 | ✅ |
| /trainee/attendance | 302 | 200 | 403 | ✅ |

- 미인증: 모든 보호 구역 → **302 로그인 리다이렉트**.
- 훈련생: 관리자·강사 구역 8종 → **전부 403** (요건 5개 이상 충족), 훈련생 구역 → 200.
- 강사: 관리자 전용 5종 → **전부 403**, 강사/공유 구역(`/instructor/**`, `/admin/{evaluation,support,notice,survey}/**`) → 허용.
- `*` 404 = 인가는 통과(`hasAnyRole ADMIN,INSTRUCTOR`)했으나 해당 base 경로에 매핑된 핸들러가 없는 **B 도메인 URL** → 즉 접근 허용을 의미(403 아님).

MockMvc(`RoleBoundary`)로 미인증/훈련생/강사 3케이스를 동일하게 고정. SecurityConfig 규칙(구체 경로 우선)이 의도대로 동작.

---

## 4. IDOR (URL 조작)

trainee1 세션으로 타인 리소스 접근을 시도. A 도메인 trainee-facing 엔드포인트 대부분은 대상 id 를 **경로가 아니라 `@AuthenticationPrincipal LoginUser` 의 id 로 고정**하므로 URL 로 타인 id 를 주입할 지점 자체가 없다.

| 시도 | 대상 | 방어 | 실증 |
|---|---|---|---|
| 남의 수강신청 취소 | `POST /trainee/enrollments/{남의id}/cancel` | 서비스 본인검증(`EnrollmentService.cancel` → `trainee.id` 비교) | ✅ 리다이렉트 + 피해자 신청 APPLIED 유지(미취소) |
| 남의 이수증 다운로드 | `GET /trainee/completion-management/{남의id}/certificate` | `CompletionService.isOwnedByTrainee` false → **404**(존재여부 미노출) | ✅ 404 |
| 이수증 소유 경계(서비스) | `isOwnedByTrainee(cid, uid)` | 본인만 true | ✅ trainee2=true / trainee1=false |
| 남의 출결 조회 | `/trainee/attendance` | principal id 고정(경로 파라미터 없음) | ✅ 조작 지점 부재, 본인 데이터만 |
| 남의 진도 조회/기록 | `/trainee/contents/{contentId}/progress` | userId=principal 고정(contentId 만 경로) | ✅ 타인 진도 접근 불가 |
| 남의 나의과정 | `/trainee/my-course` | principal id 고정 | ✅ |

전 시도가 403/404/무효처리로 차단됨(5종 이상 실증). 강사 담당 아닌 과정 접근은 강사 화면 컨트롤러가 담당강사 매핑으로 필터링(별도 강사 트랙에서 라이브 검증됨, 본 감사에서는 A 소유 trainee IDOR 를 집중 실증).

### ⚠️ 발견(설계 판단 필요): 진도 API 수강(enrollment) 미검증
`ProgressService.record()/complete()` 는 `contentId` 유효성만 확인하고 **호출자가 해당 과정 수강생인지 검증하지 않는다**.
- 영향: 훈련생이 **미수강 과정의 콘텐츠에 대해서도 자기 진도 행을 생성**할 수 있고, 콘텐츠 존재여부(200 vs 404 `ContentNotFoundException`)를 열거할 수 있다.
- 심각도: **낮음** — 기록 대상은 언제나 본인 userId 라 타인 데이터 열람/변조는 불가하고, `syncEnrollmentProgress` 는 수강 과정에만 반영되어 실제 이수 판정에는 영향이 없다.
- 수정하지 않은 이유: "미수강 콘텐츠 진도 기록"을 하드 차단할지는 **정책 결정**(미리보기 허용 여부, ADMIN 예외 등)이며 `ProgressServiceTest` 픽스처에 영향. 범위 규칙("설계 변경 필요 시 ⚠️ 보고만")에 따라 미수정.
- 권고: `record/complete` 진입부에 `enrollmentRepository.existsByTraineeIdAndCourseIdAndStatusIn(...)` 가드 추가, 위반 시 403.

---

## 5. 시험 응시 본인인증 (`IdentityVerificationService`) — ✅

A 는 본인인증 **계약**을 제공하고, 시험 회차 생성 차단은 B 의 `ExamAttemptService.start()` 가 이 계약을 호출해 강제한다(코드 확인: `verifyIdentity()` + 마지막 방어선 `ExamTakeException.identityRequired`, `ExamAttemptService.java:189-193`). 본 감사는 A 소유 계약을 실증.

- 비밀번호 일치 → 인증 성공, 수단코드 `"PASSWORD"` 반환, `access_log(IDENTITY_VERIFY)` 기록 → `lastVerifiedAt` 조회됨. ✅
- 비밀번호 불일치 → `IdentityVerificationException`. ✅
- 미지원 수단(`"SMS"`) → `IdentityVerificationException`. ✅
- 인증 이력이 응시 데이터에 남는가: `ExamAttempt.identityVerifiedAt/identityVerifyMethod` 로 저장(B 엔티티, `verifyIdentity` 결과 주입). 인증 없이(요건 true) 이력 없으면 회차 미생성 — B 계층 이중 방어 확인. ✅

MockMvc(`Identity`)로 세 경로 고정. 외부 본인인증(PASS/SMS) 연동은 `PasswordIdentityVerificationService` 의 TODO — ⚠️ 미구현(설계·외부연동, 범위 밖).

---

## 6. 비밀번호 / 개인정보 취급 — ✅

### 6-1. DB 저장값 실측 (H2 raw 조회)
`SELECT password, email, phone, birth_date FROM users WHERE login_id='trainee1'`:
- `password` → **`$2...` bcrypt 해시**, 원문 `1234` 아님. ✅
- `email/phone/birth_date` → **AES-256/GCM 암호문**(원문 `trainee1@ssa.local`/`010-2222-2222`/`1999-07-07` 아님). ✅
- 라운드트립: 엔티티 조회 시 `CryptoConverter` 가 평문 복호화(email/phone 원문 일치) 확인. ✅

### 6-2. 비밀번호 변경 — 현재 비밀번호 검증 — ✅
`MyInfoService.changePassword` 가 `passwordEncoder.matches(현재)` 실패 시 거부.
MockMvc: 틀린 현재 비밀번호로 `POST /trainee/my-info/password` → 리다이렉트(flash `pwError`), **비밀번호 미변경**(기존 해시 유지) 실증.

### 6-3. 로그/응답 원문 노출 — ✅
- `access_log` 엔티티에 credential/password 컬럼 **없음**(구조적). 실패 로그는 `loginId`만 저장.
- 실증: 구별되는 비밀번호(`S3cretProbe_9x`)로 로그인 실패 후 `access_log` 전 컬럼(login_id/ip/user_agent)에 원문 미존재 확인.
- `LoginFailureHandler`/`AuthController` 는 예외 메시지에 비밀번호를 싣지 않는다(로그인 화면은 `?error`/`?pending` 파라미터만 노출).

---

## 7. 가입 검증 — ✅
(기존 `AuthFlowTest` 에서 커버 — 재확인)
- 중복 `loginId` → `DuplicateLoginIdException` → 필드 에러, 가입 반려. ✅
- 필수 동의(개인정보 수집·제3자 제공) 미체크(`@AssertTrue`) → 반려, 사용자 미생성. ✅
- 비밀번호/확인 불일치(`@AssertTrue isPasswordMatched`) → 반려. ✅
- 입력 검증: loginId 패턴(4~20 영숫자), 비밀번호 8자+, 이메일 형식, 연락처 패턴, 필수값(`@NotBlank`). ✅
- ADMIN self-signup 차단(`UserService.parseSelfSignupRole`). ✅
- 가입 직후 PENDING + 동의시각 기록 + 비밀번호 해시 저장. ✅

---

## B 전달 사항
- 없음. (본 감사에서 발견한 결함은 모두 A 도메인. `/admin/{evaluation,notice,...}` 매트릭스 404/200 은 B 컨트롤러 매핑 정상 동작으로, 결함 아님.)

## 조치 목록
| 구분 | 내용 | 상태 |
|---|---|---|
| ❌→fixed | 정지/탈퇴 계정 로그인 메시지 오분류(?pending→?error) | 수정·테스트 고정(커밋 분리) |
| ⚠️ | 진도 API 수강검증 부재 | 권고만(정책 결정 필요) |
| ⚠️ | 동시접속 방지(A-9) 미구현 | 범위 밖(설계) |
| ⚠️ | 쿠키 `Secure` 플래그(운영 HTTPS) / h2-console permitAll 프로필 게이팅 | 운영 하드닝 권고 |
| ✅ | 그 외 인증/세션/CSRF/권한/IDOR/본인인증/암호화/가입 | 실증 통과 |

## 재현 방법
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"
./gradlew test --tests "com.ssa.lms.auth.*"      # 감사 테스트(30건)
./gradlew test                                    # 전체 173건
# 실기동 HTTP 실증: ./gradlew bootRun --args='--server.port=8086' 후 위 curl 절차
```
