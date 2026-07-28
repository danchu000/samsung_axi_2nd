# 요구사항 검증 리포트 v2 (별첨4 + 지원서 기재 기능)

- **감사일**: 2026-07-27
- **감사 대상**: 통합 `main` 브랜치 (e7d97b3 — A트랙 4개 + B슬라이스 8개 전부 머지, 72테스트 통과 상태)
- **검증 범위**: 별첨4 비대면 실시간 과정 유의사항(F그룹) + 지원서 기재 기능 명세(G~J그룹, 앨리스 항목). 별표1 법정요건·신고서류 및 3중 모니터링은 범위 제외
- **판정 방법**: 항목별 근거 코드(파일:라인) 확인, 근거 미제시 시 미구현 처리. 그룹별 독립 코드 감사 5회 수행 후 v1 감사(`requirements-audit-2026-07-27.md`)와 교차 대조 — 전 항목 일치 확인
- 경로 표기: Java는 `src/main/java/com/ssa/lms/`, 화면은 `src/main/resources/templates/`, JS/CSS는 `src/main/resources/static/` 기준 상대 경로

---

## 1. 요약

- 전체: **41개** | ✅ 구현: **4** | 🟡 부분구현: **19** | ❌ 미구현: **18** | ⚪ 범위 밖: **0**
- **별첨4(F그룹) 충족 여부 한줄 판정: 조건부 작성 가능 — 유의사항①(커뮤니티=Q&A·1:1튜터링)·②전단(시험·과제 평가)은 근거 기재 가능하나, ②후단 보충방안(F-3)이 완전 미구현이고 ③학습공간·④장비 안내(F-4/F-5)는 모집 전 공개 안내 페이지가 없어 현재 코드로는 이행방안 작성 불가. F-3 배정 기능 + 공개 안내 페이지 1장(난이도 하) 보완 시 작성 가능.**

### 교차 관찰 — 미구현 41건 중 상당수가 4개 공통 인프라 부재에서 파생

1. **엑셀(Apache POI) 미도입** → G-4, H-4, I-1, J-8 엑셀 연쇄 미비 (build.gradle에 web/security/jpa/thymeleaf/validation/pdfbox뿐)
2. **메일(spring-boot-starter-mail) + 스케줄러(@Scheduled) 전무** → H-5, I-6, J-5 이메일 연쇄
3. **WebSocket/STOMP 전무** → G-3, I-4, I-5, J-6 실시간 연쇄
4. **proctor 도메인 절반 미배선** — ExamEventLog만 완비, ExamRecording·ProctorWarning은 엔티티만 존재(Repository/Service/Controller 없음), 감독 화면(monitoring-live 등)은 컨트롤러 미매핑 정적 더미 → G-2, G-3 연쇄

---

## 2. 상세 결과

### F. 비대면 실시간 과정 유의사항 (별첨4)

