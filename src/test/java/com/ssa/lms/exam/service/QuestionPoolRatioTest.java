package com.ssa.lms.exam.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamQuestionRule;
import com.ssa.lms.exam.entity.ExamQuestionRuleDifficulty;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문제은행 3배수 확보 요건을 고정한다.
 *
 * <p>법정 요건: 자동 출제 시 문제은행에 <b>출제 문항 수의 3배 이상</b>이 있어야 한다.
 * 이 검사가 없으면 후보가 모자라도 조용히 덜 뽑고 통과해, 요건 미달인 채로
 * 시험이 확정되고 사후에 알아채기 어렵다.</p>
 */
@SpringBootTest
@Transactional
class QuestionPoolRatioTest {

    @Autowired ExamService examService;
    @Autowired ExamRepository examRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired CourseRepository courseRepository;

    private Course course;

    @BeforeEach
    void setUp() {
        course = courseRepository.save(Course.builder()
                .courseCode("POOL-" + System.nanoTime())
                .courseName("3배수 검증 과정")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .build());
    }

    /** 지정한 분류·난이도로 문제를 n개 만든다. */
    private void createQuestions(String categoryL, Difficulty difficulty, int n) {
        for (int i = 0; i < n; i++) {
            questionRepository.save(Question.builder()
                    .questionCode("POOL-" + System.nanoTime() + "-" + i)
                    .questionType(Question.QuestionType.SHORT_ANSWER)
                    .questionText("풀 검증 문항 " + i)
                    .correctAnswer("정답")
                    .difficulty(difficulty)
                    .score(5)
                    .categoryL(categoryL)
                    .status(Question.QuestionStatus.ACTIVE)
                    .build());
        }
    }

    /** 규칙 1개(난이도 지정)를 가진 시험을 만든다. */
    private Long createExamWithRule(String categoryL, Difficulty difficulty, int need) {
        Exam exam = Exam.builder()
                .examName("3배수 검증 시험")
                .examType(Exam.ExamType.UNIT)
                .course(course)
                .timeLimitMin(30)
                .autoScore(need * 5).manualScore(0).totalScore(need * 5).passScore(1)
                .maxAttempts(1)
                .windowStart(LocalDateTime.now().minusDays(1))
                .windowEnd(LocalDateTime.now().plusDays(1))
                .status(Exam.ExamStatus.DRAFT)
                .build();

        ExamQuestionRule rule = ExamQuestionRule.builder()
                .categoryL(categoryL)
                .totalCount(need)
                .build();
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(difficulty)
                .questionCount(need)
                .build());
        exam.addQuestionRule(rule);

        return examRepository.save(exam).getId();
    }

    @Test
    @DisplayName("확보량이 3배 미만이면 확정을 거부한다")
    void 부족하면_거부() {
        String cat = "POOL-A-" + System.nanoTime();
        createQuestions(cat, Difficulty.EASY, 5);          // 5문항만 확보
        Long examId = createExamWithRule(cat, Difficulty.EASY, 3);  // 3문항 출제 → 9문항 필요

        assertThatThrownBy(() -> examService.materializeRules(examId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3배 이상")
                .hasMessageContaining("9문항 필요")
                .hasMessageContaining("현재 5문항");
    }

    @Test
    @DisplayName("정확히 3배면 통과한다 (경계값)")
    void 경계값_통과() {
        String cat = "POOL-B-" + System.nanoTime();
        createQuestions(cat, Difficulty.MEDIUM, 9);        // 정확히 3배
        Long examId = createExamWithRule(cat, Difficulty.MEDIUM, 3);

        int added = examService.materializeRules(examId);

        assertThat(added).isEqualTo(3);
    }

    @Test
    @DisplayName("총합은 충분해도 특정 난이도가 모자라면 거부한다")
    void 난이도별로_따로_센다() {
        String cat = "POOL-C-" + System.nanoTime();
        createQuestions(cat, Difficulty.EASY, 30);         // 쉬움은 넉넉
        createQuestions(cat, Difficulty.HARD, 4);          // 어려움은 부족 (2문항 출제 → 6 필요)

        Exam exam = Exam.builder()
                .examName("난이도 혼합 시험")
                .examType(Exam.ExamType.UNIT)
                .course(course)
                .timeLimitMin(30)
                .autoScore(20).manualScore(0).totalScore(20).passScore(1)
                .maxAttempts(1)
                .windowStart(LocalDateTime.now().minusDays(1))
                .windowEnd(LocalDateTime.now().plusDays(1))
                .status(Exam.ExamStatus.DRAFT)
                .build();
        ExamQuestionRule rule = ExamQuestionRule.builder().categoryL(cat).totalCount(4).build();
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(Difficulty.EASY).questionCount(2).build());
        rule.addDifficulty(ExamQuestionRuleDifficulty.builder()
                .difficulty(Difficulty.HARD).questionCount(2).build());
        exam.addQuestionRule(rule);
        Long examId = examRepository.save(exam).getId();

        assertThatThrownBy(() -> examService.materializeRules(examId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HARD")
                .hasMessageContaining("현재 4문항");
    }

    @Test
    @DisplayName("거부될 때 기존 문항을 지우지 않는다 — 부분 실패 방지")
    void 거부시_기존문항_보존() {
        String cat = "POOL-D-" + System.nanoTime();
        createQuestions(cat, Difficulty.EASY, 9);
        Long examId = createExamWithRule(cat, Difficulty.EASY, 3);

        examService.materializeRules(examId);   // 1차: 성공 (3문항 확정)
        examRepository.flush();

        // 문제은행을 비활성화해 후보를 0으로 만든 뒤 재확정 시도
        questionRepository.findAll().stream()
                .filter(q -> cat.equals(q.getCategoryL()))
                .forEach(q -> q.changeStatus(Question.QuestionStatus.INACTIVE));
        questionRepository.flush();

        assertThatThrownBy(() -> examService.materializeRules(examId))
                .isInstanceOf(IllegalStateException.class);

        // 기존에 확정해둔 문항이 남아 있어야 한다
        Exam reloaded = examRepository.findWithQuestions(examId).orElseThrow();
        assertThat(reloaded.getExamQuestions())
                .as("검증 실패 시 문항을 건드리면 안 된다")
                .hasSize(3);
    }
}
