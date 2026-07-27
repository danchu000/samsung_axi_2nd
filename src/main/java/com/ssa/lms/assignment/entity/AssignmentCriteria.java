package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과제 채점 기준 한 줄.
 * 화면 admin-evaluation-assignment-add.html 의 criteria[] 반복 입력에 대응.
 */
@Entity
@Table(
        name = "assignment_criteria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_criteria_seq", columnNames = {"course_assignment_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentCriteria extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_assignment_id", nullable = false)
    private CourseAssignment courseAssignment;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    /** 이 기준의 배점. 합계가 CourseAssignment.score 와 같아야 한다(서비스에서 검증). */
    @Column(name = "score", nullable = false)
    private Integer score;

    @Builder
    public AssignmentCriteria(Integer seq, String content, Integer score) {
        this.seq = seq;
        this.content = content;
        this.score = score;
    }

    void assignCourseAssignment(CourseAssignment courseAssignment) {
        this.courseAssignment = courseAssignment;
    }
}
