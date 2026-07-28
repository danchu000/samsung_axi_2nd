package com.ssa.lms.exam.service;

import com.ssa.lms.dashboard.dto.TraineeDashboardView;
import com.ssa.lms.dashboard.service.TraineeDashboardService;
import com.ssa.lms.exam.dto.AttemptResultView;
import com.ssa.lms.exam.dto.AttemptView;
import com.ssa.lms.exam.dto.ExamTakeRow;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.entity.ExamAttempt.AttemptStatus;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 회귀 고정 — {@code assignedSetNo=null} 인 응시 회차가 있어도 학생 홈·시험 목록·응시 화면·결과 패널이
 * 500 없이 로딩된다.
 *
 * <p><b>배경(실제 재현):</b> {@link ExamAttempt#getAssignedSetNo()} 는 계약상 {@code null} 일 수 있다
 * (세트 기능 도입 이전 회차). 그런데 결과/응시 화면 DTO 가 세트 번호를 원시 {@code int} 로 받아
 * {@code null} 을 언박싱하다 NPE 가 났다. 재현 스택:</p>
 * <pre>
 *   NPE getAssignedSetNo() is null
 *     at ExamAttemptService.buildResult(:511) &lt;- finish(:455) &lt;- availableExams(:106)
 *     &lt;- TraineeDashboardService.todos(:185) &lt;- ModuleHomeController.traineeHome
 * </pre>
 *
 * <p>local 시더가 세트 없이 만든 진행중 회차가 기동 40분+ 뒤 만료 자동제출되며 이 경로를 탔다.
 * 시더는 이제 세트를 박도록 고쳤으므로, 이 테스트는 <b>계약상 허용되는 legacy null 회차</b>를
 * 직접 만들어 결정적으로 재현한다 — 시드가 다시 바뀌어도 이 방어가 유지되는지 고정한다.</p>
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ExpiredNullSetAttemptTest {

    @Autowired ExamAttemptService examAttemptService;
    @Autowired TraineeDashboardService traineeDashboardService;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired ExamRepository examRepository;
    @Autowired UserRepository userRepository;

    private User trainee() {
        return userRepository.findByLoginId("trainee1").orElseThrow();
    }

    /** 이 훈련생의 시험 목록에 실제로 뜨는 시험 하나 (수강 중 + 노출 상태). */
    private Exam visibleExamFor(User trainee) {
        List<ExamTakeRow> rows = examAttemptService.availableExams(trainee.getId());
        assertThat(rows).as("시드에 훈련생이 응시 가능한 시험이 있어야 재현 경로가 성립한다").isNotEmpty();
        Long examId = Long.valueOf(rows.get(0).id());
        return examRepository.findById(examId).orElseThrow();
    }

    /**
     * 세트 미배정({@code assignedSetNo=null}) 회차를 하나 만들어 저장한다.
     * {@code attemptNo} 는 높은 값으로 잡아 시드 회차와 유니크 충돌을 피한다.
     */
    private ExamAttempt saveNullSetAttempt(Exam exam, User user, int attemptNo,
                                           AttemptStatus status, LocalDateTime expiresAt) {
        LocalDateTime started = LocalDateTime.now().minusMinutes(30);
        return examAttemptRepository.saveAndFlush(ExamAttempt.builder()
                .exam(exam).user(user).attemptNo(attemptNo)
                // .assignedSetNo(...) 를 일부러 세팅하지 않는다 → null (legacy 회차)
                .startedAt(started)
                .expiresAt(expiresAt)
                .status(status)
                .identityVerifiedAt(started).identityVerifyMethod("PASSWORD")
                .ip("127.0.0.1").userAgent("test")
                .build());
    }

    @Test
    @DisplayName("만료된 무세트 진행중 회차가 있어도 시험 목록이 500 없이 뜨고, 그 회차는 자동제출로 닫힌다")
    void 만료_무세트_회차가_있어도_시험목록_성공() {
        User trainee = trainee();
        Exam exam = visibleExamFor(trainee);
        ExamAttempt aged = saveNullSetAttempt(exam, trainee, 901,
                AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> examAttemptService.availableExams(trainee.getId()))
                .as("만료된 무세트 진행중 회차 자동제출 중 세트번호 언박싱 NPE 가 나면 안 된다")
                .doesNotThrowAnyException();

        // 만료 회차는 자동제출로 닫혀야 한다 (IN_PROGRESS 로 남으면 재응시 횟수를 잡아먹는다).
        ExamAttempt after = examAttemptRepository.findById(aged.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(AttemptStatus.AUTO_SUBMITTED);
    }

    @Test
    @DisplayName("만료된 무세트 진행중 회차가 있어도 학생 홈 대시보드가 500 없이 뜬다")
    void 만료_무세트_회차가_있어도_학생홈_성공() {
        User trainee = trainee();
        Exam exam = visibleExamFor(trainee);
        saveNullSetAttempt(exam, trainee, 902,
                AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> {
            TraineeDashboardView view = traineeDashboardService.load(trainee.getId(), "테스트");
            assertThat(view).isNotNull();
        }).as("학생 홈이 시험 집계(availableExams)의 NPE 로 통째로 막히면 안 된다")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("무세트 진행중 회차의 응시 화면(이어하기)이 500 없이 뜬다")
    void 무세트_회차_응시화면_성공() {
        User trainee = trainee();
        Exam exam = visibleExamFor(trainee);
        // 만료되지 않은 진행중 회차 → loadAttempt 가 AttemptView 를 만든다 (세트번호 필드 언박싱 지점).
        ExamAttempt live = saveNullSetAttempt(exam, trainee, 903,
                AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(30));

        assertThatCode(() -> {
            AttemptView view = examAttemptService.loadAttempt(live.getId(), trainee.getId());
            assertThat(view.assignedSetNo()).isNull();  // legacy 회차 — null 이 그대로 내려온다
        }).as("무세트 회차 응시 화면이 세트번호 언박싱 NPE 로 막히면 안 된다")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("무세트 제출 회차의 결과 패널이 500 없이 뜬다")
    void 무세트_회차_결과패널_성공() {
        User trainee = trainee();
        Exam exam = visibleExamFor(trainee);
        ExamAttempt submitted = saveNullSetAttempt(exam, trainee, 904,
                AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> {
            AttemptResultView result = examAttemptService.loadResult(submitted.getId(), trainee.getId());
            assertThat(result).isNotNull();
        }).as("무세트 회차 결과 집계(buildResult)가 세트번호 언박싱 NPE 로 막히면 안 된다")
                .doesNotThrowAnyException();
    }
}