| ID | 요구사항 요약 | 판정 | 근거 (파일:라인 / 엔드포인트) | 미비점 / 필요 조치 |
|----|--------------|------|--------------------------------|-------------------|
| F-1 | 강사·훈련생 커뮤니티 상시 운영 | 🟡 | Q&A: `support/service/QnaService.java:152`(create)·`:195`(answer)·`:215`(assign), `web/trainee/support/TraineeSupportController.java:42,53,68`, 관리자 답변 `web/admin/support/AdminSupportController.java:91` / 1:1 튜터링: `support/service/TutoringService.java:149`(방 생성)·`:195`(sendMessage, AES-256 저장) | 강사진+훈련생 **공용 자유 커뮤니티 게시판 없음**(Q&A는 1:다, 튜터링은 1:1), Slack 등 SNS 연동 0건, 실시간 아님(폼 POST/redirect) → 공용 게시판 신설 또는 Slack 연동 |
| F-2 | 주기적 성취도 확인(레벨테스트·과제 평가) | ✅ | 시험: `exam/entity/Exam.java` ExamType(UNIT/MIDTERM/FINAL), 응시→자동채점 `exam/service/ExamAttemptService.java:125`(start)·`:379`(finish→AutoGrader) / 과제: `assignment/service/AssignmentGradingService.java:213`(grade→Grade upsert+GradeHistory) | 수단 자체는 완비. 단 반복 시험 자동 생성 스케줄 없음(단발 기간만), 서술형·코딩 수동채점 UI 미배선(G-10 참조) |
| F-3 | 성취 미달자 보충강의·추가 콘텐츠 배정 | ❌ | 없음 — 미이수 식별까지만 존재(`completion/entity/CompletionResult.java` FAIL, `completion/service/CompletionService.java:82-118` 자동판정). 보충/부진/remedial/supplement 키워드 전 소스 0건 | 부진자 산출 쿼리(FAIL·저조자) → 보충 배정 엔티티(content↔user) → 배정 서비스·관리자 화면 신설. content+grading/completion 연계 |
| F-4 | 학습공간 제공 의무 모집·시작 전 안내 수단 | 🟡 | 공지 수단 자체는 완비: `notice/service/NoticeService.java`, `web/admin/notice/AdminNoticeController.java:62`(카테고리·과정타겟·고정·publishedAt), `notice/service/NoticeVisibilityService.java` | **공지가 로그인·수강 후에만 노출** — `config/SecurityConfig.java:43` 공개 경로는 `/`,`/login`,`/signup/**`,`/error`뿐. 모집 단계 예비 훈련생용 공개(permitAll) 안내 페이지 신설 필요(난이도 최하) |
| F-5 | 노트북·웹캠 등 장비 제공 안내 수단 | 🟡 | F-4와 동일 수단(공지/알림) | 장비 제공 안내 콘텐츠·공개 고지 화면 0건(템플릿의 requireWebcam은 시험 감독 설정일 뿐). F-4 공개 안내 페이지에 장비 항목 동시 게재로 해소 가능 |

### G. 평가·테스트

