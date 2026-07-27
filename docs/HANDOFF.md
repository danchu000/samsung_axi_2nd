# 인수인계 — 개발자 B 작업

> 작성: 2026-07-27 · 다음 담당자(사람 또는 AI)가 이 문서만 읽고 이어받을 수 있게 쓴 것

---

## 1. 프로젝트와 내 역할

Samsung Academy LXP — K-디지털 트레이닝 훈련기관 학습데이터관리시스템(LMS). **정부 제출 문서(내역서)** 가 걸려 있어 보안·보존 요건이 실제 제약이다.

2인 협업이고 **나(사용자)는 개발자 B**다.

| | 담당 |
|---|---|
| **A** (`woongscoding`) | 인증/사용자/과정/콘텐츠/출결/이수 + 공통 코드 |
| **B** (나, `min03027`) | 시험·과제·채점/성적·시험모니터링·소통(Q&A/공지/알림/설문)·대시보드 |

전체 계획은 루트 `PLAN.md`, 협업 규칙은 `CLAUDE.md`. 설계 근거는 `메뉴구성도_IA - 김민아.xlsx` 의 `데이터 정리` / `권한정의서(1)(2)` 시트.

---

## 2. 저장소 — 두 개로 갈려 있다

| | |
|---|---|
| **정본** | `woongscoding/axi_project` — 로컬 리모트 이름 **`a`**. 기본 브랜치 `feat/a-phase0-skeleton` |
| 로컬 `origin` | `mina-2026-ai/samsung-lxp` — 원래 정적 프론트 저장소. **더 쓰지 않는다** |

- 두 저장소는 커밋 `0e58e08` 에서 갈라진 같은 뿌리라 리베이스가 깨끗하다.
- **푸시 권한 있음** (초대 수락 완료). `git push a <브랜치>` 로 올린다.
- `gh` CLI 없음. PR 은 GitHub REST API 로 만들었다:
  ```bash
  TOKEN=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill | grep '^password=' | cut -d= -f2-)
  curl -X POST -H "Authorization: Bearer $TOKEN" \
    https://api.github.com/repos/woongscoding/axi_project/pulls \
    -d '{"title":"...","head":"feat/b-xxx","base":"feat/b-exam","body":"..."}'
  ```

---

