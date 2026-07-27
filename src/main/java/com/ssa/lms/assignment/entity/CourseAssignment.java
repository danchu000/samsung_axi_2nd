package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
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
/*
 * uk_course_assignment(course_id, assignment_id) 유니크 제약을 뺐다.
 *
 * soft delete(@SQLDelete) 와 정면으로 충돌하기 때문이다 — 배정을 삭제하면 행이 물리적으로
 * 남아 있어(3년 보존 요건), 같은 과정에 같은 과제를 다시 배정할 때 DB 제약에 걸려
 * 500 이 났다. 실제로 재현됨.
 *
 * 부분 유니크 인덱스(where is_deleted = false)는 MySQL 이 지원하지 않아 쓸 수 없다.
 * 대신 중복 검사는 CourseAssignmentService.create() 가
 * existsByCourseIdAndAssignmentId 로 한다 (@SQLRestriction 때문에 삭제된 행은 제외된다).
 */
@Table(
        name = "course_assignment",
        indexes = {
                @Index(name = "idx_course_assignment_course", columnList = "course_id"),
                // 유니크 제약을 뺀 자리 — 중복 배정 검사(existsByCourseIdAndAssignmentId)가 탄다.
                @Index(name = "idx_course_assignment_pair", columnList = "course_id, assignment_id"),
                @Index(name = "idx_course_assignment_period", columnList = "start_at, end_at")
        }
)
@SQLDelete(sql = "UPDATE course_assignment SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("is_deleted = false")
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
    @Column(name = "override_description", columnDefinition = "TEXT")
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

    /**
     * 합격 기준 점수. assignment-grading.html 상단 "합격 기준: 70점 이상" 과
     * {@code Grade.applyScore(..., passScore, ...)} 의 합격 판정 입력이다.
     * null 이면 배점의 60% 를 기준으로 본다.
     */
    @Column(name = "pass_score")
    private Integer passScore;

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
                            boolean autoGrading, Integer score, Integer passScore, User grader,
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
        this.passScore = passScore;
        this.grader = grader;
        this.status = status;
    }

    public void addCriteria(AssignmentCriteria item) {
        this.criteria.add(item);
        item.assignCourseAssignment(this);
    }

    /**
     * 채점 기준을 통째로 교체하기 위한 비우기.
     * 호출 후 반드시 flush() 한 뒤에 addCriteria() 를 해야 한다 —
     * 그러지 않으면 Hibernate 가 orphan DELETE 보다 INSERT 를 먼저 내보내
     * uk_criteria_seq(course_assignment_id, seq) 유니크 제약에 걸린다.
     * (QuestionService.update() 에서 이미 밟은 지뢰)
     */
    public void clearCriteria() {
        this.criteria.clear();
    }

    /** 운영 설정 수정. 과정·정의(assignment)는 바꾸지 않는다 (제출물이 매달려 있다). */
    public void update(String overrideDescription, SubmissionType submissionType,
                       LocalDateTime startAt, LocalDateTime endAt,
                       boolean allowLate, boolean allowResubmit, Integer maxResubmit,
                       boolean autoGrading, Integer score, Integer passScore, User grader,
                       CourseAssignmentStatus status) {
        this.overrideDescription = overrideDescription;
        this.submissionType = submissionType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allowLate = allowLate;
        this.allowResubmit = allowResubmit;
        this.maxResubmit = maxResubmit;
        this.autoGrading = autoGrading;
        this.score = score;
        this.passScore = passScore;
        this.grader = grader;
        this.status = status;
    }

    public void changeStatus(CourseAssignmentStatus status) {
        this.status = status;
    }

    /** 합격 기준 점수. 별도 설정이 없으면 배점의 60%. */
    public int effectivePassScore() {
        if (passScore != null) {
            return passScore;
        }
        return (int) Math.ceil(score * 0.6);
    }

    /** 훈련생에게 보여줄 설명 — 과정별 덮어쓰기가 있으면 그것을 우선한다. */
    public String displayDescription() {
        return (overrideDescription != null && !overrideDescription.isBlank())
                ? overrideDescription
                : assignment.getDescription();
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
