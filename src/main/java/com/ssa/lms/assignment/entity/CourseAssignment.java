package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
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
 * 과정에 배정된 과제.
 *
 * 매핑 근거
 *  - IA "데이터 정리" 3행 (course_assignment_id ~ updated_at) 컬럼명 그대로
 *  - admin-04-evaluation/admin-evaluation-assignment-add.html
 *    (courseSelect, startDate, endDate, allowLate, allowResubmit, autoGrading, grader,
 *     score, criteria[])
 *
 * 훈련생이 실제로 보는/제출하는 단위는 항상 이 엔티티다. Assignment 는 원본 정의일 뿐이다.
 */
@Entity
@Table(
        name = "course_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_assignment", columnNames = {"course_id", "assignment_id"}),
        indexes = {
                @Index(name = "idx_course_assignment_course", columnList = "course_id"),
                @Index(name = "idx_course_assignment_period", columnList = "start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** 이 과정에서만 다르게 안내할 과제 설명. null 이면 Assignment.description 사용. */
    @Lob
    @Column(name = "override_description")
    private String overrideDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", length = 20, nullable = false)
    private SubmissionType submissionType;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** 마감 후 지각 제출 허용 여부. 화면 allowLate. */
    @Column(name = "allow_late", nullable = false)
    private boolean allowLate;

    /** 재제출 허용 여부. 화면 allowResubmit. */
    @Column(name = "allow_resubmit", nullable = false)
    private boolean allowResubmit;

    /** 최대 재제출 횟수. allowResubmit=false 이면 0. */
    @Column(name = "max_resubmit", nullable = false)
    private Integer maxResubmit;

    /** 자동 채점 사용 여부. 화면 autoGrading. */
    @Column(name = "auto_grading", nullable = false)
    private boolean autoGrading;

    /** 이 과정에서 이 과제의 배점. 화면 score. */
    @Column(name = "score", nullable = false)
    private Integer score;

    /** 지정 채점자. 화면 grader 셀렉트. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grader_id")
    private User grader;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CourseAssignmentStatus status;

    @OneToMany(mappedBy = "courseAssignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<AssignmentCriteria> criteria = new ArrayList<>();

    @Builder
    public CourseAssignment(Course course, Assignment assignment, String overrideDescription,
                            SubmissionType submissionType, LocalDateTime startAt, LocalDateTime endAt,
                            boolean allowLate, boolean allowResubmit, Integer maxResubmit,
                            boolean autoGrading, Integer score, User grader,
                            CourseAssignmentStatus status) {
        this.course = course;
        this.assignment = assignment;
        this.overrideDescription = overrideDescription;
        this.submissionType = submissionType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allowLate = allowLate;
        this.allowResubmit = allowResubmit;
        this.maxResubmit = maxResubmit;
        this.autoGrading = autoGrading;
        this.score = score;
        this.grader = grader;
        this.status = status;
    }

    public void addCriteria(AssignmentCriteria item) {
        this.criteria.add(item);
        item.assignCourseAssignment(this);
    }

    /** 지금 제출 가능한지. 지각 허용이면 마감 이후에도 true. */
    public boolean canSubmitAt(LocalDateTime at) {
        if (at.isBefore(startAt)) {
            return false;
        }
        return allowLate || !at.isAfter(endAt);
    }

    /** IA "데이터 정리" 6~7행의 제출 유형 코드 값과 동일. */
    public enum SubmissionType {
        /** 문서 제출 */
        DOCUMENT,
        /** 링크 제출 */
        LINK,
        /** 텍스트 입력 */
        TEXT,
        /** 문서 제출 + 텍스트 입력 */
        DOCUMENT_TEXT
    }

    public enum CourseAssignmentStatus {
        /** 아직 훈련생에게 공개 전. */
        DRAFT,
        /** 공개, 제출 기간 전. */
        SCHEDULED,
        /** 제출 진행 중. */
        OPEN,
        /** 마감. 화면의 pending/completed 는 채점 진행률에서 파생한다. */
        CLOSED
    }
}