## 3. 개발 환경

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # 이거 없으면 gradle 이 안 돈다
./gradlew compileJava
./gradlew test
./gradlew bootRun                                # 기본 8080, local 프로필(H2 인메모리)
```

- 프로필: `local`(H2, 기본) / `dev`(MySQL). DB 설치 없이 바로 뜬다.
- 로그인 계정: `admin` / `instructor1` / `trainee1`, 비밀번호 전부 `1234`
- **curl 검증 시 CSRF 필요**:
  ```bash
  curl -s -c cj.txt http://localhost:8080/login -o lg.html
  T=$(python3 -c "import re;print(re.search(r'name=\"_csrf\"[^>]*value=\"([^\"]+)\"',open('lg.html').read()).group(1))")
  curl -s -b cj.txt -c cj.txt -d "username=admin" -d "password=1234" -d "_csrf=$T" http://localhost:8080/login
  ```
  POST 는 `--data-urlencode` 와 `Content-Type: ...; charset=UTF-8` 을 써라 (한글 깨짐 방지).

---

## 4. 지금까지 한 것

### 올라간 PR (axi_project)

| PR | 브랜치 | base | 상태 |
|---|---|---|---|
| [#1](https://github.com/woongscoding/axi_project/pull/1) 엔티티 34개 | `feat/b-schema` | `feat/a-phase0-skeleton` | 리뷰 대기 |
| [#2](https://github.com/woongscoding/axi_project/pull/2) 문제은행 세로 완성 | `feat/b-exam` | `feat/b-schema` | 리뷰 대기 (스택 PR — #1 먼저 머지) |

### 로컬에만 있는 커밋 (아직 push 안 함)

- `feat/b-grading-contract` (`e6fa913`) — **`GradeQueryService`** (A의 이수 판정용 계약, a-requests.md P1-8) + `Exam`/`Assignment`/`CourseAssignment` 에 `@SQLDelete`. 테스트 7건 통과.

### 완성도

```
엔티티 34개          ████████████ 100%
문제은행 슬라이스     ████████████ 100%  (리포지토리→서비스→컨트롤러→Thymeleaf)
GradeQueryService   ████████████ 100%
나머지 8개 슬라이스   ░░░░░░░░░░░░   진행 중/미착수
화면 전환            2 / 41 (admin 기준)
```

---

## 5. 진행 중인 슬라이스 3개 — 코드는 있으나 **실행 검증 0**

에이전트 3개가 세션 한도로 중단됐고(2026-07-27), 이후 코덱스가 `feat/b-support` 를 일부 이어받았다.
**셋 다 컴파일만 통과했고 앱을 띄워본 적이 없다. "동작한다"고 가정하지 마라.**

| 브랜치 | 서비스 | 컨트롤러 | 화면 | 실행 검증 |
|---|---|---|---|---|
| `feat/b-notice` (공지/알림) | O | O 4개 | O 기존 화면 전환 | **X** |
| `feat/b-support` (Q&A/튜터링) | O | O 2개 | **△ 새로 만듦 — 규칙 위반** | **X** |
| `feat/b-survey` (설문) | O | O 2개 | **X 템플릿 자체가 없음** | **X** |

셋 다 베이스는 `e6fa913` 이고 워크트리는 `.claude/worktrees/agent-*` 에 있다.

**알려진 결함 2개:**

1. **설문은 지금 열면 무조건 에러난다.** 컨트롤러가 `survey/admin-list`, `survey/admin-form`,
   `survey/trainee-list`, `survey/trainee-detail` 를 반환하는데 그 템플릿 파일이 하나도 없다.
   기존 `admin-05-attendance/admin-attendance-survey.html`, `-survey-add.html`,
   `trainee/surveys.html`, `survey-detail-page.html` 을 전환해야 한다.
2. **Q&A/튜터링 화면이 두 벌이다.** 코덱스가 `admin/support-*.html`, `trainee/support-*.html`
   8개를 새로 만들었고 원본 `admin-06-support/` 6개는 손도 안 댔다. CLAUDE.md 규칙 위반이다.
   새 파일의 Thymeleaf 로직을 원본으로 이식하고 새 파일은 폐기할 것.

## 6. 절대 건드리면 안 되는 것

| 대상 | 이유 |
|---|---|
| `build.gradle`, `settings.gradle` | 공동 소유 |
| `com.ssa.lms.{common,config,user,course,auth}.**` | A 소유 (`SecurityConfig` 포함) |
| `src/main/resources/templates/fragments/*.html` | 공동 소유. **여러 슬라이스가 동시에 노리는 최대 충돌 지점** |
| `src/main/resources/static/js/contents.js` | A의 페이지 3개가 공유 |

**`fragments/admin.html` 처리 방침:** 각 도메인이 컨트롤러로 전환되면 사이드바 링크를 정적 경로(`/templates/...`)에서 실제 URL로 바꿔야 한다. 슬라이스마다 고치면 충돌하므로, **"바꿔야 할 URL 목록"만 모아뒀다가 한 번에 커밋**한다. 아직 아무도 안 고쳤다 — 이게 미결 과제다.

`SecurityConfig` 경로는 이미 전부 열려 있어 손댈 필요 없다:
`/admin/{evaluation,support,notice,survey}/**` → ADMIN·INSTRUCTOR, `/instructor/proctor/**` → ADMIN·INSTRUCTOR, `/trainee/{exam,assignment,survey,qna}/**` → TRAINEE

---

## 7. 레퍼런스 패턴 — 새 슬라이스는 이걸 그대로 따라해라

문제은행이 완성된 세로 슬라이스 표준이다. **스타일을 새로 발명하지 마라.**

```
com.ssa.lms.exam.repository.QuestionRepository      # 검색 쿼리 + 집계(N+1 회피)
com.ssa.lms.exam.service.QuestionService            # 트랜잭션 경계, 채번, soft delete
com.ssa.lms.exam.dto.{QuestionForm,QuestionListRow,QuestionSearchCond}
com.ssa.lms.web.admin.exam.AdminQuestionController  # 컨트롤러는 web.{admin|instructor|trainee}.{도메인}
com.ssa.lms.exam.LocalQuestionDataInitializer       # local 프로필 시드 (도메인별 파일로 분리)
templates/admin/admin-04-evaluation/admin-evaluation-question-bank.html      # 목록 전환
templates/admin/admin-04-evaluation/admin-evaluation-question-bank-add.html  # 폼 전환
src/test/java/com/ssa/lms/grading/service/GradeQueryServiceTest.java         # 테스트 작성 예시
```

**템플릿 전환 규칙:**
- 기존 클래스명/구조 유지, Thymeleaf 속성만 얹는다
- 헤더/사이드바를 `~{fragments/admin :: header('...')}` / `~{fragments/admin :: sidebar('키')}` 로 교체
- **기존 JS가 `getElementById` 로 쓰는 `id` 속성은 절대 바꾸지 마라.** `name` 만 camelCase 로 바꾼다
- 엔티티를 화면에 직접 넘기지 마라 → DTO 경유 (LAZY 프록시 + 개인정보 노출 방지)
- **컴파일만으로 끝내지 마라. 앱 띄우고 실제 HTTP 요청으로 검증해라.**

---

## 8. 이미 밟은 지뢰 (다시 밟지 마라)

1. **부모/자식 컬렉션 교체 시 유니크 제약 위반.** `clear()` 후 바로 `add()` 하면 Hibernate 가 orphan DELETE 보다 INSERT 를 먼저 내보낸다. → 사이에 `flush()`. `QuestionService.update()` 참고. **설문(문항→보기 2단 중첩)에서 더 크게 터진다.**
2. **`@Lob` + `AttributeConverter` 병용 금지.** `@Column(columnDefinition = "TEXT")` 를 써라. 프로젝트 전체가 이 방식으로 통일돼 있다.
3. **암호화 컬럼은 LIKE 검색 불가.** `Qna.content`, `QnaAnswer.content`, `TutoringMessage.content`, A의 `User.{email,phone,birthDate}` 는 AES-256 암호문이다. 검색은 `title`/`loginId`/`name` 으로.
4. **Thymeleaf 인라인 JS 로 내리는 값은 String 으로.** 화면 JS 가 `data-id` 문자열과 `===` 비교한다. `LocalDate` 도 직렬화 이슈가 있어 문자열로 내렸다 (`QuestionListRow` 참고).
5. **`admin-evaluation-question-bank.html` 은 A와 공유하는 화면**이다. 탭이 영상/문서/강의(A) + 과제/문제/시험(B). `AdminQuestionController.list()` 에 병합 지점을 주석으로 남겨뒀다 — A의 `Content` 가 들어오면 같은 `QuestionListRow` 모양으로 `rows` 에 합치면 된다.

---

## 9. 남은 일

### 슬라이스 (의존 순서 있음)

```
공지/알림 ─┐
Q&A/튜터링 ─┤ 진행 중 (에이전트)
설문      ─┘
과제      ─┐ 독립 — 지금 가능
시험 생성  ─┘
              ↓
        응시/제출  ← ★ 가장 무겁다. 본인인증 + 서버 타이머 + 부정행위 로그
              ↓
   채점/성적  ·  시험 모니터링
              ↓
        대시보드/분석
```

**응시/제출이 내역서 필수 요건이다** — 시험 입장 시 본인인증. A가 `com.ssa.lms.auth.IdentityVerificationService.verify(userId, VerifyRequest)` 를 제공하고 있고, 결과를 `ExamAttempt.identityVerifiedAt` / `identityVerifyMethod` 에 저장하면 된다. **제출 마감 판정은 반드시 서버가 계산한 `ExamAttempt.expiresAt` 으로 해라. 클라이언트 타이머를 믿지 마라.**

### 슬라이스 외 미결

- [ ] **`fragments/admin.html` 메뉴 URL 일괄 교체** (§6 참조)
- [ ] `Notice`, `Survey` 에 `@SQLDelete` — 에이전트 작업과 겹쳐서 일부러 미뤘다
- [ ] MySQL(`dev` 프로필)용 `data.sql` 시드 — 지금은 `local` 프로필 자바 초기화만 있다
- [ ] 문제은행 서버 페이지네이션 — 현재 전체 행을 내려주고 JS 가 페이징. `QuestionService.search(cond, pageable)` 는 이미 있다
- [ ] 저장소 루트 `a-requests.md` 와 `docs/a-requests.md` 중복 정리 (루트 쪽을 지우는 게 맞다)

---

## 10. 개발자 A에게 전달해야 할 것

1. **`static/js/contents.js` 를 건드렸다.** A의 `trainee/contents.html`, `instructor/contents.html`, `courses-detail.html` 이 공유하는 파일이다. 하위호환으로만 바꿨다(`window._serverContentRows || [기존 더미]`) — A 쪽 3개 페이지는 그대로 동작한다. PR #2 본문에 적어뒀다.
2. **`GradeQueryService` 를 제공했다.** A의 이수 로직은 `Grade` 엔티티/리포지토리를 직접 쓰지 말고 이 서비스만 호출할 것. 필요한 시그니처가 더 있으면 B가 추가한다.
3. **`hasAllRequiredGradesConfirmed` 는 성적이 하나도 없으면 `false` 다.** "평가가 없어서 자동 이수" 를 막은 것이다. 평가가 실제로 없는 과정이라면 A의 이수 기준 쪽에서 성적을 요구하지 않도록 설정해야 한다.
4. **`Survey.reflectCompletion`** (설문 이수 반영) 플래그도 A의 이수 로직이 읽어야 한다.
5. A가 `feat/a-auth-login` 브랜치를 새로 올렸다 — 로그인 슬라이스로 보인다. 머지되면 B 브랜치들을 리베이스해야 한다.

미해결 협의 사항은 `docs/a-requests.md` §"처리 현황" 과 §9(P2) 참고. 특히 **`Content` ↔ `Exam` 연결 방향**이 아직 미정이다.

---

## 11. 이어받아서 바로 할 것

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
git worktree list                          # 에이전트 3개 상태 확인
git log --oneline feat/b-notice feat/b-support feat/b-survey
./gradlew test                             # 현재 상태 정상인지
```

그 다음: 에이전트 결과 수거 → 리뷰 → push·PR → `fragments/admin.html` 일괄 교체 → 2차 웨이브(과제 / 시험 생성).
