package com.ssa.lms.course.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SubjectRepository;
import com.ssa.lms.course.web.InstructorCourseView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 강사 "담당 과정/훈련생" 조회 (읽기 전용). 강사는 자기 담당 과정만 볼 수 있으며,
 * 권한 경계는 {@link CourseQueryService}(담당 과정 id 일괄 조회·isInstructorOf)로 판정한다
 * — 전체 과정 순회 + 개별 확인(N+1)을 하지 않는다 (PARALLEL-P3 권한 경계 규칙).
 *
 * <ul>
 *   <li>존재하지 않는 과정 → {@link CourseNotFoundException}(GlobalExceptionHandler 에서 404)</li>
 *   <li>존재하지만 담당이 아닌 과정 → {@link AccessDeniedException}(Spring Security 403)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorCourseService {

    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;

    /** 강사 담당 과정 목록 (시작일 최신순). 과목 수·수강생 수 포함. */
    public List<InstructorCourseView> myCourses(Long instructorId) {
        List<Long> courseIds = courseQueryService.findCourseIdsByInstructorId(instructorId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findAllById(courseIds).stream()
                .sorted(Comparator.comparing(Course::getStartDate).reversed())
                .map(c -> InstructorCourseView.of(c,
                        subjectRepository.countByCourseId(c.getId()),
                        courseQueryService.findUserIdsByCourseId(c.getId()).size()))
                .toList();
    }

    /** 강사 담당 과정 셀렉트 옵션 (시작일 최신순) — 훈련생/일정 화면의 과정 선택용. */
    public List<CourseQueryService.CourseOption> myCourseOptions(Long instructorId) {
        List<Long> courseIds = courseQueryService.findCourseIdsByInstructorId(instructorId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseRepository.findAllById(courseIds).stream()
                .sorted(Comparator.comparing(Course::getStartDate).reversed())
                .map(c -> new CourseQueryService.CourseOption(
                        c.getId(), c.getCourseCode(), c.getCourseName(), c.getCohort()))
                .toList();
    }

    /**
     * 담당 과정 여부를 강제한 뒤 Course 반환. 상세/훈련생/일정 진입 시 권한 게이트.
     * @throws CourseNotFoundException 과정이 없거나 삭제됨(404)
     * @throws AccessDeniedException   담당하지 않는 과정(403)
     */
    public Course requireOwnedCourse(Long instructorId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        if (!courseQueryService.isInstructorOf(instructorId, courseId)) {
            throw new AccessDeniedException("담당하지 않는 과정입니다.");
        }
        return course;
    }

    /** 담당 과정의 수강생(승인/수료) 명단 — 이름 가나다순. 권한 경계 강제. */
    public List<CourseQueryService.UserDisplay> traineesOf(Long instructorId, Long courseId) {
        requireOwnedCourse(instructorId, courseId);
        return courseQueryService.findUserDisplays(courseQueryService.findUserIdsByCourseId(courseId));
    }
}
