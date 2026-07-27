# 개발자 B 도메인 — 엔티티 ↔ 테이블 ↔ 화면 매핑표

> 작성: 개발자 B · 브랜치 `feat/b-schema`
> 소스: `src/main/java/com/ssa/lms/{exam,proctor,assignment,grading,notice,support,survey}/entity/`

매핑은 두 방향으로 읽는다.

- **엔티티 → 테이블**: JPA `@Table(name=...)` / `@Column(name=...)`. 컬럼명은 자동 변환에 맡기지 않고 전부 명시했다. IA 엑셀 "데이터 정리" 시트와 눈으로 대조해야 하기 때문.
- **엔티티 → 화면**: 엔티티를 Thymeleaf에 직접 넘기지 않는다. 반드시 DTO를 거친다. (LAZY 프록시 문제 + 개인정보 노출 방지)

```
Entity  --(Repository)-->  Service  --(DTO)-->  Controller  --(Model)-->  Thymeleaf
```

---

## 1. 시험 — `com.ssa.lms.exam.entity`

| 엔티티 | 테이블 | 담당 화면 / 근거 |
|---|---|---|
| `Question` | `question` | `admin-04-evaluation/admin-evaluation-question-bank.html`, `-add.html` · IA "데이터 정리" 1행 |
| `QuestionChoice` | `question_choice` | 위 화면의 `choice_1~4` (정규화) |
| `Exam` | `exam` | `admin-evaluation-test.html`, `-add.html`, `-update.html` · `static/js/exams.js` `testList` · `trainee/online-test.js` `mockExams` |
| `ExamQuestion` | `exam_question` | `admin-evaluation-test-add.html` 수동 출제(`manualQuestionList`) |
| `ExamQuestionRule` | `exam_question_rule` | `addRuleModal` (대/중/소분류 + 태그 + 총 문항수) |
| `ExamQuestionRuleDifficulty` | `exam_question_rule_difficulty` | `addRuleDifficultyLevel1~3` / `Count1~3` |
| `ExamAttempt` | `exam_attempt` | `trainee/online-test.js` (`attemptsUsed/Total`, `status`) · 본인인증 요건 |
| `Answer` | `answer` | 응시 화면 답안 임시저장/제출 |
| `Difficulty` (enum) | — | `easy/medium/hard` — 문제·과제 공용 |

## 2. 시험 모니터링 — `com.ssa.lms.proctor.entity`

| 엔티티 | 테이블 | 담당 화면 |
|---|---|---|
| `ExamEventLog` | `exam_event_log` | `admin-evaluation-monitoring.html`, `-live.html` (입·퇴장/이상행위) |
| `ProctorWarning` | `proctor_warning` | 위 화면의 경고 발송 |
| `ExamRecording` | `exam_recording` | `admin-04-evaluation/recordings.html` |

## 3. 과제 — `com.ssa.lms.assignment.entity`

| 엔티티 | 테이블 | 담당 화면 / 근거 |
|---|---|---|
| `Assignment` | `assignment` | 과제 정의(재사용 원본) · IA "데이터 정리" 2행 |
| `CourseAssignment` | `course_assignment` | `admin-evaluation-assignment.html`, `-add.html` · IA 3행 **컬럼명 그대로** |
| `AssignmentCriteria` | `assignment_criteria` | `-add.html` 의 `criteria[]` |
| `Submission` | `submission` | `assignment-grading.html` · `assignments-grading.js` `gradingStudentData` |
| `SubmissionFile` | `submission_file` | 문서 제출 첨부 |
| `Feedback` | `feedback` | 피드백 작성 (append-only) |

## 4. 채점/성적 — `com.ssa.lms.grading.entity`

| 엔티티 | 테이블 | 담당 화면 |
|---|---|---|
| `Grade` | `grade` | `admin-evaluation-result.html`, `result-grading.html` · `result-grading.js` `studentTableData` |
| `GradeHistory` | `grade_history` | `assignments-grading.js` `gradingHistoryData` (변경 이력 탭) |

## 5. 공지/알림 — `com.ssa.lms.notice.entity`

| 엔티티 | 테이블 | 담당 화면 |
|---|---|---|
| `NoticeCategory` | `notice_category` | 공지 분류 코드 (기존 하드코딩 문자열 대체) |
| `Notice` | `notice` | `admin-07-notice/admin-notice.html`, `notice-add.html`, `notice-detail.html` |
| `NoticeAttachment` | `notice_attachment` | `noticeAttachments` |
| `Notification` | `notification` | `admin-07-notice/admin-alarm.html`, `admin-alarm-add.html` |
| `NotificationRecipient` | `notification_recipient` | 수신자별 읽음 상태 (`markReadBtn`) |

## 6. 소통 — `com.ssa.lms.support.entity`

| 엔티티 | 테이블 | 담당 화면 |
|---|---|---|
| `Qna` | `qna` | `admin-06-support/admin-support-qna.html`, `admin-support-response.html` · `trainee/qna.js` |
| `QnaAnswer` | `qna_answer` | 답변 |
| `TutoringRoom` | `tutoring_room` | `admin-support-tutoring.html`, `tutoring-detail.html` |
| `TutoringMessage` | `tutoring_message` | `admin-support-chat-monitoring.html` · IA 의 `chat_log` |

## 7. 설문 — `com.ssa.lms.survey.entity`

| 엔티티 | 테이블 | 담당 화면 |
|---|---|---|
| `Survey` | `survey` | `admin-05-attendance/admin-attendance-survey.html`, `-add.html` · IA 8행 |
| `SurveyQuestion` | `survey_question` | `questionModal` (`qText`, `qType`) |
| `SurveyChoice` | `survey_choice` | `addChoice` 동적 보기 |
| `SurveyResponse` | `survey_response` | 응답 1건 |
| `SurveyAnswer` | `survey_answer` | 문항별 응답값 |

## 8. 대시보드/분석 — `com.ssa.lms.dashboard`

**신규 테이블 없음.** 위 테이블들의 집계 쿼리로 처리한다.

---

## 설계 결정 기록

| # | 결정 | 이유 |
|---|---|---|
| 1 | `choice_1~4` 플랫 컬럼 → `question_choice` 정규화 | 보기 수 가변, 오답률 통계 쿼리 단순화 |
| 2 | `grade` 는 B 소유. A 의 이수 판정은 `GradeQueryService` 로 조회 | 채점 로직이 B에 있고, 양방향 의존을 막기 위함 |
| 3 | 공지 분류를 문자열 → `notice_category` 코드 테이블 | 기수/훈련유형 추가 때마다 코드 수정하지 않기 위함 |
| 4 | 재제출/재응시는 덮어쓰지 않고 `attempt_no` 를 올린 새 행 | 3년 보존 + 채점 이력 추적 |
| 5 | `exam_event_log`, `feedback`, `grade_history` 는 append-only | 증빙 무결성 |
| 6 | `ExamAttempt.expiresAt` 을 서버가 계산해 저장 | 클라이언트 타이머 조작 방지 |
| 7 | `Grade.evalRefId` 는 FK 없는 다형 참조 | exam / course_assignment 양쪽을 가리켜야 함. 유효성은 서비스에서 검증 |

## 아직 안 정한 것

- **녹화물(`exam_recording`) 보존기간** — 개인영상정보라 3년 일괄 보존 대상이 아닐 수 있다. 기관 확인 필요.
- **설문 익명 + 이수반영 조합** — 서비스 계층에서 막기로 했으나 화면에서도 막을지 미정.
