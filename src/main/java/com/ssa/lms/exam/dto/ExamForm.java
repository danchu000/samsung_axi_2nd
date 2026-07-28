package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.exam.entity.ExamQuestion;
import com.ssa.lms.exam.entity.ExamQuestionRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시험 생성/수정 폼.
 *
 * 화면 admin-evaluation-test-add.html / -test-update.html 의 입력에 대응한다.
 * 기존 JS 가 getElementById 로 쓰는 id 속성(testName, testType, timeLimit, passScore,
 * randomOrder, addRule* 등)은 그대로 두고 name 만 이 DTO 에 맞췄다.
 *
 * 체크박스는 Thymeleaf 폼에서 hidden "_필드명" 마커를 같이 보낸다.
 * (Spring WebDataBinder 의 필드 마커 규약 — 체크 해제 시 false 로 리셋된다.
 *  requireIdentityVerification 기본값 true 를 유지하면서도 해제가 반영되게 하려면 이 방식이 필요하다.)
 */
@Getter
@Setter
public class ExamForm {

    private Long id;

    @NotBlank(message = "시험명을 입력하세요.")
    private String examName;

    /** 화면 값: 단위시험 / 중간고사 / 기말고사 */
    @NotBlank(message = "시험 유형을 선택하세요.")
    private String examType = "단위시험";

    @NotNull(message = "과정을 선택하세요.")
    private Long courseId;

    private Long subjectId;
    private Long sessionId;
    private Long instructorId;

    @NotNull(message = "제한시간을 입력하세요.")
    @Min(value = 1, message = "제한시간은 1분 이상이어야 합니다.")
    private Integer timeLimitMin = 60;

    @NotNull(message = "자동채점 배점을 입력하세요.")
    @Min(value = 0, message = "배점은 0 이상이어야 합니다.")
    private Integer autoScore = 100;

    @NotNull(message = "수동채점 배점을 입력하세요.")
    @Min(value = 0, message = "배점은 0 이상이어야 합니다.")
    private Integer manualScore = 0;

    @NotNull(message = "총점을 입력하세요.")
    @Min(value = 1, message = "총점은 1 이상이어야 합니다.")
    private Integer totalScore = 100;

    @NotNull(message = "합격점수를 입력하세요.")
    @Min(value = 0, message = "합격점수는 0 이상이어야 합니다.")
    private Integer passScore = 70;

    private boolean randomOrder = true;

    /**
     * 문제 세트 개수. 1 이면 전원 동일 세트(기존 동작).
     * 2 이상이면 응시 시작 시 서버가 그중 하나를 무작위로 배정한다.
     */
    @NotNull(message = "문제 세트 개수를 입력하세요.")
    @Min(value = 1, message = "문제 세트는 1개 이상이어야 합니다.")
    private Integer questionSetCount = 1;

    /* ===== 성적 공개 (앨리스 항목) ===== */

    /** 화면 값: 즉시 공개 / 채점 완료 후 / 지정 일시 / 비공개 */
    private String resultRelease = "즉시 공개";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime resultReleaseAt;

    /* ===== 사전 모의 테스트 (앨리스 항목) ===== */

    /** true 면 성적 미반영 + 응시 횟수 무제한. 본인인증·부정행위 감지는 그대로 동작한다. */
    private boolean practiceMode;

    /* ===== 응시 기간 (내역서: 응시 가능 기간 명시) ===== */

