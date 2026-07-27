# Samsung Academy LXP — 개발 가이드 (2인 협업)

K-디지털 트레이닝 훈련기관 학습데이터관리시스템(LMS). 정적 프론트엔드(HTML 155페이지)는 완성되어 있고, **Spring Boot 백엔드를 새로 구축해 기존 화면과 연동**하는 단계다. 전체 계획·역할 분담·정부 제출 증빙 체크리스트는 반드시 [PLAN.md](PLAN.md)를 먼저 읽을 것.

## 기술 스택 (확정)
- Java 17+, Spring Boot 3.x, Spring Security, Spring Data JPA, Thymeleaf, MySQL(또는 MariaDB), Gradle
- 프론트: 기존 정적 HTML/CSS/Vanilla JS를 Thymeleaf 템플릿으로 전환 (JS 주석에 "th:each 교체 대비" 표기 있음)
- 패키지 루트: `com.ssa.lms` — 컨트롤러는 `com.ssa.lms.web.{admin|instructor|trainee}.{도메인}` (메뉴구성도_IA 엑셀의 확정안)

## 빌드/실행
- 요구사항: JDK 17 (Gradle은 wrapper가 자동 다운로드)
- 실행: `./gradlew bootRun` → http://localhost:8080 (기본 `local` 프로필: 인메모리 H2, 시드 계정 admin / instructor1 / trainee1, pw `1234`)
- 로컬 MySQL 사용: DB `lms` 생성 후 `./gradlew bootRun --args='--spring.profiles.active=dev'` (계정은 환경변수 `LMS_DB_USER`/`LMS_DB_PASSWORD`)
- 테스트: `./gradlew test`
- 정적 리소스는 `src/main/resources/static/`, 화면 템플릿은 `src/main/resources/templates/` (기존 `/static/...` 절대경로 링크는 WebConfig 매핑으로 그대로 동작)
- 공통 레이아웃 fragment: `templates/fragments/{admin,instructor,trainee}.html` — 각자 자기 도메인 페이지를 컨트롤러로 연결할 때 복붙된 사이드바/헤더/푸터를 fragment 호출로 교체할 것 (사용법은 각 파일 상단 주석)

## 디렉터리/도메인 소유권 — 상대 소유 코드는 수정 금지
| 소유자 | 도메인 | 패키지/템플릿 |
|---|---|---|
| **개발자 A** | 인증/본인인증, 사용자(강사·훈련생) 관리, 과정/과목/차시/수강신청, 콘텐츠/진도, 출결/이수/이수증 | `com.ssa.lms.{auth,user,course,content,attendance,completion}.*`, `templates/admin/admin-02-user`, `admin-03-courses`, `admin-05-attendance`(출결·이수), `01-login/` |
| **개발자 B** | 시험/문제은행, 과제, 채점/성적, 시험 모니터링, Q&A/튜터링/공지/알림/설문, 대시보드/분석 | `com.ssa.lms.{exam,assignment,grading,proctor,support,notice,survey,dashboard}.*`, `templates/admin/admin-04-evaluation`, `admin-05-attendance`(설문), `admin-06-support`, `admin-07-notice`, `instructor/proctor` |
| **공동(합의 후 수정)** | 공통 레이아웃 fragment, 공통 엔티티(User, Course 등의 스키마 변경), SecurityConfig, build.gradle, data.sql | `templates/fragments/`, `com.ssa.lms.common.*`, `com.ssa.lms.config.*` |

- 상대 도메인의 기능이 필요하면 코드를 직접 고치지 말고 **필요한 인터페이스/메서드를 정리해 상대에게 요청**할 것.
- 공통 파일(위 표 3행) 수정 전에는 반드시 상대에게 먼저 공유.
- 엔티티/DB 스키마 변경은 양쪽 모두에 영향 → 변경 전 합의 필수.

## Git 규칙
- `main` 직접 푸시 금지. 개발자 A는 `feat/a-*`, 개발자 B는 `feat/b-*` 브랜치에서 작업 후 PR 머지.
- 작은 단위로 자주 커밋·머지해서 충돌을 줄인다.
- 커밋 메시지는 한글 허용, 기존 스타일 유지.

## 코드 컨벤션
- 도메인 하위 패키지 구조: `<domain>/{entity,repository,service,web}` (A·B 합의 — a-requests.md)
- 전 엔티티는 `com.ssa.lms.common.entity.BaseEntity` 상속 (createdAt/updatedAt/createdBy/updatedBy/deletedAt/is_deleted). soft delete는 `@SQLDelete`+`@SQLRestriction` 패턴 (User/Course 참고).
- UI 텍스트는 한국어. 기존 화면의 클래스명/구조를 최대한 유지하며 Thymeleaf 속성만 추가하는 방식으로 전환.
- JS의 하드코딩 더미 배열(`const ...Data = [...]`)은 서버 데이터로 교체하되, 원본 더미는 `data.sql` 시드 데이터로 옮긴다.
- 개인정보 컬럼(전화번호·이메일·생년월일 등)은 **엔티티 설계 시점부터** AES-256 `AttributeConverter` 적용, 비밀번호는 bcrypt (내역서 증빙 요건 — PLAN.md §2-1).
- 조회수·이력·로그 테이블(access_log 등)은 초기부터 기록 (3년 보존 요건, soft delete 사용).
- 알려진 파일명 오타: `alram/arlam`(→alarm), `sesstion`(→session), `trainee/index copy.html` — 정리는 Phase 0에서 합의 후 일괄 진행.

## 참고 자료
- `PLAN.md` — 역할 분담, 단계별 일정, 내역서 요건 매핑, 인프라 증빙 체크리스트
- `메뉴구성도_IA - 김민아.xlsx` — 메뉴 IA, 권한 CRUD 매트릭스, URL/컨트롤러 설계, 데이터 필드 정의("데이터 정리" 시트)
- `(양식3) ...내역서.hwp` — 정부 제출 양식 (기능 요건 + 증빙 요구사항)
