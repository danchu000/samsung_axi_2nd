package com.ssa.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    List<CourseSession> findBySubjectIdOrderByOrderNo(Long subjectId);

    List<CourseSession> findBySubjectCourseIdOrderBySubjectOrderNoAscOrderNoAsc(Long courseId);
}
