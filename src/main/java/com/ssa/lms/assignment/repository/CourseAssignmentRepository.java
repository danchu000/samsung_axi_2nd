package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.CourseAssignment;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                   or lower(a.title) like lower(concat('%', :keyword, '%'))
                   or lower(c.courseName) like lower(concat('%', :keyword, '%'))
                   or lower(g.name) like lower(concat('%', :keyword, '%')))
            order by ca.startAt desc, ca.id desc
            """)
    List<CourseAssignment> search(@Param("courseId") Long courseId,
                                  @Param("graderId") Long graderId,
                                  @Param("status") CourseAssignment.CourseAssignmentStatus status,
                                  @Param("keyword") String keyword);

    /**
     * 관리자/강사 목록 검색 — <b>서버 페이징 + 강사 담당 과정 제한</b> 버전.
     *
     * <p>강사 제한을 자바에서 걸면 페이징 건수가 어긋나므로(시험 목록에서 실제로 겪은 문제)
     * 담당 과정 조건을 쿼리로 내렸다. 권한 필터가 SQL 안에 있어 page 파라미터를 조작해도
     * 담당 아닌 과정이 새어 나오지 않는다.</p>
     *
     * <p>정렬은 {@code Pageable} 로 받는다 — 쿼리에 {@code order by} 를 박아두면
     * Spring Data 가 Sort 를 뒤에 덧붙일 때 문법이 깨진다.</p>
     *
     * @param scoped    true 면 {@code courseIds} 로 제한한다 (강사). 관리자는 false.
     * @param courseIds 비어 있으면 안 된다 — 빈 in 절은 DB 별로 동작이 갈린다.
     *                  담당 과정이 없는 강사는 서비스가 빈 페이지로 끊는다.
     */
    @Query(value = """
            select ca from CourseAssignment ca
            join fetch ca.course c
            join fetch ca.assignment a
            left join fetch ca.grader g
            where (:courseId is null or c.id = :courseId)
              and (:graderId is null or g.id = :graderId)
              and (:status is null or ca.status = :status)
              and (:scoped = false or c.id in :courseIds)
              and (:keyword is null
                   or lower(a.title) like lower(concat('%', :keyword, '%'))
                   or lower(c.courseName) like lower(concat('%', :keyword, '%'))
                   or lower(g.name) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(ca) from CourseAssignment ca
            join ca.course c
            join ca.assignment a
            left join ca.grader g
            where (:courseId is null or c.id = :courseId)
              and (:graderId is null or g.id = :graderId)
              and (:status is null or ca.status = :status)
              and (:scoped = false or c.id in :courseIds)
              and (:keyword is null
                   or lower(a.title) like lower(concat('%', :keyword, '%'))
                   or lower(c.courseName) like lower(concat('%', :keyword, '%'))
                   or lower(g.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<CourseAssignment> searchPage(@Param("courseId") Long courseId,
                                      @Param("graderId") Long graderId,
                                      @Param("status") CourseAssignment.CourseAssignmentStatus status,
                                      @Param("keyword") String keyword,
                                      @Param("scoped") boolean scoped,
                                      @Param("courseIds") Collection<Long> courseIds,
                                      Pageable pageable);

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
