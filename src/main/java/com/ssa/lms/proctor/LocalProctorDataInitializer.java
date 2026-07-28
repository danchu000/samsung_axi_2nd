package com.ssa.lms.proctor;

import com.ssa.lms.course.entity.*;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.exam.entity.ExamQuestion;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.repository.ExamAttemptRepository;
import com.ssa.lms.exam.repository.ExamRefRepository;
import com.ssa.lms.exam.repository.ExamRepository;
import com.ssa.lms.exam.repository.QuestionRepository;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.entity.ExamRecording;
import com.ssa.lms.proctor.entity.ProctorWarning;
import com.ssa.lms.proctor.repository.ExamRecordingRepository;
import com.ssa.lms.proctor.repository.ProctorWarningRepository;
import com.ssa.lms.proctor.service.ExamEventLogService;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * local 프로필 전용 <b>시험 모니터링(감독) 슬라이스</b> 시드 (개발자 B).
 *
 * <p>다른 슬라이스의 시드 파일은 건드리지 않는다. 감독 화면 검증에 필요한 데이터는 전부 여기서 만든다.</p>
 *
 * <p><b>CommandLineRunner 를 쓰지 않는 이유</b> — A의 {@code LocalDataInitializer} 에 {@code @Order} 가 없어
 * (미지정 = LOWEST_PRECEDENCE) B 시드가 먼저 돌아 과정·사용자가 없는 상태로 실패한다.
 * 러너가 전부 끝난 뒤 발행되는 {@code ApplicationReadyEvent} 를 쓴다.
 * (다섯 슬라이스가 연속으로 밟은 함정이다)</p>
 *
 * <p>여기서 만드는 것</p>
 * <ul>
 *   <li>훈련생 4명 추가(trainee3~6) + 수강신청 — 응시중/완료/미응시 수치가 의미를 가지려면 인원이 있어야 한다</li>
 *   <li>감독 대상 시험 1건 + 응시 회차 6건 (응시중 2 · 제출 2 · 자동제출 0 · 무효 1 · 중단 1)</li>
 *   <li>이상행위 이벤트 로그 (INFO/WARN/CRITICAL 전부) — 심각도는 서버가 정한다</li>
 *   <li>녹화 3건 + 실제 재생 가능한 더미 파일 (웹 루트 밖 디렉터리)</li>
 *   <li><b>미담당 과정</b> 1개와 그 과정의 시험 — "강사가 담당 아닌 과정 접근 → 403" 검증용</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalProctorDataInitializer {

    /** 이 시드가 이미 돌았는지 판정하는 마커 시험명. */
    private static final String MARKER = "[감독검증] 실시간 감독 시험";
    private static final String OTHER_MARKER = "[감독검증] 미담당 과정 시험";

    private final ExamRepository examRepository;
    private final ExamRefRepository examRefRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExamEventLogService examEventLogService;
    private final ProctorWarningRepository proctorWarningRepository;
    private final ExamRecordingRepository examRecordingRepository;

    @Value("${lms.proctor.recording-dir:#{systemProperties['java.io.tmpdir']}/lms-proctor-recordings}")
    private String recordingDir;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!examRepository.search(null, List.of(Exam.ExamStatus.values()), null, "[감독검증]").isEmpty()) {
            return;
        }
        List<Course> courses = examRefRepository.findAllCourses();
        if (courses.isEmpty()) {
            log.warn("[local] 과정이 없어 감독 시드를 건너뛴다");
            return;
        }
        Course course = courses.get(0);
        User instructor = examRefRepository.findUsersByRole(Role.INSTRUCTOR).stream()
                .findFirst().orElse(null);
        Optional<Question> anyQuestion = questionRepository.findByQuestionCode("Q-0001");
        if (anyQuestion.isEmpty()) {
            log.warn("[local] 문제은행 시드가 없어 감독 시드를 건너뛴다 (Q-0001)");
            return;
        }

        List<User> trainees = seedTrainees(course);

        /* ---------- 1) 감독 대상 시험 ---------- */
        Exam exam = examRepository.save(monitoredExam(MARKER, course, instructor, anyQuestion.get(),
                Exam.ExamStatus.OPEN, now().minusMinutes(30), now().plusHours(2)));

        // 응시 회차: 응시중 2 / 제출 2 / 무효 1 / 중단 1, 나머지 훈련생은 미응시로 남는다.
        ExamAttempt live1 = attempt(exam, trainees.get(0), 1,
                now().minusMinutes(20), now().plusMinutes(40), ExamAttempt.AttemptStatus.IN_PROGRESS,
                "10.0.1.11");
        ExamAttempt live2 = attempt(exam, trainees.get(1), 1,
                now().minusMinutes(12), now().plusMinutes(48), ExamAttempt.AttemptStatus.IN_PROGRESS,
                "10.0.1.12");
        ExamAttempt done1 = attempt(exam, trainees.get(2), 1,
                now().minusMinutes(90), now().minusMinutes(30), ExamAttempt.AttemptStatus.SUBMITTED,
                "10.0.1.13");
        ExamAttempt done2 = attempt(exam, trainees.get(3), 1,
                now().minusMinutes(95), now().minusMinutes(35), ExamAttempt.AttemptStatus.AUTO_SUBMITTED,
                "10.0.1.14");
        ExamAttempt abandoned = attempt(exam, trainees.get(4), 1,
                now().minusMinutes(80), now().minusMinutes(20), ExamAttempt.AttemptStatus.ABANDONED,
                "10.0.1.15");
        examAttemptRepository.saveAll(List.of(live1, live2, done1, done2, abandoned));

        done1.submit(now().minusMinutes(35), ExamAttempt.AttemptStatus.SUBMITTED);
        done2.submit(now().minusMinutes(36), ExamAttempt.AttemptStatus.AUTO_SUBMITTED);

        /* ---------- 2) 이상행위 로그 ----------
           심각도를 시드가 직접 지정하지 않는다. 서버(ExamEventLogService)가 타입으로 정하는
           그 경로를 그대로 태워야 화면에 뜨는 값이 실제 운영과 같아진다. */
        appendEvents(live1, "10.0.1.11", List.of(
                ExamEventLog.EventType.ENTER,
                ExamEventLog.EventType.TAB_BLUR,
                ExamEventLog.EventType.TAB_FOCUS,
                ExamEventLog.EventType.COPY,
                ExamEventLog.EventType.MULTI_FACE));
        appendEvents(live2, "10.0.1.12", List.of(
                ExamEventLog.EventType.ENTER,
                ExamEventLog.EventType.NETWORK_DROP,
                ExamEventLog.EventType.RESUME));
        appendEvents(done1, "10.0.1.13", List.of(
                ExamEventLog.EventType.ENTER,
                ExamEventLog.EventType.EXIT));
        appendEvents(done2, "10.0.1.14", List.of(
                ExamEventLog.EventType.ENTER,
                ExamEventLog.EventType.FULLSCREEN_EXIT,
                ExamEventLog.EventType.NO_FACE,
                ExamEventLog.EventType.EXIT));
        appendEvents(abandoned, "10.0.1.15", List.of(
                ExamEventLog.EventType.ENTER,
                ExamEventLog.EventType.NETWORK_DROP));

        /* ---------- 3) 이미 발송된 경고 1건 ---------- */
        if (instructor != null) {
            proctorWarningRepository.save(ProctorWarning.builder()
                    .attempt(live1).sentBy(instructor)
                    .message("탭 전환이 반복 감지되었습니다. 시험 화면을 벗어나지 마세요.")
                    .sentAt(now().minusMinutes(5))
                    .build());
        }

        /* ---------- 4) 녹화 ---------- */
        seedRecordings(List.of(done1, done2, live1));

        /* ---------- 5) 미담당 과정 + 그 과정의 시험 ----------
           instructor1 을 CourseInstructor 로 붙이지 않는다. 이 시험을 강사가 열려고 하면 403 이어야 한다. */
        Course otherCourse = seedOtherCourse();
        Exam otherExam = examRepository.save(monitoredExam(OTHER_MARKER, otherCourse, null,
                anyQuestion.get(), Exam.ExamStatus.OPEN, now().minusMinutes(10), now().plusHours(3)));
        ExamAttempt otherAttempt = attempt(otherExam, trainees.get(0), 1,
                now().minusMinutes(5), now().plusMinutes(55), ExamAttempt.AttemptStatus.IN_PROGRESS,
                "10.0.9.9");
        examAttemptRepository.save(otherAttempt);
        appendEvents(otherAttempt, "10.0.9.9", List.of(ExamEventLog.EventType.ENTER));

        log.info("[local] 감독 검증 시드 생성 — examId={} (담당), examId={} (미담당), 응시 {}건",
                exam.getId(), otherExam.getId(), 6);
    }

    /* ===================== 내부 ===================== */

    /**
     * 감독 검증용 훈련생. 수치(응시중/완료/미응시)가 의미를 가지려면 최소 6명은 있어야 한다.
     * A 소유 리포지토리를 쓰는 지점이지만 local 시드에 한정한다 (운영 로직에서는 쓰지 않는다).
     */
    private List<User> seedTrainees(Course course) {
        List<User> trainees = new ArrayList<>();
        String[][] specs = {
                {"trainee1", "이훈련"}, {"trainee2", "박훈련"},
                {"trainee3", "최감독"}, {"trainee4", "정감독"},
                {"trainee5", "한감독"}, {"trainee6", "오감독"},
        };
        for (int i = 0; i < specs.length; i++) {
            String loginId = specs[i][0];
            String name = specs[i][1];
            int seq = i + 1;
            User user = userRepository.findByLoginId(loginId).orElseGet(() ->
                    userRepository.save(User.builder()
                            .loginId(loginId).password(passwordEncoder.encode("1234"))
                            .name(name).role(Role.TRAINEE).status(UserStatus.ACTIVE)
                            .email(loginId + "@ssa.local")
                            .phone(String.format("010-4000-%04d", seq))
                            .birthDate("2000-01-0" + Math.min(seq, 9))
                            .privacyConsentAt(LocalDateTime.now())
                            .thirdPartyConsentAt(LocalDateTime.now())
                            .build()));
            if (enrollmentRepository.findByTraineeIdAndCourseId(user.getId(), course.getId()).isEmpty()) {
                enrollmentRepository.save(Enrollment.builder()
                        .trainee(user).course(course)
                        .status(EnrollmentStatus.APPROVED)
                        .appliedAt(LocalDateTime.now().minusDays(8))
                        .build());
            }
            trainees.add(user);
        }
        return trainees;
    }

    /** 담당 강사가 없는 과정. 강사 권한 차단 검증에만 쓴다. */
    private Course seedOtherCourse() {
        return courseRepository.findAll().stream()
                .filter(c -> "COURSE-2026-909".equals(c.getCourseCode()))
                .findFirst()
                .orElseGet(() -> courseRepository.save(Course.builder()
                        .courseCode("COURSE-2026-909")
                        .courseName("[감독검증] 미담당 과정")
                        .cohort("1기")
                        .category("검증용")
                        .description("instructor1 이 담당하지 않는 과정 — 강사 접근 차단 검증용")
                        .startDate(LocalDate.now().minusDays(3))
                        .endDate(LocalDate.now().plusMonths(3))
                        .capacity(10)
                        .status(CourseStatus.IN_PROGRESS)
                        .completionProgressRate(80)
                        .build()));
    }

    private Exam monitoredExam(String name, Course course, User instructor, Question question,
                               Exam.ExamStatus status, LocalDateTime start, LocalDateTime end) {
        Exam exam = Exam.builder()
                .examName(name)
                .examType(Exam.ExamType.UNIT)
                .course(course)
                .instructor(instructor)
                .timeLimitMin(60)
                .autoScore(5).manualScore(0).totalScore(5).passScore(3)
                .randomOrder(false)
                .retakeAllowed(false).maxAttempts(1)
                .windowStart(start).windowEnd(end)
                .requireIdentityVerification(true)
                .proctorEnabled(true).requireWebcam(true)
                .blockTabSwitch(true).blockCopyPaste(true)
                .note("감독(모니터링) 검증용 시험입니다.")
                .status(status)
                .build();
        exam.addExamQuestion(ExamQuestion.builder().question(question).seq(1).fromRule(false).build());
        return exam;
    }

    private ExamAttempt attempt(Exam exam, User user, int attemptNo, LocalDateTime startedAt,
                                LocalDateTime expiresAt, ExamAttempt.AttemptStatus status, String ip) {
        return ExamAttempt.builder()
                .exam(exam).user(user).attemptNo(attemptNo)
                // 실제 응시(start→assignSet)는 세트를 반드시 박는다. 시드도 같게 맞춘다 —
                // 안 박으면 만료 자동제출/결과 집계에서 세트 번호 언박싱 NPE 로 학생 화면이 500 이 난다.
                .assignedSetNo(exam.availableSetNos().stream().findFirst().orElse(1))
                .startedAt(startedAt).expiresAt(expiresAt)
                .status(status)
                .identityVerifiedAt(startedAt).identityVerifyMethod("MOBILE")
                .ip(ip).userAgent("Mozilla/5.0 (seed)")
                .build();
    }

    /** 이벤트를 서비스 경로로 넣는다 — 심각도를 시드가 정하지 않게 하려는 것이다. */
    private void appendEvents(ExamAttempt attempt, String ip, List<ExamEventLog.EventType> types) {
        int minutesAgo = types.size() * 3;
        for (ExamEventLog.EventType type : types) {
            examEventLogService.append(attempt, type,
                    examEventLogService.severityOf(type),
                    "{\"source\":\"seed\"}", ip, now().minusMinutes(minutesAgo));
            minutesAgo -= 3;
        }
    }

    /**
     * 녹화 3건. 재생 검증이 되려면 실제 파일이 있어야 해서 더미 파일을 만든다.
     * 위치는 웹 루트 밖({@code lms.proctor.recording-dir}) — static 아래 두면 URL 만으로 받아진다.
     */
    private void seedRecordings(List<ExamAttempt> attempts) {
        Path base = Paths.get(recordingDir).toAbsolutePath().normalize();
        int index = 0;
        for (ExamAttempt attempt : attempts) {
            index++;
            String relative = "seed/attempt-" + attempt.getId() + ".mp4";
            boolean available = index < attempts.size();   // 마지막 1건은 녹화중 상태로 둔다
            long size = 0L;
            if (available) {
                size = writeDummy(base.resolve(relative));
            }
            examRecordingRepository.save(ExamRecording.builder()
                    .attempt(attempt)
                    .filePath(relative)
                    .durationSec(available ? 3600 + index * 137 : null)
                    .sizeBytes(available ? size : null)
                    .recordedAt(attempt.getStartedAt())
                    .status(available ? ExamRecording.RecordingStatus.AVAILABLE
                            : ExamRecording.RecordingStatus.RECORDING)
                    .build());
        }
    }

    /** 실제 영상이 아니라 바이트 몇 개짜리 자리표시자다. 스트리밍 엔드포인트 동작 확인이 목적. */
    private long writeDummy(Path path) {
        try {
            Files.createDirectories(path.getParent());
            byte[] content = ("SEED-RECORDING-" + path.getFileName()).getBytes();
            Files.write(path, content);
            return content.length;
        } catch (IOException e) {
            log.warn("[local] 더미 녹화 파일 생성 실패 {}", path, e);
            return 0L;
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
