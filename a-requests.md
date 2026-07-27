# 개발자 A에게 요청 — B 도메인이 의존하는 것들

> 작성: 개발자 B · 브랜치 `feat/b-schema`
> B가 만든 엔티티 34개가 아래 것들을 `import` 하고 있다. 이게 들어와야 컴파일된다.

---

## P0 — 스켈레톤과 함께 반드시 필요

### 1. `build.gradle` 의존성

B 엔티티는 아래를 쓴다. 빠지면 전부 컴파일 실패.

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    compileOnly 'org.projectlombok:lombok'          // ★ B 엔티티 전체가 @Getter/@Builder 사용
    annotationProcessor 'org.projectlombok:lombok'  // ★

    runtimeOnly 'com.mysql:mysql-connector-j'       // 또는 MariaDB 드라이버
}
```

**Lombok을 안 쓰기로 하면 미리 알려줄 것.** B 엔티티 34개를 전부 손봐야 한다.

### 2. `com.ssa.lms.common.entity.BaseEntity`

B의 모든 엔티티가 `extends BaseEntity` 한다. 아래 형태를 가정하고 작성했다.

```java
package com.ssa.lms.common.entity;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @CreatedDate   @Column(name="created_at", updatable=false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(name="updated_at")               private LocalDateTime updatedAt;
    @CreatedBy     @Column(name="created_by", updatable=false) private Long createdBy;
    @LastModifiedBy @Column(name="updated_by")                 private Long updatedBy;

    // 내역서 3년 보존 요건 — soft delete
    @Column(name="deleted_at") private LocalDateTime deletedAt;
    @Column(name="is_deleted", nullable=false) private boolean deleted;
}
```

- `@Id` 는 BaseEntity에 넣지 말 것. B 엔티티가 각자 선언한다.
- `@EnableJpaAuditing` 도 A 쪽 설정 클래스에 필요.
- **필드명이 위와 다르면 알려줄 것.** B가 정렬(`@OrderBy("createdAt ASC")`)에 쓰고 있다.

### 3. A 소유 엔티티 — 클래스명과 패키지 확정

B가 `@ManyToOne` 으로 참조하는 것들. **이름이 다르면 지금 알려줘야 한다.**

| B가 가정한 FQCN | 필요한 필드(읽기) | B에서 쓰는 곳 |
|---|---|---|
| `com.ssa.lms.user.entity.User` | `id`, `loginId`, `name`, `role` | 응시자/제출자/작성자/채점자 — 거의 전 엔티티 |
| `com.ssa.lms.course.entity.Course` | `id`, `courseCode`, `courseName`, `cohort` | `Exam`, `CourseAssignment`, `Grade`, `Notice`, `Qna`, `Survey`, `Question`, `TutoringRoom` |
| `com.ssa.lms.course.entity.Subject` | `id`, `name` | `Exam` |
| `com.ssa.lms.course.entity.Session` (차시) | `id`, `seq`, `name` | `Exam`, `Qna`, `Survey` |

> `courseCode` 는 화면에 `COURSE-2024-001` 형태로 이미 박혀 있다(`exams.js`, `assignments.js`).
> `Course.id`(PK)와 별개 컬럼으로 있어야 한다.

### 4. 조회 인터페이스 (B가 호출)

```java
// 과정 수강생 명단 — 응시 대상자, 미제출자 목록, 설문 배포 대상 산출에 필수
List<Long> findUserIdsByCourseId(Long courseId);

// 강사가 해당 과정 담당인지 — 권한정의서의 △(담당 과정 한정) 판정에 전부 필요
boolean isInstructorOf(Long userId, Long courseId);
```

이 두 개가 없으면 B의 강사 권한 체크를 하나도 못 만든다.

### 5. `CryptoConverter` (AES-256 `AttributeConverter`)

`Qna.content`, `QnaAnswer.content`, `TutoringMessage.content` 에 적용해야 한다.
현재 소스에 `// TODO: A 가 CryptoConverter 를 제공하면 @Convert 추가` 로 표시해 뒀다.

```java
package com.ssa.lms.common.converter;
@Converter
public class CryptoConverter implements AttributeConverter<String, String> { ... }
```

PLAN.md §2-1대로 **엔티티 설계 단계에서 붙여야** 나중에 데이터 마이그레이션이 없다.

---

## P1 — Phase 1 안에 필요

