package com.ssa.lms.ai.repository;

import com.ssa.lms.ai.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

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
}
