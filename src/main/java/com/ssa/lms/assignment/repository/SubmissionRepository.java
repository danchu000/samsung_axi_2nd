package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 제출물 리포지토리.
 *
 * 재제출은 같은 행을 덮어쓰지 않고 attemptNo 를 올린 새 행으로 쌓기 때문에
 * "현재 제출물" 은 언제나 (course_assignment, user) 별 최대 attemptNo 행이다.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** 특정 훈련생의 최신 제출물 1건. 없으면 미제출. */
    Optional<Submission> findTopByCourseAssignmentIdAndUserIdOrderByAttemptNoDesc(
            Long courseAssignmentId, Long userId);

    /** 특정 훈련생의 제출 이력 전체 (최신 회차 우선). */
    List<Submission> findByCourseAssignmentIdAndUserIdOrderByAttemptNoDesc(
            Long courseAssignmentId, Long userId);

    /** 채점 화면의 학생 목록 — 제출한 사람 전부, 회차 포함. */
    @Query("""
            select s from Submission s
            join fetch s.user u
            where s.courseAssignment.id = :courseAssignmentId
            order by u.id asc, s.attemptNo asc
            """)
    List<Submission> findAllWithUserByCourseAssignmentId(
            @Param("courseAssignmentId") Long courseAssignmentId);

    /** 제출물 상세 — 첨부/피드백까지. files 와 feedbacks 를 동시에 fetch 하면 MultipleBagFetch 라 files 만. */
    @Query("""
            select s from Submission s
            join fetch s.user
            join fetch s.courseAssignment ca
            join fetch ca.course
            join fetch ca.assignment
            left join fetch s.files
            where s.id = :id
            """)
    Optional<Submission> findDetailById(@Param("id") Long id);

    /** 훈련생 목록 화면용 — 여러 배정에 대한 그 사람의 최신 제출물. */
    @Query("""
            select s from Submission s
            where s.user.id = :userId
              and s.courseAssignment.id in :courseAssignmentIds
              and s.attemptNo = (select max(s2.attemptNo) from Submission s2
                                 where s2.courseAssignment.id = s.courseAssignment.id
                                   and s2.user.id = s.user.id)
            """)
    List<Submission> findLatestByUserAndCourseAssignments(
            @Param("userId") Long userId,
            @Param("courseAssignmentIds") Collection<Long> courseAssignmentIds);

    /** 그 훈련생이 이 과제에 지금까지 제출한 횟수 (attemptNo 채번 및 재제출 한도 판정). */
    long countByCourseAssignmentIdAndUserId(Long courseAssignmentId, Long userId);

    /** 제출한 훈련생 id 목록 — 미제출자 산출에 쓴다(전체 수강생 − 이 목록). */
    @Query("""
            select distinct s.user.id from Submission s
            where s.courseAssignment.id = :courseAssignmentId
            """)
    List<Long> findSubmittedUserIds(@Param("courseAssignmentId") Long courseAssignmentId);
}
