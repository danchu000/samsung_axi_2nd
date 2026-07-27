package com.ssa.lms.proctor.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.exam.entity.ExamAttempt;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시험 녹화 파일.
 *
 * 담당 화면: admin-04-evaluation/recordings.html
 * (현재 정적 화면은 20분할 카메라 그리드 더미이므로, 응시 1건당 녹화 1건 구조로 정리했다.)
 *
 * 녹화물은 개인영상정보라 보존기간 정책이 별도다. 3년 일괄 보존 대상이 아닐 수 있으므로
 * 기관에 보존기간을 확인해야 한다. (A에게 요청 목록과 별개로 기관 확인 필요)
 */
@Entity
@Table(
        name = "exam_recording",
        indexes = @Index(name = "idx_recording_attempt", columnList = "attempt_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamRecording extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    /** 저장소 경로. 웹 루트에 직접 노출하지 말고 인증된 스트리밍 엔드포인트로만 서빙할 것. */
    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RecordingStatus status;

    @Builder
    public ExamRecording(ExamAttempt attempt, String filePath, Integer durationSec,
                         Long sizeBytes, LocalDateTime recordedAt, RecordingStatus status) {
        this.attempt = attempt;
        this.filePath = filePath;
        this.durationSec = durationSec;
        this.sizeBytes = sizeBytes;
        this.recordedAt = recordedAt;
        this.status = status;
    }

    public enum RecordingStatus {
        /** 녹화 진행 중. */
        RECORDING,
        /** 인코딩/업로드 처리 중. */
        PROCESSING,
        /** 재생 가능. */
        AVAILABLE,
        FAILED,
        /** 보존기간 경과로 삭제됨. */
        PURGED
    }
}
