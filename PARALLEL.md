# 병렬 작업 가이드 (개발자 A — 2 세션)

> 목적: A의 Phase 1 남은 작업을 2개의 Claude Code 세션으로 병렬 진행하되 **파일 충돌 0**을 목표로 한다.
> 기준 커밋: `feat/a-auth-login` (로그인/가입 슬라이스 완료 + 이 prep 커밋).

## worktree = 세션별 독립 폴더

**절대 같은 폴더(`samsung-lxp/`)에서 세션 2개를 열지 말 것.** 한 세션의 `git checkout`/커밋이
다른 세션 파일을 바꿔버린다. 반드시 아래 worktree(폴더 분리, .git은 공유)에서 연다.

| 세션 | 폴더 | 브랜치 | 도메인 |
|---|---|---|---|
| **A-1** | `../lxp-user` | `feat/a-user-mgmt` | 사용자 관리 |
| **A-2** | `../lxp-course` | `feat/a-course` | 과정/과목/차시/수강신청 |

## 트랙별 담당 (자기 폴더/패키지 밖은 수정 금지)

### A-1 사용자 관리 (`feat/a-user-mgmt`)
- 패키지: `com.ssa.lms.user.*` (web 컨트롤러/서비스 추가)
- 템플릿: `templates/admin/admin-02-user/**`
- 첫 작업: **관리자 승인**(PENDING→ACTIVE — 지금 가입 계정이 이거 없으면 로그인 불가),
  강사/훈련생 목록·상세·수정, 접속 이력(access_log) 조회.

### A-2 과정/수강신청 (`feat/a-course`)
- 패키지: `com.ssa.lms.course.*` (web 컨트롤러/서비스 추가)
- 템플릿: `templates/admin/admin-03-courses/**`, 훈련생 수강 화면(`trainee/my-course.html` 등)
- 첫 작업: 과정 CRUD, 과목/차시 구성, 강사-과정 매핑, **수강신청**(훈련생 신청 → 관리자 승인).

## 공유 파일 규칙 (여기서만 충돌 남 — 규칙 지키면 거의 안 남)

1. **SecurityConfig** — 이번 두 트랙은 기존 `/admin/**`(ADMIN)·`/trainee/**`(TRAINEE,ADMIN)
   catch-all로 이미 커버된다 → **원칙적으로 수정 불필요.** 꼭 새 경로 규칙이 필요하면 커밋을 따로 떼고
   상대 트랙에 알린 뒤, 통합 머지 때 수동 확인.
2. **`fragments/admin.html`(사이드바)** — 컨트롤러 연결하며 링크를 바꿀 때 **자기 도메인 메뉴 `<li>`만**
   수정한다. 다른 메뉴 줄은 건드리지 말 것(줄이 분리돼 있어 auto-merge 됨).
3. **`LocalDataInitializer`** — **동결. 수정 금지.** 도메인 시드는 새
   `@Component @Profile("local") @Order(n)` CommandLineRunner 로 추가(A-1은 `@Order(10)`,
   A-2는 `@Order(20)` 권장). 기본 시더가 `@Order(0)`이라 계정/데모 과정은 이미 만들어져 있다.
4. **엔티티 스키마** — A-1은 `User` 계열, A-2는 `Course` 계열만. 공유 엔티티에 필드 추가가 필요하면
   상대 트랙에 먼저 알림(양쪽 빌드에 영향). B 계약(courseCode/courseName/cohort, Session seq/name,
   BaseEntity 필드)은 깨지 말 것.
5. **build.gradle** — 라이브러리 추가(예: 이수증 PDF)는 커밋 분리 + 알림.

## 실행

- JDK 17: `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot` (셸 PATH에 없음).
  bash: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"` 후 `./gradlew`.
- 테스트: `./gradlew test`
- **bootRun 포트 분리(동시에 8080 못 씀)**: A-1은 기본 8080, A-2는
  `./gradlew bootRun --args='--server.port=8081'`.
- 시드 계정: admin / instructor1 / trainee1 (pw `1234`), 데모 과정 `COURSE-2026-001`.

## 커밋/푸시/통합

- 작은 단위로 커밋. 각 트랙은 `personal` 원격에 자기 브랜치 푸시(origin 금지).
- 완료되면 두 트랙을 `feat/a-auth-login`(통합 기준)으로 각각 머지. 충돌은 위 공유 파일에서만
  소량 발생 → 규칙대로면 사이드바 `<li>` 정도.
- 공통 파일/계약 변경은 `a-requests.md` 에 기록해 B가 zip 받을 때 알 수 있게.
