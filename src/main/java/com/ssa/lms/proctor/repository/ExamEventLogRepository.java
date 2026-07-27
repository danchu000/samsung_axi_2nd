package com.ssa.lms.proctor.repository;

import com.ssa.lms.proctor.entity.ExamEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 부정행위/입퇴장 이벤트 로그.
 *
 * <b>append-only</b> — 내역서 3년 보존 대상이라 저장 후 절대 UPDATE/DELETE 하지 않는다.
 * 그래서 여기에는 조회와 save 만 둔다 (수정 메서드를 추가하지 말 것).
 */
public interface ExamEventLogRepository extends JpaRepository<ExamEventLog, Long> {

    List<ExamEventLog> findByAttemptIdOrderByOccurredAtAsc(Long attemptId);

    long countByAttemptId(Long attemptId);
}
