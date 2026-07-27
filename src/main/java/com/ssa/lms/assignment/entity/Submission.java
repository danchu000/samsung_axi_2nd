package com.ssa.lms.assignment.entity;

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
 * 과제 제출물.
 *
 * 매핑 근거: static/js/assignments-grading.js gradingStudentData
 *  (submitStatus, gradingStatus, score, resubmit 횟수, submitDate)
 *
 * 재제출은 같은 행을 덮어쓰지 않고 attemptNo 를 올린 새 행으로 쌓는다.
 * 이전 제출물도 3년 보존 대상이고, 채점 이력 추적이 필요하기 때문이다.
 */
@Entity
@Table(
        name = "submission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submission_attempt",
                columnNames = {"course_assignment_id", "user_id", "attempt_no"}),
        indexes = {
                @Index(name = "idx_submission_assignment", columnList = "course_assignment_id, status"),
                @Index(name = "idx_submission_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_assignment_id", nullable = false)
    private CourseAssignment courseAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 1부터 시작. 2 이상이면 재제출. 화면의 "재제출 n회". */
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "submit_type", length = 20, nullable = false)
    private CourseAssignment.SubmissionType submitType;

    /** 텍스트 입력형 본문. */
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    /** 링크 제출형 URL. */
    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** 마감 이후 제출 여부. 채점 시 감점 근거가 되므로 제출 시점에 확정 저장한다. */
    @Column(name = "is_late", nullable = false)
    private boolean late;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SubmissionStatus status;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Feedback> feedbacks = new ArrayList<>();

    @Builder
    public Submission(CourseAssignment courseAssignment, User user, Integer attemptNo,
                      CourseAssignment.SubmissionType submitType, String contentText, String linkUrl,
                      LocalDateTime submittedAt, boolean late, SubmissionStatus status) {
        this.courseAssignment = courseAssignment;
        this.user = user;
        this.attemptNo = attemptNo;
        this.submitType = submitType;
        this.contentText = contentText;
        this.linkUrl = linkUrl;
        this.submittedAt = submittedAt;
        this.late = late;
        this.status = status;
    }

    public void addFile(SubmissionFile file) {
        this.files.add(file);
        file.assignSubmission(this);
    }

    public void addFeedback(Feedback feedback) {
        this.feedbacks.add(feedback);
        feedback.assignSubmission(this);
    }

    public void changeStatus(SubmissionStatus status) {
        this.status = status;
    }

    /** 화면 값: 미제출 / 제출 / 채점중 / 채점완료 / 확정 */
    public enum SubmissionStatus {
        NOT_SUBMITTED,
        SUBMITTED,
        GRADING,
        GRADED,
        CONFIRMED
    }
}
