package com.ssa.lms.attendance;

import com.ssa.lms.attendance.repository.AttendanceRepository;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.completion.entity.CompletionResult;
import com.ssa.lms.completion.repository.CompletionRepository;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.AccessLogRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * local 프로필 출결/이수 도메인 데모 시드 (P2-B 트랙, {@code @Order(40)}).
 *
 * <p>기본 시더({@code @Order(0)})가 만든 데모 과정(COURSE-2026-001, trainee1 승인 수강)에 얹는다.
 * 진행일이 지정된 각 차시에 trainee1 의 접속(LOGIN) 로그를 심고 → 출결을 접속 기반 산정 →
 * 이수 기준 저장 → 자동 판정 → 이수 확정까지 수행해, 부팅 직후 출결현황/이수관리/이수증(PDF)을
 * 바로 확인할 수 있게 한다.</p>
 *
 * <p><b>주의</b>: 진도(ProgressQueryService)는 P2-A 통합 전 fallback 0 을 주므로, 데모 이수 기준의
 * 최소 진도율을 0 으로 둔다(진도 통합 후 정상 기준으로 재설정). 다른 러너/기본 시더는 수정하지 않는다.</p>
 */
@Slf4j
@Component
@Profile("local")
@Order(40)
@RequiredArgsConstructor
public class AttendanceCompletionDemoDataInitializer implements CommandLineRunner {

    private static final String DEMO_COURSE_CODE = "COURSE-2026-001";
    private static final String DEMO_TRAINEE_LOGIN = "trainee1";

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final AccessLogRepository accessLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final CompletionRepository completionRepository;
    private final CompletionService completionService;

    @Override
    @Transactional
    public void run(String... args) {
        Course course = courseRepository.findByCourseCode(DEMO_COURSE_CODE).orElse(null);
        User trainee = userRepository.findByLoginId(DEMO_TRAINEE_LOGIN).orElse(null);
        if (course == null || trainee == null) {
            log.warn("[local] 데모 과정/계정이 없어 출결·이수 시드를 건너뜁니다.");
            return;
        }
        if (!attendanceRepository.findByCourseId(course.getId()).isEmpty()) {
            return;   // 이미 시드됨
        }

        // 1) 접속 로그 — 진행일이 잡힌 각 차시 날짜에 trainee1 로그인 이력 심기
        List<Session> sessions = sessionRepository
                .findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(course.getId()).stream()
                .filter(s -> s.getLessonDate() != null)
                .toList();
        for (Session s : sessions) {
            accessLogRepository.save(AccessLog.builder()
                    .userId(trainee.getId()).loginId(DEMO_TRAINEE_LOGIN).type(AccessLog.Type.LOGIN)
                    .ipAddress("127.0.0.1").userAgent("seed")
                    .occurredAt(s.getLessonDate().atTime(10, 0)).build());
        }

        // 2) 접속 기반 출결 산정
        int touched = attendanceService.recalculate(course.getId());

        // 3) 이수 기준(로컬 데모: 진도 통합 전이라 최소 진도율 0) → 자동 판정 → 확정
        completionService.saveCriteria(course.getId(), 0, 50, false, "로컬 데모 기준(진도 통합 전)");
        completionService.evaluate(course.getId());
        completionRepository.findByCourseIdAndTraineeId(course.getId(), trainee.getId())
                .filter(c -> c.getResult() == CompletionResult.PASS)
                .ifPresent(c -> completionService.confirm(c.getId()));

        log.info("[local] 출결·이수 데모 시드 완료 — {} 차시 출결 {}건 산정, trainee1 이수 확정",
                sessions.size(), touched);
    }
}
