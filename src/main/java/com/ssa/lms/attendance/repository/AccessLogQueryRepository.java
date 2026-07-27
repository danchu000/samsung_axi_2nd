package com.ssa.lms.attendance.repository;

import com.ssa.lms.user.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * 출결 산정 전용 access_log 조회 — 접속 기반 출결의 근거 데이터.
 *
 * <p>{@link AccessLog} 엔티티(사용자 도메인 소유, 읽기 전용)를 출결 트랙에서 조회하기 위한 리포지토리.
 * 사용자 패키지의 {@code AccessLogRepository} 를 수정하지 않고 이 트랙 안에 조회 메서드를 둔다
 * (병렬 작업 파일 충돌 방지 — PARALLEL-P2.md). 로그를 생성/수정하지는 않는다.</p>
 */
public interface AccessLogQueryRepository extends JpaRepository<AccessLog, Long> {

    /** 해당 사용자가 [start, end) 구간에 특정 유형(예: LOGIN) 이력이 몇 건인지. */
    long countByUserIdAndTypeAndOccurredAtBetween(Long userId, AccessLog.Type type,
                                                  LocalDateTime start, LocalDateTime end);
}
