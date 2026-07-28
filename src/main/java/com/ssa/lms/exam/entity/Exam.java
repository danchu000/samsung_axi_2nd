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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@SQLDelete(sql = "UPDATE exam SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
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

    /**
     * 문제 세트 개수 (앨리스 항목: "문제 세트를 여러 개 제작하여 응시자에게 랜덤으로 배정").
     *
     * <p>1 이면 세트가 하나뿐이라 전원 동일 문항 — 세트 기능이 들어오기 전과 완전히 같은 동작이다.
     * 2 이상이면 {@link ExamQuestion#getSetNo()} 로 문항이 세트별로 나뉘고, 응시 시작 시점에
     * {@code ExamAttempt.assignedSetNo} 로 그중 하나가 무작위 배정된다.</p>
     *
     * <p>실제로 편성된 세트 번호는 {@link #availableSetNos()} 가 진실이다. 이 값은
     * "관리자가 선언한 목표 세트 수"이고 규칙 확정(materializeRules)이 몇 벌을 뽑을지 결정한다.</p>
     */
    @Column(name = "question_set_count", nullable = false)
    private Integer questionSetCount;

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

    /**
     * 성적 공개 방식 (앨리스 항목: "성적 공개 여부를 설정할 수 있습니다").
     *
     * <p>응시자에게만 적용된다. 관리자·강사는 채점 화면에서 언제나 점수를 본다.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result_release", length = 20, nullable = false)
    private ResultRelease resultRelease;

    /** {@link ResultRelease#SCHEDULED} 일 때의 공개 시각. 다른 방식에서는 무시된다. */
    @Column(name = "result_release_at")
    private LocalDateTime resultReleaseAt;

    /**
     * 사전 모의 테스트 여부 (앨리스 항목: "사전 모의 테스트를 제공하여 응시 환경에 미리 적응").
     *
     * <p>true 면 성적에 반영되지 않고(= Grade 를 만들지 않는다) 응시 횟수 제한도 걸리지 않는다.
     * 본인인증·부정행위 감지는 그대로 동작한다 — 응시 환경 적응이 목적이라 끄면 의미가 없다.
     * 다만 위반해도 자동 무효 처리는 하지 않는다(무효 처리는 감독관의 수동 조치 뿐이다).</p>
     */
    @Column(name = "practice_mode", nullable = false)
    private boolean practiceMode;

    /** 응시 안내 문구. online-test.js 의 note. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ExamStatus status;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNo ASC, seq ASC")
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamQuestionRule> questionRules = new ArrayList<>();

    @Builder
    public Exam(String examName, ExamType examType, Course course, Subject subject, Session session,
                User instructor, Integer timeLimitMin, Integer autoScore, Integer manualScore,
                Integer totalScore, Integer passScore, boolean randomOrder, Integer questionSetCount,
                boolean retakeAllowed, Integer maxAttempts, LocalDateTime windowStart, LocalDateTime windowEnd,
                boolean requireIdentityVerification, boolean proctorEnabled, boolean requireWebcam,
                boolean blockTabSwitch, boolean blockCopyPaste, ResultRelease resultRelease,
                LocalDateTime resultReleaseAt, boolean practiceMode, String note, ExamStatus status) {
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
        // 세트 수와 성적 공개 방식은 기본값을 여기서 박는다 — 기존 시드/테스트가 값을 넘기지 않아도
        // NOT NULL 컬럼이 비지 않고, 세트 1개 = 기존 동작(하위호환)이 된다.
        this.questionSetCount = normalizeSetCount(questionSetCount);
        this.retakeAllowed = retakeAllowed;
        this.maxAttempts = maxAttempts;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.requireIdentityVerification = requireIdentityVerification;
        this.proctorEnabled = proctorEnabled;
        this.requireWebcam = requireWebcam;
        this.blockTabSwitch = blockTabSwitch;
        this.blockCopyPaste = blockCopyPaste;
        this.resultRelease = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        this.resultReleaseAt = resultReleaseAt;
        this.practiceMode = practiceMode;
        this.note = note;
        this.status = status;
    }

    /** 시험 기본 정보 수정. 컬렉션(문항·규칙)은 서비스가 따로 갈아끼운다. */
    public void update(String examName, ExamType examType, Course course, Subject subject,
                       Session session, User instructor, Integer timeLimitMin, Integer autoScore,
                       Integer manualScore, Integer totalScore, Integer passScore, boolean randomOrder,
                       Integer questionSetCount, boolean retakeAllowed, Integer maxAttempts,
                       LocalDateTime windowStart, LocalDateTime windowEnd,
                       boolean requireIdentityVerification, boolean proctorEnabled, boolean requireWebcam,
                       boolean blockTabSwitch, boolean blockCopyPaste, ResultRelease resultRelease,
                       LocalDateTime resultReleaseAt, boolean practiceMode, String note, ExamStatus status) {
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
        this.questionSetCount = normalizeSetCount(questionSetCount);
        this.retakeAllowed = retakeAllowed;
        this.maxAttempts = maxAttempts;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.requireIdentityVerification = requireIdentityVerification;
        this.proctorEnabled = proctorEnabled;
        this.requireWebcam = requireWebcam;
        this.blockTabSwitch = blockTabSwitch;
        this.blockCopyPaste = blockCopyPaste;
        this.resultRelease = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        this.resultReleaseAt = resultReleaseAt;
        this.practiceMode = practiceMode;
        this.note = note;
        this.status = status;
    }

    private static Integer normalizeSetCount(Integer value) {
        return (value == null || value < 1) ? 1 : value;
    }

    public void changeStatus(ExamStatus status) {
        this.status = status;
    }

    /**
     * 성적 공개 방식만 교체. 문항·규칙 컬렉션은 건드리지 않는다.
     * 응시 기록이 있어 {@code update()} 가 잠긴 시험도 이 경로로는 공개 설정을 바꿀 수 있다
     * (채점이 끝난 뒤 공개로 돌리는 것이 정상 운영이라 잠그면 안 된다).
     */
    public void changeResultRelease(ResultRelease resultRelease, LocalDateTime resultReleaseAt) {
        this.resultRelease = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        this.resultReleaseAt = resultReleaseAt;
    }

    /**
     * 편성 문항 전체 제거.
     * 호출 후 곧바로 add 하면 안 된다 — orphan DELETE 보다 INSERT 가 먼저 나가
     * uk_exam_question(exam_id, question_id) 유니크 제약에 걸린다. 사이에 flush() 할 것.
     */
    public void clearExamQuestions() {
        this.examQuestions.clear();
    }

    /** 자동 출제 규칙으로 편성된 문항만 제거 (규칙 재확정용). 마찬가지로 add 전에 flush 필요. */
    public void removeRuleQuestions() {
        this.examQuestions.removeIf(ExamQuestion::isFromRule);
    }

    /** 출제 규칙 전체 제거. 자식(ExamQuestionRuleDifficulty)도 cascade 로 함께 지워진다. */
    public void clearQuestionRules() {
        this.questionRules.clear();
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

    /* ===================== 문제 세트 ===================== */

    /**
     * 실제로 편성된 세트 번호 (오름차순, 중복 제거).
     *
     * <p>{@link #questionSetCount} 는 "선언한 목표"일 뿐이라 배정은 반드시 이 값으로 한다.
     * 규칙 확정 전이거나 문제은행이 모자라 일부 세트가 비어 있을 수 있기 때문이다.
     * 컬렉션이 초기화된 상태(fetch join)에서 불러야 한다.</p>
     */
    public List<Integer> availableSetNos() {
        return examQuestions.stream()
                .map(ExamQuestion::getSetNo)
                .map(no -> no == null ? 1 : no)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 특정 세트의 편성 문항.
     *
     * @param setNo 세트 번호. {@code null} 이면 세트 개념 도입 이전 응시 회차이므로 전체를 돌려준다(하위호환).
     */
    public List<ExamQuestion> questionsOfSet(Integer setNo) {
        if (setNo == null) {
            return List.copyOf(examQuestions);
        }
        return examQuestions.stream()
                .filter(eq -> setNo.equals(eq.getSetNo() == null ? 1 : eq.getSetNo()))
                .toList();
    }

    /* ===================== 성적 공개 ===================== */

    /**
     * 응시자에게 점수·정답·해설을 보여도 되는지.
     *
     * <p>관리자·강사 화면은 이 판정을 거치지 않는다 — 채점을 해야 하므로 항상 보인다.</p>
     *
     * @param manualPending 수동 채점이 남아 있는지. {@link ResultRelease#AFTER_GRADING} 판정에만 쓴다.
     */
    public boolean isResultVisibleToTrainee(LocalDateTime at, boolean manualPending) {
        ResultRelease mode = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        return switch (mode) {
            case IMMEDIATE -> true;
            case AFTER_GRADING -> !manualPending;
            case SCHEDULED -> resultReleaseAt != null && !at.isBefore(resultReleaseAt);
            case HIDDEN -> false;
        };
    }

    /** 결과가 가려졌을 때 응시자에게 보여줄 안내 문구. */
    public String resultHiddenMessage() {
        ResultRelease mode = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        return switch (mode) {
            case AFTER_GRADING -> "채점 결과는 채점 완료 후 확인할 수 있습니다.";
            case SCHEDULED -> resultReleaseAt == null
                    ? "채점 결과는 공개 후 확인할 수 있습니다."
                    : "채점 결과는 공개 후 확인할 수 있습니다. (공개 예정 "
                            + resultReleaseAt.toLocalDate() + " " + resultReleaseAt.toLocalTime() + ")";
            case HIDDEN -> "이 시험은 성적을 공개하지 않습니다.";
            case IMMEDIATE -> "채점 결과는 공개 후 확인할 수 있습니다.";
        };
    }

    /** 화면 표시용 성적 공개 방식 문구. */
    public String resultReleaseText() {
        ResultRelease mode = resultRelease == null ? ResultRelease.IMMEDIATE : resultRelease;
        return switch (mode) {
            case IMMEDIATE -> "즉시 공개";
            case AFTER_GRADING -> "채점 완료 후 공개";
            case SCHEDULED -> resultReleaseAt == null
                    ? "지정 일시 공개"
                    : resultReleaseAt.toLocalDate() + " " + resultReleaseAt.toLocalTime() + " 공개";
            case HIDDEN -> "비공개";
        };
    }

    /**
     * 성적 공개 방식.
     *
     * <p>{@code IMMEDIATE} 가 기본값이다 — 세 옵션이 들어오기 전 동작(제출 직후 점수 노출)과 같다.</p>
     */
    public enum ResultRelease {
        /** 제출 즉시 공개. */
        IMMEDIATE,
        /** 수동 채점이 모두 끝난 뒤 공개. */
        AFTER_GRADING,
        /** {@link Exam#getResultReleaseAt()} 이후 공개. */
        SCHEDULED,
        /** 응시자에게 공개하지 않음. */
        HIDDEN
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
