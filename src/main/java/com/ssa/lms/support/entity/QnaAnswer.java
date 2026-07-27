package com.ssa.lms.support.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Q&A 답변. 한 질문에 여러 답변(추가 설명, 재질문에 대한 응답)이 달릴 수 있다.
 */
@Entity
@Table(name = "qna_answer", indexes = @Index(name = "idx_qna_answer_qna", columnList = "qna_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qna_id", nullable = false)
    private Qna qna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    // TODO: A 가 CryptoConverter 를 제공하면 @Convert 추가
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    public QnaAnswer(User responder, String content) {
        this.responder = responder;
        this.content = content;
    }

    void assignQna(Qna qna) {
        this.qna = qna;
    }
}