### 6. `SecurityConfig` 경로 인가 규칙

공통 파일이라 B가 못 건드린다. 아래 경로를 추가해 줄 것.

| 경로 | 접근 권한 (권한정의서 기준) |
|---|---|
| `/admin/evaluation/**` | ADMIN, INSTRUCTOR |
| `/admin/support/**` | ADMIN, INSTRUCTOR |
| `/admin/notice/**` | ADMIN, INSTRUCTOR |
| `/admin/survey/**` | ADMIN, INSTRUCTOR |
| `/instructor/proctor/**` | ADMIN, INSTRUCTOR |
| `/trainee/exam/**`, `/trainee/assignment/**`, `/trainee/survey/**`, `/trainee/qna/**` | TRAINEE |

### 7. ★ 본인인증을 재사용 가능한 컴포넌트로

**이게 제일 중요하다.** 내역서 필수 요건이 "평가자 판별(진행단계/최종/과정) **본인인증**" 인데,
로그인 시 본인인증과 **시험 입장 시 재인증**이 같은 모듈이어야 한다.

```java
public interface IdentityVerificationService {
    /** 인증 수행. 성공 시 사용한 수단 코드를 반환. */
    String verify(Long userId, VerifyRequest request);

    /** 세션에 기록된 최근 인증 시각. 시험 입장 시 유효기간 판정에 쓴다. */
    Optional<LocalDateTime> lastVerifiedAt(Long userId);
}
```

B는 이 결과를 `ExamAttempt.identityVerifiedAt` / `identityVerifyMethod` 에 저장한다.
**A가 인증 모듈을 설계하는 시점에 이 요구를 반영해야 한다.** 나중에 붙이면 뜯어고쳐야 함.

### 8. 이수 판정 ↔ 성적 경계

`Grade` 는 B 소유다. A의 이수(completion) 로직이 성적을 읽어야 하므로, B가 아래를 제공한다.

```java
// com.ssa.lms.grading.service.GradeQueryService
List<GradeSummary> findConfirmedGrades(Long userId, Long courseId);
boolean hasAllRequiredGradesConfirmed(Long userId, Long courseId);
```

A는 `Grade` 엔티티/리포지토리를 직접 쓰지 말고 이 서비스만 호출할 것.
**필요한 시그니처가 더 있으면 알려주면 B가 추가한다.**

또 `Survey.reflectCompletion` (설문 이수 반영) 플래그도 A의 이수 로직이 읽어야 한다.

---

## P2 — 나중에

### 9. `Content` 조회
`admin-04-evaluation/contents-test.html` 이 콘텐츠 유형 "시험"과 `Exam` 을 연결한다.
콘텐츠는 A 소유이므로 연결 방향(콘텐츠가 exam_id를 갖는지, exam이 content_id를 갖는지)을 정해야 한다.

---

## 합의가 필요한 공통 결정

| # | 항목 | B의 제안 |
|---|---|---|
| 1 | 패키지 루트 | `com.ssa.lms` (IA 문서대로) |
| 2 | 도메인 하위 구조 | `<domain>/entity`, `/repository`, `/service`, `/web` |
| 3 | `templates/`, `static/` 을 `src/main/resources/` 아래로 이동 | **A가 스켈레톤 올릴 때 A가 같이 해줄 것.** 155개 파일이 움직이는 커밋이라 나눠서 하면 충돌난다 |
| 4 | `ddl-auto` | 개발 중 `update`, 스키마 확정 후 `validate` + `schema.sql` |
| 5 | DB | MySQL 8 / MariaDB 중 확정 필요 (B는 예약어 회피만 해둠) |

---

# A 답변 (2026-07-27, 커밋 f8be486 + 후속 커밋 반영)

