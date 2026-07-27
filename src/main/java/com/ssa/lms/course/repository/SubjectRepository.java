package com.ssa.lms.course.repository;

import com.ssa.lms.course.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByCourseIdOrderByOrderNo(Long courseId);
}
