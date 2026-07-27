package com.ssa.lms.assignment.repository;

import com.ssa.lms.assignment.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 피드백 리포지토리.
 * 피드백은 append-only 다 — 수정 시 덮어쓰지 않고 새 행을 쌓으므로
 * "현재 피드백" 은 언제나 가장 마지막 행이다.
 */
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("""
            select f from Feedback f
            join fetch f.instructor
            where f.submission.id = :submissionId
            order by f.id asc
            """)
    List<Feedback> findBySubmissionId(@Param("submissionId") Long submissionId);

    /** 훈련생 화면용 — 공개 피드백만. 내부 메모는 절대 내려가면 안 된다. */
    @Query("""
            select f from Feedback f
            where f.submission.id = :submissionId and f.visibleToTrainee = true
            order by f.id asc
            """)
    List<Feedback> findVisibleBySubmissionId(@Param("submissionId") Long submissionId);
}
