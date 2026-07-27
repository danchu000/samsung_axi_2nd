package com.ssa.lms.support.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 튜터링 채팅 메시지. IA 의 chat_log 에 해당한다.
 *
 * 관리자 화면 admin-support-chat-monitoring.html 에서 열람되므로,
 * 권한정의서(2) 18행대로 관리자·강사는 R 만 가능하고 수정/삭제는 두지 않는다.
 *
 * 개인정보 주의: 채팅 본문에 연락처가 오가는 일이 흔해 AES-256 컨버터 대상이다.
 */
@Entity
@Table(
        name = "tutoring_message",
        indexes = @Index(name = "idx_message_room", columnList = "room_id, sent_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TutoringMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private TutoringRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // TODO: A 가 CryptoConverter 를 제공하면 @Convert 추가
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    public TutoringMessage(TutoringRoom room, User sender, String content, LocalDateTime sentAt) {
        this.room = room;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
    }

    public void markRead(LocalDateTime at) {
        if (this.readAt == null) {
            this.readAt = at;
        }
    }
}
