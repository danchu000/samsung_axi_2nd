package com.ssa.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatusOrderByStartDateDesc(CourseStatus status);
}