| ID | 요구사항 요약 | 판정 | 근거 | 미비점 / 필요 조치 |
|----|--------------|------|------|-------------------|
| G-1 | 문제세트 랜덤배정·제한시간·성적공개·재응시 설정 | 🟡 | 제한시간 `exam/entity/Exam.java:76-78`(timeLimitMin→`ExamAttemptService.java:199` expiresAt), 재응시 `Exam.java:99-105`(retakeAllowed/maxAttempts, 판정 `ExamAttemptService.java:174-181`), 순서 셔플 `Exam.java:95-97`+`ExamAttemptService.java:500-507` | **복수 문제세트 랜덤 배정 없음** — 출제규칙 확정(`ExamService.materializeRules():212-249`) 후 전원 동일 문항. **성적 공개 여부 토글 없음**(Exam 전 필드 확인). 세트 엔티티+응시 시점 배정, resultReveal 필드 추가 |
| G-2 | 참가자 목록·입퇴장 기록·응시 녹화 저장 | 🟡 | 입퇴장 ✅: `proctor/entity/ExamEventLog.java`(ENTER/EXIT/RESUME append-only), 기록 `ExamAttemptService.java:208,415-419` | 참가자 목록 화면은 하드코딩 더미(`admin-evaluation-monitoring-live.html:352-357`, 렌더 컨트롤러 없음). 녹화는 `ExamRecording` 엔티티만(Repository/Service/Controller 전무), MediaRecorder/업로드 0건 |
| G-3 | 시험 중 채팅·전체공지·개별 경고 | ❌ | `proctor/entity/ProctorWarning.java` 엔티티만 존재(저장·발송 계층 없음), `ExamAttempt.voidAttempt():141-145` 호출처 없음 | WebSocket 기반 감독 메시징(1:1 채팅·broadcast·경고) + do-test.html 수신 UI + proctor 컨트롤러 신설 |
| G-4 | 결과 엑셀 일괄 다운로드 | ❌ | POI/Workbook/CSV export 전 소스 0건, 시험 결과 조회 컨트롤러 자체 부재 | POI 도입 + `/admin/.../exams/{id}/results/export` 엔드포인트 (공통 엑셀 모듈로 H-4·I-1·J-8 동시 해소) |
| G-5 | 사전 모의 테스트 | ❌ | practice/모의 모드 없음(`online-test.js:7` mockExams는 더미 변수명, `AttemptStatus`에 연습 상태 없음) | Exam에 practice 플래그 + 성적 미반영 모드 |
| G-6 | 레이아웃 커스터마이징·잔여시간 표시 | 🟡 | 잔여시간 카운트다운 ✅: `templates/trainee/do-test.html:66-69,332-343`(서버 remainSeconds 기반) — 단 위치는 우측 상단 | 문항/코드창 리사이즈·레이아웃 조절 없음(코딩 문항도 일반 textarea `do-test.html:274-290`) |
| G-7 | QR 원격 신분증 확인 | ❌ | 본인인증은 비밀번호 전용: `auth/PasswordIdentityVerificationService.java:31-47`, `VerifyRequest.java:6`("현재 PASSWORD만 지원") | QR 발급→모바일 신분증 업로드→운영진 승인→시험 중/후 조회 플로우 신설(자체 구현 가능, `IdentityVerificationService` 확장점 존재) |
| G-8 | 화면·웹캠 공유 미허용 시 진입 차단·듀얼모니터 통제 | ❌ | getUserMedia/getDisplayMedia 전 소스 0건. `Exam.requireWebcam:126-128`은 뷰까지만 내려가는 **미사용 플래그**(do-test.html에 소비 JS 없음) | do-test.html에 미디어 권한 게이트 + `screen.isExtended` 검사 → 미허용 시 시작 차단 |
| G-9 | 부정행위 감지(전체화면·특수키·다중모니터·복붙) | 🟡 | 탭전환 ✅ `do-test.html:357-364`(visibilitychange/blur→TAB_BLUR), 복사·붙여넣기 차단+감지 ✅ `:365-369`, 서버 기록 `TraineeExamController.java:153-173`→`ExamEventLogService`(심각도 서버 결정) | **전체화면 해제·특수키·다중모니터 감지 미구현** — `ExamEventLog.EventType.FULLSCREEN_EXIT:80` enum만 있고 클라이언트가 발신 안 함. fullscreenchange/keydown/screen.isExtended 리스너 추가(기존 /events 재사용) |
| G-10 | 자동채점 그레이더(자동/수동/혼합·상대점수·주관식) | 🟡 | 객관식 `exam/service/AutoGrader.java:47-53`, 단답형 `:55-74`(대소문자·부분점수), 제출 시 실시간 채점 `ExamAttemptService.finish():379-422`, 자동/수동 배점 분리 `Exam.autoScore/manualScore`+검증 `ExamService.validate():286-308` | **수동 채점 서비스·화면 전무**(코딩은 0점 처리 `AutoGrader.java:41`, 서술형 manualPending 보류 후 종착지 없음). **시험 성적→Grade 승격 코드 없음**(이수판정이 시험점수 못 읽음). 코딩 상대점수 채점 없음. ExamGradingService+채점 화면 배선 필요 |

### H. 콘텐츠·설문·수요반영

| ID | 요구사항 요약 | 판정 | 근거 | 미비점 / 필요 조치 |
|----|--------------|------|------|-------------------|
| H-1 | 콘텐츠 라이브러리(등록·불러오기·일괄반영) | 🟡 | 등록·DB화·업로드·차시 연결 ✅: `content/entity/Content.java:34-152`, `content/service/ContentService.java:90-146` | **참조 공유 구조 아님** — `Content.java:40-42` course_id 필수 단일 FK(콘텐츠 1건=과정 1개 종속), 다대다 링크·라이브러리 엔티티 없음 → "원본 수정 시 연결 과목 일괄 반영" 성립 불가. Content를 과정 독립 원본으로 분리+content_link 신설 |
| H-2 | 콘텐츠 버전 관리(수정 이력 기록·조회) | ❌ | 없음 — `ContentService.update():121-135` 덮어쓰기(파일 교체 시 구파일 삭제 `:133`), BaseEntity updatedAt 최신값만 | content_version(스냅샷·changeNote) 엔티티 + update 시 적재 + 조회 화면 |
| H-3 | 설문 문항 유형(객관식 단일/중복·단답·주관식) | ✅ | `survey/entity/SurveyQuestion.java:92-101` SINGLE/MULTI/SCALE/TEXT, 검증 `survey/service/SurveyService.java:289-343`, 보기 동적 추가·필수 여부·문항 교체 편집 `:150-162` | 경미: 단답/서술이 TEXT 1종으로 통합(구분 없음) |
| H-4 | 설문 결과 관리(훈련생별 진행률·문항별 답변·엑셀) | 🟡 | 설문 단위 응답률 ✅: `SurveyService.search():79-99`+`SurveyResponseRepository.countBySurveyIds:33-39`, 답변 정규화 저장(SurveyResponse/SurveyAnswer) | 관리자 결과 조회 엔드포인트 전무(`AdminSurveyController`는 CRUD만), 문항별 집계 쿼리·훈련생별 제출현황·엑셀 없음(POI 미도입). `/{id}/results`+`/results/excel` 신설 |
| H-5 | 미참여자·연계기업 참여현황 리포트 자동 발송 | ❌ | 메일 인프라 0건(starter-mail/JavaMailSender 없음), @Scheduled 0건, 미참여자 차집합 쿼리 없음, 연계기업 엔티티 없음(재직회사 입력필드뿐) | 메일+스케줄러 공통 인프라 구축 후 미참여자 산출 쿼리+발송주기 설정+수신처 관리 |

