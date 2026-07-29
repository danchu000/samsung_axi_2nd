package com.ssa.lms.ai.repository;

import com.ssa.lms.ai.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    /**
     * 오늘 성공한 호출 수. 하루 상한 판정에 쓴다.
     *
     * <p>실패는 세지 않는다 — 모델이 죽어서 실패한 것까지 한도에 넣으면
     * 장애가 곧 서비스 차단이 된다.</p>
     */
    @Query("""
           select count(l) from AiUsageLog l
            where l.success = true
              and l.calledAt >= :from
           """)
    long countSuccessSince(@Param("from") LocalDateTime from);

    @Query("""
           select count(l) from AiUsageLog l
            where l.success = true
              and l.userId = :userId
              and l.calledAt >= :from
           """)
    long countSuccessByUserSince(@Param("userId") Long userId, @Param("from") LocalDateTime from);

    /* ===== [기능 5] 대시보드 위젯 집계 ===== */

    /**
     * 기간 내 호출 통계 — 전체 / 성공 / 이용자 수.
     *
     * <p>세 값을 한 번에 뽑는다. 각각 따로 세면 그 사이 새 호출이 들어와
     * "성공 > 전체" 같은 앞뒤 안 맞는 숫자가 나올 수 있다.</p>
     */
    @Query("""
           select count(l),
                  sum(case when l.success = true then 1 else 0 end),
                  count(distinct l.userId)
             from AiUsageLog l
            where l.purpose = :purpose
              and l.calledAt >= :from
           """)
    Object[] statsSince(@Param("purpose") String purpose, @Param("from") LocalDateTime from);

    /* ===== [기능 4] 학습 진단 ===== */

    /**
     * 오늘 들어온 훈련생 질문. <b>질문이 있을 때만</b> 진단을 돌리기 위한 조회다.
     *
     * <p>질문 본문은 암호문이라 DB 에서 검색·분류할 수 없다(CLAUDE.md 규칙 7).
     * 그래서 하루치를 읽어 자바에서 처리한다 — 하루 수십 건 수준이라 문제되지 않는다.</p>
     */
    @Query("""
           select l from AiUsageLog l
            where l.purpose = 'QNA'
              and l.success = true
              and l.question is not null
              and l.courseId in :courseIds
              and l.calledAt >= :from
            order by l.calledAt asc
           """)
    List<AiUsageLog> findQuestionsSince(@Param("courseIds") List<Long> courseIds,
                                        @Param("from") LocalDateTime from);

    /** 보존기간이 지난 기록 — 내용만 지우기 위해 찾는다. 통계는 남긴다. */
    @Query("""
           select l from AiUsageLog l
            where l.question is not null
              and l.calledAt < :before
           """)
    List<AiUsageLog> findExpiredQuestions(@Param("before") LocalDateTime before);
}
