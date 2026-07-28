package com.ssa.lms.course.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * B 도메인(시험/과제/설문 등)이 호출하는 과정 조회 계약 (a-requests.md P0-4, b-audit-reply.md §3-3(2)).
 * B 는 A 소유 리포지토리를 직접 쓰지 말고 이 서비스만 호출할 것.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    /** 과정 셀렉트 옵션 (시작일 내림차순). soft delete 된 과정 제외. */
    public record CourseOption(Long id, String courseCode, String courseName, String cohort) {
    }

    /** 사용자 표시 정보 — id 목록을 화면에 이름으로 풀어 쓸 때 사용. */
    public record UserDisplay(Long userId, String loginId, String name) {
    }

    /** 과정 담당 강사 — 채점자 지정 셀렉트용. 대표 강사가 앞에 온다. */
    public record InstructorDisplay(Long userId, String name, boolean primaryInstructor) {
    }

    /**
     * 과정 수강생 명단 — 응시 대상자, 미제출자 목록, 설문 배포 대상 산출용.
     * 승인(APPROVED)·수료(COMPLETED) 상태만 포함한다 (신청/반려/취소 제외).
     */
    public List<Long> findUserIdsByCourseId(Long courseId) {
        return enrollmentRepository.findTraineeIdsByCourseIdAndStatusIn(
                courseId, List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED));
    }

    /** 강사가 해당 과정 담당인지 — 권한정의서 △(담당 과정 한정) 판정용. */
    public boolean isInstructorOf(Long userId, Long courseId) {
        return courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, userId);
    }

    /**
     * 강사가 담당하는 과정 id 목록 — "전체 과정 순회 + isInstructorOf" N+1 패턴 대체.
     * 담당 과정이 없으면 빈 목록.
     */
    public List<Long> findCourseIdsByInstructorId(Long instructorId) {
        return courseInstructorRepository.findCourseIdsByInstructorId(instructorId);
    }

    /**
     * 훈련생이 수강 중인 과정 id 목록 — {@link #findUserIdsByCourseId(Long)} 의 반대 방향.
     * 동일하게 승인(APPROVED)·수료(COMPLETED) 상태만 포함한다 (판정 규칙 단일화).
     */
    public List<Long> findCourseIdsByUserId(Long userId) {
        return enrollmentRepository.findCourseIdsByTraineeIdAndStatusIn(
                userId, List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED));
    }

    /**
     * 과정 엔티티 1건 조회 — B 엔티티(Exam/CourseAssignment/Survey 등)의 {@code course} 연관 설정용.
     * soft delete 된 과정은 {@code @SQLRestriction} 에 의해 빈 Optional. <b>읽기/연관 설정 전용</b> —
     * 반환 엔티티의 상태 변경은 A 소유 서비스에서만 한다.
     */
    public Optional<Course> findCourse(Long courseId) {
        return courseRepository.findById(courseId);
    }

    /** 전체 과정 셀렉트 옵션 — 시작일 내림차순. */
    public List<CourseOption> findAllCourseOptions() {
        return courseRepository.findAllByOrderByStartDateDesc().stream()
                .map(c -> new CourseOption(c.getId(), c.getCourseCode(), c.getCourseName(), c.getCohort()))
                .toList();
    }

    /**
     * 사용자 id 목록 → 표시 정보(로그인 id·이름). 이름 가나다순 정렬.
     * 존재하지 않거나 soft delete 된 id 는 결과에서 빠진다 (호출부에서 개수 불일치 허용할 것).
     */
    public List<UserDisplay> findUserDisplays(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(u -> new UserDisplay(u.getId(), u.getLoginId(), u.getName()))
                .sorted(Comparator.comparing(UserDisplay::name))
                .toList();
    }

    /** 과정 담당 강사 목록 — 대표 강사 우선, 이름 가나다순. 채점자 지정 셀렉트용. */
    public List<InstructorDisplay> findInstructorsByCourseId(Long courseId) {
        return courseInstructorRepository.findWithInstructorByCourseId(courseId).stream()
                .map(ci -> new InstructorDisplay(
                        ci.getInstructor().getId(), ci.getInstructor().getName(), ci.isPrimaryInstructor()))
                .toList();
    }
}