### I. 학습 데이터·대시보드·맞춤 관리

| ID | 요구사항 요약 | 판정 | 근거 | 미비점 / 필요 조치 |
|----|--------------|------|------|-------------------|
| I-1 | 관리자 학습 대시보드+엑셀 | ❌ | `web/ModuleHomeController.java:16-19`가 정적 `admin/index` 임시 렌더(주석 명시), `static/js/dashboard.js:2-78` 전부 하드코딩 더미, 집계 서비스·dashboard 패키지·POI 없음 | 집계 서비스 신설(`ProgressQueryService`·grading·attendance·exam 취합) + 시각화 바인딩 + 엑셀 export |
| I-2 | 분반 기능 | ❌ | Section/분반 엔티티 0건(`course/entity/Course.java:44-46` cohort는 "기수" 라벨) | Section 도메인 신설(Enrollment에 FK)+분반별 대시보드·리포트 |
| I-3 | 훈련생 내 대시보드(수강과목·이어서 학습·출석) | 🟡 | 수강과목 ✅ `course/web/MyCourseController.java:25-30`, 이어보기 ✅ `content/web/TraineeLearningController.java:56-63`+`Progress.lastPositionSeconds:46`+`play-video.html:110-119` | 훈련생 홈(index) 정적 미배선, continue-learning.html 컨트롤러 없음(죽은 링크 20여 곳), **본인 출석 화면 없음**(출결은 /admin만) |
| I-4 | 실시간 튜터링(에디터 반영·코드 첨삭·1:1 채팅) | 🟡 | 1:1 채팅 백엔드 ✅: `TutoringService.java:194-220`(AES 저장·당사자 검증·읽음처리) | **실시간 아님**(폼 POST→redirect, WebSocket 0건), 실습 에디터 연동·원격 코드 첨삭 전무 |
| I-5 | 헬프센터(코드 드래그→질문→실시간 답변) | 🟡 | Q&A 백엔드 ✅(QnaService+컨트롤러) | 코드 드래그→질문 스니펫 전달 0건, 질문 모달 JS는 클라이언트 더미(`static/js/trainee/qna-modal-q.js:134-144` QNA_STORE push만), 실시간 채팅 아님 |
| I-6 | 학습알림(부진자 이메일·24h/1h 리마인드) | ❌ | 인앱 알림만 `notice/service/NotificationService.java:105-130`. sendAt 예약 필드는 있으나 발송 워커 없음(`:126-128` SENT만 fan-out) | 스케줄러+메일 인프라 + 부진자(진도 임계치) 쿼리 + 수업 전 리마인드 잡 |
| I-7 | AI 챗봇(GPT 코드 질의응답) | ❌ | `static/js/chatbot.js:31-37` — setTimeout 목업 응답("곧 답변 예정"). LLM/HTTP 클라이언트 의존성 0건, 백엔드 없음 | LLM API 키 발급 후 chatbot 컨트롤러/클라이언트 신설, JS 목업부를 fetch로 교체 |
| I-8 | 이탈 예측 점수(EPS) | ❌ | 스코어링 로직·엔티티·분포 시각화·필터 0건(`SurveyListRow` 등 매치는 무관 UI 라벨) | 학습 데이터(진도·출결·성적) 룰 기반 스코어부터 신설, 대시보드에 분포·필터 |
| I-9 | 이수증(기준 설정→자동 이수·발급, 에디터) | 🟡 | 기준 설정 `completion/service/CompletionService.java:63-72`, 자동판정 `:82-118`, 확정 `:122-125`, PDF 발급 `completion/service/CertificateService.java:42-65`(openhtmltopdf·한글 폰트)+`CompletionAdminController.java:88-96` | **이수증 에디터 없음** — 서식이 Java 문자열 하드코딩(`CertificateService.java:91-128`). CertificateTemplate 엔티티(HTML 본문 저장)로 분리 |

