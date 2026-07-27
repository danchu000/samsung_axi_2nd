package com.ssa.lms.course.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseInstructor;
import com.ssa.lms.course.repository.CourseInstructorRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.web.InstructorOption;
import com.ssa.lms.course.web.InstructorView;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강사-과정 매핑. 한 과정에 복수 강사 배정 가능하며 대표 강사 여부를 둔다.
 *
 * <p>강사 목록은 user 도메인(A-1 소유)의 {@link UserRepository} 를 <b>읽기</b>로만 사용한다
 * (role=INSTRUCTOR 필터링은 여기서 수행 — user 패키지 미수정 원칙 준수).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseInstructorService {

    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;
    private final UserRepository userRepository;

    /** 과정 담당 강사 목록. */
    public List<InstructorView> instructorsOf(Long courseId) {
        return courseInstructorRepository.findByCourseId(courseId).stream()
                .map(InstructorView::of).toList();
    }

    /** 배정 가능한 강사(role=INSTRUCTOR) 드롭다운 옵션. */
    public List<InstructorOption> availableInstructors() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INSTRUCTOR)
                .map(u -> new InstructorOption(u.getId(), u.getName()))
                .toList();
    }

    @Transactional
    public void assign(Long courseId, Long instructorUserId, boolean primary) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        User user = userRepository.findById(instructorUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + instructorUserId));
        if (user.getRole() != Role.INSTRUCTOR) {
            throw new IllegalArgumentException("강사(INSTRUCTOR) 계정만 배정할 수 있습니다.");
        }
        if (courseInstructorRepository.existsByCourseIdAndInstructorId(courseId, instructorUserId)) {
            throw new IllegalStateException("이미 배정된 강사입니다.");
        }
        courseInstructorRepository.save(CourseInstructor.builder()
                .course(course).instructor(user).primaryInstructor(primary).build());
    }

    @Transactional
    public void unassign(Long mappingId) {
        courseInstructorRepository.deleteById(mappingId);
    }
}
