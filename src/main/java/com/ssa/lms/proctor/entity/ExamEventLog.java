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
 * 시험 입·퇴장 및 이상행위 로그.
 *
 * 담당 화면: admin-04-evaluation/admin-evaluation-monitoring.html,
 *           admin-evaluation-monitoring-live.html
 *
 * 내역서 3년 보존 대상. 이 테이블은 append-only 로 취급하고 절대 UPDATE 하지 않는다.
 */
@Entity
@Table(
        name = "exam_event_log",
        indexes = {
                @Index(name = "idx_event_attempt", columnList = "attempt_id, occurred_at"),
                @Index(name = "idx_event_severity", columnList = "severity, occurred_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamEventLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30, nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 10, nullable = false)
    private Severity severity;

    /** 이벤트 부가 정보(JSON 문자열). 예: 탭 전환 지속시간, 감지된 얼굴 수. */
    @Lob
    @Column(name = "detail")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "ip", length = 45)
    private String ip;

    @Builder
    public ExamEventLog(ExamAttempt attempt, EventType eventType, Severity severity,
                        String detail, LocalDateTime occurredAt, String ip) {
        this.attempt = attempt;
        this.eventType = eventType;
        this.severity = severity;
        this.detail = detail;
        this.occurredAt = occurredAt;
        this.ip = ip;
    }

    public enum EventType {
        /** 입장 (본인인증 통과 후). */
        ENTER,
        /** 퇴장 / 제출. */
        EXIT,
        /** 네트워크 끊김 후 재접속. */
        RESUME,
        TAB_BLUR,
        TAB_FOCUS,
        FULLSCREEN_EXIT,
        COPY,
        PASTE,
        /** 웹캠에 얼굴이 둘 이상 감지. */
        MULTI_FACE,
        /** 웹캠에 얼굴이 감지되지 않음. */
        NO_FACE,
        NETWORK_DROP
    }

    public enum Severity {
        INFO,
        WARN,
        CRITICAL
    }
}
