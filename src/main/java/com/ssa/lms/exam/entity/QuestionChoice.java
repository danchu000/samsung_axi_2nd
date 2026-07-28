package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 객관식 보기.
 *
 * IA "데이터 정리" 1행은 choice_1~4 플랫 컬럼이지만, 보기 개수가 가변이고
 * 정답 판정/오답률 통계 쿼리가 훨씬 단순해지므로 자식 테이블로 정규화했다.
 * (설계안 §9-1 결정 사항)
 */
@Entity
@Table(
        name = "question_choice",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_choice_seq", columnNames = {"question_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionChoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** 보기 번호 1~n. 화면의 choice_1~4 순서. */
    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Builder
    public QuestionChoice(Integer seq, String content, boolean correct) {
        this.seq = seq;
        this.content = content;
        this.correct = correct;
    }

    void assignQuestion(Question question) {
        this.question = question;
    }

    /**
     * 내용·정답 여부만 바꾼다. 행(id)은 유지된다.
     * answer.choice_id 가 이 행을 참조하므로 삭제 후 재생성하면 FK 위반이 난다.
     */
    void updateContent(String content, boolean correct) {
        this.content = content;
        this.correct = correct;
    }
}
