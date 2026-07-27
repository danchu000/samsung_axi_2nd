package com.ssa.lms.proctor.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.proctor.dto.EventLogRow;
import com.ssa.lms.proctor.dto.LiveAttemptRow;
import com.ssa.lms.proctor.dto.LiveMonitorView;
import com.ssa.lms.proctor.dto.MonitoringRow;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.repository.ProctorMonitorRepository;
import com.ssa.lms.proctor.repository.ProctorWarningRepository;
import com.ssa.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시험 모니터링(감독) 조회 — 내역서 "부정행위 방지" 증빙과 직결된다.
 *
 * <p><b>이 서비스는 ExamEventLog 를 읽기만 한다.</b> append-only 계약(앞 슬라이스가 확립)을
 * 유지하려고 수정/삭제 경로를 아예 두지 않았다. 감독 화면이 하는 유일한 쓰기는
 * {@link ProctorWarningService}(경고 발송)와 {@link #voidAttempt} (무효 처리)뿐이다.</p>
 *
 * <p><b>권한</b> — 관리자는 전체, 강사는 담당 과정만
 * ({@code CourseQueryService.isInstructorOf}). 화면에서 감추는 것과 별개로
 * 조회·발송·무효 처리 <b>모든 진입점</b>에서 서버가 다시 검사한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProctorMonitorService {

    /** 감독 대상 시험 상태. DRAFT(작성중)는 응시가 있을 수 없어 목록에서 뺀다. */
    private static final Collection<Exam.ExamStatus> MONITORED_STATUSES = EnumSet.of(
            Exam.ExamStatus.SCHEDULED, Exam.ExamStatus.OPEN,
            Exam.ExamStatus.CLOSED, Exam.ExamStatus.ARCHIVED);

    private final ProctorMonitorRepository proctorMonitorRepository;
    private final ProctorWarningRepository proctorWarningRepository;
    private final ExamEventLogService examEventLogService;
    private final CourseQueryService courseQueryService;
    private final ExamRefRepository examRefRepository;

    /* ===================== 모니터링 목록 ===================== */

    /**
     * 진행 중인 시험별 응시 현황.
     *
     * <p>수치 정의 — 화면 헤더가 "응시중 / 완료 / 미응시" 세 개다.</p>
     * <ul>
     *   <li>응시중 = IN_PROGRESS</li>
     *   <li>완료 = SUBMITTED + AUTO_SUBMITTED (자동 제출도 제출이다)</li>
     *   <li>미응시 = 수강생 수 - 응시 기록이 하나라도 있는 인원</li>
     *   <li>무효(VOIDED)는 셋 중 어디에도 넣지 않고 따로 센다 — 완료로 세면 부정행위가 정상 응시로 보인다</li>
     * </ul>
     */
    public List<MonitoringRow> monitoringList(ProctorViewer viewer, String detailUrlPrefix) {
        List<Exam> exams = viewer.isAdmin()
                ? proctorMonitorRepository.findMonitoredExams(MONITORED_STATUSES)
                : findExamsForInstructor(viewer.userId());
        if (exams.isEmpty()) {
            return List.of();
        }

        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, Map<ExamAttempt.AttemptStatus, Long>> byStatus = statusCounts(examIds);
        Map<Long, Long> attemptedUsers = toCountMap(
                proctorMonitorRepository.countDistinctAttemptUsersByExam(examIds));

        // 수강생 수는 과정 단위라 과정별로 한 번만 센다 (시험 수만큼 세면 중복 조회다).
        Map<Long, Integer> enrolledByCourse = new HashMap<>();

        List<MonitoringRow> rows = new ArrayList<>(exams.size());
        int number = 0;
        for (Exam exam : exams) {
            Course course = exam.getCourse();
            int enrolled = enrolledByCourse.computeIfAbsent(course.getId(),
                    id -> courseQueryService.findUserIdsByCourseId(id).size());

            Map<ExamAttempt.AttemptStatus, Long> counts =
                    byStatus.getOrDefault(exam.getId(), Map.of());
            int inProgress = intOf(counts.get(ExamAttempt.AttemptStatus.IN_PROGRESS));
            int completed = intOf(counts.get(ExamAttempt.AttemptStatus.SUBMITTED))
                    + intOf(counts.get(ExamAttempt.AttemptStatus.AUTO_SUBMITTED));
            int voided = intOf(counts.get(ExamAttempt.AttemptStatus.VOIDED));
            int attempted = intOf(attemptedUsers.get(exam.getId()));
            int notStarted = Math.max(enrolled - attempted, 0);

            rows.add(new MonitoringRow(
                    ++number,
                    exam.getId(),
                    course.getCourseName(),
                    course.getCourseCode(),
                    exam.getExamName(),
                    exam.getInstructor() == null ? "-" : exam.getInstructor().getName(),
                    exam.getWindowStart().format(ProctorLabels.DATE),
                    exam.getWindowStart().format(ProctorLabels.TIME) + " ~ "
                            + exam.getWindowEnd().format(ProctorLabels.TIME),
                    "-", "-",   // 남은시간/경과시간은 화면 JS(exams.js)가 data-start/end 로 매초 계산한다
                    inProgress, completed, notStarted, voided, enrolled,
                    exam.getWindowStart().format(ProctorLabels.ISO),
                    exam.getWindowEnd().format(ProctorLabels.ISO),
                    detailUrlPrefix + exam.getId()));
        }
        return rows;
    }

    /* ===================== 실시간 감독 화면 ===================== */

    /** 응시자별 상태 · 남은 시간 · 이상행위 카운트. 카메라 스트리밍은 이 슬라이스 범위 밖이다. */
    public LiveMonitorView live(Long examId, ProctorViewer viewer, ProctorUrls urls) {
        Exam exam = proctorMonitorRepository.findExamForMonitoring(examId)
                .orElseThrow(() -> new ProctorNotFoundException("시험을 찾을 수 없습니다: " + examId));
        assertCanMonitor(exam, viewer);

        List<ExamAttempt> attempts = proctorMonitorRepository.findAttemptsByExam(examId);
        List<Long> attemptIds = attempts.stream().map(ExamAttempt::getId).toList();

        Map<Long, Map<ExamEventLog.Severity, Long>> events = eventCounts(attemptIds);
        Map<Long, Long> warnings = attemptIds.isEmpty()
                ? Map.of()
                : toCountMap(proctorWarningRepository.countByAttemptIds(attemptIds));

        LocalDateTime now = LocalDateTime.now();
        List<LiveAttemptRow> rows = new ArrayList<>(attempts.size());
        int inProgress = 0;
        int completed = 0;
        int voided = 0;
        for (ExamAttempt attempt : attempts) {
            Map<ExamEventLog.Severity, Long> bySeverity =
                    events.getOrDefault(attempt.getId(), Map.of());
            long warn = longOf(bySeverity.get(ExamEventLog.Severity.WARN));
            long critical = longOf(bySeverity.get(ExamEventLog.Severity.CRITICAL));
            long info = longOf(bySeverity.get(ExamEventLog.Severity.INFO));

            boolean live = attempt.getStatus() == ExamAttempt.AttemptStatus.IN_PROGRESS;
            long remain = remainSeconds(attempt, now);
            switch (attempt.getStatus()) {
                case IN_PROGRESS -> inProgress++;
                case SUBMITTED, AUTO_SUBMITTED -> completed++;
                case VOIDED -> voided++;
                default -> { /* NOT_STARTED / ABANDONED 는 별도 집계하지 않는다 */ }
            }

            User user = attempt.getUser();
            rows.add(new LiveAttemptRow(
                    attempt.getId(),
                    user.getId(),
                    user.getName(),
                    user.getLoginId(),
                    attempt.getAttemptNo() == null ? 1 : attempt.getAttemptNo(),
                    attempt.getStatus().name(),
                    ProctorLabels.attemptStatus(attempt.getStatus()),
                    format(attempt.getStartedAt()),
                    format(attempt.getExpiresAt()),
                    format(attempt.getSubmittedAt()),
                    remain,
                    live ? ProctorLabels.duration(remain) : "-",
                    info + warn + critical,
                    warn,
                    critical,
                    longOf(warnings.get(attempt.getId())),
                    attempt.getIp() == null ? "-" : attempt.getIp(),
                    live));
        }

        int enrolled = courseQueryService.findUserIdsByCourseId(exam.getCourse().getId()).size();
        long attemptedUsers = attempts.stream().map(a -> a.getUser().getId()).distinct().count();

        return new LiveMonitorView(
                exam.getId(),
                exam.getExamName(),
                exam.getCourse().getCourseName(),
                exam.getCourse().getCourseCode(),
                exam.getInstructor() == null ? "-" : exam.getInstructor().getName(),
                exam.getWindowStart().format(ProctorLabels.DATE_TIME),
                exam.getWindowEnd().format(ProctorLabels.DATE_TIME),
                exam.getTimeLimitMin(),
                exam.isProctorEnabled(),
                exam.isRequireWebcam(),
                inProgress, completed,
                Math.max(enrolled - (int) attemptedUsers, 0),
                voided, enrolled,
                viewer.isAdmin(),
                urls.attemptUrlPrefix(), urls.listUrl(),
                rows);
    }

    /* ===================== 입·퇴장 로그 ===================== */

    /**
     * 응시자 1명의 전체 이벤트 로그 (시각 · 유형 · 심각도 · IP).
     *
     * <p>{@code ExamEventLogService.findByAttempt} 를 그대로 쓴다 — 조회 경로를 새로 만들지 않는다.</p>
     */
    public List<EventLogRow> events(Long attemptId, ProctorViewer viewer) {
        ExamAttempt attempt = requireAttempt(attemptId);
        assertCanMonitor(attempt.getExam(), viewer);

        return examEventLogService.findByAttempt(attemptId).stream()
                .map(log -> new EventLogRow(
                        log.getId(),
                        format(log.getOccurredAt()),
                        log.getEventType().name(),
                        ProctorLabels.eventType(log.getEventType()),
                        log.getSeverity().name(),
                        ProctorLabels.severity(log.getSeverity()),
                        log.getDetail(),
                        log.getIp() == null ? "-" : log.getIp()))
                .toList();
    }

    /* ===================== 응시 무효 처리 ===================== */

    /**
     * 부정행위로 응시를 무효 처리한다. <b>관리자만</b> — 권한정의서(1) 16행.
     *
     * <p>강사는 경고 발송까지만 할 수 있다 ({@link ProctorWarningService#send}).
     * SecurityConfig 는 {@code /admin/evaluation/**} 을 강사에게도 열어두므로
     * URL 만으로는 막히지 않는다. 여기서 역할을 직접 본다.</p>
     */
    @Transactional
    public void voidAttempt(Long attemptId, ProctorViewer viewer, String reason) {
        if (!viewer.isAdmin()) {
            throw new ProctorAccessDeniedException("응시 무효 처리는 관리자만 가능합니다.");
        }
        ExamAttempt attempt = requireAttempt(attemptId);
        User admin = examRefRepository.findUser(viewer.userId())
                .orElseThrow(() -> new IllegalStateException("처리자를 찾을 수 없습니다: " + viewer.userId()));
        attempt.voidAttempt(admin, reason == null || reason.isBlank() ? "부정행위 적발" : reason.strip());
    }

    /* ===================== 권한 ===================== */

    /** 담당 과정 판정. 강사가 담당하지 않는 과정이면 403. */
    public void assertCanMonitor(Exam exam, ProctorViewer viewer) {
        if (viewer.isAdmin()) {
            return;
        }
        if (viewer.isInstructor()
                && courseQueryService.isInstructorOf(viewer.userId(), exam.getCourse().getId())) {
            return;
        }
        throw new ProctorAccessDeniedException("담당 과정이 아닙니다.");
    }

    public ExamAttempt requireAttempt(Long attemptId) {
        return proctorMonitorRepository.findAttemptWithExamAndUser(attemptId)
                .orElseThrow(() -> new ProctorNotFoundException("응시 회차를 찾을 수 없습니다: " + attemptId));
    }

    /**
     * 강사가 담당하는 과정의 시험.
     *
     * <p>{@code CourseQueryService} 에는 "내가 담당하는 과정 목록" 계약이 아직 없어서
     * 전체 과정을 훑고 {@code isInstructorOf} 로 거른다. 과정 수가 커지면 A에게
     * {@code findCourseIdsByInstructorId} 를 요청할 것 (a-requests 후보).</p>
     */
    private List<Exam> findExamsForInstructor(Long instructorId) {
        List<Long> courseIds = examRefRepository.findAllCourses().stream()
                .map(Course::getId)
                .filter(courseId -> courseQueryService.isInstructorOf(instructorId, courseId))
                .toList();
        return courseIds.isEmpty()
                ? List.of()
                : proctorMonitorRepository.findMonitoredExamsByCourses(MONITORED_STATUSES, courseIds);
    }

    /* ===================== 내부 ===================== */

    private Map<Long, Map<ExamAttempt.AttemptStatus, Long>> statusCounts(List<Long> examIds) {
        Map<Long, Map<ExamAttempt.AttemptStatus, Long>> result = new HashMap<>();
        for (Object[] row : proctorMonitorRepository.countAttemptsByExamAndStatus(examIds)) {
            result.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((ExamAttempt.AttemptStatus) row[1], (Long) row[2]);
        }
        return result;
    }

    private Map<Long, Map<ExamEventLog.Severity, Long>> eventCounts(List<Long> attemptIds) {
        if (attemptIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<ExamEventLog.Severity, Long>> result = new HashMap<>();
        for (Object[] row : proctorMonitorRepository.countEventsByAttemptAndSeverity(attemptIds)) {
            result.computeIfAbsent((Long) row[0], k -> new HashMap<>())
                    .put((ExamEventLog.Severity) row[1], (Long) row[2]);
        }
        return result;
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    /**
     * 남은 시간은 <b>서버가 저장한 expiresAt</b> 으로만 계산한다.
     * 화면 타이머는 표시용이라 감독 화면이 그 값을 믿으면 안 된다 (응시 슬라이스와 같은 원칙).
     */
    private long remainSeconds(ExamAttempt attempt, LocalDateTime now) {
        if (attempt.getStatus() != ExamAttempt.AttemptStatus.IN_PROGRESS
                || attempt.getExpiresAt() == null) {
            return 0;
        }
        long seconds = Duration.between(now, attempt.getExpiresAt()).getSeconds();
        return Math.max(seconds, 0);
    }

    private String format(LocalDateTime at) {
        return at == null ? "-" : at.format(ProctorLabels.DATE_TIME);
    }

    private int intOf(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private long longOf(Long value) {
        return value == null ? 0L : value;
    }

    /** 화면이 쓸 액션 URL 묶음. 관리자/강사 화면이 같은 뷰 모델을 쓰되 URL 만 다르다. */
    public record ProctorUrls(String attemptUrlPrefix, String listUrl) {
    }
}