### J. 강의·출결·플랫폼 일반

| ID | 요구사항 요약 | 판정 | 근거 | 미비점 / 필요 조치 |
|----|--------------|------|------|-------------------|
| J-1 | 멀티 디바이스/반응형 | 🟡 | viewport meta 전 템플릿, `@media` 18개 CSS 파일 29회 | 반응형 프레임워크 미사용, 관리자 화면 고정 사이드바(고정폭) — 핵심 화면 반응형 보강 |
| J-2 | 온라인 코딩 실습(브라우저 IDE·다중언어·Jupyter·비율조절) | ❌ | Monaco/CodeMirror/Jupyter/샌드박스/실행기 0건. `trainee/play-practice.html:147-181`은 파일 제출 폼일 뿐 | 외부 실행 샌드박스(Judge0 등)+에디터 도입 — 대형 과제 |
| J-3 | 콘텐츠 유형(VOD·PDF·퀴즈·마크다운·실습)+완료 기록 | 🟡 | `content/entity/ContentType.java:8-11` **VIDEO/DOCUMENT 2종만**. 완료 기록은 유형별 분산(Progress.completed / ExamAttempt / Submission.status) | 퀴즈·마크다운·실습 콘텐츠 유형 부재(시험·과제는 별도 도메인, 콘텐츠 흐름 미통합). ContentType 확장+완료 기록 통합 |
| J-4 | VOD 재생+시청 기반 개인 진도 | ✅ | `content/entity/Progress.java:76-83`(updateVideoProgress, 이어보기·진도하락 방지·90% 완료), `content/web/ProgressApiController.java:31-42`, 사용자×콘텐츠 유니크 `:20-22` | 경미: 재생 위치 조작 방지 검증 없음 |
| J-5 | 게시판(전체+과목별 공지·팝업 설정·이메일 발송) | 🟡 | 전체/과정별 공지 ✅ `notice/entity/Notice.java:50-53`+`NoticeService.searchPublished():59-64` | **팝업 노출 설정 없음**(pinned=상단 고정뿐), **이메일 자동 발송 없음**(NoticeService가 알림 트리거 안 함, 메일 인프라 부재). popup 필드+홈 조회, 게시 시 @Async 메일 훅 |
| J-6 | 화상강의(플랫폼 내·화면공유·레이아웃·대규모) | ❌ | WebRTC/Zoom/LiveKit/Agora/Jitsi 0건. `play-class.html:153-156` 정적 목업(라우팅도 안 됨 — `TraineeLearningController.play():62`는 video/document만 분기) | 자체 구축 비현실적 — Zoom/LiveKit 연동 권장(현재 연동 코드 0건) |
| J-7 | 게이미피케이션(경험치·랭킹) | ❌ | XP/포인트/랭킹/뱃지 관련 0건 | gamification 도메인 신설(출결·진도·완료 이벤트 소비) |
| J-8 | 데이터 기반 출결(자동 기록·인정시간·출석부·엑셀) | 🟡 | 접속 이력 기반 자동 출석 ✅ `attendance/service/AttendanceService.recalculate():51-89`(차시일 LOGIN→PRESENT/ABSENT), 수동 보정 `:92-97`, 출석부 매트릭스 `attendance/web/AttendanceAdminController.java:38-60`, 출석률 `:103-110` | **퇴실·체류시간 없음**(Attendance에 accessCount만 `:63-65`, LOGOUT 미기록·미사용), **인정 시간 설정 없음**(1회 로그인=출석), **엑셀 없음**. LOGOUT 기록→체류 산정→인정 분 임계 판정+export |
| J-9 | 안면인식 출석·자리이탈 이력 | ❌ | 출결 도메인 face 0건. `ExamEventLog.MULTI_FACE/NO_FACE:83-86`는 시험 감독용 enum일 뿐이며 그것도 감지 로직 미구현 | 외부 안면인식 솔루션 연동 필요 |
| J-10 | 부정수강 방지(본인인증·OTP·공단 연동·설치 고지) | ❌ | 비밀번호 재확인 스텁만(`PasswordIdentityVerificationService.java:31-47`, SMS/PASS는 TODO 주석). OTP·HRD 연동·CI/DI 식별값(`SignupForm.java:19-63`에 없음)·설치 고지 화면 전부 0건 | PASS/SMS 사업자 계약(외부 병목) 후 IdentityVerificationService 구현체 추가, 설치 사전 고지 화면은 즉시 가능(난이도 하) |
| J-11 | 과제 제출·1:1 피드백·영구 보존 | ✅ | 재제출 append-only `assignment/entity/Submission.java:52-54`(attemptNo 유니크), 피드백 `Feedback.java:46-47`(visibleToTrainee)+제출 페이지 확인(`TraineeAssignmentCard.java:27`), soft-delete·물리삭제 없음 → 수료 후 열람 가능 | 경미: 훈련생 피드백 전용 상세 페이지는 없음(목록 카드 경유) |
| J-12 | 취업지원 데이터 활용(추출 가능성) | 🟡 | 원천 데이터 존재: 과목별 점수 `grading`(Grade/GradeHistory/GradeQueryService), 선호·만족 `survey`(SurveyResponse/SurveyAnswer) — PostgreSQL 영속 | 추출(export) 기능 없음 — 별도 취업 시스템 부재 전제이므로 "데이터 추출 가능성"은 충족, 조회·export 엔드포인트만 추가하면 완결 |

