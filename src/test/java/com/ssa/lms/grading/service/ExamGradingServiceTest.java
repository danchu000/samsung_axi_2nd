package com.ssa.lms.grading.service;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.exam.entity.*;
import com.ssa.lms.exam.repository.AnswerRepository;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.grading.dto.AttemptGradingDetail;
import com.ssa.lms.grading.dto.GradeHistoryRow;
import com.ssa.lms.grading.dto.GradeSummary;
import com.ssa.lms.grading.dto.ManualScoreRequest;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 시험 수동 채점 → 확정 → Grade 반영 → 정정 이력 전 구간.
 *
 * <p>특히 <b>{@code GradeQueryService.findConfirmedGrades} 로 조회되는지</b>를 함께 확인한다 —
 * 개발자 A의 이수 판정이 그 경로만 쓰기 때문에, 여기서 끊기면 채점을 아무리 해도 이수가 안 된다.</p>
 */
@SpringBootTest
@Transactional
class ExamGradingServiceTest {

    private static final String URL = "/admin/evaluation/grading";

    @Autowired ExamGradingService examGradingService;
    @Autowired GradeQueryService gradeQueryService;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired ExamRepository examRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired AnswerRepository answerRepository;

    private User grader;
    private User trainee;
    private Course course;
    private Exam exam;
    private ExamAttempt attempt;
    private Question mc;        // 객관식 5점 — 자동 채점 완료 상태로 둔다
    private Question partial;   // 서술형 20점, 부분점수 허용
    private Question strict;    // 서술형 10점, 부분점수 불가

    @BeforeEach
    void setUp() {
        long n = System.nanoTime();

        grader = userRepository.save(User.builder()
                .loginId("grader-" + n).password("{noop}t").name("채점자")
                .role(Role.INSTRUCTOR).status(UserStatus.ACTIVE).build());
        trainee = userRepository.save(User.builder()
                .loginId("tr-" + n).password("{noop}t").name("응시자")
                .role(Role.TRAINEE).status(UserStatus.ACTIVE).build());

        course = courseRepository.save(Course.builder()
                .courseCode("C-" + n).courseName("채점 테스트 과정").cohort("1기")
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().plusDays(10))
                .capacity(10).status(CourseStatus.IN_PROGRESS).build());
        courseInstructorRepository.save(CourseInstructor.builder()
                .course(course).instructor(grader).primaryInstructor(true).build());
        enrollmentRepository.save(Enrollment.builder()
                .trainee(trainee).course(course)
                .status(EnrollmentStatus.APPROVED).appliedAt(LocalDateTime.now()).build());

        mc = questionRepository.save(question("MC-" + n, Question.QuestionType.MULTIPLE_CHOICE, 5, false));
        partial = questionRepository.save(question("CP-" + n, Question.QuestionType.CODING, 20, true));
        strict = questionRepository.save(question("CS-" + n, Question.QuestionType.CODING, 10, false));

        exam = Exam.builder()
                .examName("채점 테스트 시험").examType(Exam.ExamType.MIDTERM)
                .course(course).instructor(grader)
                .timeLimitMin(60).autoScore(35).manualScore(0).totalScore(35).passScore(20)
                .randomOrder(false).retakeAllowed(false).maxAttempts(1)
                .windowStart(LocalDateTime.now().minusDays(2)).windowEnd(LocalDateTime.now().minusHours(1))
                .requireIdentityVerification(true).proctorEnabled(false).requireWebcam(false)
                .blockTabSwitch(false).blockCopyPaste(false).status(Exam.ExamStatus.CLOSED)
                .build();
        int seq = 0;
        for (Question q : List.of(mc, partial, strict)) {
            exam.addExamQuestion(ExamQuestion.builder()
                    .question(q).seq(++seq).scoreOverride(q.getScore()).fromRule(false).build());
        }
        exam = examRepository.save(exam);

