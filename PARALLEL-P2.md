# 병렬 작업 가이드 — Phase 2 (개발자 A, 2 세션)

> 목적: A의 Phase 2(운영 기능)를 2개 Claude Code 세션으로 병렬 진행하되 **파일 충돌 0** 을 목표로 한다.
> 기준 커밋: `feat/a-auth-login` (Phase 1(A) 통합 = 인증/사용자관리/과정 + 이 prep 커밋).
> Phase 1 규칙은 [PARALLEL.md](PARALLEL.md) 와 동일한 원칙을 따른다. 여기서는 Phase 2 차이만 적는다.

## worktree = 세션별 독립 폴더

**절대 같은 폴더에서 세션 2개를 열지 말 것.** 반드시 아래 worktree(폴더 분리, .git 공유)에서 연다.

| 세션 | 폴더 | 브랜치 | 도메인 |
|---|---|---|---|
| **P2-A** | `../lxp-content` | `feat/a-content` | 콘텐츠 + 진도 추적 |
| **P2-B** | `../lxp-attendance` | `feat/a-attendance` | 출결 + 이수 + 이수증(PDF) |

## 트랙별 담당 (자기 폴더/패키지 밖은 수정 금지)

### P2-A 콘텐츠/진도 (`feat/a-content`)
- 패키지: `com.ssa.lms.content.*` (entity/repository/service/web)
- 템플릿: `templates/instructor/contents-*.html`(강사 콘텐츠 등록/업로드), 훈련생 콘텐츠 시청·진도 화면(trainee)
  - ⚠️ `templates/admin/admin-04-evaluation/contents-*.html` 은 **B 폴더(admin-04-evaluation)** 안에 있다. 여기는 손대지 말고, 필요하면 B와 조율(연결 방향은 아래 계약 참고).
- 첫 작업: **Content 엔티티 신설**(VOD/문서 유형, 과정/차시 연결) → 콘텐츠 CRUD/업로드 → **진도 추적 API**(프론트 JS 진도 로직을 서버 저장). soft delete + BaseEntity 상속.
- ★ **계약 제공**: `content.service.ProgressQueryService` 의 실제 구현체를 만든다(아래 참고).

### P2-B 출결/이수/이수증 (`feat/a-attendance`)
- 패키지: `com.ssa.lms.attendance.*`, `com.ssa.lms.completion.*`
- 템플릿: `templates/admin/admin-05-attendance/**` 중 **출결·이수 페이지만**
  (`admin-attendance*.html`, `admin-attendance-graduate.html`). ⚠️ `admin-attendance-survey*.html` 은 **B(설문)** 소유 — 손대지 말 것.
- 첫 작업: **접속 기반 출결**(access_log/차시 기반 Attendance 엔티티) → 출결 현황 → **이수 처리**(기준 설정 + 자동 판정) → **이수증 PDF 발급**.
- ★ **계약 소비**: 이수 판정은 진도(`ProgressQueryService`, P2-A) + 출결(자기 트랙) + 성적(`GradeQueryService`, **B — 아직 없음**)을 읽는다.

## 계약 — ProgressQueryService (base 에 이미 있음)

`com.ssa.lms.content.service.ProgressQueryService` 인터페이스와 **기본 fallback 빈**을 이 prep 커밋에 넣어 뒀다.
양쪽 트랙이 독립적으로 컴파일·부팅된다.

```java
public interface ProgressQueryService {
    int completedRatio(Long userId, Long courseId);       // 진도율 0~100
    boolean hasCompletedAll(Long userId, Long courseId);  // 필수 콘텐츠 전부 이수?
}
```

- **P2-A**: 실제 구현체(`@Service`)를 추가하면 `@ConditionalOnMissingBean` fallback 이 자동으로 물러난다. (시그니처가 부족하면 P2-B와 합의 후 인터페이스에 메서드 추가 — 인터페이스 변경은 양쪽 영향이므로 알림 필수.)
- **P2-B**: 이 인터페이스에 대고 이수 로직을 만든다. 로컬 단독 실행 시 fallback 이 0/false 를 주므로 부팅은 된다(실제 값은 통합 후).
- **성적(GradeQueryService)**: B가 아직 제공 안 함. 이수 판정에서 성적 조건은 인터페이스/TODO 로 두고, 진도+출결 기반부터 먼저 완성. **B의 grading 패키지를 직접 만들지 말 것.**

## 공유 파일 규칙 (여기서만 충돌 — 규칙 지키면 거의 안 남)

1. **SecurityConfig** — 두 트랙 경로는 기존 `/admin/**`·`/instructor/**`·`/trainee/**` catch-all 로 커버 → **원칙 수정 불필요.** 꼭 새 규칙이 필요하면 커밋 분리 + 상대 알림.
2. **`fragments/admin.html`(사이드바)** — 자기 도메인 `<li>` 만: P2-A=콘텐츠 관리, P2-B=출결/이수 관리. 다른 줄 건드리지 말 것(auto-merge).
3. **`LocalDataInitializer` — 동결. 수정 금지.** 도메인 시드는 새 `@Component @Profile("local") @Order(n)` 러너로: **P2-A=@Order(30), P2-B=@Order(40)**. (기존: core=0, user=10, course=20.)
4. **엔티티** — P2-A는 `Content`/`Progress` 계열, P2-B는 `Attendance`/`Completion` 계열만. 공유 엔티티(User/Course/Session)는 **읽기만**. B 계약(courseCode/courseName/cohort, Session seq/name, BaseEntity)은 깨지 말 것.
5. **`build.gradle`** — **이수증 PDF 라이브러리는 P2-B만** 추가(예: openhtmltopdf/openpdf). 커밋 분리 + **B에게 알림**. P2-A는 build.gradle 손대지 말 것.

## 실행

- JDK 17: `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot` (셸 PATH 없음 → JAVA_HOME 수동).
  bash: `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot"` 후 `./gradlew`.
- 포트: P2-A=8080 기본, P2-B=`--args='--server.port=8081'`.
  (8080 에 samsung-lxp 본폴더 stray 인스턴스가 떠 있으면 임시 포트 8090 등으로 확인.)
- **OneDrive 빌드 잠금**: `build/` 삭제 실패("Unable to delete directory"/"stale outputs") 시
  `./gradlew --stop && rm -rf build/test-results` 후 재실행. (자세히는 메모리 lxp-onedrive-build-workaround.)

## 커밋/푸시/통합

- 작은 단위 커밋. 각 트랙은 `personal` 원격에 자기 브랜치만 푸시(origin 금지).
- 완료 후 두 트랙을 `feat/a-auth-login` 으로 각각 머지. 충돌은 사이드바 `<li>`·build.gradle 정도.
- 공통 파일/계약 변경(build.gradle PDF, Content↔Exam 연결 방향, ProgressQueryService 시그니처)은
  `a-requests.md` 에 기록해 B 가 알 수 있게. **Content↔Exam**: a-requests P2 제안대로 Exam 이 content_id(nullable) 를 갖는 방향 유지.
