package com.ssa.lms.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Page<AccessLog> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable pageable);
}
