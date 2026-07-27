package com.ssa.lms.course.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강신청/수강 이력. 내역서 요건: 성명·과정명·훈련 개시/종료일·수강일 웹 조회.
 * 취소·반려는 삭제가 아니라 상태 변경으로 남긴다 (3년 보존).
 */
@Entity
@Table(name = "enrollment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_user_course", columnNames = {"user_id", "course_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** role = TRAINEE 인 사용자 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User trainee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    /** 승인/반려 처리 시각 */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 수료 처리 시각 (자동 이수 처리 시 기록) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 전체 진도율(%) — 진도 추적 API 가 갱신 */
    @Column(name = "progress_rate", nullable = false)
    private double progressRate = 0.0;

    @Builder
    private Enrollment(User trainee, Course course, EnrollmentStatus status, LocalDateTime appliedAt) {
        this.trainee = trainee;
        this.course = course;
        this.status = status != null ? status : EnrollmentStatus.APPLIED;
        this.appliedAt = appliedAt != null ? appliedAt : LocalDateTime.now();
    }

    public void approve(LocalDateTime at) {
        this.status = EnrollmentStatus.APPROVED;
        this.decidedAt = at;
    }

    public void reject(LocalDateTime at) {
        this.status = EnrollmentStatus.REJECTED;
        this.decidedAt = at;
    }

    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
    }

    public void complete(LocalDateTime at) {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = at;
    }

    public void updateProgressRate(double progressRate) {
        this.progressRate = progressRate;
    }
}
