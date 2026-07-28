package com.ssa.lms.grading.service;

import com.ssa.lms.dashboard.dto.InstructorDashboardView;
import com.ssa.lms.dashboard.service.InstructorDashboardService;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.entity.ExamAttempt.AttemptStatus;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.grading.dto.AttemptGradingDetail;
import com.ssa.lms.proctor.dto.LiveMonitorView;
import com.ssa.lms.proctor.service.ProctorMonitorService;
import com.ssa.lms.proctor.service.ProctorMonitorService.ProctorUrls;
import com.ssa.lms.proctor.service.ProctorViewer;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 강사 화면(감독·채점·대시보드)이 {@code assignedSetNo=null} 회차에도 500 없이 뜨는지 고정한다.
 *
 * <p><b>배경:</b> 훈련생 홈/시험탭 500 은 세트 번호 필드를 원시 int 로 언박싱한 DTO 때문이었다.
 * 사용자가 "강사쪽도 같은 에러가 날 것 같다"고 하여 강사가 <b>같은 응시 회차 데이터를 집계하는</b>
 * 경로 전부를 검증한다. 강사 서비스는 {@code assignedSetNo} 를 읽지 않고
 * {@code exam.getExamQuestions()}(전체 문항)로 집계하므로 이론상 안전하지만,
 * 계약상 허용되는 legacy null-set 회차를 실제로 주입해 결정적으로 확인한다.</p>
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class InstructorNullSetAttemptTest {

    @Autowired ProctorMonitorService proctorMonitorService;
    @Autowired ExamGradingService examGradingService;
    @Autowired InstructorDashboardService instructorDashboardService;
    @Autowired ExamRepository examRepository;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired UserRepository userRepository;

    private User instructor() {
        return userRepository.findByLoginId("instructor1").orElseThrow();
    }

    private ProctorViewer instructorViewer() {
        return new ProctorViewer(instructor().getId(), Role.INSTRUCTOR);
    }

    /** instructor1 담당 과정의, 감독·채점 대상이 되는 시험(채점검증 서술형 포함 — CLOSED). */
    private Exam monitoredGradableExam() {
        return examRepository.findAll().stream()
                .filter(e -> e.getExamName().contains("[채점검증] 서술형"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "채점검증 시험 시드가 없다 — LocalGradingDataInitializer 확인"));
    }

    /** 세트 미배정(null) 회차 저장. attemptNo 를 높게 잡아 시드 회차와 유니크 충돌을 피한다. */
    private ExamAttempt saveNullSetAttempt(Exam exam, User user, int attemptNo,
                                           AttemptStatus status, LocalDateTime expiresAt) {
        LocalDateTime started = LocalDateTime.now().minusMinutes(50);
        return examAttemptRepository.saveAndFlush(ExamAttempt.builder()
                .exam(exam).user(user).attemptNo(attemptNo)
                // .assignedSetNo(...) 미지정 → null (legacy 회차)
                .startedAt(started)
                .expiresAt(expiresAt)
                .status(status)
                .identityVerifiedAt(started).identityVerifyMethod("PASSWORD")
                .ip("127.0.0.1").userAgent("test")
                .build());
    }

    @Test
    @DisplayName("감독 목록/실시간 감독이 무세트 만료 진행중 회차가 있어도 500 없이 뜬다")
    void 감독화면_무세트_회차_성공() {
        Exam exam = monitoredGradableExam();
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        ExamAttempt aged = saveNullSetAttempt(exam, trainee, 950,
                AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> proctorMonitorService.monitoringList(instructorViewer(), "/x/"))
                .doesNotThrowAnyException();

        LiveMonitorView[] holder = new LiveMonitorView[1];
        assertThatCode(() -> holder[0] = proctorMonitorService.live(
                exam.getId(), instructorViewer(), new ProctorUrls("/x/", "/list")))
                .as("무세트 회차가 실시간 감독 집계를 NPE 로 막으면 안 된다")
                .doesNotThrowAnyException();
        // 재현 경로를 실제로 탔는지 — 그 회차가 감독 목록에 잡혀야 한다.
        assertThat(holder[0].rows())
                .anyMatch(r -> r.attemptId().equals(aged.getId()));
    }

    @Test
    @DisplayName("채점 목록/현황/응시자목록/채점팝업이 무세트 제출 회차가 있어도 500 없이 뜬다")
    void 채점화면_무세트_회차_성공() {
        Exam exam = monitoredGradableExam();
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        ExamAttempt submitted = saveNullSetAttempt(exam, trainee, 951,
                AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(10));
        submitted.submit(LocalDateTime.now().minusMinutes(9), AttemptStatus.SUBMITTED);
        Long instId = instructor().getId();

        assertThatCode(() -> {
            examGradingService.examList(null, null, null, instId, false, "/g");
            examGradingService.summary(exam.getId(), instId, false, "/g");
            examGradingService.attemptList(exam.getId(), instId, false, "/g");
        }).as("무세트 회차가 채점 목록/현황 집계를 막으면 안 된다").doesNotThrowAnyException();

        AttemptGradingDetail[] holder = new AttemptGradingDetail[1];
        assertThatCode(() -> holder[0] =
                examGradingService.attemptDetail(submitted.getId(), instId, false, "/g"))
                .as("무세트 회차 채점 팝업이 NPE 로 막히면 안 된다")
                .doesNotThrowAnyException();
        assertThat(holder[0].questions()).isNotEmpty(); // 전체 문항으로 집계됨
    }

    @Test
    @DisplayName("강사 대시보드가 무세트 만료 진행중 회차가 있어도 500 없이 뜬다")
    void 강사대시보드_무세트_회차_성공() {
        Exam exam = monitoredGradableExam();
        User trainee = userRepository.findByLoginId("trainee1").orElseThrow();
        saveNullSetAttempt(exam, trainee, 952,
                AttemptStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));

        assertThatCode(() -> {
            InstructorDashboardView view =
                    instructorDashboardService.load(instructor().getId(), instructor().getName());
            assertThat(view).isNotNull();
        }).as("강사 홈이 응시 집계 NPE 로 통째로 막히면 안 된다").doesNotThrowAnyException();
    }
}
