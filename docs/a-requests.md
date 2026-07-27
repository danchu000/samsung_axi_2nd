# 개발자 A에게 요청 — B 도메인이 의존하는 것들

> 작성: 개발자 B · 브랜치 `feat/b-schema`
> B가 만든 엔티티 34개가 아래 것들을 `import` 하고 있다. 이게 들어와야 컴파일된다.

## 처리 현황 (2026-07-27, A의 `feat/a-phase0-skeleton` 확인 결과)

| # | 항목 | 상태 |
|---|---|---|
| 1 | build.gradle 의존성 (Lombok 포함) | ✅ 반영 |
| 2 | `common.entity.BaseEntity` | ✅ 요청한 필드명 그대로 |
| 3 | `User` / `Course` / `Subject` / `Session` | ✅ FQCN·필드명 일치 (`Session` 테이블명만 `course_session`) |
| 4 | `CourseQueryService.findUserIdsByCourseId` / `isInstructorOf` | ✅ 반영 |
| 5 | `common.converter.CryptoConverter` (AES-256/GCM) | ✅ 반영 → B가 `Qna`/`QnaAnswer`/`TutoringMessage` 본문에 적용 완료 |
| 6 | SecurityConfig 경로 인가 | ✅ B 요청 경로 전부 반영 |
| 7 | `IdentityVerificationService` | ✅ 인터페이스 + `PasswordIdentityVerificationService` 구현체 |
| 8 | 이수 판정 ↔ 성적 경계 (`GradeQueryService`) | ⬜ **B가 아직 미구현** — Phase 1에서 제공 |
| 9 | `Content` ↔ `Exam` 연결 | ⬜ 미정 (P2) |
| — | `templates/`·`static/` → `src/main/resources/` 이동 | ✅ A가 처리 |

**남은 이슈: 저장소가 둘로 갈렸다.**
A는 `woongscoding/axi_project`, B의 origin은 `mina-2026-ai/samsung-lxp`.
어느 쪽을 정본으로 할지 정해야 PR을 올릴 수 있다.

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
