package com.ssa.lms.assignment.service;

import com.ssa.lms.assignment.dto.GradeHistoryRow;
import com.ssa.lms.assignment.dto.GradingForm;
import com.ssa.lms.assignment.dto.GradingStudentRow;
import com.ssa.lms.assignment.dto.GradingSummaryView;
import com.ssa.lms.assignment.dto.SubmissionDetailView;
import com.ssa.lms.assignment.dto.SubmissionTypes;
import com.ssa.lms.assignment.dto.UserOption;
import com.ssa.lms.assignment.entity.AssignmentCriteria;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.entity.Feedback;
import com.ssa.lms.assignment.entity.Submission;
import com.ssa.lms.assignment.entity.SubmissionFile;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.repository.FeedbackRepository;
import com.ssa.lms.assignment.repository.SubmissionFileRepository;
import com.ssa.lms.assignment.repository.SubmissionRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.entity.GradeHistory;
import com.ssa.lms.grading.repository.GradeHistoryRepository;
import com.ssa.lms.grading.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 과제 채점 서비스.
 *
 * <p><b>채점 결과는 반드시 {@code Grade} 에 반영한다</b> — evalType=ASSIGNMENT,
 * evalRefId=course_assignment.id 로 upsert. A의 이수 판정은 {@code GradeQueryService} 를
 * 통해 이 값만 읽기 때문에, Grade 를 안 쓰면 채점해도 이수 판정에 잡히지 않는다.</p>
 *
 * <p><b>성적 정정 시 {@code GradeHistory} 를 사유와 함께 반드시 남긴다.</b>
 * 내역서 증빙 요건이고 화면에도 "변경 이력" 탭이 이미 있다. 최초 채점은 정정이 아니라
 * 이력을 남기지 않는다 (before 가 없다).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentGradingService {

    private static final DateTimeFormatter SUBMIT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;
    private final FeedbackRepository feedbackRepository;
    private final GradeRepository gradeRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final AssignmentLookupRepository lookupRepository;
    private final CourseAssignmentService courseAssignmentService;
    private final CourseQueryService courseQueryService;

    /** 채점 화면 상단 요약. */
    public GradingSummaryView loadSummary(Long courseAssignmentId) {
        CourseAssignment ca = courseAssignmentService.getOrThrow(courseAssignmentId);
        List<Long> enrolled = courseQueryService.findUserIdsByCourseId(ca.getCourse().getId());
        List<Long> submittedUserIds = submissionRepository.findSubmittedUserIds(courseAssignmentId);
        long graded = gradeRepository
                .findByCourseIdAndEvalTypeAndEvalRefId(
                        ca.getCourse().getId(), Grade.EvalType.ASSIGNMENT, courseAssignmentId)
                .stream()
                .filter(g -> g.getStatus() == Grade.GradeStatus.GRADED
                        || g.getStatus() == Grade.GradeStatus.CONFIRMED)
                .count();
        return GradingSummaryView.of(ca, enrolled.size(), submittedUserIds.size(), graded);
    }

    /**
     * 채점 화면 학생 목록.
     *
     * 수강생 전원이 행으로 나온다 — 미제출자를 별도 화면이 아니라 이 표에서 확인하기 때문이다.
     * 수강생 명단은 A의 {@link CourseQueryService} 계약으로만 가져온다.
     *
     * @param gradingUrlPrefix 채점 팝업 URL 앞부분 (관리자/강사가 다르다)
     */
    public List<GradingStudentRow> loadStudents(Long courseAssignmentId, String gradingUrlPrefix) {
        CourseAssignment ca = courseAssignmentService.getOrThrow(courseAssignmentId);

        List<Long> enrolled = courseQueryService.findUserIdsByCourseId(ca.getCourse().getId());
        Map<Long, UserOption> users = lookupRepository.findUserOptions(enrolled);

        // (user → 최신 제출물) 로 접는다. 재제출은 새 행으로 쌓이므로 마지막 회차가 현재 제출물이다.
        Map<Long, Submission> latestByUser = new LinkedHashMap<>();
        for (Submission s : submissionRepository.findAllWithUserByCourseAssignmentId(courseAssignmentId)) {
            latestByUser.put(s.getUser().getId(), s);
        }

        Map<Long, Grade> gradeByUser = new HashMap<>();
        for (Grade g : gradeRepository.findByCourseIdAndEvalTypeAndEvalRefId(
                ca.getCourse().getId(), Grade.EvalType.ASSIGNMENT, courseAssignmentId)) {
            gradeByUser.put(g.getUser().getId(), g);
        }

        String courseLabel = ca.getCourse().getCourseName();
        String assignmentLabel = ca.getAssignment().getTitle();

        List<GradingStudentRow> rows = new ArrayList<>();
        int no = 0;
        for (Long userId : enrolled) {
            UserOption user = users.get(userId);
            if (user == null) {
                continue;
            }
            Submission latest = latestByUser.get(userId);
            Grade grade = gradeByUser.get(userId);
            no++;

            List<GradeHistoryRow> history = grade == null
                    ? List.of()
                    : gradeHistoryRepository.findByGradeId(grade.getId())
                    .stream().map(GradeHistoryRow::of).toList();

            rows.add(new GradingStudentRow(
                    no,
                    userId,
                    latest == null ? null : latest.getId(),
                    user.name(),
                    user.loginId(),
                    latest == null ? "미제출" : "제출",
                    gradingStatusLabel(latest, grade),
                    grade == null || grade.getTotalScore() == null ? "-" : String.valueOf(grade.getTotalScore()),
                    (latest == null ? 0 : latest.getAttemptNo() - 1) + "회",
                    buttonTypeOf(latest, grade),
                    courseLabel,
                    assignmentLabel,
                    latest == null || latest.getSubmittedAt() == null
                            ? "-" : latest.getSubmittedAt().format(SUBMIT_FMT),
                    latest != null && latest.isLate(),
                    latest == null ? "" : gradingUrlPrefix + courseAssignmentId
                            + "/submissions/" + latest.getId() + "/grading",
                    history));
        }
        return rows;
    }

    /** 채점 팝업 상세. */
    public SubmissionDetailView loadSubmissionDetail(Long submissionId) {
        Submission s = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("제출물을 찾을 수 없습니다. id=" + submissionId));
        CourseAssignment ca = s.getCourseAssignment();

        Grade grade = gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(
                s.getUser().getId(), Grade.EvalType.ASSIGNMENT, ca.getId()).orElse(null);

        String feedback = null;
        String memo = null;
        for (Feedback f : feedbackRepository.findBySubmissionId(submissionId)) {
            // append-only 라 같은 종류의 마지막 행이 현재 값이다.
            if (f.isVisibleToTrainee()) {
                feedback = f.getContent();
            } else {
                memo = f.getContent();
            }
        }

        List<SubmissionDetailView.CriteriaRow> criteria = ca.getCriteria().stream()
                .map(c -> new SubmissionDetailView.CriteriaRow(c.getSeq(), c.getContent(), c.getScore()))
                .toList();

        List<SubmissionDetailView.FileRow> files = submissionFileRepository
                .findBySubmissionId(submissionId).stream()
                .map(f -> new SubmissionDetailView.FileRow(
                        f.getId(), f.getOriginalName(), f.getSizeBytes(),
                        "/admin/evaluation/assignments/files/" + f.getId()))
                .toList();

        Integer currentScore = grade == null ? null : grade.getTotalScore();
        return new SubmissionDetailView(
                s.getId(),
                ca.getId(),
                s.getUser().getName(),
                ca.getAssignment().getTitle(),
                ca.getCourse().getCourseName(),
                ca.displayDescription(),
                criteria,
                SubmissionTypes.label(s.getSubmitType()),
                s.getContentText(),
                s.getLinkUrl(),
                files,
                s.getAttemptNo(),
                s.isLate(),
                s.getSubmittedAt() == null ? "-" : s.getSubmittedAt().format(SUBMIT_FMT),
                ca.getScore(),
                ca.effectivePassScore(),
                currentScore,
                feedback,
                memo,
                currentScore != null
        );
    }

    /**
     * 채점 저장.
     *
     * <ol>
     *   <li>{@code Grade} upsert (evalType=ASSIGNMENT, evalRefId=course_assignment.id)</li>
     *   <li>점수가 바뀌는 정정이면 {@code GradeHistory} 를 사유와 함께 남긴다</li>
     *   <li>피드백/내부 메모는 append-only 로 새 행을 쌓는다</li>
     *   <li>제출물 상태를 GRADED / CONFIRMED 로 올린다</li>
     * </ol>
     */
    @Transactional
    public void grade(Long submissionId, GradingForm form, Long graderId) {
        Submission s = submissionRepository.findDetailById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("제출물을 찾을 수 없습니다. id=" + submissionId));
        CourseAssignment ca = s.getCourseAssignment();

        if (form.getScore() > ca.getScore()) {
            throw new IllegalArgumentException(
                    "점수가 배점(" + ca.getScore() + "점)을 넘습니다.");
        }

        Long userId = s.getUser().getId();
        Grade grade = gradeRepository
                .findByUserIdAndEvalTypeAndEvalRefId(userId, Grade.EvalType.ASSIGNMENT, ca.getId())
                .orElseGet(() -> gradeRepository.save(Grade.builder()
                        .user(s.getUser())
                        .course(ca.getCourse())
                        .evalType(Grade.EvalType.ASSIGNMENT)
                        .evalRefId(ca.getId())
                        .status(Grade.GradeStatus.UNGRADED)
                        .build()));

        Integer beforeScore = grade.getTotalScore();
        Boolean beforePassed = grade.getPassed();
        boolean isCorrection = beforeScore != null;

        if (isCorrection && !beforeScore.equals(form.getScore())
                && (form.getReason() == null || form.getReason().isBlank())) {
            // 내역서 증빙 요건: 정정은 사유 없이 남을 수 없다.
            throw new IllegalArgumentException("이미 채점된 성적을 변경하려면 사유를 입력해야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        var grader = lookupRepository.userRef(graderId);
        grade.applyScore(null, form.getScore(), ca.effectivePassScore(), grader, now);

        if (isCorrection && !beforeScore.equals(form.getScore())) {
            gradeHistoryRepository.save(GradeHistory.builder()
                    .grade(grade)
                    .beforeScore(beforeScore)
                    .afterScore(grade.getTotalScore())
                    .beforePassed(beforePassed)
                    .afterPassed(grade.getPassed())
                    .reason(form.getReason().strip())
                    .changedBy(grader)
                    .changedAt(now)
                    .build());
        }

        if (form.isConfirm()) {
            grade.confirm(grader, now);
            s.changeStatus(Submission.SubmissionStatus.CONFIRMED);
        } else {
            s.changeStatus(Submission.SubmissionStatus.GRADED);
        }

        appendFeedback(s, grader, form.getFeedback(), true);
        appendFeedback(s, grader, form.getMemo(), false);
    }

    /* ===== 내부 ===== */

    /** 피드백은 덮어쓰지 않는다. 값이 실제로 바뀐 경우에만 새 행을 쌓는다. */
    private void appendFeedback(Submission s, com.ssa.lms.user.entity.User instructor,
                                String content, boolean visibleToTrainee) {
        if (content == null || content.isBlank()) {
            return;
        }
        String trimmed = content.strip();
        String latest = null;
        for (Feedback f : feedbackRepository.findBySubmissionId(s.getId())) {
            if (f.isVisibleToTrainee() == visibleToTrainee) {
                latest = f.getContent();
            }
        }
        if (trimmed.equals(latest)) {
            return;
        }
        s.addFeedback(Feedback.builder()
                .instructor(instructor)
                .content(trimmed)
                .visibleToTrainee(visibleToTrainee)
                .build());
    }

    private static String gradingStatusLabel(Submission latest, Grade grade) {
        if (latest == null) {
            return "-";
        }
        if (grade == null) {
            return "미채점";
        }
        return switch (grade.getStatus()) {
            case UNGRADED -> "미채점";
            case GRADING -> "채점중";
            case GRADED -> "채점완료";
            case CONFIRMED -> "확정";
        };
    }

    /** 화면 버튼: 미제출이면 비활성, 이미 채점됐으면 "수정", 아니면 "채점". */
    private static String buttonTypeOf(Submission latest, Grade grade) {
        if (latest == null) {
            return "disabled";
        }
        boolean alreadyGraded = grade != null
                && (grade.getStatus() == Grade.GradeStatus.GRADED
                || grade.getStatus() == Grade.GradeStatus.CONFIRMED);
        return alreadyGraded ? "edit" : "grading";
    }

    /** 채점 기준 배점 합계 — 화면 안내용. */
    public static int criteriaTotal(List<AssignmentCriteria> criteria) {
        return criteria.stream().mapToInt(AssignmentCriteria::getScore).sum();
    }

    /** 첨부파일 다운로드 시 권한 판정에 쓴다. */
    public boolean isOwner(SubmissionFile file, Long userId) {
        return file.getSubmission().getUser().getId().equals(userId);
    }
}
