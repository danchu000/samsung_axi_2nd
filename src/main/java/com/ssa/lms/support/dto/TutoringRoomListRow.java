package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.TutoringRoom;

/**
 * 튜터링 방 목록 한 행.
 *
 * 화면 대응: admin-support-tutoring.html 튜터링 탭 테이블
 * (상태 / 과정명 / 차시 / 작성자 / 등록일 / 조회수 / 관리)
 * 및 admin-support-chat-monitoring.html 방 목록.
 *
 * 메시지 본문은 담지 않는다 — 암호화 대상이고 목록에 필요도 없다.
 */
public record TutoringRoomListRow(
        Long id,
        String title,
        String statusLabel,
        String statusClass,
        String courseName,
        String traineeName,
        String instructorName,
        boolean assigned,
        String createdAt,
        String createdAtShort,
        String lastMessageAt,
        long messageCount
) {

    public static TutoringRoomListRow of(TutoringRoom r, long messageCount) {
        return new TutoringRoomListRow(
                r.getId(),
                r.getTitle() == null ? "(제목 없음)" : r.getTitle(),
                statusLabel(r.getStatus()),
                statusClass(r.getStatus()),
                r.getCourse() == null ? "-" : r.getCourse().getCourseName(),
                r.getTrainee() == null ? "-" : r.getTrainee().getName(),
                r.getInstructor() == null ? "미배정" : r.getInstructor().getName(),
                r.getInstructor() != null,
                SupportFormat.date(r.getCreatedAt()),
                SupportFormat.shortDate(r.getCreatedAt()),
                SupportFormat.dateTime(r.getLastMessageAt()),
                messageCount
        );
    }

    public static String statusLabel(TutoringRoom.RoomStatus status) {
        return switch (status) {
            case WAITING -> "대기";
            case ACTIVE -> "진행중";
            case CLOSED -> "종료";
        };
    }

    public static String statusClass(TutoringRoom.RoomStatus status) {
        return switch (status) {
            case WAITING -> "status-dropout";
            case ACTIVE -> "status-pending";
            case CLOSED -> "status-disabled";
        };
    }
}
