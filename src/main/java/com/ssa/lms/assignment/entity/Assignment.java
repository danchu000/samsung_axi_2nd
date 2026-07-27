package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.exam.entity.Difficulty;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과제 정의 (재사용 가능한 원본).
 *
 * IA "데이터 정리" 2~3행이 "과제 정의"와 "과정에 배정된 과제"를 분리해뒀으므로 그대로 따른다.
 * 같은 과제를 여러 기수/과정에 배정할 때 정의를 복제하지 않기 위한 구조다.
 * 실제 기간·제출유형·마감은 CourseAssignment 에 있다.
 */
@Entity
@Table(name = "assignment", indexes = @Index(name = "idx_assignment_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 배정 시 기본으로 채워질 제출 유형. CourseAssignment 에서 덮어쓸 수 있다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_submission_type", length = 20, nullable = false)
    private CourseAssignment.SubmissionType defaultSubmissionType;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "category", length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AssignmentStatus status;

    @Builder
    public Assignment(String title, String description,
                      CourseAssignment.SubmissionType defaultSubmissionType, Integer maxScore,
                      String category, Difficulty difficulty, AssignmentStatus status) {
        this.title = title;
        this.description = description;
        this.defaultSubmissionType = defaultSubmissionType;
        this.maxScore = maxScore;
        this.category = category;
        this.difficulty = difficulty;
        this.status = status;
    }

    /** 화면 값: 활성화(Active) / 비활성화(Archived) */
    public enum AssignmentStatus {
        ACTIVE,
        ARCHIVED
    }
}
