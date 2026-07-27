package com.ssa.lms.notice.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림(발송 단위).
 *
 * 매핑 근거: templates/admin/admin-07-notice/admin-alarm-add.html, admin-alarm.html
 *   (alarmTitle, alarmContent, prioritySelect, statusSelect, dueDate)
 *
 * 주의: 정적 파일명이 alram/arlam 으로 흔들려 있다. PLAN.md Phase 0 에서 alarm 으로 통일하기로
 * 했으므로, 패키지/클래스명은 표준어인 Notification 으로 가고 템플릿 경로만 alarm 을 쓴다.
 *
 * 수신자별 읽음 상태는 NotificationRecipient 가 갖는다.
 */
@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_send_at", columnList = "send_at"),
                @Index(name = "idx_notification_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20, nullable = false)
    private TargetType targetType;

    /** targetType=COURSE 면 course.id, USER 면 user.id, ALL 이면 null. */
    @Column(name = "target_ref_id")
    private Long targetRefId;

    /** 예약 발송 시각. 즉시 발송이면 생성 시각과 동일. */
    @Column(name = "send_at", nullable = false)
    private LocalDateTime sendAt;

    /** 화면의 dueDate. 알림이 가리키는 마감 기한(과제/시험 마감 등). */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private NotificationStatus status;

    @Builder
    public Notification(String title, String content, Priority priority, TargetType targetType,
                        Long targetRefId, LocalDateTime sendAt, LocalDateTime dueDate,
                        User sender, NotificationStatus status) {
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.targetType = targetType;
        this.targetRefId = targetRefId;
        this.sendAt = sendAt;
        this.dueDate = dueDate;
        this.sender = sender;
        this.status = status;
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum TargetType {
        /** 전체 사용자. */
        ALL,
        /** 특정 과정 수강생. */
        COURSE,
        /** 특정 사용자 1명. */
        USER
    }

    public enum NotificationStatus {
        DRAFT,
        SCHEDULED,
        SENT,
        CANCELED
    }
}