---

## 3. 미구현 항목 우선순위

### 1순위 — F그룹 (별첨4 이행방안·조치사항 직접 기재 항목)

1. **F-4/F-5 공개 안내 페이지** — 왜: 유의사항③·④는 "모집 및 과정 시작 전 안내"가 요건인데 현재 공지가 로그인 후에만 노출. 제안: permitAll 정적 안내 페이지 1장(학습공간+장비 제공 동시 게재), SecurityConfig 공개 경로 추가. 난이도 **하**, 0.5일.
2. **F-3 부진자 보충 배정** — 왜: 유의사항② 후단 "보충강의 등 보완방안"의 유일한 코드 근거가 될 기능. 제안: CompletionResult.FAIL/성적 저조자 산출 쿼리 → 보충 콘텐츠 배정 엔티티·서비스 → 관리자 배정 화면 + 훈련생 노출. 난이도 **중**, 2~3일.
3. **F-1 커뮤니티 보강** — 왜: "상시 운영 커뮤니티"는 Q&A·1:1보다 공용 소통 공간을 요구. 제안: 공용 게시판(글+댓글) 신설(notice/support 패턴 재사용) 또는 Slack 워크스페이스 연동 안내로 대체 기재. 난이도 **중하**, 1~2일.

### 2순위 — 현장점검 시연 요구 가능성 높은 기능 (출결·부정수강·시험)

4. **J-8 출결 완성(퇴실·체류·인정시간·엑셀)** — 왜: 출결은 현장점검 단골 시연 항목, "설정 기반 자동 처리"가 지원서 문구. 제안: LOGOUT 기록(AccessLogEventListener 확장)→체류시간 산정→인정 분 설정·판정, POI export. 난이도 **중**, 2~3일.
5. **G-9/G-8 부정행위 감지·환경 통제 완성** — 왜: 서버 파이프라인(EventType·/events·심각도)이 이미 있어 클라이언트 리스너만 추가하면 됨. 제안: do-test.html에 fullscreenchange·keydown·screen.isExtended·getUserMedia 게이트. 난이도 **중하**, 1~2일.
6. **G-10 시험 수동채점 배선 + Grade 승격** — 왜: 현재 서술형·코딩 시험은 성적이 확정되지 않아 이수 판정에도 미반영(기능 단절). 제안: ExamGradingService(수동채점→applyScore→Grade upsert)+admin-evaluation-test-grading.html 배선. 난이도 **중**, 2~3일.
7. **엑셀 공통 모듈(POI)** — 왜: G-4·H-4·I-1·J-8 네 항목 동시 해소, 가성비 최상. 제안: common/excel 유틸+각 도메인 export 엔드포인트. 난이도 **중**, 인프라 1회+항목당 0.5일.
8. **G-1 보완(성적 공개 토글=하, 훈련생별 세트 랜덤 배정=중)**, **G-5 모의 테스트**(practice 플래그, 하~중), **G-7 QR 신분증**(업로드+승인 플로우 자체 구현, 중), **G-2/G-3 proctor 배선**(Recording/Warning repository·service·컨트롤러+WebSocket, 중상), **J-10 설치 고지 화면**(하 — 본인인증·OTP 자체는 외부 계약 병목).

