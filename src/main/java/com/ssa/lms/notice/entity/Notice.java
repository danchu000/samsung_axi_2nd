package com.ssa.lms.notice.entity;

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
 * 공지사항.
 *
 * 매핑 근거
 *  - static/js/notices.js noticeData (id, category, title, author, date, editDate, views)
 *  - templates/admin/admin-07-notice/notice-add.html, notice-detail.html
 *
 * 내역서 요건 "훈련생 유의사항 등재(정보제공)" 의 근거 화면이다.
 */
@Entity
@Table(
        name = "notice",
        indexes = {
                @Index(name = "idx_notice_course", columnList = "course_id"),
                @Index(name = "idx_notice_published", columnList = "published_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private NoticeCategory category;

    /** null = 전체 공지, 값 존재 = 해당 과정 수강생에게만 노출. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    /** 상단 고정. */
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    /** 게시 시각. null 이면 미게시(임시저장). */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoticeAttachment> attachments = new ArrayList<>();

    @Builder
    public Notice(NoticeCategory category, Course course, String title, String content,
                  User author, boolean pinned, LocalDateTime publishedAt) {
        this.category = category;
        this.course = course;
        this.title = title;
        this.content = content;
        this.author = author;
        this.pinned = pinned;
        this.publishedAt = publishedAt;
        this.viewCount = 0;
    }

    public void addAttachment(NoticeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.assignNotice(this);
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void update(NoticeCategory category, String title, String content, boolean pinned) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }
}
