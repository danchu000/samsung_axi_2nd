package com.ssa.lms.user.repository;

import com.ssa.lms.user.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);

    Optional<AccessLog> findTopByUserIdAndTypeOrderByOccurredAtDesc(Long userId, AccessLog.Type type);

    /** local 시드 멱등 가드용 — 같은 시각의 접속 로그가 이미 심겼는지 확인한다. */
    boolean existsByUserIdAndTypeAndOccurredAt(Long userId, AccessLog.Type type, LocalDateTime occurredAt);
}
