package com.ssa.lms.proctor.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.exam.entity.ExamAttempt;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감독관이 응시자에게 실제로 발송한 경고.
 *
 * ExamEventLog 는 시스템이 자동 수집한 정황이고, 이 테이블은 사람이 내린 조치다.
 * 권한정의서(1) 16행: 부정행위 제재는 관리자 O / 강사 △(경고). 따라서
 *  - 경고 발송(이 테이블 INSERT): 관리자 + 강사
 *  - 응시 무효 처리(ExamAttempt.voidAttempt): 관리자만
 */
@Entity
@Table(name = "proctor_warning", indexes = @Index(name = "idx_warning_attempt", columnList = "attempt_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProctorWarning extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sent_by", nullable = false)
    private User sentBy;

    @Column(name = "message", length = 500, nullable = false)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /** 응시자가 경고를 확인한 시각. */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Builder
    public ProctorWarning(ExamAttempt attempt, User sentBy, String message, LocalDateTime sentAt) {
        this.attempt = attempt;
        this.sentBy = sentBy;
        this.message = message;
        this.sentAt = sentAt;
    }

    public void acknowledge(LocalDateTime at) {
        this.acknowledgedAt = at;
    }
}
