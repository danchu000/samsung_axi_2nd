package com.ssa.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, Long> {

    List<CourseInstructor> findByCourseId(Long courseId);

    List<CourseInstructor> findByInstructorId(Long instructorId);
}
