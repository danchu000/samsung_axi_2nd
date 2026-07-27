package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.entity.Subject;
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
 * 시험 정의.
 *
 * 매핑 근거
 *  - templates/admin/admin-04-evaluation/admin-evaluation-test-add.html (testName, testType,
 *    timeLimit, autoScore, manualScore, passScore, randomOrder)
 *  - static/js/trainee/online-test.js mockExams (windowStart/End, durationMin, retakePolicy,
 *    attemptsTotal, status)
 *  - 내역서: "평가자 판별(진행단계/최종/과정) 본인인증" -> requireIdentityVerification
 *  - 내역서: "부정행위 방지" -> proctor* / block* 플래그
 */
@Entity
@Table(
        name = "exam",
        indexes = {
                @Index(name = "idx_exam_course", columnList = "course_id"),
                @Index(name = "idx_exam_status", columnList = "status"),
                @Index(name = "idx_exam_window", columnList = "window_start, window_end")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "exam_name", length = 200, nullable = false)
    private String examName;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", length = 20, nullable = false)
    private ExamType examType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /** 연계 차시. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    /** 출제자(강사 또는 관리자). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    /** 응시 제한시간(분). */
    @Column(name = "time_limit_min", nullable = false)
    private Integer timeLimitMin;

    /** 자동채점 영역 배점 합계. */
    @Column(name = "auto_score", nullable = false)
    private Integer autoScore;

    /** 수동채점 영역 배점 합계. */
    @Column(name = "manual_score", nullable = false)
    private Integer manualScore;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    /** 합격 기준 점수. */
    @Column(name = "pass_score", nullable = false)
    private Integer passScore;

    /** 문항 순서 랜덤 출제. */
    @Column(name = "random_order", nullable = false)
    private boolean randomOrder;

    /** 재응시 허용 여부. */
    @Column(name = "retake_allowed", nullable = false)
    private boolean retakeAllowed;

    /** 최대 응시 횟수. retakeAllowed=false 이면 1. */
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    /** 응시 가능 기간 시작. */
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    /** 응시 가능 기간 종료. */
    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    /**
     * 입장 시 본인인증 강제 여부.
     * 내역서 필수 요건이므로 기본값 true 로 운영할 것.
     */
    @Column(name = "require_identity_verification", nullable = false)
    private boolean requireIdentityVerification;

    /** 감독(모니터링) 사용 여부. */
    @Column(name = "proctor_enabled", nullable = false)
    private boolean proctorEnabled;

    /** 웹캠 필수 여부. proctorEnabled=true 일 때만 의미 있음. */
    @Column(name = "require_webcam", nullable = false)
    private boolean requireWebcam;

    /** 탭 전환 차단/경고. */
    @Column(name = "block_tab_switch", nullable = false)
    private boolean blockTabSwitch;

    /** 복사/붙여넣기 차단. */
    @Column(name = "block_copy_paste", nullable = false)
    private boolean blockCopyPaste;

    /** 응시 안내 문구. online-test.js 의 note. */
    @Lob
    @Column(name = "note")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ExamStatus status;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamQuestionRule> questionRules = new ArrayList<>();

    @Builder
    public Exam(String examName, ExamType examType, Course course, Subject subject, Session session,
                User instructor, Integer timeLimitMin, Integer autoScore, Integer manualScore,
                Integer totalScore, Integer passScore, boolean randomOrder, boolean retakeAllowed,
                Integer maxAttempts, LocalDateTime windowStart, LocalDateTime windowEnd,
                boolean requireIdentityVerification, boolean proctorEnabled, boolean requireWebcam,
                boolean blockTabSwitch, boolean blockCopyPaste, String note, ExamStatus status) {
        this.examName = examName;
        this.examType = examType;
        this.course = course;
        this.subject = subject;
        this.session = session;
        this.instructor = instructor;
        this.timeLimitMin = timeLimitMin;
        this.autoScore = autoScore;
        this.manualScore = manualScore;
        this.totalScore = totalScore;
        this.passScore = passScore;
        this.randomOrder = randomOrder;
        this.retakeAllowed = retakeAllowed;
        this.maxAttempts = maxAttempts;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.requireIdentityVerification = requireIdentityVerification;
        this.proctorEnabled = proctorEnabled;
        this.requireWebcam = requireWebcam;
        this.blockTabSwitch = blockTabSwitch;
        this.blockCopyPaste = blockCopyPaste;
        this.note = note;
        this.status = status;
    }

    public void addExamQuestion(ExamQuestion examQuestion) {
        this.examQuestions.add(examQuestion);
        examQuestion.assignExam(this);
    }

    public void addQuestionRule(ExamQuestionRule rule) {
        this.questionRules.add(rule);
        rule.assignExam(this);
    }

    /** 지금 응시 가능한 기간인지. 상태와 별개로 기간만 판정한다. */
    public boolean isWithinWindow(LocalDateTime at) {
        return !at.isBefore(windowStart) && !at.isAfter(windowEnd);
    }

    /** 화면 값: 단위시험 / 중간고사 / 기말고사 */
    public enum ExamType {
        UNIT,
        MIDTERM,
        FINAL
    }

    public enum ExamStatus {
        DRAFT,
        SCHEDULED,
        OPEN,
        CLOSED,
        ARCHIVED
    }
}
