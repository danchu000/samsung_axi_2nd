# 병렬 작업 가이드 P3 (개발자 A — 미구현 화면 3세션)

> 목적: "준비 중" 처리된 A 몫 미구현 화면을 2~3개 Claude Code 세션으로 병렬 구현. **파일 충돌 0** 목표.
> 기준 커밋: `main` `aeeedbc` (B 대시보드·A 요청분 통합, 테스트 111건 통과).
> 이번 worktree 는 **OneDrive 밖**(`C:\work\`)이라 빌드 잠금 문제 없음 — `--init-script` 불필요, 그냥 `./gradlew test`.

## worktree = 세션별 독립 폴더 (이미 생성됨)

**절대 같은 폴더에서 세션을 2개 열지 말 것.** 각 세션은 아래 자기 폴더에서 연다.

| 세션 | 폴더 | 브랜치 | 담당 |
|---|---|---|---|
| **A-1** | `C:\work\lxp-a1` | `feat/a-user-myinfo` | 내 정보 + 관리자 계정 관리 |
| **A-2** | `C:\work\lxp-a2` | `feat/a-course-instructor` | 강사 담당과정·훈련생 + 일정 관리 |
| **A-3** | `C:\work\lxp-a3` | `feat/a-attendance-views` | 강사·훈련생용 출결/이수 화면 |

시간이 없으면 A-3 를 생략하고 그 범위를 A-1 이 이어받아도 된다(패키지가 겹치지 않음).

## 트랙별 범위 (자기 패키지/템플릿 밖은 수정 금지)

### A-1 내 정보/관리자 관리 (`feat/a-user-myinfo`)
- 패키지: `com.ssa.lms.user.*`
- 화면: **내 정보 조회/수정 + 비밀번호 변경** — 관리자·강사·훈련생 3역할
  (`templates/admin/my-info*.html`, `instructor/my-info.html`, `trainee/my-info.html` 기존 정적 화면을 컨트롤러 연결),
  **관리자 계정 관리** (`admin-02-user/admin-user.html` — 목록·등록·수정·비활성).
- fragment 수정 허용 범위: **내 정보 아이콘(3파일)과 "관리자 관리" 서브메뉴 링크만** — 지금 `alert('준비 중...')`으로 되어 있는 곳을 컨트롤러 URL 로 교체.
- 주의: User 개인정보 컬럼은 AES-256 암호화라 email/phone 으로 DB 검색 불가(loginId/name 만). 비밀번호는 bcrypt.

### A-2 강사 과정/일정 (`feat/a-course-instructor`)
- 패키지: `com.ssa.lms.course.*`
- 화면: **강사 담당 과정 목록/상세**(읽기 위주, `instructor/courses.html`), **강사 담당 훈련생 목록**(`instructor/trainees.html`),
  **일정 관리**(`admin-03-courses/admin-courses-schedule.html`, `instructor/scheduler.html` — 차시(Session) lessonDate 기반 달력/목록).
- 강사 권한 경계: 자기 담당 과정만 — `CourseQueryService.findCourseIdsByInstructorId()` 사용(신규 추가됨).
- fragment 수정 허용 범위: **"일정 관리" 서브메뉴(admin)·강사 fragment 의 담당과정/훈련생/일정 링크만**.
- 주의: `th:each` 변수명 `session` 금지(Thymeleaf 3.1 예약어 — 응답이 200인 채 잘림). `lesson` 등 사용.

### A-3 출결/이수 뷰 (`feat/a-attendance-views`)
- 패키지: `com.ssa.lms.attendance.*`, `com.ssa.lms.completion.*`
- 화면: **강사용 출결현황/이수 관리**(`instructor/attendance.html`, `instructor/graduate.html` — 담당 과정 한정),
  **훈련생용 출결현황/이수관리**(`trainee/attendance.html`, `trainee/completion-management.html` — 본인 것만).
- 기존 admin 용 서비스(AttendanceService·CompletionService) 재사용, 권한 필터만 추가.
- fragment 수정 허용 범위: **강사/훈련생 fragment 의 출결·이수 링크만**.

## 공유 파일 규칙 (PARALLEL.md 와 동일 + 이번 특이사항)

1. **fragment 3종** — 자기 담당 메뉴 줄만 수정(줄 단위 분리라 auto-merge 됨). 지금 미구현 링크는 `alert('준비 중인 기능입니다.')` 형태 — 자기 것만 URL 로 되돌릴 것.
2. **SecurityConfig** — `/admin/**`·`/instructor/**`·`/trainee/**` catch-all 로 대부분 커버, 원칙적으로 수정 불필요. 필요 시 커밋 분리 + 다른 세션에 공유.
3. **LocalDataInitializer 동결.** 데모 데이터가 필요하면 각자 새 시더: A-1 `@Order(30)`, A-2 `@Order(40)`, A-3 `@Order(50)` 또는 `@EventListener(ApplicationReadyEvent)` 패턴.
4. **엔티티 스키마** — 공유 엔티티(User/Course/Session) 필드 추가는 다른 세션 + B 에 먼저 알림. B 계약 필드(courseCode/courseName/cohort, seq/name) 불변.
5. **build.gradle / application*.yml** — 수정 금지(필요 시 통합 세션에 요청).
6. B 소유 패키지·템플릿(`exam/assignment/grading/proctor/support/notice/survey/dashboard`) 수정 금지.

## 실행

- JDK 17: bash 기준 `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"`
- 테스트: `./gradlew test` (OneDrive 밖이라 init-script 불필요)
- bootRun 포트 분리: A-1 `8080`, A-2 `--args='--server.port=8081'`, A-3 `--args='--server.port=8082'`
- 완료 기준: 테스트 통과 + **렌더 테스트(`</html>` 포함 확인)** + 실기동 화면 확인 후 브랜치 푸시.

## 통합

- 각 세션 완료 후 메인 세션(samsung-lxp 폴더)에서 main 에 순차 머지 → 전체 테스트 → personal 푸시.
- 머지 순서 무관하나 fragment 충돌 시 각 트랙의 자기 메뉴 줄만 살리면 됨.
