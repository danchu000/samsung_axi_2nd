package com.ssa.lms.exam.repository;

import com.ssa.lms.exam.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 답안 조회.
 *
 * (attempt_id, question_id) 유니크라 임시저장은 항상 같은 행을 upsert 한다.
 * 새 행을 만들기 전에 반드시 {@link #findByAttemptIdAndQuestionId} 로 확인할 것.
 */
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    long countByAttemptId(Long attemptId);

    /** 자동 채점·화면 복원용. 보기(choice)까지 한 번에 끌어온다. */
    @Query("""
            select a from Answer a
              left join fetch a.choice
              join fetch a.question
            where a.attempt.id = :attemptId
            """)
    List<Answer> findAllByAttemptId(@Param("attemptId") Long attemptId);

    /**
     * 이 보기들을 고른 답안이 하나라도 있는지 — 문제 수정 시 보기를 지워도 되는지 판단한다.
     * answer.choice_id 에 FK 가 걸려 있어, 참조가 있는 보기를 지우면 500 이 난다.
     */
    @Query("select distinct a.choice.id from Answer a where a.choice.id in :choiceIds")
    List<Long> findReferencedChoiceIds(@Param("choiceIds") Collection<Long> choiceIds);
}
