package com.ssa.lms.proctor.service;

import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.proctor.entity.ExamEventLog;
import com.ssa.lms.proctor.repository.ExamEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 시험 입·퇴장 및 이상행위 로그 기록 (내역서: 부정행위 방지 / 3년 보존).
 *
 * <b>append-only</b> — 이 서비스에는 저장과 조회만 있다. 수정/삭제 메서드를 추가하지 말 것.
 * 응시 화면이 보내는 이벤트는 신뢰할 수 없는 입력이라, 알 수 없는 타입은 저장하지 않고 버린다
 * (임의 문자열이 로그 테이블에 쌓이면 모니터링 화면 집계가 오염된다).
 */
@Service
@RequiredArgsConstructor
public class ExamEventLogService {

    private static final int DETAIL_MAX = 1000;

    private final ExamEventLogRepository examEventLogRepository;

    /** 화면에서 올라온 이벤트 타입 문자열 파싱. 모르는 값이면 null. */
    public ExamEventLog.EventType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ExamEventLog.EventType.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional
    public ExamEventLog append(ExamAttempt attempt, ExamEventLog.EventType type,
                               String detail, String ip) {
        return append(attempt, type, severityOf(type), detail, ip, LocalDateTime.now());
    }

    @Transactional
    public ExamEventLog append(ExamAttempt attempt, ExamEventLog.EventType type,
                               ExamEventLog.Severity severity, String detail, String ip,
                               LocalDateTime occurredAt) {
        return examEventLogRepository.save(ExamEventLog.builder()
                .attempt(attempt)
                .eventType(type)
                .severity(severity)
                .detail(truncate(detail))
                .occurredAt(occurredAt)
                .ip(ip)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ExamEventLog> findByAttempt(Long attemptId) {
        return examEventLogRepository.findByAttemptIdOrderByOccurredAtAsc(attemptId);
    }

    @Transactional(readOnly = true)
    public long countByAttempt(Long attemptId) {
        return examEventLogRepository.countByAttemptId(attemptId);
    }

    /**
     * 이벤트별 심각도. 모니터링 화면(다음 슬라이스)이 이 값으로 경고를 띄운다.
     * 심각도는 서버가 정한다 — 클라이언트가 보낸 값을 그대로 믿으면 부정행위를 INFO 로 낮춰 보낼 수 있다.
     */
    private ExamEventLog.Severity severityOf(ExamEventLog.EventType type) {
        return switch (type) {
            case ENTER, EXIT, RESUME, TAB_FOCUS -> ExamEventLog.Severity.INFO;
            case TAB_BLUR, COPY, PASTE, FULLSCREEN_EXIT, NETWORK_DROP -> ExamEventLog.Severity.WARN;
            case MULTI_FACE, NO_FACE -> ExamEventLog.Severity.CRITICAL;
        };
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= DETAIL_MAX ? detail : detail.substring(0, DETAIL_MAX);
    }
}
