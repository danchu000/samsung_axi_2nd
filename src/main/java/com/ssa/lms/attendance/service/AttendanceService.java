package com.ssa.lms.attendance.service;

import com.ssa.lms.attendance.entity.Attendance;
import com.ssa.lms.attendance.entity.AttendanceStatus;
import com.ssa.lms.attendance.repository.AccessLogQueryRepository;
import com.ssa.lms.attendance.repository.AttendanceRepository;
import com.ssa.lms.attendance.web.AttendanceMatrixView;
import com.ssa.lms.attendance.web.TraineeAttendanceView;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.entity.AccessLog;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 접속 기반 출결 산정/조회.
 *
 * <p><b>산정 규칙</b>: 일정(lessonDate)이 잡힌 각 차시에 대해, 해당 수강생이 그 날짜에 로그인(access_log LOGIN)
 * 이력이 있으면 출석(PRESENT), 없으면 결석(ABSENT). 지각/공결은 관리자 수동 보정으로만 지정한다.
 * 수동 보정된 행({@code manual=true})은 재산정 시 덮어쓰지 않는다.</p>
 *
 * <p>과정/차시/수강생/접속로그는 모두 읽기 전용으로만 참조한다(A 기존 도메인). 출결 데이터만 쓴다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AccessLogQueryRepository accessLogQueryRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;
    private final CourseQueryService courseQueryService;
    private final UserRepository userRepository;

    /**
     * 과정 전체 출결을 접속 로그 기준으로 (재)산정한다. 일정이 잡힌 차시만 대상.
     *
     * @return 갱신/생성된 출결 행 수
     */
    @Transactional
    public int recalculate(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("과정을 찾을 수 없습니다: " + courseId));

        List<Session> sessions = sessionRepository
                .findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(courseId).stream()
                .filter(s -> s.getLessonDate() != null)
                .toList();
        List<User> trainees = userRepository.findAllById(courseQueryService.findUserIdsByCourseId(courseId));
        if (sessions.isEmpty() || trainees.isEmpty()) {
            return 0;
        }

        int touched = 0;
        for (User trainee : trainees) {
            for (Session session : sessions) {
                LocalDate date = session.getLessonDate();
                int accessCount = (int) accessLogQueryRepository.countByUserIdAndTypeAndOccurredAtBetween(
                        trainee.getId(), AccessLog.Type.LOGIN,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay());
                AttendanceStatus autoStatus = accessCount > 0 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;

                Attendance attendance = attendanceRepository
                        .findBySessionIdAndTraineeId(session.getId(), trainee.getId())
                        .orElse(null);
                if (attendance == null) {
                    attendanceRepository.save(Attendance.builder()
                            .course(course).session(session).trainee(trainee)
                            .attendanceDate(date).status(autoStatus).accessCount(accessCount)
                            .manual(false).build());
                    touched++;
                } else if (!attendance.isManual()) {
                    attendance.applyAuto(autoStatus, accessCount, date);
                    touched++;
                }
            }
        }
        return touched;
    }

    /** 관리자 수동 보정(지각/공결 등). */
    @Transactional
    public void override(Long attendanceId, AttendanceStatus status, String note) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출결 기록을 찾을 수 없습니다: " + attendanceId));
        attendance.override(status, note);
    }

    /**
     * 수강생의 과정 출석률(%) — 기록된 출결 행 중 출석 인정(결석 제외) 비율.
     * 산정된 출결이 없으면 0. 이수 판정({@code completion})이 소비한다.
     */
    public int attendanceRate(Long traineeId, Long courseId) {
        List<Attendance> rows = attendanceRepository.findByCourseIdAndTraineeId(courseId, traineeId);
        if (rows.isEmpty()) {
            return 0;
        }
        long credited = rows.stream().filter(a -> a.getStatus().isCredited()).count();
        return (int) Math.round(credited * 100.0 / rows.size());
    }

    /** 과정의 모든 출결 행(현황 화면 매트릭스 구성용). */
    public List<Attendance> findByCourse(Long courseId) {
        return attendanceRepository.findByCourseId(courseId);
    }

    /**
     * 과정 출결현황 매트릭스(수강생×차시) 뷰 — 관리자/강사 화면 공용.
     * 강사 권한(담당 과정 한정) 검증은 호출부(컨트롤러)에서 수행한다.
     */
    public AttendanceMatrixView matrixOf(Long courseId) {
        List<Session> sessions = sessionRepository
                .findBySubjectCourseIdOrderBySubjectOrderNoAscSeqAsc(courseId).stream()
                .filter(s -> s.getLessonDate() != null)
                .toList();
        List<User> trainees = userRepository.findAllById(courseQueryService.findUserIdsByCourseId(courseId)).stream()
                .sorted(Comparator.comparing(User::getName))
                .toList();
        return AttendanceMatrixView.of(trainees, sessions, findByCourse(courseId));
    }

    /** 수강생 본인의 출결 현황(과정별 그룹핑 뷰). 훈련생 출결현황 화면 전용. */
    public List<TraineeAttendanceView> traineeAttendance(Long traineeId) {
        return TraineeAttendanceView.group(attendanceRepository.findByTraineeId(traineeId));
    }

    /** 과정에서 수강생별 출석률 맵(수강생 id → %). */
    public Map<Long, Integer> attendanceRates(Long courseId) {
        return attendanceRepository.findByCourseId(courseId).stream()
                .collect(Collectors.groupingBy(a -> a.getTrainee().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    long credited = e.getValue().stream().filter(a -> a.getStatus().isCredited()).count();
                    return (int) Math.round(credited * 100.0 / e.getValue().size());
                }));
    }
}