## P0 — 전부 반영 완료
1. **의존성** ✅ 요청한 것 전부 포함 + validation/thymeleaf-extras-springsecurity6/H2(local). **Lombok 사용 확정.**
2. **BaseEntity** ✅ `com.ssa.lms.common.entity.BaseEntity` — 제안한 필드명 그대로 (createdAt/updatedAt/createdBy/updatedBy/deletedAt/deleted, 컬럼 is_deleted). `@EnableJpaAuditing` + `AuditorAware<Long>`(로그인 사용자 id) 구성 완료. `@Id` 없음. soft delete가 필요한 엔티티는 User/Course처럼 `@SQLDelete`+`@SQLRestriction` 패턴 사용.
3. **FQCN** ✅ 전부 B 가정대로: `user.entity.User`(id/loginId/name/role), `course.entity.Course`(**courseCode**·**courseName**·**cohort** — cohort는 더미 데이터 형식대로 `"1기"` 문자열), `course.entity.Subject`(name), `course.entity.Session`(**seq**/**name**, 테이블명만 예약어 회피로 `course_session`).
4. **조회 인터페이스** ✅ `com.ssa.lms.course.service.CourseQueryService`
   - `findUserIdsByCourseId(courseId)` — APPROVED+COMPLETED 수강생만 반환 (신청/반려/취소 제외). 다른 기준 필요하면 말해줘.
   - `isInstructorOf(userId, courseId)` ✅
5. **CryptoConverter** ✅ `com.ssa.lms.common.converter.CryptoConverter` (AES-256/GCM, 키: `lms.crypto.secret` → 운영은 env `LMS_CRYPTO_SECRET`). `@Convert(converter = CryptoConverter.class)` 붙이면 됨 — User의 email/phone/birthDate에 적용 예시 있음.

## P1
6. **SecurityConfig** ✅ 요청 표 그대로 반영 (구체 경로가 /admin/** 앞에 오도록 배치). 단 `/trainee/exam/**` 등은 표대로 **TRAINEE 전용**으로 했는데 ADMIN 열람이 필요해지면 알려줘.
7. **본인인증** ✅ `com.ssa.lms.auth.IdentityVerificationService` — 제안 시그니처 그대로 + `VerifyRequest(method, credential)` record. 기본 구현 `PasswordIdentityVerificationService`(비밀번호 재확인, 성공 시 access_log에 IDENTITY_VERIFY 기록 → lastVerifiedAt은 이 로그로 판정). 외부 인증(PASS/SMS)은 같은 인터페이스로 확장 예정. 실패 시 `IdentityVerificationException`.
8. **이수↔성적** 👍 A는 `GradeQueryService` 두 메서드만 호출하겠음. `Survey.reflectCompletion` 읽기 OK. 이수 로직 만들 때 시그니처 더 필요하면 요청할게.

## P2
9. **Content↔Exam 연결 방향 제안**: 의존 방향(B→A)을 유지하기 위해 **Exam이 content_id(nullable)를 갖는 쪽**을 제안. A의 Content 엔티티는 Phase 2 초에 올릴 테니 그때 확정하자.

## 공통 결정 답변
| # | 답변 |
|---|---|
| 1 | ✅ `com.ssa.lms` |
| 2 | ✅ A 도메인도 `<domain>/entity|repository|service|web` 구조로 재배치 완료 |
| 3 | ✅ 이동 완료 (aed241d) — `/static/**` 절대경로 링크는 WebConfig 매핑으로 그대로 동작 |
| 4 | ✅ local=H2 create-drop, dev=PostgreSQL update. 스키마 확정 후 validate+schema.sql 동의 |
| 5 | ~~MySQL 8 확정 제안~~ → **PostgreSQL로 확정** (2026-07-27, 추후 AI 기능 호환성/pgvector 대비. build.gradle 드라이버·dev 프로필·H2 모드 전부 전환 완료. B 쪽은 PostgreSQL 예약어만 확인해줘 — `user`, `session`, `order` 등. A 테이블은 users/course_session으로 이미 회피) |

---

# 공통 파일 변경 알림 (A, feat/a-auth-login — 로그인/가입 슬라이스)

> B에게: 아래는 **공동 소유 파일 SecurityConfig** 변경 및 신규 진입 경로다. zip 받을 때 참고.
> 엔티티/스키마 변경은 **없음** (User 필드 그대로, B 계약 유지). DB PostgreSQL 전환도 그대로 유지 — 이 슬라이스는 DB 비의존.

### SecurityConfig (공동 파일) 변경
- `formLogin` 을 커스텀 로그인 화면에 연동: `.loginPage("/login")` + `.loginProcessingUrl("/login")`
  (username/password 파라미터), 기존 `defaultSuccessUrl("/")` 제거.
- **로그인 성공 시 역할별 리다이렉트** — `RoleBasedAuthenticationSuccessHandler`:
  ADMIN→`/admin`, INSTRUCTOR→`/instructor`, TRAINEE→`/trainee`.
- **로그인 실패 분기** — `LoginFailureHandler`: 자격증명 오류→`/login?error`,
  비활성(승인대기 등, `DisabledException`)→`/login?pending`.
- **경로 인가 규칙(P1-6)·CSRF·H2 콘솔 설정은 그대로 유지** — B 도메인 경로 규칙 변경 없음.

### 신규 컨트롤러 / 경로 (A 소유)
- `auth.web.AuthController` : `GET /login`, `GET /signup`(유형 선택),
  `GET|POST /signup/trainee`, `GET|POST /signup/instructor`, `GET /signup/complete`.
- `auth.web.SignupForm` (@Valid DTO), `user.service.UserService#signup` :
  가입 시 상태 **PENDING**, loginId 중복 검사, 개인정보/제3자 제공 동의 시각 저장
  (`privacyConsentAt`/`thirdPartyConsentAt`). 관리자(ADMIN)는 self-signup 불가.
- `web.ModuleHomeController` : `GET /admin|/instructor|/trainee` → 기존 index.html **임시 연결**.
  각 모듈의 실제 대시보드/컨트롤러가 생기면 이 매핑은 이관/제거 예정 (B 대시보드와 충돌 시 알려줘).

### 참고 (B 영향 없음)
- 가입 계정은 PENDING 이라 **관리자 승인 UI(사용자 관리, 후속 슬라이스)** 전까지는 로그인 불가.
  현재 로그인/역할 리다이렉트 확인은 시드 계정(admin/instructor1/trainee1, pw 1234)으로 가능.

---

# 공통 파일 변경 알림 (A, feat/a-course — 과정/수강신청 슬라이스)

> B에게: 아래는 **공동 소유 레이아웃 fragment** 변경이다. **엔티티/스키마 변경 없음**
> (Course/Subject/Session/Enrollment 필드·계약 그대로, B가 읽는 courseCode/courseName/cohort,
> Session seq/name 유지). **SecurityConfig·build.gradle 변경 없음** — 신규 경로는 기존
> `/admin/**`(ADMIN)·`/trainee/**`(TRAINEE,ADMIN) catch-all 로 커버되어 규칙 수정 불필요.

### 공동 소유 fragment 변경 (자기 도메인 메뉴/링크 `<li>`만 수정)
- `fragments/admin.html` : 사이드바 “과정 관리” 링크 `/templates/.../admin-courses-edu.html`
  → **`/admin/courses`** (한 줄만 변경, 다른 메뉴 줄 그대로 → auto-merge).
- `fragments/trainee.html` : GNB “나의 과정 / 수강 과정 목록” 링크 → **`/trainee/my-course`** (두 줄).

### 신규 컨트롤러 / 경로 (A 소유, com.ssa.lms.course.web)
- `CourseAdminController` : `/admin/courses` (목록), `/new`·`POST /`(등록),
  `/{id}`(상세), `/{id}/edit`·`POST /{id}`(수정), `POST /{id}/status`, `POST /{id}/delete`.
- `CurriculumAdminController` : `POST /admin/courses/{id}/subjects[/{sid}[/delete|/sessions]]`,
  `POST .../sessions/{sid}[/delete]` (과목/차시 구성).
- `InstructorAdminController` : `POST /admin/courses/{id}/instructors[/{mid}/delete]` (강사 배정/해제).
- `EnrollmentAdminController` : `POST /admin/courses/{id}/enrollments/{eid}/approve|reject`.
- `MyCourseController` : `GET /trainee/my-course`, `POST /trainee/courses/{id}/enroll`,
  `POST /trainee/enrollments/{eid}/cancel`.

### 계약 재확인 (B가 쓰는 것 — 그대로 유지)
- `CourseQueryService.findUserIdsByCourseId` = APPROVED+COMPLETED 수강생, `isInstructorOf` 정상 동작.
- 수강 상태 enum: APPLIED/APPROVED/REJECTED/CANCELLED/COMPLETED (취소·반려는 상태로만 남김, soft delete 무관).

### 데모 시드 (local 전용)
- `course.CourseDemoDataInitializer @Order(20)` 추가 — 모집중 과정 `COURSE-2026-002`(trainee1 신청 대기),
  `COURSE-2026-003`. 기본 시더(@Order 0)는 미수정. B의 `@Order(10)` 대와 충돌 없음.