    @NotNull(message = "응시 시작일시를 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime windowStart;

    @NotNull(message = "응시 종료일시를 입력하세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime windowEnd;

    /* ===== 재응시 ===== */

    private boolean retakeAllowed;

    @NotNull(message = "최대 응시 횟수를 입력하세요.")
    @Min(value = 1, message = "최대 응시 횟수는 1 이상이어야 합니다.")
    private Integer maxAttempts = 1;

    /* ===== 내역서 보안 요건 ===== */

    /** 입장 시 본인인증. 정부 제출 필수 요건이라 기본 true. */
    private boolean requireIdentityVerification = true;
    private boolean proctorEnabled = true;
    private boolean requireWebcam;
    private boolean blockTabSwitch = true;
    private boolean blockCopyPaste = true;

    private String note;

    /** 화면 값: 임시저장 / 예정 / 진행중 / 종료 / 보관 */
    private String status = "임시저장";

    /** 수동 출제로 편성한 문제 id 목록. 화면의 manualQuestionList 가 hidden input 으로 전송한다. */
    private List<Long> questionIds = new ArrayList<>();

    /**
     * 위 {@link #questionIds} 와 <b>같은 순서로 짝지어지는</b> 세트 번호.
     * 화면이 questionIds 하나당 questionSetNos 하나를 같이 보낸다.
     * 비어 있으면 전부 1번 세트로 본다 (세트 기능 이전 폼과의 하위호환).
     */
    private List<Integer> questionSetNos = new ArrayList<>();

    /** 자동 출제 규칙 목록. addRuleModal 이 hidden input(rules[i].xxx)으로 전송한다. */
    @Valid
    private List<ExamRuleForm> rules = new ArrayList<>();

    /* ===== 변환 ===== */

    public Exam.ExamType toExamType() {
        return switch (examType) {
            case "단위시험", "UNIT" -> Exam.ExamType.UNIT;
            case "중간고사", "MIDTERM" -> Exam.ExamType.MIDTERM;
            case "기말고사", "FINAL" -> Exam.ExamType.FINAL;
            default -> throw new IllegalArgumentException("알 수 없는 시험 유형: " + examType);
        };
    }

    public Exam.ExamStatus toStatus() {
        return switch (status == null ? "" : status) {
            case "예정", "SCHEDULED" -> Exam.ExamStatus.SCHEDULED;
            case "진행중", "OPEN" -> Exam.ExamStatus.OPEN;
            case "종료", "CLOSED" -> Exam.ExamStatus.CLOSED;
            case "보관", "ARCHIVED" -> Exam.ExamStatus.ARCHIVED;
            default -> Exam.ExamStatus.DRAFT;
        };
    }

    public static String statusLabel(Exam.ExamStatus status) {
        return switch (status) {
            case DRAFT -> "임시저장";
            case SCHEDULED -> "예정";
            case OPEN -> "진행중";
            case CLOSED -> "종료";
            case ARCHIVED -> "보관";
        };
    }

    public Exam.ResultRelease toResultRelease() {
        return switch (resultRelease == null ? "" : resultRelease) {
            case "채점 완료 후", "AFTER_GRADING" -> Exam.ResultRelease.AFTER_GRADING;
            case "지정 일시", "SCHEDULED" -> Exam.ResultRelease.SCHEDULED;
            case "비공개", "HIDDEN" -> Exam.ResultRelease.HIDDEN;
            default -> Exam.ResultRelease.IMMEDIATE;
        };
    }

    public static String resultReleaseLabel(Exam.ResultRelease mode) {
        if (mode == null) {
            return "즉시 공개";
        }
        return switch (mode) {
            case IMMEDIATE -> "즉시 공개";
            case AFTER_GRADING -> "채점 완료 후";
            case SCHEDULED -> "지정 일시";
            case HIDDEN -> "비공개";
        };
    }

    public static String typeLabel(Exam.ExamType type) {
        return switch (type) {
            case UNIT -> "단위시험";
            case MIDTERM -> "중간고사";
            case FINAL -> "기말고사";
        };
    }

    /** 비어 있는 규칙 행(모달을 열었다 닫은 흔적 등)은 버린다. */
    public List<ExamRuleForm> effectiveRules() {
        List<ExamRuleForm> result = new ArrayList<>();
        for (ExamRuleForm rule : rules) {
            if (rule != null && !rule.isEmptyRow()) {
                result.add(rule);
            }
        }
        return result;
    }

    /**
     * 수동 편성 문항 — (문제 id, 세트 번호) 짝.
     *
     * <p>중복 제거는 <b>짝 단위</b>로 한다. 같은 문제가 다른 세트에 들어가는 것은 정상이고
     * (문제은행이 모자랄 때 실제로 그렇게 된다), 같은 세트에 두 번 들어가는 것만 막아야 한다.
     * 세트 번호가 선언된 세트 수를 넘으면 1번으로 접는다 — 화면 조작으로 유령 세트가 생기면
     * 아무에게도 배정되지 않는 문항이 남기 때문.</p>
     */
    public List<QuestionSlot> effectiveQuestions() {
        int setCount = questionSetCount == null || questionSetCount < 1 ? 1 : questionSetCount;
        List<QuestionSlot> result = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            if (questionId == null) {
                continue;
            }
            Integer rawSetNo = i < questionSetNos.size() ? questionSetNos.get(i) : null;
            int setNo = (rawSetNo == null || rawSetNo < 1 || rawSetNo > setCount) ? 1 : rawSetNo;
            QuestionSlot slot = new QuestionSlot(questionId, setNo);
            if (!result.contains(slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    /** 수동 편성 문항 한 칸: 어느 세트의 어느 문제인지. */
    public record QuestionSlot(Long questionId, int setNo) {
    }

    /**
     * 엔티티 → 폼. 연관 엔티티는 id 만 꺼낸다 (LAZY 프록시를 화면에 넘기지 않기 위함).
     * 호출 측이 트랜잭션 안에서 부르는 것을 전제로 한다.
     */
    public static ExamForm from(Exam exam) {
        ExamForm form = new ExamForm();
        form.id = exam.getId();
        form.examName = exam.getExamName();
        form.examType = typeLabel(exam.getExamType());
        form.courseId = exam.getCourse() == null ? null : exam.getCourse().getId();
        form.subjectId = exam.getSubject() == null ? null : exam.getSubject().getId();
        form.sessionId = exam.getSession() == null ? null : exam.getSession().getId();
        form.instructorId = exam.getInstructor() == null ? null : exam.getInstructor().getId();
        form.timeLimitMin = exam.getTimeLimitMin();
        form.autoScore = exam.getAutoScore();
        form.manualScore = exam.getManualScore();
        form.totalScore = exam.getTotalScore();
        form.passScore = exam.getPassScore();
        form.randomOrder = exam.isRandomOrder();
        form.questionSetCount = exam.getQuestionSetCount();
        form.resultRelease = resultReleaseLabel(exam.getResultRelease());
        form.resultReleaseAt = exam.getResultReleaseAt();
        form.practiceMode = exam.isPracticeMode();
        form.retakeAllowed = exam.isRetakeAllowed();
        form.maxAttempts = exam.getMaxAttempts();
        form.windowStart = exam.getWindowStart();
        form.windowEnd = exam.getWindowEnd();
        form.requireIdentityVerification = exam.isRequireIdentityVerification();
        form.proctorEnabled = exam.isProctorEnabled();
        form.requireWebcam = exam.isRequireWebcam();
        form.blockTabSwitch = exam.isBlockTabSwitch();
        form.blockCopyPaste = exam.isBlockCopyPaste();
        form.note = exam.getNote();
        form.status = statusLabel(exam.getStatus());

        for (ExamQuestion eq : exam.getExamQuestions()) {
            // 규칙으로 확정된 문항은 수동 목록에 섞지 않는다 (확정 버튼으로 다시 뽑는다).
            if (!eq.isFromRule()) {
                form.questionIds.add(eq.getQuestion().getId());
                form.questionSetNos.add(eq.getSetNo() == null ? 1 : eq.getSetNo());
            }
        }
        for (ExamQuestionRule rule : exam.getQuestionRules()) {
            form.rules.add(ExamRuleForm.from(rule));
        }
        return form;
    }
}
