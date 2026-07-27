package com.ssa.lms.notice.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지 첨부파일. 화면 notice-add.html 의 noticeAttachments / files 입력에 대응.
 */
@Entity
@Table(name = "notice_attachment", indexes = @Index(name = "idx_notice_attachment", columnList = "notice_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeAttachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "original_name", length = 255, nullable = false)
    private String originalName;

    @Column(name = "stored_path", length = 500, nullable = false)
    private String storedPath;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Builder
    public NoticeAttachment(String originalName, String storedPath, Long sizeBytes, String contentType) {
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
    }

    void assignNotice(Notice notice) {
        this.notice = notice;
    }
}
