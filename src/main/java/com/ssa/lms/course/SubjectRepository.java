package com.ssa.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByCourseIdOrderByOrderNo(Long courseId);
}
