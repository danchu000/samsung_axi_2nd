package com.ssa.lms.support.repository;

import com.ssa.lms.support.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /** 질문별 답변 (작성순). 본문은 암호문 저장이라 정렬은 createdAt 으로만 한다. */
    @Query("""
            select a from QnaAnswer a
            left join fetch a.responder
            where a.qna.id = :qnaId
            order by a.createdAt asc
            """)
    List<QnaAnswer> findByQnaId(@Param("qnaId") Long qnaId);

    long countByQnaId(Long qnaId);
}
