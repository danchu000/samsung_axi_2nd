package com.ssa.lms.proctor.dto;

/**
 * 녹화 목록 한 행.
 *
 * <p>{@code streamUrl} 은 인증된 스트리밍 엔드포인트다. {@code ExamRecording.filePath} 는
 * 절대 화면으로 내려보내지 않는다 — 저장소 경로가 노출되면 웹 루트 우회 접근 시도의 단서가 된다.</p>
 */
public record RecordingRow(
        Long recordingId,
        Long attemptId,
        Long userId,
        String traineeName,
        String examName,
        String courseName,
        String courseId,
        String recordedAt,
        String durationLabel,
        String sizeLabel,
        String status,
        String statusLabel,
        boolean playable,
        long warnCount,
        long criticalCount,
        String streamUrl
) {
}
