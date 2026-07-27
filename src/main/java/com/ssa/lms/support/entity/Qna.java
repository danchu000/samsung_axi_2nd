package com.ssa.lms.support.entity;

import com.ssa.lms.common.converter.CryptoConverter;
import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
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
 * Q&A 질문.
 *
 * 매핑 근거
 *  - static/js/tutoring.js responseData (type, category, status, course, requester, manager, elapsed)
 *  - static/js/trainee/qna.js 질문하기 모달 (aTitle, aBody)
 *  - templates/admin/admin-06-support/admin-support-qna.html, admin-support-response.html
 *
 * 내역서 "고객 의견 수렴·불만 처리" 증빙의 근거 데이터다.
 *
 * 개인정보 주의: 질문 본문에 연락처·상황 설명이 섞여 들어오는 경우가 많아
 * content 에 AES-256 컨버터(A 의 CryptoConverter)를 건다. 암호문이 저장되므로
 * 이 컬럼으로는 LIKE 검색이 불가능하다 — 검색은 title 로만 한다.
 */
@Entity
@Table(
        name = "qna",
        indexes = {
                @Index(name = "idx_qna_course_status", columnList = "course_id, status"),
                @Index(name = "idx_qna_assignee", columnList = "assignee_id"),
                @Index(name = "idx_qna_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Qna extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 질문자(훈련생). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /** 연계 차시. 화면의 "AI 분석 / 3차시" 표기. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    /** 화면 값: 수업 / 진도 / 기타 */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private QnaCategory category;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QnaStatus status;

    /** 담당자. null 이면 화면의 "미배정". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * 최초 응답 시각. 화면의 경과시간("26h")은 answeredAt 이 아니라
     * (firstResponseAt ?: now) - createdAt 으로 계산한다.
     */
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "qna", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QnaAnswer> answers = new ArrayList<>();

    @Builder
    public Qna(User user, Course course, Session session, QnaCategory category,
               String title, String content, QnaStatus status) {
        this.user = user;
        this.course = course;
        this.session = session;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void addAnswer(QnaAnswer answer, LocalDateTime at) {
        this.answers.add(answer);
        answer.assignQna(this);
        if (this.firstResponseAt == null) {
            this.firstResponseAt = at;
        }
        this.status = QnaStatus.ANSWERED;
    }

    public void assign(User assignee) {
        this.assignee = assignee;
        if (this.status == QnaStatus.WAITING) {
            this.status = QnaStatus.IN_PROGRESS;
        }
    }

    public void close(LocalDateTime at) {
        this.closedAt = at;
        this.status = QnaStatus.CLOSED;
    }

    public enum QnaCategory {
        /** 수업 */
        LECTURE,
        /** 진도 */
        PROGRESS,
        /** 기타 */
        ETC
    }

    /** 화면 값: 미답변 / 진행중 / 답변완료 / 종료 */
    public enum QnaStatus {
        WAITING,
        IN_PROGRESS,
        ANSWERED,
        CLOSED
    }
}
