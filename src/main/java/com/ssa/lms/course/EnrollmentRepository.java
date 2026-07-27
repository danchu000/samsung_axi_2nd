package com.ssa.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByTraineeIdAndCourseId(Long traineeId, Long courseId);

    List<Enrollment> findByTraineeIdOrderByAppliedAtDesc(Long traineeId);

    List<Enrollment> findByCourseIdOrderByAppliedAtDesc(Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
}
