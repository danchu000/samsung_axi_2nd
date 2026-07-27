package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과제 제출물에 대한 강사 피드백.
 *
 * 권한정의서(1) 29행: 피드백 작성은 관리자 O / 강사 O / 훈련생 X.
 * 수정 시 덮어쓰지 않고 새 행을 쌓는다(append-only). 훈련생이 이미 읽은 피드백이
 * 조용히 바뀌면 분쟁 소지가 있고, 내역서의 "고객 의견 처리" 증빙에도 이력이 필요하다.
 */
@Entity
@Table(name = "feedback", indexes = @Index(name = "idx_feedback_submission", columnList = "submission_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    public Feedback(User instructor, String content) {
        this.instructor = instructor;
        this.content = content;
    }

    void assignSubmission(Submission submission) {
        this.submission = submission;
    }
}
