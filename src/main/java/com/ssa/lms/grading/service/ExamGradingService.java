package com.ssa.lms.grading.service;

import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.entity.ExamAttempt.AttemptStatus;
import com.ssa.lms.exam.repository.AnswerRepository;
import com.ssa.lms.grading.dto.*;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.entity.GradeHistory;
import com.ssa.lms.grading.repository.ExamGradingRepository;
import com.ssa.lms.grading.repository.GradeHistoryRepository;
import com.ssa.lms.grading.repository.GradeRepository;
import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 시험 수동 채점 · 성적 확정 · Grade 반영.
 *
 * <p><b>지켜야 하는 불변식</b></p>
 * <ol>
 *   <li><b>자동 채점 결과는 건드리지 않는다.</b> 객관식·주관식은 제출 시점에 {@code AutoGrader} 가
 *       {@code Answer.gradeAuto()} 로 이미 점수를 박아뒀다. 수동 채점은
 *       {@code Question.isAutoGradable() == false} 인 문항만 대상이고, 총점은 늘
 *       <b>자동 점수 + 수동 점수</b> 로 다시 합산한다 (덮어쓰지 않는다).</li>
 *   <li><b>합격 확정은 수동 채점이 끝난 뒤에만.</b> 남은 수동 문항이 있으면 passScore 를 넘기지 않아
 *       {@code passed} 가 null(미정)로 남는다.</li>
 *   <li><b>확정 후 점수 변경은 사유 없이 불가.</b> 사유와 함께 {@code GradeHistory} 를 남긴다.
 *       내역서 증빙 요건이라 예외를 두지 않는다.</li>
 *   <li><b>강사는 담당 과정만.</b> {@code CourseQueryService.isInstructorOf} 로만 판정한다.</li>
 * </ol>
 *
 * <p>문항 자체는 절대 수정하지 않는다. {@code ExamService.ensureNotAttempted()} 가 응시 기록이 있는
 * 시험의 문항 변경을 막고 있고, 채점 중 문항이 바뀌면 이미 채점한 답안의 배점 근거가 사라진다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamGradingService {

    /* 화면 필터(#filterGradingStatus)의 값과 1:1 이어야 한다. 바꾸면 필터가 조용히 죽는다. */
    public static final String ST_UNGRADED = "미채점";
    public static final String ST_GRADING = "채점중";
    public static final String ST_GRADED = "채점완료";
    public static final String ST_CONFIRMED = "확정";

    /**
     * {@code Question.isAutoGradable()} 이 true 를 돌려주는 유형.
     * JPQL 집계에서 인스턴스 메서드를 부를 수 없어 여기에 한 번만 적어두고 파라미터로 넘긴다.
     * <b>{@code Question.isAutoGradable()} 을 바꾸면 이 목록도 같이 바꿔야 한다.</b>
     */
    private static final List<Question.QuestionType> AUTO_TYPES =
            List.of(Question.QuestionType.MULTIPLE_CHOICE, Question.QuestionType.SHORT_ANSWER);

    /** 채점 대상 회차 — 제출된 것만. 진행중/무효 회차는 채점하지 않는다. */
    private static final List<AttemptStatus> GRADABLE_STATUSES =
            List.of(AttemptStatus.SUBMITTED, AttemptStatus.AUTO_SUBMITTED);

    /** 채점 목록에 뜨는 시험 상태. 작성중(DRAFT)은 채점 대상이 아니다. */
    private static final List<Exam.ExamStatus> LISTED_STATUSES = List.of(
            Exam.ExamStatus.SCHEDULED, Exam.ExamStatus.OPEN,
            Exam.ExamStatus.CLOSED, Exam.ExamStatus.ARCHIVED);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExamGradingRepository examGradingRepository;
    private final GradeRepository gradeRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final AnswerRepository answerRepository;
    private final CourseQueryService courseQueryService;

    /* ===================== 1. 시험 채점 목록 ===================== */

    /**
     * 채점 대상 시험 목록.
     *
     * @param admin true 면 전체, false(강사)면 담당 과정의 시험만 돌려준다.
     */
    public List<ExamGradingRow> examList(Long courseId, String instructor, String keyword,
                                         Long viewerId, boolean admin, String urlPrefix) {
        List<Exam> exams = examGradingRepository.findExams(
                courseId, LISTED_STATUSES, blankToNull(instructor), blankToNull(keyword));

        if (!admin) {
            exams = exams.stream().filter(e -> canGrade(e, viewerId, false)).toList();
        }
        if (exams.isEmpty()) {
            return List.of();
        }

        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, Integer> manualTotals = toIntMap(
                examGradingRepository.countManualQuestions(examIds, AUTO_TYPES));

        // 시험 → (사용자별 최신 제출 회차)
        Map<Long, List<ExamAttempt>> latestByExam = new LinkedHashMap<>();
        List<ExamAttempt> all = examGradingRepository.findAttempts(examIds, GRADABLE_STATUSES);
        for (Map.Entry<Long, List<ExamAttempt>> e : groupByExam(all).entrySet()) {
            latestByExam.put(e.getKey(), latestPerUser(e.getValue()));
        }
        Map<Long, Integer> manualGraded = manualGradedCounts(
                latestByExam.values().stream().flatMap(List::stream).map(ExamAttempt::getId).toList());

        List<ExamGradingRow> rows = new ArrayList<>();
        int no = 0;
        for (Exam exam : exams) {
            List<ExamAttempt> attempts = latestByExam.getOrDefault(exam.getId(), List.of());
            int manualTotal = manualTotals.getOrDefault(exam.getId(), 0);

            int submitted = attempts.size();
            int graded = 0;
            for (ExamAttempt a : attempts) {
                if (isGradingDone(manualTotal, manualGraded.getOrDefault(a.getId(), 0))) {
                    graded++;
                }
            }
            int ungraded = submitted - graded;
            int enrolled = exam.getCourse() == null
                    ? submitted
                    : courseQueryService.findUserIdsByCourseId(exam.getCourse().getId()).size();

            rows.add(new ExamGradingRow(
                    ++no,
                    String.valueOf(exam.getId()),
                    exam.getCourse() == null ? "-" : exam.getCourse().getCourseName(),
                    exam.getCourse() == null ? "-" : "COURSE-" + exam.getCourse().getId(),
                    exam.getExamName(),
                    exam.getInstructor() == null ? "-" : exam.getInstructor().getName(),
                    "test",
                    format(exam.getWindowStart(), DATE),
                    format(exam.getWindowStart(), TIME) + "부터",
                    format(exam.getWindowEnd(), DATE),
                    format(exam.getWindowEnd(), TIME) + "까지",
                    submitted,
                    Math.max(0, enrolled - submitted),
                    listStatus(submitted, ungraded),
                    urlPrefix + "/exams/" + exam.getId(),
                    graded,
                    ungraded));
        }
        return rows;
    }

    /* ===================== 2. 시험별 채점 현황 + 응시자 목록 ===================== */

    public ExamGradingSummary summary(Long examId, Long viewerId, boolean admin, String urlPrefix) {
        Exam exam = requireExam(examId);
        // 목록에서 걸러지긴 하지만 URL 의 examId 만 바꿔서 들어올 수 있으므로 여기서 막는다.
        // 이 화면은 응시자 실명·점수를 보여주기 때문에 canGrade=false 로 내려보내는 것으로는 부족하다.
        ensureCanGrade(exam, viewerId, admin);
        boolean canGrade = true;

        List<ExamAttempt> attempts = latestPerUser(
                examGradingRepository.findAttempts(List.of(examId), GRADABLE_STATUSES));
        int manualTotal = toIntMap(examGradingRepository.countManualQuestions(List.of(examId), AUTO_TYPES))
                .getOrDefault(examId, 0);
        Map<Long, Integer> manualGraded =
                manualGradedCounts(attempts.stream().map(ExamAttempt::getId).toList());
        Map<Long, Grade> grades = gradesByUser(examId);

        int graded = 0;
        int confirmed = 0;
        for (ExamAttempt a : attempts) {
            Grade g = grades.get(a.getUser().getId());
            String status = resolveStatus(g, manualTotal, manualGraded.getOrDefault(a.getId(), 0));
            if (ST_CONFIRMED.equals(status)) {
                confirmed++;
                graded++;
            } else if (ST_GRADED.equals(status)) {
                graded++;
            }
        }
        int enrolled = exam.getCourse() == null
                ? attempts.size()
                : courseQueryService.findUserIdsByCourseId(exam.getCourse().getId()).size();

        String status = listStatus(attempts.size(), attempts.size() - graded);
        return new ExamGradingSummary(
                String.valueOf(exam.getId()),
                exam.getCourse() == null ? "-" : exam.getCourse().getCourseName(),
                exam.getExamName(),
                exam.getInstructor() == null ? "-" : exam.getInstructor().getName(),
                format(exam.getWindowStart(), ISO_DATE),
                format(exam.getWindowEnd(), ISO_DATE),
                statusText(status),
                statusClass(status),
                attempts.size(),
                graded,
                attempts.size() - graded,
                confirmed,
                enrolled,
                exam.getPassScore(),
                exam.getTotalScore(),
                manualTotal,
                canGrade,
                urlPrefix + "/exams/" + examId + "/grades",
                urlPrefix + "/exams/" + examId + "/grades.csv");
    }

    /**
     * 응시자 목록. 제출한 사람은 최신 회차 기준으로, <b>미응시자도 disabled 행으로</b> 함께 내린다
     * (원본 화면 더미 studentTableData 가 이미 그런 행을 갖고 있었다 — 미제출자 확인이 실무 요건).
     */
    public List<AttemptGradingRow> attemptList(Long examId, Long viewerId, boolean admin, String urlPrefix) {
        Exam exam = requireExam(examId);
        ensureCanGrade(exam, viewerId, admin);

        List<ExamAttempt> attempts = latestPerUser(
                examGradingRepository.findAttempts(List.of(examId), GRADABLE_STATUSES));
        int manualTotal = toIntMap(examGradingRepository.countManualQuestions(List.of(examId), AUTO_TYPES))
                .getOrDefault(examId, 0);
        Map<Long, Integer> manualGraded =
                manualGradedCounts(attempts.stream().map(ExamAttempt::getId).toList());
        Map<Long, Grade> grades = gradesByUser(examId);

        List<AttemptGradingRow> rows = new ArrayList<>();
        Set<Long> submittedUsers = new HashSet<>();
        int no = 0;
        for (ExamAttempt a : attempts) {
            User u = a.getUser();
            submittedUsers.add(u.getId());
            Grade g = grades.get(u.getId());
            String status = resolveStatus(g, manualTotal, manualGraded.getOrDefault(a.getId(), 0));
            Integer total = g != null && g.getTotalScore() != null ? g.getTotalScore() : a.getTotalScore();
            Boolean passed = g != null ? g.getPassed() : a.getPassed();

            rows.add(new AttemptGradingRow(
                    ++no,
                    String.valueOf(a.getId()),
                    String.valueOf(u.getId()),
                    u.getName(),
                    u.getLoginId() == null ? "-" : u.getLoginId(),
                    format(a.getSubmittedAt(), FULL),
                    total == null ? "-" : String.valueOf(total),
                    status,
                    scoreStatus(status, passed),
                    false,
                    urlPrefix + "/attempts/" + a.getId()));
        }

        // 미응시자 — 채점 버튼은 비활성
        if (exam.getCourse() != null) {
            List<Long> notSubmitted = courseQueryService.findUserIdsByCourseId(exam.getCourse().getId())
                    .stream().filter(id -> !submittedUsers.contains(id)).toList();
            if (!notSubmitted.isEmpty()) {
                for (User u : examGradingRepository.findUsers(notSubmitted)) {
                    rows.add(new AttemptGradingRow(
                            ++no, null, String.valueOf(u.getId()), u.getName(),
                            u.getLoginId() == null ? "-" : u.getLoginId(),
                            "-", "-", ST_UNGRADED, "미정", true, null));
                }
            }
        }
        return rows;
    }

    /* ===================== 3. 채점 팝업 ===================== */

    public AttemptGradingDetail attemptDetail(Long attemptId, Long viewerId, boolean admin, String urlPrefix) {
        ExamAttempt attempt = requireAttempt(attemptId);
        Exam exam = examGradingRepository.findExamWithQuestions(attempt.getExam().getId())
                .orElseThrow(() -> new GradingException("NOT_FOUND", "시험을 찾을 수 없습니다."));
        ensureCanGrade(exam, viewerId, admin);
        boolean canGrade = true;

        Map<Long, Answer> answers = answersByQuestion(attemptId);

        List<ExamQuestion> ordered = new ArrayList<>(exam.getExamQuestions());
        ordered.sort(Comparator.comparing(ExamQuestion::getSeq));
        // 보기는 별도 쿼리로 초기화한다 (문항 컬렉션과 함께 fetch 하면 카테시안 곱).
        List<Long> questionIds = ordered.stream().map(eq -> eq.getQuestion().getId()).toList();
        Map<Long, Question> withChoices = new HashMap<>();
        if (!questionIds.isEmpty()) {
            for (Question q : examGradingRepository.findQuestionsWithChoices(questionIds)) {
                withChoices.put(q.getId(), q);
            }
        }

        List<GradingQuestionRow> questions = new ArrayList<>();
        int autoScore = 0;
        int manualScore = 0;
        boolean manualPending = false;
        int firstManual = -1;
        int no = 0;

        for (ExamQuestion eq : ordered) {
            Question q = withChoices.getOrDefault(eq.getQuestion().getId(), eq.getQuestion());
            Answer answer = answers.get(q.getId());
            int max = eq.resolveScore();
            Integer given = answer == null ? null : answer.getScore();
            boolean manual = !q.isAutoGradable();

            if (manual) {
                if (given == null) {
                    manualPending = true;
                    if (firstManual < 0) {
                        firstManual = no;
                    }
                } else {
                    manualScore += given;
                }
            } else if (given != null) {
                autoScore += given;
            }

            String status = manual
                    ? (given == null ? ST_UNGRADED : ST_GRADED)
                    : "자동채점";
            questions.add(new GradingQuestionRow(
                    String.valueOf(q.getId()),
                    ++no,
                    typeLabel(q.getQuestionType()),
                    status,
                    ST_UNGRADED.equals(status) ? "status-pending" : "status-done",
                    max,
                    given,
                    q.getQuestionText(),
                    modelAnswer(q),
                    studentAnswer(answer),
                    answer == null ? "" : nullToEmpty(answer.getGraderComment()),
                    manual,
                    q.isAllowPartial()));
        }

        Grade grade = gradeRepository
                .findByUserIdAndEvalTypeAndEvalRefId(
                        attempt.getUser().getId(), Grade.EvalType.EXAM, exam.getId())
                .orElse(null);
        String status = resolveStatus(grade, manualPending, questions);
        boolean confirmed = grade != null && grade.isConfirmed();

        return new AttemptGradingDetail(
                String.valueOf(attempt.getId()),
                String.valueOf(exam.getId()),
                exam.getCourse() == null ? "-" : exam.getCourse().getCourseName(),
                exam.getExamName(),
                new AttemptGradingDetail.Student(
                        attempt.getUser().getName(),
                        attempt.getUser().getLoginId() == null ? "-" : attempt.getUser().getLoginId()),
                format(attempt.getStartedAt(), FULL) + " ~ " + format(attempt.getSubmittedAt(), FULL),
                status,
                statusBadgeClass(status),
                autoScore,
                manualScore,
                autoScore + manualScore,
                exam.getTotalScore(),
                exam.getPassScore(),
                grade != null ? grade.getPassed() : attempt.getPassed(),
                confirmed,
                canGrade,
                manualPending,
                Math.max(firstManual, 0),
                questions,
                urlPrefix + "/attempts/" + attempt.getId() + "/scores",
                urlPrefix + "/attempts/" + attempt.getId() + "/confirm",
                urlPrefix + "/exams/" + exam.getId());
    }

    /* ===================== 4. 수동 채점 저장 / 확정 / 정정 ===================== */

    /**
     * 수동 채점 저장. {@code request.confirm()} 이면 저장과 동시에 성적을 확정한다.
     *
     * <p>확정된 성적에 대한 호출은 <b>정정</b>으로 취급한다 — 사유가 없으면 거부하고,
     * 있으면 {@code GradeHistory} 를 남긴 뒤 확정 상태를 유지한 채 점수만 바꾼다.</p>
     */
    @Transactional
    public AttemptGradingDetail saveScores(Long attemptId, ManualScoreRequest request,
                                           Long graderId, boolean admin, String urlPrefix) {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        ExamAttempt attempt = requireAttempt(attemptId);
        Exam exam = examGradingRepository.findExamWithQuestions(attempt.getExam().getId())
                .orElseThrow(() -> new GradingException("NOT_FOUND", "시험을 찾을 수 없습니다."));
        ensureCanGrade(exam, graderId, admin);
        User grader = examGradingRepository.findUsers(List.of(graderId)).stream().findFirst()
                .orElseThrow(() -> new GradingException("NOT_FOUND", "채점자를 찾을 수 없습니다."));

        Grade grade = gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(
                attempt.getUser().getId(), Grade.EvalType.EXAM, exam.getId()).orElse(null);
        boolean wasConfirmed = grade != null && grade.isConfirmed();
        String reason = blankToNull(request.reason());

        // ★ 확정 이후의 점수 변경은 사유 없이 통과시키지 않는다 (내역서 증빙 요건).
        if (wasConfirmed && !request.safeScores().isEmpty() && reason == null) {
            throw GradingException.reasonRequired();
        }

        Integer beforeScore = grade == null ? null : grade.getTotalScore();
        Boolean beforePassed = grade == null ? null : grade.getPassed();

        Map<Long, ExamQuestion> byQuestionId = new HashMap<>();
        for (ExamQuestion eq : exam.getExamQuestions()) {
            byQuestionId.put(eq.getQuestion().getId(), eq);
        }
        Map<Long, Answer> answers = answersByQuestion(attemptId);

        for (ManualScoreRequest.ScoreEntry entry : request.safeScores()) {
            if (entry == null || entry.questionId() == null) {
                continue;
            }
            ExamQuestion eq = byQuestionId.get(entry.questionId());
            if (eq == null) {
                throw new GradingException("BAD_QUESTION", "이 시험에 편성되지 않은 문항입니다.");
            }
            Question question = eq.getQuestion();
            if (question.isAutoGradable()) {
                // 자동 채점 점수를 사람이 덮어쓰면 재현이 깨진다. 화면도 입력을 막지만 서버가 최종 방어선이다.
                throw new GradingException("AUTO_QUESTION",
                        "자동 채점 문항(" + typeLabel(question.getQuestionType()) + ")은 수동으로 점수를 바꿀 수 없습니다.");
            }
            if (entry.score() == null) {
                continue; // 미입력 — 채점하지 않은 것으로 둔다
            }
            int max = eq.resolveScore();
            int score = entry.score();
            if (score < 0 || score > max) {
                throw new GradingException("OUT_OF_RANGE", question.getQuestionCode()
                        + " 점수는 0 ~ " + max + " 사이여야 합니다. (입력: " + score + ")");
            }
            if (!question.isAllowPartial() && score != 0 && score != max) {
                throw new GradingException("PARTIAL_NOT_ALLOWED",
                        question.getQuestionCode() + " 문항은 부분점수를 허용하지 않습니다. 0 또는 " + max + "점만 입력하세요.");
            }

            Answer answer = answers.get(question.getId());
            if (answer == null) {
                // 무응답 문항도 0점 근거를 남겨야 하므로 답안 행을 만들어 채점 결과를 붙인다.
                answer = answerRepository.save(Answer.builder()
                        .attempt(attempt)
                        .question(question)
                        .answerText(null)
                        .savedAt(now)
                        .build());
                answers.put(question.getId(), answer);
            }
            answer.gradeManual(grader, score, score >= max, blankToNull(entry.comment()), now);
        }

        // ── 총점 재계산: 자동 점수는 그대로 두고 다시 합산한다 (덮어쓰지 않는다)
        Totals totals = recalc(exam, answers);

        // 회차 점수 반영. 수동 채점이 남아 있으면 passScore 를 넘기지 않아 passed 가 null(미정)로 남는다.
        attempt.applyScore(totals.autoScore(), totals.manualScore(),
                totals.manualPending() ? null : exam.getPassScore());

        if (grade == null) {
            grade = gradeRepository.save(Grade.builder()
                    .user(attempt.getUser())
                    .course(exam.getCourse())
                    .evalType(Grade.EvalType.EXAM)
                    .evalRefId(exam.getId())
                    .status(Grade.GradeStatus.UNGRADED)
                    .build());
        }

        if (wasConfirmed) {
            grade.correctScore(totals.autoScore(), totals.manualScore(), exam.getPassScore(), grader, now);
        } else if (request.confirm()) {
            if (totals.manualPending()) {
                throw new GradingException("MANUAL_PENDING",
                        "수동 채점이 끝나지 않아 성적을 확정할 수 없습니다. (미채점 " + totals.pendingCount() + "문항)");
            }
            grade.applyScore(totals.autoScore(), totals.manualScore(), exam.getPassScore(), grader, now);
            grade.confirm(grader, now);
        } else if (totals.manualPending()) {
            grade.applyInterimScore(totals.autoScore(), totals.manualScore(), grader, now);
        } else {
            grade.applyScore(totals.autoScore(), totals.manualScore(), exam.getPassScore(), grader, now);
        }

        // 사유가 붙은 변경은 확정 여부와 무관하게 이력을 남긴다 (append-only).
        if (reason != null) {
            gradeHistoryRepository.save(GradeHistory.builder()
                    .grade(grade)
                    .beforeScore(beforeScore)
                    .afterScore(grade.getTotalScore())
                    .beforePassed(beforePassed)
                    .afterPassed(grade.getPassed())
                    .reason(reason)
                    .changedBy(grader)
                    .changedAt(now)
                    .build());
            log.info("[채점] 성적 정정 grade={} {}->{} 사유={}",
                    grade.getId(), beforeScore, grade.getTotalScore(), reason);
        }

        return attemptDetail(attemptId, graderId, admin, urlPrefix);
    }

    /** 성적 확정만 (점수 입력 없이). */
    @Transactional
    public AttemptGradingDetail confirm(Long attemptId, Long graderId, boolean admin, String urlPrefix) {
        return saveScores(attemptId, new ManualScoreRequest(List.of(), null, true),
                graderId, admin, urlPrefix);
    }

    /* ===================== 5. 성적 목록 / 변경 이력 / CSV ===================== */

    public List<GradeListRow> gradeList(Long examId, Long viewerId, boolean admin) {
        Exam exam = requireExam(examId);
        ensureCanGrade(exam, viewerId, admin);
        List<Grade> grades = examGradingRepository.findGradesByExam(examId);
        Map<Long, Integer> historyCounts = toIntMap(gradeHistoryRepository.countByGrades(
                grades.isEmpty() ? List.of(-1L) : grades.stream().map(Grade::getId).toList()));

        List<GradeListRow> rows = new ArrayList<>();
        int no = 0;
        for (Grade g : grades) {
            rows.add(new GradeListRow(
                    ++no,
                    String.valueOf(g.getId()),
                    g.getUser().getName(),
                    g.getUser().getLoginId() == null ? "-" : g.getUser().getLoginId(),
                    g.getAutoScore(),
                    g.getManualScore(),
                    g.getTotalScore(),
                    g.getPassed() == null ? "미정" : (g.getPassed() ? "합격" : "불합격"),
                    g.getStatus().name(),
                    statusOf(g.getStatus()),
                    g.getGradedBy() == null ? "-" : g.getGradedBy().getName(),
                    format(g.getGradedAt(), FULL),
                    g.getConfirmedBy() == null ? "-" : g.getConfirmedBy().getName(),
                    format(g.getConfirmedAt(), FULL),
                    historyCounts.getOrDefault(g.getId(), 0)));
        }
        return rows;
    }

    public List<GradeHistoryRow> historyList(Long examId, Long viewerId, boolean admin) {
        Exam exam = requireExam(examId);
        ensureCanGrade(exam, viewerId, admin);
        List<Grade> grades = examGradingRepository.findGradesByExam(examId);
        if (grades.isEmpty()) {
            return List.of();
        }
        List<Long> ids = grades.stream().map(Grade::getId).toList();
        List<GradeHistoryRow> rows = new ArrayList<>();
        for (GradeHistory h : gradeHistoryRepository.findByGrades(ids)) {
            rows.add(new GradeHistoryRow(
                    String.valueOf(h.getGrade().getId()),
                    h.getGrade().getUser().getName(),
                    format(h.getChangedAt(), FULL),
                    h.getChangedBy().getName(),
                    (h.getBeforeScore() == null ? "-" : h.getBeforeScore())
                            + " → " + (h.getAfterScore() == null ? "-" : h.getAfterScore()),
                    h.getAfterPassed() == null ? "미정" : (h.getAfterPassed() ? "합격" : "불합격"),
                    h.getReason()));
        }
        return rows;
    }

    /**
     * 성적 목록 CSV.
     *
     * <p><b>엑셀(xlsx)이 아니라 CSV 인 이유</b>: Apache POI 가 {@code build.gradle} 에 없고,
     * build.gradle 은 공동 소유라 이 슬라이스에서 의존성을 추가할 수 없다.
     * 엑셀에서 한글이 깨지지 않도록 UTF-8 BOM 을 앞에 붙인다.</p>
     */
    public String gradeCsv(Long examId, Long viewerId, boolean admin) {
        Exam exam = requireExam(examId);
        List<GradeListRow> rows = gradeList(examId, viewerId, admin);

        StringBuilder sb = new StringBuilder("﻿");
        sb.append("번호,이름,아이디,자동채점,수동채점,총점,합격여부,상태,채점자,채점일시,확정자,확정일시,정정이력\n");
        for (GradeListRow r : rows) {
            sb.append(r.no()).append(',')
                    .append(csv(r.userName())).append(',')
                    .append(csv(r.loginId())).append(',')
                    .append(r.autoScore() == null ? "" : r.autoScore()).append(',')
                    .append(r.manualScore() == null ? "" : r.manualScore()).append(',')
                    .append(r.totalScore() == null ? "" : r.totalScore()).append(',')
                    .append(csv(r.passedText())).append(',')
                    .append(csv(r.statusText())).append(',')
                    .append(csv(r.gradedBy())).append(',')
                    .append(csv(r.gradedAt())).append(',')
                    .append(csv(r.confirmedBy())).append(',')
                    .append(csv(r.confirmedAt())).append(',')
                    .append(r.historyCount()).append('\n');
        }
        if (rows.isEmpty()) {
            sb.append("# 확정/채점된 성적이 없습니다: ").append(csv(exam.getExamName())).append('\n');
        }
        return sb.toString();
    }

    public String examName(Long examId) {
        return requireExam(examId).getExamName();
    }

    /* ===================== 내부 ===================== */

    /** 자동/수동 점수 재합산 결과. */
    private record Totals(int autoScore, int manualScore, boolean manualPending, int pendingCount) {
    }

    private Totals recalc(Exam exam, Map<Long, Answer> answers) {
        int auto = 0;
        int manual = 0;
        int pending = 0;
        for (ExamQuestion eq : exam.getExamQuestions()) {
            Question q = eq.getQuestion();
            Answer a = answers.get(q.getId());
            Integer score = a == null ? null : a.getScore();
            if (q.isAutoGradable()) {
                // AutoGrader 가 매긴 점수를 그대로 쓴다. 여기서 다시 채점하지 않는다.
                auto += score == null ? 0 : score;
            } else if (score == null) {
                pending++;
            } else {
                manual += score;
            }
        }
        return new Totals(auto, manual, pending > 0, pending);
    }

    private Map<Long, Answer> answersByQuestion(Long attemptId) {
        Map<Long, Answer> map = new HashMap<>();
        for (Answer a : examGradingRepository.findAnswers(attemptId)) {
            map.put(a.getQuestion().getId(), a);
        }
        return map;
    }

    private Map<Long, Grade> gradesByUser(Long examId) {
        Map<Long, Grade> map = new HashMap<>();
        for (Grade g : examGradingRepository.findGradesByExam(examId)) {
            map.put(g.getUser().getId(), g);
        }
        return map;
    }

    private Map<Long, Integer> manualGradedCounts(Collection<Long> attemptIds) {
        if (attemptIds.isEmpty()) {
            return Map.of();
        }
        return toIntMap(examGradingRepository.countManualGraded(attemptIds));
    }

    private Map<Long, Long> confirmedCountsByExam(Collection<Long> examIds) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : examGradingRepository.countGradeStatuses(examIds)) {
            if (row[1] == Grade.GradeStatus.CONFIRMED) {
                map.merge((Long) row[0], ((Number) row[2]).longValue(), Long::sum);
            }
        }
        return map;
    }

    private Map<Long, List<ExamAttempt>> groupByExam(List<ExamAttempt> attempts) {
        Map<Long, List<ExamAttempt>> map = new LinkedHashMap<>();
        for (ExamAttempt a : attempts) {
            map.computeIfAbsent(a.getExam().getId(), k -> new ArrayList<>()).add(a);
        }
        return map;
    }

    /**
     * 사용자별 <b>최신 회차</b>만 남긴다.
     * 재응시가 허용된 시험은 회차가 여러 개인데, 성적은 (user, exam) 당 한 행이라
     * 어느 회차를 성적으로 볼지 정해야 한다. 마지막 제출을 기준으로 한다.
     */
    private List<ExamAttempt> latestPerUser(List<ExamAttempt> attempts) {
        Map<Long, ExamAttempt> latest = new LinkedHashMap<>();
        for (ExamAttempt a : attempts) {
            latest.merge(a.getUser().getId(), a,
                    (old, cur) -> cur.getAttemptNo() >= old.getAttemptNo() ? cur : old);
        }
        return new ArrayList<>(latest.values());
    }

    private boolean isGradingDone(int manualTotal, int manualGraded) {
        return manualTotal == 0 || manualGraded >= manualTotal;
    }

    private String resolveStatus(Grade grade, int manualTotal, int manualGraded) {
        if (grade != null && grade.isConfirmed()) {
            return ST_CONFIRMED;
        }
        if (manualTotal == 0) {
            return ST_GRADED; // 전부 자동 채점 — 사람이 더 할 일이 없다
        }
        if (manualGraded <= 0) {
            return ST_UNGRADED;
        }
        return manualGraded >= manualTotal ? ST_GRADED : ST_GRADING;
    }

    private String resolveStatus(Grade grade, boolean manualPending, List<GradingQuestionRow> questions) {
        int manualTotal = (int) questions.stream().filter(GradingQuestionRow::manual).count();
        int manualGraded = (int) questions.stream()
                .filter(q -> q.manual() && q.given() != null).count();
        return resolveStatus(grade, manualTotal, manualGraded);
    }

    /** 화면 필터(#filterScoreStatus)의 값 — 합격 / 불합격 / 미정 */
    private String scoreStatus(String status, Boolean passed) {
        if (!ST_CONFIRMED.equals(status) && !ST_GRADED.equals(status)) {
            return "미정";
        }
        if (passed == null) {
            return "미정";
        }
        return passed ? "합격" : "불합격";
    }

    private String statusOf(Grade.GradeStatus status) {
        return switch (status) {
            case UNGRADED -> ST_UNGRADED;
            case GRADING -> ST_GRADING;
            case GRADED -> ST_GRADED;
            case CONFIRMED -> ST_CONFIRMED;
        };
    }

    /** assignments.js 의 상태 키. waiting(평가예정) / pending(채점예정) / completed(채점완료) */
    private String listStatus(int submitted, int ungraded) {
        if (submitted == 0) {
            return "waiting";
        }
        return ungraded > 0 ? "pending" : "completed";
    }

    private String statusText(String listStatus) {
        return switch (listStatus) {
            case "waiting" -> "평가예정";
            case "pending" -> "채점예정";
            default -> "채점완료";
        };
    }

    /** result-grading.css 의 exam-status-* 클래스. */
    private String statusClass(String listStatus) {
        return switch (listStatus) {
            case "waiting" -> "exam-status-wait";
            case "pending" -> "exam-status-ing";
            default -> "exam-status-done";
        };
    }

    private String statusBadgeClass(String status) {
        return ST_CONFIRMED.equals(status) ? "badge-done" : "badge-wip";
    }

    private String modelAnswer(Question q) {
        if (q.getQuestionType() == Question.QuestionType.MULTIPLE_CHOICE) {
            for (QuestionChoice c : q.getChoices()) {
                if (c.isCorrect()) {
                    return c.getSeq() + ". " + c.getContent();
                }
            }
            return nullToEmpty(q.getCorrectAnswer());
        }
        return nullToEmpty(q.getCorrectAnswer());
    }

    private String studentAnswer(Answer answer) {
        if (answer == null) {
            return "(미응답)";
        }
        if (answer.getChoice() != null) {
            return answer.getChoice().getSeq() + ". " + answer.getChoice().getContent();
        }
        return answer.getAnswerText() == null || answer.getAnswerText().isBlank()
                ? "(미응답)" : answer.getAnswerText();
    }

    private String typeLabel(Question.QuestionType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> "객관식";
            case SHORT_ANSWER -> "주관식";
            case CODING -> "서술형";
        };
    }

    /** 강사는 담당 과정만. 판정은 A 의 계약(CourseQueryService)으로만 한다. */
    private boolean canGrade(Exam exam, Long viewerId, boolean admin) {
        if (admin) {
            return true;
        }
        if (exam.getCourse() == null || viewerId == null) {
            return false;
        }
        return courseQueryService.isInstructorOf(viewerId, exam.getCourse().getId());
    }

    /** 담당하지 않는 과정이면 403. 모든 진입점(목록·상세·채점·CSV)이 반드시 거쳐야 한다. */
    private void ensureCanGrade(Exam exam, Long viewerId, boolean admin) {
        if (!canGrade(exam, viewerId, admin)) {
            throw new AccessDeniedException("담당하지 않는 과정의 시험입니다.");
        }
    }

    private Exam requireExam(Long examId) {
        return examGradingRepository.findExam(examId)
                .orElseThrow(() -> new GradingException("NOT_FOUND", "시험을 찾을 수 없습니다."));
    }

    private ExamAttempt requireAttempt(Long attemptId) {
        return examGradingRepository.findAttempt(attemptId)
                .orElseThrow(() -> new GradingException("NOT_FOUND", "응시 회차를 찾을 수 없습니다."));
    }

    private static Map<Long, Integer> toIntMap(List<Object[]> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private static String format(LocalDateTime at, DateTimeFormatter formatter) {
        return at == null ? "-" : formatter.format(at);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** CSV 셀 이스케이프 — 쉼표/따옴표/줄바꿈이 들어간 값이 컬럼을 밀어내지 않게 한다. */
    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }
}
