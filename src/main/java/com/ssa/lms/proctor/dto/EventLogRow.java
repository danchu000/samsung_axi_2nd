package com.ssa.lms.proctor.dto;

/**
 * 입·퇴장 / 이상행위 로그 한 줄 (내역서: 부정행위 방지 증빙, 3년 보존).
 *
 * <p>심각도는 <b>서버가 정한 값</b>을 그대로 내려준다. 클라이언트가 보낸 severity 는
 * 수집 단계에서 이미 버려졌다 ({@code ExamEventLogService.severityOf}).</p>
 */
public record EventLogRow(
        Long id,
        String occurredAt,
        String eventType,
        String eventLabel,
        String severity,
        String severityLabel,
        String detail,
        String ip
) {
}
