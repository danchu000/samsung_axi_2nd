package com.ssa.lms.course.repository;

import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByTraineeIdAndCourseId(Long traineeId, Long courseId);

    List<Enrollment> findByTraineeIdOrderByAppliedAtDesc(Long traineeId);

    List<Enrollment> findByCourseIdOrderByAppliedAtDesc(Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    /** 과정 수강생(승인/수료) user id 목록 — CourseQueryService 전용 */
    @Query("select e.trainee.id from Enrollment e where e.course.id = :courseId and e.status in :statuses")
    List<Long> findTraineeIdsByCourseIdAndStatusIn(@Param("courseId") Long courseId,
                                                   @Param("statuses") Collection<EnrollmentStatus> statuses);
}
