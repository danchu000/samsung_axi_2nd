package com.ssa.lms.exam.repository;

import com.ssa.lms.exam.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 응시 회차 조회.
 *
 * ExamAttempt 에는 uk_exam_attempt(exam_id, user_id, attempt_no) 유니크 제약이 있고
 * soft delete(@SQLDelete)를 걸지 않았다. soft delete 를 걸면 삭제된 행이 남아
 * 같은 회차를 다시 만들 때 반드시 유니크 충돌이 난다 (과제·설문 슬라이스에서 실제로 터진 사례).
 */
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /** 시험 수정 잠금 가드용 — 응시 기록이 하나라도 있으면 문항을 갈아끼울 수 없다. */
    boolean existsByExamId(Long examId);

    /** 응시 횟수 소진 판정. VOIDED(무효 처리)도 소진으로 본다 — 무효는 제재이지 재응시권이 아니다. */
    long countByExamIdAndUserId(Long examId, Long userId);

    List<ExamAttempt> findByExamIdAndUserIdOrderByAttemptNoDesc(Long examId, Long userId);

    /** 응시 목록 화면용 — 내 응시 이력을 시험 묶음으로 한 번에 가져온다 (행마다 조회하면 N+1). */
    @Query("""
            select a from ExamAttempt a
            where a.user.id = :userId and a.exam.id in :examIds
            order by a.attemptNo asc
            """)
    List<ExamAttempt> findByUserAndExams(@Param("userId") Long userId,
                                         @Param("examIds") Collection<Long> examIds);

    /** 응시 화면용 — 시험과 편성 문항까지 한 번에. */
    @Query("""
            select distinct a from ExamAttempt a
              join fetch a.exam e
              left join fetch e.examQuestions eq
              left join fetch eq.question
            where a.id = :id
            """)
    Optional<ExamAttempt> findWithExamQuestions(@Param("id") Long id);

    @Query("""
            select a from ExamAttempt a join fetch a.exam where a.id = :id
            """)
    Optional<ExamAttempt> findWithExam(@Param("id") Long id);
}
