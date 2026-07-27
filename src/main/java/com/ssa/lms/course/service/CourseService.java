package com.ssa.lms.course.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SubjectRepository;
import com.ssa.lms.course.web.CourseForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 과정(Course) CRUD. 과목/차시 구성은 {@link CurriculumService},
 * 강사 배정은 {@link CourseInstructorService}, 수강신청은 {@code EnrollmentService} 가 담당한다.
 *
 * <p>과정 코드(courseCode)는 등록 시에만 지정하며 이후 변경하지 않는다 (B 계약 컬럼).
 * 삭제는 soft delete (@SQLDelete) — 3년 보존 요건.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;

    /** 시작일 최신순 전체 과정 (삭제된 것 제외 — @SQLRestriction). */
    public List<Course> findAll() {
        return courseRepository.findAllByOrderByStartDateDesc();
    }

    /**
     * 과정별 과목 개수 맵 (courseId → count).
     * OSIV 비활성 환경에서 목록 뷰가 lazy 컬렉션을 건드리지 않도록 서비스에서 미리 집계한다.
     */
    public Map<Long, Long> subjectCounts(List<Course> courses) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Course c : courses) {
            counts.put(c.getId(), subjectRepository.countByCourseId(c.getId()));
        }
        return counts;
    }

    public Course get(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    @Transactional
    public Long create(CourseForm form) {
        if (courseRepository.existsByCourseCode(form.getCourseCode())) {
            throw new DuplicateCourseCodeException(form.getCourseCode());
        }
        Course saved = courseRepository.save(form.toNewCourse());
        return saved.getId();
    }

    @Transactional
    public void update(Long id, CourseForm form) {
        Course course = get(id);
        course.update(form.getCourseName(), form.getCohort(), form.getCategory(),
                form.getDescription(), form.getStartDate(), form.getEndDate(),
                form.getCapacity(), form.getCompletionProgressRate());
    }

    @Transactional
    public void changeStatus(Long id, CourseStatus status) {
        get(id).changeStatus(status);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.delete(get(id));
    }
}
