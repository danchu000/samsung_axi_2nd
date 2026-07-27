package com.ssa.lms.notice.dto;

import com.ssa.lms.notice.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 알림 등록/수정 폼 (admin-alarm-add.html).
 *
 * 화면의 select 값(high/normal/low, unread/read/scheduled/sent/hidden)을 그대로 받아
 * 엔티티 enum 으로 변환한다. 화면 id 는 기존 JS(applyStatusUI 등)가 잡고 있어 유지했다.
 */
@Getter
@Setter
public class NotificationForm {

    private Long id;

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력하세요.")
    private String title;

    @NotBlank(message = "내용을 입력하세요.")
    private String content;

    /** 화면 prioritySelect: high / normal / low */
    private String priority = "high";

    /** 화면 statusSelect: unread(=예약전 임시) / read / scheduled / sent / hidden */
    private String status = "sent";

    /** ALL / COURSE / USER */
    private String targetType = "ALL";

    private Long targetRefId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sendAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dueDate;

    public Notification.Priority toPriority() {
        return switch (priority == null ? "" : priority.toLowerCase()) {
            case "urgent", "긴급" -> Notification.Priority.URGENT;
            case "normal", "중간", "보통" -> Notification.Priority.NORMAL;
            case "low", "낮음" -> Notification.Priority.LOW;
            default -> Notification.Priority.HIGH;
        };
    }

    public Notification.TargetType toTargetType() {
        return switch (targetType == null ? "" : targetType.toUpperCase()) {
            case "COURSE" -> Notification.TargetType.COURSE;
            case "USER" -> Notification.TargetType.USER;
            default -> Notification.TargetType.ALL;
        };
    }

    /**
     * 화면 상태값 → NotificationStatus.
     * 화면의 unread/read 는 "수신자별 읽음 상태"라 발송 단위 상태가 아니다.
     * 여기서는 발송 여부만 판단하고, 읽음은 NotificationRecipient 가 갖는다.
     */
    public Notification.NotificationStatus toStatus() {
        return switch (status == null ? "" : status.toLowerCase()) {
            case "scheduled" -> Notification.NotificationStatus.SCHEDULED;
            case "hidden", "canceled" -> Notification.NotificationStatus.CANCELED;
            case "draft" -> Notification.NotificationStatus.DRAFT;
            default -> Notification.NotificationStatus.SENT;
        };
    }

    /** 미입력이면 즉시 발송(현재 시각). */
    public LocalDateTime resolveSendAt() {
        return sendAt == null ? LocalDateTime.now() : sendAt;
    }

    public static NotificationForm from(Notification n) {
        NotificationForm form = new NotificationForm();
        form.id = n.getId();
        form.title = n.getTitle();
        form.content = n.getContent();
        form.priority = n.getPriority().name().toLowerCase();
        form.status = n.getStatus().name().toLowerCase();
        form.targetType = n.getTargetType().name();
        form.targetRefId = n.getTargetRefId();
        form.sendAt = n.getSendAt();
        form.dueDate = n.getDueDate();
        return form;
    }
}
