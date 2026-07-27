package com.ssa.lms.proctor.service;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.proctor.dto.EventLogRow;
import com.ssa.lms.proctor.dto.LiveMonitorView;
import com.ssa.lms.proctor.dto.MonitoringRow;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.repository.ExamEventLogRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 시험 모니터링(감독) — 내역서 "부정행위 방지" 증빙과 직결되는 규칙을 고정한다.
 *
 * <p>여기서 지키는 것</p>
 * <ul>
 *   <li>응시중/완료/미응시 집계 정의 (무효는 완료로 세지 않는다)</li>
 *   <li>강사는 담당 과정만 (권한정의서 △)</li>
 *   <li>응시 무효 처리는 관리자만, 경고 발송은 관리자·강사 (권한정의서(1) 16행)</li>
 *   <li>심각도는 서버가 정한다 — 이벤트 타입에서 결정된 값이 그대로 조회된다</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class ProctorMonitorServiceTest {

    private static final ProctorMonitorService.ProctorUrls URLS =
            new ProctorMonitorService.ProctorUrls("/admin/evaluation/monitoring/attempt/",
                    "/admin/evaluation/monitoring");

    @Autowired ProctorMonitorService proctorMonitorService;
    @Autowired ProctorWarningService proctorWarningService;
    @Autowired ExamEventLogService examEventLogService;
    @Autowired ExamEventLogRepository examEventLogRepository;
    @Autowired ExamRepository examRepository;
    @Autowired ExamAttemptRepository examAttemptRepository;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseInstructorRepository courseInstructorRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    private User admin;
    private User owner;        // 담당 강사
    private User outsider;     // 담당하지 않는 강사
    private Exam exam;
    private ExamAttempt inProgress;
    private ExamAttempt submitted;

    private ProctorViewer adminViewer;
    private ProctorViewer ownerViewer;
    private ProctorViewer outsiderViewer;

    @BeforeEach
    void setUp() {
        admin = saveUser("proctor-admin", Role.ADMIN);
        owner = saveUser("proctor-owner", Role.INSTRUCTOR);
        outsider = saveUser("proctor-outsider", Role.INSTRUCTOR);

        Course course = courseRepository.save(Course.builder()
                .courseCode("COURSE-PROCTOR-" + System.nanoTime())
                .courseName("감독 테스트 과정")
                .cohort("1기")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusMonths(3))
                .capacity(30)
                .status(CourseStatus.IN_PROGRESS)
                .build());
        courseInstructorRepository.save(CourseInstructor.builder()
                .course(course).instructor(owner).primaryInstructor(true).build());

        // 수강생 3명 중 2명만 응시한다 → 미응시 1명이 나와야 한다
        List<User> trainees = List.of(
                saveUser("proctor-t1", Role.TRAINEE),
                saveUser("proctor-t2", Role.TRAINEE),
                saveUser("proctor-t3", Role.TRAINEE));
        for (User trainee : trainees) {
            enrollmentRepository.save(Enrollment.builder()
                    .trainee(trainee).course(course)
                    .status(EnrollmentStatus.APPROVED)
                    .appliedAt(LocalDateTime.now().minusDays(5))
                    .build());
        }

        exam = examRepository.save(Exam.builder()
                .examName("감독 테스트 시험")
                .examType(Exam.ExamType.UNIT)
                .course(course).instructor(owner)
                .timeLimitMin(60)
                .autoScore(10).manualScore(0).totalScore(10).passScore(6)
                .randomOrder(false).retakeAllowed(false).maxAttempts(1)
                .windowStart(LocalDateTime.now().minusHours(1))
                .windowEnd(LocalDateTime.now().plusHours(1))
                .requireIdentityVerification(true)
                .proctorEnabled(true).requireWebcam(true)
                .blockTabSwitch(true).blockCopyPaste(true)
                .status(Exam.ExamStatus.OPEN)
                .build());

        inProgress = examAttemptRepository.save(attempt(trainees.get(0),
                ExamAttempt.AttemptStatus.IN_PROGRESS, LocalDateTime.now().plusMinutes(30)));
        submitted = examAttemptRepository.save(attempt(trainees.get(1),
                ExamAttempt.AttemptStatus.SUBMITTED, LocalDateTime.now().minusMinutes(5)));

        adminViewer = new ProctorViewer(admin.getId(), Role.ADMIN);
        ownerViewer = new ProctorViewer(owner.getId(), Role.INSTRUCTOR);
        outsiderViewer = new ProctorViewer(outsider.getId(), Role.INSTRUCTOR);
    }

    @Test
    @DisplayName("모니터링 목록: 응시중 1 / 완료 1 / 미응시 1 — 수강생 수에서 응시자를 뺀 값이 미응시다")
    void monitoringListCounts() {
        MonitoringRow row = findRow(proctorMonitorService.monitoringList(adminViewer, "/x/"));

        assertThat(row.inProgress()).isEqualTo(1);
        assertThat(row.completed()).isEqualTo(1);
        assertThat(row.notStarted()).isEqualTo(1);
        assertThat(row.enrolled()).isEqualTo(3);
        assertThat(row.voided()).isZero();
    }

    @Test
    @DisplayName("무효 처리한 응시는 완료로 세지 않는다 — 부정행위가 정상 응시로 보이면 안 된다")
    void voidedIsNotCountedAsCompleted() {
        proctorMonitorService.voidAttempt(submitted.getId(), adminViewer, "부정행위");

        MonitoringRow row = findRow(proctorMonitorService.monitoringList(adminViewer, "/x/"));
        assertThat(row.completed()).isZero();
        assertThat(row.voided()).isEqualTo(1);
        // 응시 기록 자체는 남아 있으므로 미응시는 늘지 않는다
        assertThat(row.notStarted()).isEqualTo(1);
    }

    @Test
    @DisplayName("실시간 감독: 남은 시간은 서버가 저장한 expiresAt 으로만 계산한다")
    void liveViewUsesServerExpiry() {
        LiveMonitorView view = proctorMonitorService.live(exam.getId(), adminViewer, URLS);

        assertThat(view.inProgress()).isEqualTo(1);
        assertThat(view.completed()).isEqualTo(1);
        assertThat(view.canVoid()).isTrue();

        var live = view.rows().stream()
                .filter(r -> r.attemptId().equals(inProgress.getId())).findFirst().orElseThrow();
        assertThat(live.remainSeconds()).isBetween(1700L, 1800L);

        // 제출된 회차는 남은 시간이 0 이어야 한다 (만료 시각이 미래여도 마찬가지)
        var done = view.rows().stream()
                .filter(r -> r.attemptId().equals(submitted.getId())).findFirst().orElseThrow();
        assertThat(done.remainSeconds()).isZero();
    }

    @Test
    @DisplayName("심각도는 서버가 정한다 — 이벤트 타입에서 결정된 값이 그대로 조회된다")
    void severityIsDecidedByServer() {
        examEventLogService.append(inProgress, ExamEventLog.EventType.ENTER, null, "10.0.0.1");
        examEventLogService.append(inProgress, ExamEventLog.EventType.TAB_BLUR, null, "10.0.0.1");
        examEventLogService.append(inProgress, ExamEventLog.EventType.MULTI_FACE, null, "10.0.0.1");

        List<EventLogRow> rows = proctorMonitorService.events(inProgress.getId(), adminViewer);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(EventLogRow::eventType, EventLogRow::severity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ENTER", "INFO"),
                        org.assertj.core.groups.Tuple.tuple("TAB_BLUR", "WARN"),
                        org.assertj.core.groups.Tuple.tuple("MULTI_FACE", "CRITICAL"));
    }

    @Test
    @DisplayName("알 수 없는 이벤트 타입은 파싱 단계에서 거부된다 (저장 자체를 하지 않는다)")
    void unknownEventTypeIsRejected() {
        assertThat(examEventLogService.parseType("NOT_A_REAL_EVENT")).isNull();
        assertThat(examEventLogService.parseType("tab_blur")).isEqualTo(ExamEventLog.EventType.TAB_BLUR);
    }

    @Test
    @DisplayName("강사는 담당 과정만 볼 수 있다 — 담당이 아니면 목록에도 없고 상세는 403")
    void instructorScopedToOwnCourses() {
        assertThat(proctorMonitorService.monitoringList(ownerViewer, "/x/"))
                .extracting(MonitoringRow::examId).contains(exam.getId());

        assertThat(proctorMonitorService.monitoringList(outsiderViewer, "/x/"))
                .extracting(MonitoringRow::examId).doesNotContain(exam.getId());

        assertThatThrownBy(() -> proctorMonitorService.live(exam.getId(), outsiderViewer, URLS))
                .isInstanceOf(ProctorAccessDeniedException.class);
        assertThatThrownBy(() -> proctorMonitorService.events(inProgress.getId(), outsiderViewer))
                .isInstanceOf(ProctorAccessDeniedException.class);
    }

    @Test
    @DisplayName("응시 무효 처리는 관리자만 — 담당 강사여도 막힌다 (권한정의서(1) 16행)")
    void onlyAdminCanVoid() {
        assertThatThrownBy(() -> proctorMonitorService.voidAttempt(inProgress.getId(), ownerViewer, "사유"))
                .isInstanceOf(ProctorAccessDeniedException.class);

        proctorMonitorService.voidAttempt(inProgress.getId(), adminViewer, "부정행위 적발");

        ExamAttempt reloaded = examAttemptRepository.findById(inProgress.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ExamAttempt.AttemptStatus.VOIDED);
        assertThat(reloaded.getVoidReason()).isEqualTo("부정행위 적발");
    }

    @Test
    @DisplayName("경고 발송은 담당 강사도 가능하고, 담당이 아닌 강사는 막힌다")
    void warningAllowedForInstructorOfCourse() {
        var sent = proctorWarningService.send(inProgress.getId(), ownerViewer, "탭 전환이 감지되었습니다.");
        assertThat(sent.message()).isEqualTo("탭 전환이 감지되었습니다.");
        assertThat(sent.acknowledged()).isFalse();

        assertThatThrownBy(() -> proctorWarningService.send(inProgress.getId(), outsiderViewer, "x"))
                .isInstanceOf(ProctorAccessDeniedException.class);
    }

    @Test
    @DisplayName("응시자는 본인 경고만 볼 수 있고, 확인하면 목록에서 빠진다")
    void traineeSeesOwnWarningsOnly() {
        var sent = proctorWarningService.send(inProgress.getId(), adminViewer, "경고합니다.");
        Long ownerId = inProgress.getUser().getId();

        assertThat(proctorWarningService.findPendingForTrainee(inProgress.getId(), ownerId)).hasSize(1);

        assertThatThrownBy(() -> proctorWarningService
                .findPendingForTrainee(inProgress.getId(), submitted.getUser().getId()))
                .isInstanceOf(ProctorAccessDeniedException.class);

        proctorWarningService.acknowledge(sent.id(), ownerId);
        assertThat(proctorWarningService.findPendingForTrainee(inProgress.getId(), ownerId)).isEmpty();
    }

    @Test
    @DisplayName("append-only: 감독 기능을 거쳐도 이벤트 로그 건수는 줄지 않는다")
    void eventLogIsAppendOnly() {
        examEventLogService.append(inProgress, ExamEventLog.EventType.TAB_BLUR, null, "10.0.0.1");
        long before = examEventLogRepository.countByAttemptId(inProgress.getId());

        proctorMonitorService.events(inProgress.getId(), adminViewer);
        proctorWarningService.send(inProgress.getId(), adminViewer, "경고");
        proctorMonitorService.voidAttempt(inProgress.getId(), adminViewer, "무효");

        assertThat(examEventLogRepository.countByAttemptId(inProgress.getId())).isEqualTo(before);
    }

    /* ===== helper ===== */

    private MonitoringRow findRow(List<MonitoringRow> rows) {
        return rows.stream().filter(r -> r.examId().equals(exam.getId())).findFirst().orElseThrow();
    }

    private ExamAttempt attempt(User user, ExamAttempt.AttemptStatus status, LocalDateTime expiresAt) {
        return ExamAttempt.builder()
                .exam(exam).user(user).attemptNo(1)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .expiresAt(expiresAt)
                .status(status)
                .identityVerifiedAt(LocalDateTime.now().minusMinutes(30))
                .identityVerifyMethod("MOBILE")
                .ip("10.0.0.1")
                .build();
    }

    private User saveUser(String prefix, Role role) {
        return userRepository.save(User.builder()
                .loginId(prefix + "-" + System.nanoTime())
                .password("{noop}test")
                .name(prefix)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
