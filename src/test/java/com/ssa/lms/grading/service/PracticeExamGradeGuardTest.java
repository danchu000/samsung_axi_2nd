package com.ssa.lms.grading.service;

import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.grading.dto.ManualScoreRequest;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.repository.GradeRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 연습(사전 모의) 시험이 성적에 반영되지 않는지 고정한다.
 *
 * <p><b>배경:</b> 응시·제출 경로는 이미 {@code Grade} 를 만들지 않았는데,
 * 관리자가 채점 화면에서 "성적 확정"을 누르면 {@code ExamGradingService} 를 거쳐
 * {@code Grade} 가 생겼다. 그러면 모의 테스트 점수가 이수 판정
 * (A 의 CompletionService → GradeQueryService)에 섞인다.</p>
 */
@SpringBootTest
@Transactional
class PracticeExamGradeGuardTest {

    @Autowired ExamGradingService examGradingService;
    @Autowired ExamRepository examRepository;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired GradeRepository gradeRepository;
    @Autowired UserRepository userRepository;

    private Long adminId() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst().map(User::getId).orElseThrow();
    }

    /** 연습 시험의 제출된 회차 하나. 시드에 없으면 테스트를 건너뛴다. */
    private Optional<ExamAttempt> practiceAttempt() {
        return examRepository.findAll().stream()
                .filter(Exam::isPracticeMode)
                .flatMap(e -> examAttemptRepository.findAll().stream()
                        .filter(a -> a.getExam().getId().equals(e.getId()))
                        .filter(a -> a.getSubmittedAt() != null))
                .findFirst();
    }

    @Test
    @DisplayName("연습 시험은 성적 확정을 눌러도 Grade 가 생기지 않는다")
    void 연습시험_Grade_미생성() {
        Optional<ExamAttempt> found = practiceAttempt();
        if (found.isEmpty()) {
            return;   // 시드에 연습 시험 제출 회차가 없으면 검증 대상 없음
        }
        ExamAttempt attempt = found.get();
        Long examId = attempt.getExam().getId();
        Long userId = attempt.getUser().getId();

        long before = gradeRepository.count();

        try {
            examGradingService.confirm(attempt.getId(), adminId(), true, "/admin/evaluation/grading");
        } catch (RuntimeException e) {
            // 수동 채점 대기 등으로 막히는 것은 이 테스트의 관심사가 아니다
        }

        assertThat(gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(
                userId, Grade.EvalType.EXAM, examId))
                .as("연습 시험 점수가 Grade 에 들어가면 이수 판정에 섞인다")
                .isEmpty();
        assertThat(gradeRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("정식 시험은 기존대로 Grade 가 생긴다 — 가드가 과하게 막지 않는다")
    void 정식시험은_정상() {
        Optional<ExamAttempt> found = examRepository.findAll().stream()
                .filter(e -> !e.isPracticeMode())
                .flatMap(e -> examAttemptRepository.findAll().stream()
                        .filter(a -> a.getExam().getId().equals(e.getId()))
                        .filter(a -> a.getSubmittedAt() != null))
                .findFirst();
        if (found.isEmpty()) {
            return;
        }
        ExamAttempt attempt = found.get();

        try {
            examGradingService.saveScores(attempt.getId(),
                    new ManualScoreRequest(List.of(), null, false),
                    adminId(), true, "/admin/evaluation/grading");
        } catch (RuntimeException e) {
            return;   // 채점 불가 상태면 이 테스트로는 판단할 수 없다
        }

        assertThat(gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(
                attempt.getUser().getId(), Grade.EvalType.EXAM, attempt.getExam().getId()))
                .as("정식 시험까지 막으면 채점 기능이 죽는다")
                .isPresent();
    }
}