### 3순위 — 나머지

9. **메일+스케줄러 공통 인프라** — H-5·I-6·J-5 세 항목 커버(SMTP 계정 1개 선행). 난이도 **중**, 인프라 1회+항목당 1일.
10. **H-1 콘텐츠 라이브러리 구조 전환 + H-2 버전 관리** — Content 참조 분리는 스키마 변경(A·B 합의 필요). 난이도 **중**, 3~4일.
11. **I-1 관리자 대시보드 + I-3 훈련생 출석 화면 배선 + I-8 룰 기반 EPS** — 집계 서비스 1개 신설로 공용. 난이도 **중**, 3~5일.
12. **I-2 분반, I-9 이수증 에디터, J-3 콘텐츠 유형 확장, J-5 팝업, J-7 게이미피케이션, J-1 반응형 보강, J-12 export, H-3 단답/서술 분리** — 각 하~중.
13. **대형(외부 도입 전제)**: J-2 브라우저 IDE > J-6 화상강의 > I-7 AI 챗봇 > J-9 안면인식, G-2 녹화 저장. 주 단위 이상 또는 솔루션 선정 과제.

---

## 4. 외부 서비스로 대체 가능한 항목

| 항목 | 대체 서비스 예 | 현재 연동 코드 | 비고 |
|---|---|---|---|
| J-6 화상강의 | Zoom / LiveKit / Agora | **없음** | 자체 WebRTC 구축 비현실적 — 연동이 정석. "설치 없이 플랫폼 내" 요건은 Zoom Web SDK·LiveKit 임베드로 충족 가능 |
| J-2 코딩 실습 | Judge0(실행) + Monaco(에디터) / JupyterHub / 구름IDE류 | **없음** | AI 프레임워크 실행환경까지 요구되므로 컨테이너 기반 외부 솔루션 권장 |
| I-7 AI 챗봇 | OpenAI / Claude API | **없음** (chatbot.js 목업만) | API 키만 발급되면 코드는 소규모(컨트롤러+클라이언트+JS fetch 교체) |
| F-1 커뮤니티 | Slack / Discord 연동 | **없음** | 자체 게시판 신설과 양자택일. 이행방안에는 "자체 Q&A·튜터링 + Slack 병행" 기재 가능 |
| J-9 안면인식 | AWS Rekognition / face-api.js | **없음** | 자체 구현 비권장 |
| J-10/G-7 본인인증·OTP | PASS / NICE / 산업인력공단 HRD API | **없음** (IdentityVerificationService 확장점만 존재) | 사업자 계약·공단 스펙 확보가 병목 (코드 확장 구조는 준비됨) |
| H-5/I-6/J-5 메일 발송 | SMTP(Gmail/AWS SES) | **없음** (starter-mail 미도입) | 계정 1개 + 스케줄러 인프라 1회 구축으로 3항목 해소 |
| G-2 응시 녹화 | 미디어 서버(외부 스토리지/스트리밍) | 엔티티(ExamRecording)만 존재 | 브라우저 MediaRecorder→업로드는 자체 가능하나 저장·재생 인프라는 외부 권장 |

---

*본 리포트는 main(e7d97b3) 기준 정적 코드 감사이며, 그룹별 독립 감사 5회 + v1 감사 문서 교차 대조로 작성됨. 런타임 동작 검증은 별도 필요.*
