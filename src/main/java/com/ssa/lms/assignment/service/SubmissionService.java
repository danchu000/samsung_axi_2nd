package com.ssa.lms.assignment.service;

import com.ssa.lms.assignment.dto.SubmissionForm;
import com.ssa.lms.assignment.dto.SubmissionTypes;
import com.ssa.lms.assignment.dto.TraineeAssignmentCard;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.assignment.entity.Feedback;
import com.ssa.lms.assignment.entity.Submission;
import com.ssa.lms.assignment.entity.SubmissionFile;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.repository.CourseAssignmentRepository;
import com.ssa.lms.assignment.repository.FeedbackRepository;
import com.ssa.lms.assignment.repository.SubmissionFileRepository;
import com.ssa.lms.assignment.repository.SubmissionRepository;
import com.ssa.lms.grading.entity.Grade;
import com.ssa.lms.grading.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 훈련생 과제 제출 서비스.
 *
 * <p><b>재제출은 덮어쓰지 않는다.</b> attemptNo 를 올린 새 행으로 쌓는다.
 * 이전 제출물도 3년 보존 대상이고 채점 이력 추적이 필요하기 때문이다.
 * 유니크 제약 uk_submission_attempt(course_assignment_id, user_id, attempt_no) 가 이를 강제한다.</p>
 *
 * <p><b>isLate 는 제출 시점에 확정 저장한다.</b> 나중에 계산하면 마감일을 뒤로 미룬 순간
 * 과거 지각 제출이 정상 제출로 둔갑한다. 감점 근거는 제출 당시 사실로 남아야 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SubmissionRepository submissionRepository;
    private final SubmissionFileRepository submissionFileRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final FeedbackRepository feedbackRepository;
    private final GradeRepository gradeRepository;
    private final AssignmentLookupRepository lookupRepository;
    private final SubmissionFileStorage fileStorage;

    /**
     * 제출 / 재제출.
     *
     * @return 새로 만들어진 제출물 id
     */
    @Transactional
    public Long submit(Long userId, SubmissionForm form) {
        CourseAssignment ca = courseAssignmentRepository.findDetailById(form.getCourseAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "배정된 과제를 찾을 수 없습니다. id=" + form.getCourseAssignmentId()));

        LocalDateTime now = LocalDateTime.now();
        if (!ca.canSubmitAt(now)) {
            throw new IllegalStateException("지금은 제출할 수 없는 과제입니다. (기간 종료 또는 시작 전)");
        }
        if (ca.getStatus() == CourseAssignment.CourseAssignmentStatus.DRAFT) {
            throw new IllegalStateException("아직 공개되지 않은 과제입니다.");
        }

        long previous = submissionRepository.countByCourseAssignmentIdAndUserId(ca.getId(), userId);
        if (previous > 0) {
            if (!ca.isAllowResubmit()) {
                throw new IllegalStateException("재제출이 허용되지 않은 과제입니다.");
            }
            // maxResubmit 은 "최초 제출 이후 다시 낼 수 있는 횟수" 다.
            if (previous > ca.getMaxResubmit()) {
                throw new IllegalStateException(
                        "재제출 한도(" + ca.getMaxResubmit() + "회)를 초과했습니다.");
            }
        }

        validatePayload(ca.getSubmissionType(), form);

        Submission submission = Submission.builder()
                .courseAssignment(ca)
                .user(lookupRepository.userRef(userId))
                .attemptNo((int) previous + 1)
                .submitType(ca.getSubmissionType())
                .contentText(blankToNull(form.getContentText()))
                .linkUrl(blankToNull(form.getLinkUrl()))
                .submittedAt(now)
                // 마감 이후 제출인지를 "지금" 확정한다. 나중에 계산하지 않는다.
                .late(now.isAfter(ca.getEndAt()))
                .status(Submission.SubmissionStatus.SUBMITTED)
                .build();

        if (SubmissionTypes.requiresFile(ca.getSubmissionType())) {
            for (MultipartFile file : form.getFiles()) {
                SubmissionFile stored = fileStorage.store(file);
                if (stored != null) {
                    submission.addFile(stored);
                }
            }
        }
        return submissionRepository.save(submission).getId();
    }

    /** 훈련생 과제 카드 목록 — 수강 중인 과정에 배정된, 공개된 과제 전부. */
    public List<TraineeAssignmentCard> findMyAssignments(Long userId) {
        List<Long> courseIds = lookupRepository.findEnrolledCourseIds(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        List<CourseAssignment> assignments =
                courseAssignmentRepository.findVisibleByCourseIds(courseIds);
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> caIds = assignments.stream().map(CourseAssignment::getId).toList();
        Map<Long, Submission> latestByCa = new HashMap<>();
        for (Submission s : submissionRepository.findLatestByUserAndCourseAssignments(userId, caIds)) {
            latestByCa.put(s.getCourseAssignment().getId(), s);
        }
        Map<Long, Grade> gradeByCa = new HashMap<>();
        for (Long caId : caIds) {
            gradeRepository.findByUserIdAndEvalTypeAndEvalRefId(userId, Grade.EvalType.ASSIGNMENT, caId)
                    .ifPresent(g -> gradeByCa.put(caId, g));
        }

        LocalDateTime now = LocalDateTime.now();
        List<TraineeAssignmentCard> cards = new ArrayList<>();
        for (CourseAssignment ca : assignments) {
            Submission latest = latestByCa.get(ca.getId());
            Grade grade = gradeByCa.get(ca.getId());
            cards.add(toCard(ca, latest, grade, now, userId));
        }
        return cards;
    }

    public Optional<Submission> findLatest(Long courseAssignmentId, Long userId) {
        return submissionRepository
                .findTopByCourseAssignmentIdAndUserIdOrderByAttemptNoDesc(courseAssignmentId, userId);
    }

    public SubmissionFile getFileOrThrow(Long fileId) {
        return submissionFileRepository.findDetailById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("첨부파일을 찾을 수 없습니다. id=" + fileId));
    }

    public SubmissionFileStorage storage() {
        return fileStorage;
    }

    /* ===== 내부 ===== */

    private TraineeAssignmentCard toCard(CourseAssignment ca, Submission latest, Grade grade,
                                         LocalDateTime now, Long userId) {
        int attempts = latest == null ? 0 : latest.getAttemptNo();
        boolean submitted = latest != null;

        String status;
        if (submitted) {
            status = "SUBMITTED";
        } else if (ca.canSubmitAt(now)) {
            status = "ONGOING";
        } else {
            status = "LATE";
        }

        boolean canSubmit = ca.canSubmitAt(now)
                && ca.getStatus() != CourseAssignment.CourseAssignmentStatus.DRAFT
                && (!submitted || (ca.isAllowResubmit() && attempts <= ca.getMaxResubmit()));

        boolean graded = grade != null
                && (grade.getStatus() == Grade.GradeStatus.GRADED
                || grade.getStatus() == Grade.GradeStatus.CONFIRMED);

        String feedback = null;
        if (submitted) {
            List<Feedback> visible = feedbackRepository.findVisibleBySubmissionId(latest.getId());
            if (!visible.isEmpty()) {
                // 피드백은 append-only 라 마지막 행이 현재 피드백이다.
                feedback = visible.get(visible.size() - 1).getContent();
            }
        }

        return new TraineeAssignmentCard(
                String.valueOf(ca.getId()),
                ca.getAssignment().getTitle(),
                ca.getCourse().getCourseName(),
                ca.getCourse().getCohort() == null ? "-" : ca.getCourse().getCohort(),
                status,
                ca.getEndAt().format(DEADLINE_FMT),
                SubmissionTypes.toScreenCode(ca.getSubmissionType()),
                ca.isAllowResubmit(),
                ca.displayDescription(),
                graded,
                feedback,
                submitted ? answerOf(latest) : null,
                graded ? grade.getTotalScore() + " / " + ca.getScore() : null,
                canSubmit,
                attempts,
                submitted && latest.isLate()
        );
    }

    /** 결과 모달의 "내 답안" — 텍스트/링크/첨부명을 사람이 읽을 형태로 합친다. */
    private String answerOf(Submission s) {
        StringBuilder sb = new StringBuilder();
        if (s.getContentText() != null && !s.getContentText().isBlank()) {
            sb.append(s.getContentText());
        }
        if (s.getLinkUrl() != null && !s.getLinkUrl().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(s.getLinkUrl());
        }
        List<SubmissionFile> files = submissionFileRepository.findBySubmissionId(s.getId());
        if (!files.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("첨부: ");
            sb.append(String.join(", ", files.stream().map(SubmissionFile::getOriginalName).toList()));
        }
        return sb.isEmpty() ? "-" : sb.toString();
    }

    /**
     * 유형별 필수값 검증.
     * 화면 JS 도 같은 검증을 하지만 그것만 믿으면 안 된다 — POST 는 직접 호출될 수 있다.
     */
    private void validatePayload(CourseAssignment.SubmissionType type, SubmissionForm form) {
        boolean hasFile = form.getFiles().stream().anyMatch(f -> f != null && !f.isEmpty());
        boolean hasText = form.getContentText() != null && !form.getContentText().isBlank();
        boolean hasLink = form.getLinkUrl() != null && !form.getLinkUrl().isBlank();

        if (SubmissionTypes.requiresFile(type) && !hasFile) {
            throw new IllegalArgumentException("제출할 파일을 첨부하세요.");
        }
        if (SubmissionTypes.requiresText(type) && !hasText) {
            throw new IllegalArgumentException("텍스트 내용을 입력하세요.");
        }
        if (SubmissionTypes.requiresLink(type) && !hasLink) {
            throw new IllegalArgumentException("링크를 입력하세요.");
        }
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
