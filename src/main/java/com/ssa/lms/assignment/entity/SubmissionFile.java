package com.ssa.lms.assignment.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 과제 첨부파일.
 *
 * originalName 은 훈련생이 올린 원본 파일명이라 개인정보(이름 등)가 섞이는 일이 잦다.
 * 저장은 storedPath(무의미한 UUID 경로)로 하고 다운로드 시에만 원본명을 붙인다.
 */
@Entity
@Table(name = "submission_file", indexes = @Index(name = "idx_file_submission", columnList = "submission_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmissionFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    /** 실제 저장 경로. 웹 루트 밖이어야 한다. */
    @Column(name = "stored_path", length = 500, nullable = false)
    private String storedPath;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Builder
    public SubmissionFile(String originalName, String storedPath, Long sizeBytes, String contentType) {
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
    }

    void assignSubmission(Submission submission) {
        this.submission = submission;
    }
}
