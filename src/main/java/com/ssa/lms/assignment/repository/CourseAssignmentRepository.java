package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.CourseAssignment;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 과정에 배정된 과제 리포지토리.
 *
 * 목록 화면(admin-evaluation-assignment.html)이 과정명·과제명·강사명을 전부 보여주므로
 * 조회는 항상 course / assignment / grader 를 fetch join 한다 (LAZY 프록시 + N+1 회피).
 */
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

    boolean existsByCourseIdAndAssignmentId(Long courseId, Long assignmentId);

    /**
     * 관리자/강사 목록 검색.
     * 화면 필터: 과정 / 상태 / 강사 / 검색어(과정명·과제명·강사명).
     * graderId 는 강사 화면에서 "내가 채점자인 것만" 으로도 쓴다.
     */
    @Query("""
            select ca from CourseAssignment ca
            join fetch ca.course c
            join fetch ca.assignment a
            left join fetch ca.grader g
            where (:courseId is null or c.id = :courseId)
              and (:graderId is null or g.id = :graderId)
              and (:status is null or ca.status = :status)
              and (:keyword is null
                   or lower(a.title) like lower(concat('%', cast(:keyword as string), '%'))
                   or lower(c.courseName) like lower(concat('%', cast(:keyword as string), '%'))
                   or lower(g.name) like lower(concat('%', cast(:keyword as string), '%')))
            order by ca.startAt desc, ca.id desc
            """)
    List<CourseAssignment> search(@Param("courseId") Long courseId,
                                  @Param("graderId") Long graderId,
                                  @Param("status") CourseAssignment.CourseAssignmentStatus status,
                                  @Param("keyword") String keyword);

    @Query("""
            select ca from CourseAssignment ca
            join fetch ca.course
            join fetch ca.assignment
            left join fetch ca.grader
            where ca.id = :id
            """)
    Optional<CourseAssignment> findDetailById(@Param("id") Long id);

    /** 훈련생이 수강 중인 과정들에 배정된 과제 (DRAFT 제외 — 아직 공개 전이다). */
    @Query("""
            select ca from CourseAssignment ca
            join fetch ca.course c
            join fetch ca.assignment a
            left join fetch ca.grader
            where c.id in :courseIds
              and ca.status <> com.ssa.lms.assignment.entity.CourseAssignment$CourseAssignmentStatus.DRAFT
            order by ca.endAt asc
            """)
    List<CourseAssignment> findVisibleByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    /**
     * 배정별 제출 인원(중복 제출은 1명으로) — 목록의 "제출 n명".
     * 반환: [courseAssignmentId(Long), submittedUserCount(Long)]
     */
    @Query("""
            select s.courseAssignment.id, count(distinct s.user.id)
            from Submission s
            where s.courseAssignment.id in :ids
            group by s.courseAssignment.id
            """)
    List<Object[]> countSubmittedUsers(@Param("ids") Collection<Long> ids);

    /**
     * 배정별 채점 완료(확정 포함) 인원 — 목록의 채점 진행 상태(채점예정/채점완료) 파생용.
     * Grade 는 evalType=ASSIGNMENT, evalRefId=course_assignment.id 로 저장된다.
     * 반환: [evalRefId(Long), gradedCount(Long)]
     */
    @Query("""
            select g.evalRefId, count(g)
            from Grade g
            where g.evalType = com.ssa.lms.grading.entity.Grade$EvalType.ASSIGNMENT
              and g.evalRefId in :ids
              and g.status in (com.ssa.lms.grading.entity.Grade$GradeStatus.GRADED,
                               com.ssa.lms.grading.entity.Grade$GradeStatus.CONFIRMED)
            group by g.evalRefId
            """)
    List<Object[]> countGraded(@Param("ids") Collection<Long> ids);

    /** 마감이 구간 안에 드는 공개 과제 — 리마인드 대상 산출용. */
    @Query("""
            select ca from CourseAssignment ca
              join fetch ca.assignment
              join fetch ca.course
            where ca.status = com.ssa.lms.assignment.entity.CourseAssignment$CourseAssignmentStatus.OPEN
              and ca.endAt between :from and :to
            """)
    List<CourseAssignment> findDueBetween(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /** 이 과제들에 한 번이라도 제출한 사용자 — [courseAssignmentId, userId]. */
    @Query("""
            select s.courseAssignment.id, s.user.id from Submission s
            where s.courseAssignment.id in :ids
            """)
    List<Object[]> findSubmittedPairs(@Param("ids") Collection<Long> ids);
}
