package com.ssa.lms.support.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1:1 튜터링 채팅방.
 *
 * 매핑 근거: templates/admin/admin-06-support/admin-support-tutoring.html,
 *           tutoring-detail.html, modal-tutoring.html
 * 권한정의서(1) 26행: 1:1 채팅은 관리자/강사/훈련생 모두 O.
 */
@Entity
@Table(
        name = "tutoring_room",
        indexes = {
                @Index(name = "idx_room_trainee", columnList = "trainee_id"),
                @Index(name = "idx_room_instructor", columnList = "instructor_id"),
                @Index(name = "idx_room_last_message", columnList = "last_message_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TutoringRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private User trainee;

    /** 배정된 강사. null 이면 화면의 "미배정". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RoomStatus status;

    /** 목록 정렬용. 메시지 저장 시 갱신한다. */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder
    public TutoringRoom(Course course, User trainee, User instructor, String title, RoomStatus status) {
        this.course = course;
        this.trainee = trainee;
        this.instructor = instructor;
        this.title = title;
        this.status = status;
    }

    public void assignInstructor(User instructor) {
        this.instructor = instructor;
        if (this.status == RoomStatus.WAITING) {
            this.status = RoomStatus.ACTIVE;
        }
    }

    public void touch(LocalDateTime at) {
        this.lastMessageAt = at;
    }

    public void close() {
        this.status = RoomStatus.CLOSED;
    }

    public enum RoomStatus {
        /** 강사 미배정. */
        WAITING,
        ACTIVE,
        CLOSED
    }
}
