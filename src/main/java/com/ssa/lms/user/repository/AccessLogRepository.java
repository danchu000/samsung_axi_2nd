package com.ssa.lms.user.repository;

import com.ssa.lms.user.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);

    Optional<AccessLog> findTopByUserIdAndTypeOrderByOccurredAtDesc(Long userId, AccessLog.Type type);
}