        attempt = examAttemptRepository.save(ExamAttempt.builder()
                .exam(exam).user(trainee).attemptNo(1)
                .startedAt(LocalDateTime.now().minusHours(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .status(ExamAttempt.AttemptStatus.SUBMITTED)
                .build());
        attempt.submit(LocalDateTime.now().minusHours(1), ExamAttempt.AttemptStatus.SUBMITTED);

        // 객관식은 이미 자동 채점된 상태 (AutoGrader 가 제출 시점에 해둔 것과 같은 모양)
        Answer a1 = answerRepository.save(Answer.builder()
                .attempt(attempt).question(mc).answerText(null).savedAt(LocalDateTime.now()).build());
        a1.gradeAuto(true, 5, LocalDateTime.now());
        answerRepository.save(Answer.builder()
                .attempt(attempt).question(partial).answerText("부분적으로 맞는 답").savedAt(LocalDateTime.now()).build());
        answerRepository.save(Answer.builder()
                .attempt(attempt).question(strict).answerText("정답").savedAt(LocalDateTime.now()).build());

        attempt.applyScore(5, null, null);   // 수동 채점 대기 → passed 는 미정(null)이어야 한다
    }

    private Question question(String code, Question.QuestionType type, int score, boolean allowPartial) {
        return Question.builder()
                .questionCode(code).questionType(type).questionText(code + " 지문")
                .correctAnswer("정답").difficulty(Difficulty.MEDIUM).score(score)
                .caseSensitive(false).allowPartial(allowPartial)
                .status(Question.QuestionStatus.ACTIVE).build();
    }

    private AttemptGradingDetail save(List<ManualScoreRequest.ScoreEntry> scores, String reason, boolean confirm) {
        return examGradingService.saveScores(attempt.getId(),
                new ManualScoreRequest(scores, reason, confirm), grader.getId(), false, URL);
    }

    private ManualScoreRequest.ScoreEntry entry(Question q, Integer score) {
        return new ManualScoreRequest.ScoreEntry(q.getId(), score, null);
    }

    /* ===================== ===================== */

    @Test
    @DisplayName("수동 채점 대기 상태에서는 합격 여부가 미정(null)이다 — false 로 저장되면 안 된다")
    void 미채점_합격여부_미정() {
        assertThat(attempt.getPassed()).isNull();

        AttemptGradingDetail detail = examGradingService.attemptDetail(
                attempt.getId(), grader.getId(), false, URL);
        assertThat(detail.status()).isEqualTo(ExamGradingService.ST_UNGRADED);
        assertThat(detail.passed()).isNull();
        assertThat(detail.manualPending()).isTrue();
    }

    @Test
    @DisplayName("자동 채점된 객관식 점수는 수동 채점 후에도 그대로 유지되고 총점은 자동+수동으로 재합산된다")
    void 자동채점_점수_보존() {
        AttemptGradingDetail detail = save(List.of(entry(partial, 14)), null, false);

        assertThat(detail.autoScore()).isEqualTo(5);
        assertThat(detail.manualScore()).isEqualTo(14);
        assertThat(detail.totalScore()).isEqualTo(19);
        assertThat(detail.status()).isEqualTo(ExamGradingService.ST_GRADING);
        assertThat(detail.questions())
                .filteredOn(q -> !q.manual())
                .allSatisfy(q -> assertThat(q.given()).isEqualTo(5));
    }

    @Test
    @DisplayName("allowPartial=false 문항은 0 또는 만점만 받는다")
    void 부분점수_규칙() {
        // 허용 문항은 중간 점수 OK
        assertThat(save(List.of(entry(partial, 7)), null, false).manualScore()).isEqualTo(7);

        assertThatThrownBy(() -> save(List.of(entry(strict, 6)), null, false))
                .isInstanceOf(GradingException.class)
                .hasMessageContaining("부분점수를 허용하지 않습니다");

        assertThat(save(List.of(entry(strict, 10)), null, false).manualScore()).isEqualTo(17);
    }

    @Test
    @DisplayName("배점 초과 / 자동 채점 문항 수정은 거부한다")
    void 입력_검증() {
        assertThatThrownBy(() -> save(List.of(entry(partial, 21)), null, false))
                .isInstanceOf(GradingException.class)
                .hasMessageContaining("0 ~ 20");

        assertThatThrownBy(() -> save(List.of(entry(mc, 0)), null, false))
                .isInstanceOf(GradingException.class)
                .hasMessageContaining("자동 채점 문항");
    }

    @Test
    @DisplayName("수동 채점이 남아 있으면 확정할 수 없다")
    void 확정_전제조건() {
        save(List.of(entry(partial, 20)), null, false);

        assertThatThrownBy(() -> examGradingService.confirm(attempt.getId(), grader.getId(), false, URL))
                .isInstanceOf(GradingException.class)
                .hasMessageContaining("수동 채점이 끝나지 않아");
    }

    @Test
    @DisplayName("확정하면 Grade 가 evalType=EXAM 으로 생기고 GradeQueryService(A의 이수 판정)로 조회된다")
    void 확정_Grade반영() {
        save(List.of(entry(partial, 20), entry(strict, 10)), null, false);
        AttemptGradingDetail detail = examGradingService.confirm(
                attempt.getId(), grader.getId(), false, URL);

        assertThat(detail.status()).isEqualTo(ExamGradingService.ST_CONFIRMED);
        assertThat(detail.totalScore()).isEqualTo(35);
        assertThat(detail.passed()).isTrue();

        // ★ A의 이수 판정 경로
        List<GradeSummary> confirmed =
                gradeQueryService.findConfirmedGrades(trainee.getId(), course.getId());
        assertThat(confirmed).hasSize(1);
        assertThat(confirmed.get(0).evalType()).isEqualTo(Grade.EvalType.EXAM);
        assertThat(confirmed.get(0).evalRefId()).isEqualTo(exam.getId());
        assertThat(confirmed.get(0).totalScore()).isEqualTo(35);
        assertThat(confirmed.get(0).passed()).isTrue();
        assertThat(gradeQueryService.hasAllRequiredGradesConfirmed(trainee.getId(), course.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("확정 후 사유 없는 점수 변경은 거부하고, 사유가 있으면 GradeHistory 를 남기며 확정 상태를 유지한다")
    void 확정후_정정() {
        save(List.of(entry(partial, 20), entry(strict, 10)), null, false);
        examGradingService.confirm(attempt.getId(), grader.getId(), false, URL);

        assertThatThrownBy(() -> save(List.of(entry(partial, 10)), null, false))
                .isInstanceOf(GradingException.class)
                .hasMessageContaining("변경 사유");

        AttemptGradingDetail corrected =
                save(List.of(entry(partial, 10), entry(strict, 10)), "이의제기 반영", false);

        assertThat(corrected.confirmed()).isTrue();          // 확정이 풀리면 안 된다
        assertThat(corrected.totalScore()).isEqualTo(25);

        List<GradeHistoryRow> histories =
                examGradingService.historyList(exam.getId(), grader.getId(), false);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).score()).isEqualTo("35 → 25");
        assertThat(histories.get(0).reason()).isEqualTo("이의제기 반영");

        // 확정 상태가 유지되므로 A의 이수 판정도 계속 이 성적을 본다
        assertThat(gradeQueryService.findConfirmedGrades(trainee.getId(), course.getId()))
                .singleElement()
                .satisfies(g -> assertThat(g.totalScore()).isEqualTo(25));
    }

    @Test
    @DisplayName("담당하지 않는 과정의 강사는 채점할 수 없다")
    void 강사_담당과정_제한() {
        User other = userRepository.save(User.builder()
                .loginId("other-" + System.nanoTime()).password("{noop}t").name("타강사")
                .role(Role.INSTRUCTOR).status(UserStatus.ACTIVE).build());

        assertThatThrownBy(() -> examGradingService.attemptDetail(
                attempt.getId(), other.getId(), false, URL))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> examGradingService.summary(exam.getId(), other.getId(), false, URL))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> examGradingService.gradeCsv(exam.getId(), other.getId(), false))
                .isInstanceOf(AccessDeniedException.class);

        // 관리자는 통과한다
        assertThat(examGradingService.summary(exam.getId(), other.getId(), true, URL).canGrade()).isTrue();
    }

    @Test
    @DisplayName("성적 CSV — 헤더 + 확정된 행이 나온다")
    void csv_다운로드() {
        save(List.of(entry(partial, 20), entry(strict, 10)), null, false);
        examGradingService.confirm(attempt.getId(), grader.getId(), false, URL);

        String csv = examGradingService.gradeCsv(exam.getId(), grader.getId(), false);
        assertThat(csv).startsWith("﻿번호,이름,아이디");
        assertThat(csv).contains("\"응시자\"").contains(",5,30,35,").contains("\"합격\"").contains("\"확정\"");
    }
}
