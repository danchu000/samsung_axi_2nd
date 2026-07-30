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

    /** 전체 과정의 특정 상태 신청 목록 — 통합 승인 화면 전용 (신청 오래된 순) */
    @Query("select e from Enrollment e join fetch e.trainee join fetch e.course " +
           "where e.status = :status order by e.appliedAt asc")
    List<Enrollment> findAllByStatusWithTraineeAndCourse(@Param("status") EnrollmentStatus status);

    /** 과정 수강생(승인/수료) user id 목록 — CourseQueryService 전용 */
    @Query("select e.trainee.id from Enrollment e where e.course.id = :courseId and e.status in :statuses")
    List<Long> findTraineeIdsByCourseIdAndStatusIn(@Param("courseId") Long courseId,
                                                   @Param("statuses") Collection<EnrollmentStatus> statuses);

    /** 훈련생이 수강 중(승인/수료)인 과정 id 목록 — CourseQueryService 전용 (위의 반대 방향) */
    @Query("select e.course.id from Enrollment e where e.trainee.id = :traineeId and e.status in :statuses")
    List<Long> findCourseIdsByTraineeIdAndStatusIn(@Param("traineeId") Long traineeId,
                                                   @Param("statuses") Collection<EnrollmentStatus> statuses);
}
