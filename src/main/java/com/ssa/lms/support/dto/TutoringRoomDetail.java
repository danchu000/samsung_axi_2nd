package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.TutoringMessage;
import com.ssa.lms.support.entity.TutoringRoom;

import java.util.List;

/**
 * 튜터링 방 상세 (대화 로그 포함).
 *
 * 화면 대응: admin-06-support/tutoring-detail.html, modal-tutoring.html,
 *           trainee/tutoring.html, instructor/tutoring-detail.html,
 *           admin-support-chat-monitoring.html (읽기 전용)
 *
 * <p><b>권한:</b> 권한정의서(2) 18행 — 채팅 메시지는 관리자 R / 강사 R / 훈련생 C·R.
 * 관리자·강사에게는 수정·삭제 기능을 두지 않는다. 그래서 이 DTO 에도
 * 메시지 편집용 필드가 없다.</p>
 */
public record TutoringRoomDetail(
        Long id,
        String title,
        String statusLabel,
        String statusClass,
        String courseName,
        String courseSession,
        Long traineeId,
        String traineeName,
        Long instructorId,
        String instructorName,
        boolean assigned,
        boolean closed,
        String createdAt,
        String lastMessageAt,
        List<MessageRow> messages
) {

    /**
     * 메시지 한 건.
     *
     * @param mine 보는 사람이 보낸 메시지인지 — 화면의 말풍선 좌/우(.msg-row user / bot) 판정용
     */
    public record MessageRow(
            Long id,
            String content,
            String senderName,
            String senderRole,
            boolean mine,
            String sentAt,
            String sentTime,
            boolean read
    ) {
        public static MessageRow of(TutoringMessage m, Long viewerId) {
            Long senderId = m.getSender() == null ? null : m.getSender().getId();
            return new MessageRow(
                    m.getId(),
                    m.getContent(),
                    m.getSender() == null ? "-" : m.getSender().getName(),
                    m.getSender() == null || m.getSender().getRole() == null
                            ? "-" : m.getSender().getRole().getLabel(),
                    senderId != null && senderId.equals(viewerId),
                    SupportFormat.dateTime(m.getSentAt()),
                    SupportFormat.time(m.getSentAt()),
                    m.getReadAt() != null
            );
        }
    }

    public static TutoringRoomDetail of(TutoringRoom r, List<TutoringMessage> messages, Long viewerId) {
        return new TutoringRoomDetail(
                r.getId(),
                r.getTitle() == null ? "(제목 없음)" : r.getTitle(),
                TutoringRoomListRow.statusLabel(r.getStatus()),
                TutoringRoomListRow.statusClass(r.getStatus()),
                r.getCourse() == null ? "-" : r.getCourse().getCourseName(),
                r.getCourse() == null ? "-" : r.getCourse().getCourseName(),
                r.getTrainee() == null ? null : r.getTrainee().getId(),
                r.getTrainee() == null ? "-" : r.getTrainee().getName(),
                r.getInstructor() == null ? null : r.getInstructor().getId(),
                r.getInstructor() == null ? "미배정" : r.getInstructor().getName(),
                r.getInstructor() != null,
                r.getStatus() == TutoringRoom.RoomStatus.CLOSED,
                SupportFormat.dateTime(r.getCreatedAt()),
                SupportFormat.dateTime(r.getLastMessageAt()),
                messages.stream().map(m -> MessageRow.of(m, viewerId)).toList()
        );
    }
}
