package com.ssa.lms.grading.repository;

import com.ssa.lms.grading.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByUserIdAndEvalTypeAndEvalRefId(
            Long userId, Grade.EvalType evalType, Long evalRefId);

    List<Grade> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Grade> findByUserIdAndCourseIdAndStatus(
            Long userId, Long courseId, Grade.GradeStatus status);

    List<Grade> findByCourseIdAndEvalTypeAndEvalRefId(
            Long courseId, Grade.EvalType evalType, Long evalRefId);

    /** 과정 내에서 아직 확정되지 않은 성적 건수. 0 이면 전부 확정된 것이다. */
    @Query("""
            select count(g) from Grade g
            where g.user.id = :userId
              and g.course.id = :courseId
              and g.status <> com.ssa.lms.grading.entity.Grade$GradeStatus.CONFIRMED
            """)
    long countUnconfirmed(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
