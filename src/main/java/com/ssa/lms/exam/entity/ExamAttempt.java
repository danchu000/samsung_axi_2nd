package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 응시 회차.
 *
 * 매핑 근거
 *  - static/js/trainee/online-test.js: attemptsUsed / attemptsTotal / status
 *  - 내역서: 시험 입장 시 본인인증 -> identityVerifiedAt / identityVerifyMethod
 *
 * 주의: expiresAt 은 응시 시작 시각 + Exam.timeLimitMin 을 서버가 계산해 저장한다.
 * 클라이언트 타이머는 표시용일 뿐이며 제출 마감 판정은 반드시 이 컬럼으로 한다.
 */
@Entity
@Table(
        name = "exam_attempt",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_attempt", columnNames = {"exam_id", "user_id", "attempt_no"}),
        indexes = {
                @Index(name = "idx_attempt_exam_status", columnList = "exam_id, status"),
                @Index(name = "idx_attempt_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 1부터 시작하는 응시 회차. Exam.maxAttempts 를 넘을 수 없다. */
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 서버가 계산한 제출 마감 시각. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AttemptStatus status;

    /** 본인인증 완료 시각. Exam.requireIdentityVerification=true 면 null 인 상태로 시작 불가. */
    @Column(name = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    /** 본인인증 수단 (A의 IdentityVerificationService 가 돌려주는 값). */
    @Column(name = "identity_verify_method", length = 30)
    private String identityVerifyMethod;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "auto_score")
    private Integer autoScore;

    @Column(name = "manual_score")
    private Integer manualScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "passed")
    private Boolean passed;

    /** 부정행위로 무효 처리한 관리자. 권한정의서상 제재는 관리자(O)만 가능. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Builder
    public ExamAttempt(Exam exam, User user, Integer attemptNo, LocalDateTime startedAt,
                       LocalDateTime expiresAt, AttemptStatus status,
                       LocalDateTime identityVerifiedAt, String identityVerifyMethod,
                       String ip, String userAgent) {
        this.exam = exam;
        this.user = user;
        this.attemptNo = attemptNo;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.identityVerifiedAt = identityVerifiedAt;
        this.identityVerifyMethod = identityVerifyMethod;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public void markIdentityVerified(LocalDateTime at, String method) {
        this.identityVerifiedAt = at;
        this.identityVerifyMethod = method;
    }

    public void submit(LocalDateTime at, AttemptStatus status) {
        this.submittedAt = at;
        this.status = status;
    }

    /**
     * 점수 반영.
     *
     * <p><b>passScore 가 null 이면 passed 도 null 로 남긴다</b> — "수동 채점이 남아 합격 여부 미정"
     * 이라는 뜻이다. 예전 구현은 {@code passScore != null && ...} 이라 미정인 회차가 항상
     * {@code false}(불합격)로 저장됐고, 그 값이 결과 화면과 채점 목록에 "불합격"으로 표시됐다.
     * (시험 채점 슬라이스에서 발견해 수정 — ExamAttemptService.finish() 가 manualPending 일 때
     * passScore=null 로 부르는 것이 원래 의도다.)</p>
     */
    public void applyScore(Integer autoScore, Integer manualScore, Integer passScore) {
        this.autoScore = autoScore;
        this.manualScore = manualScore;
        this.totalScore = (autoScore == null ? 0 : autoScore) + (manualScore == null ? 0 : manualScore);
        this.passed = passScore == null ? null : this.totalScore >= passScore;
    }

    public void voidAttempt(User admin, String reason) {
        this.voidedBy = admin;
        this.voidReason = reason;
        this.status = AttemptStatus.VOIDED;
    }

    public boolean isExpired(LocalDateTime at) {
        return expiresAt != null && at.isAfter(expiresAt);
    }

    public enum AttemptStatus {
        /** 입장 전 (본인인증 대기 포함). */
        NOT_STARTED,
        IN_PROGRESS,
        /** 응시자가 직접 제출. */
        SUBMITTED,
        /** 시간 만료로 서버가 자동 제출. */
        AUTO_SUBMITTED,
        /** 기간 내 재접속 없이 종료됨. */
        ABANDONED,
        /** 부정행위 등으로 관리자가 무효 처리. */
        VOIDED
    }
}
