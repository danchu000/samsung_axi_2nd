package com.ssa.lms.survey.entity;

import com.ssa.lms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 문항의 보기. 화면의 addChoice 로 동적 추가되므로 개수 제한 없음.
 */
/*
 * soft delete 를 걸지 않는다.
 *
 * 설문 수정 시 문항·보기는 통째로 교체되는데, @SQLDelete 로 두면 DELETE 가
 * UPDATE is_deleted=true 로 바뀌어 행이 그대로 남는다. 그런데 유니크 제약
 * uk_survey_choice_seq 는 is_deleted 를 보지 않으므로, 같은 seq 로 새 행을 넣는 순간
 * 반드시 충돌한다(실제로 500 발생). 부분 유니크 인덱스는 MySQL 이 지원하지 않는다.
 *
 * 이 둘은 독립적으로 조회되는 기록이 아니라 부모(Survey)에 종속된 값이고,
 * 보존 요건은 부모의 soft delete 로 충족된다. 그래서 물리 삭제를 쓴다.
 * (과제 슬라이스의 course_assignment 유니크 제약 충돌과 같은 구조의 문제다)
 */
@Entity
@Table(
        name = "survey_choice",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_survey_choice_seq", columnNames = {"question_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyChoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @Builder
    public SurveyChoice(Integer seq, String content) {
        this.seq = seq;
        this.content = content;
    }

    void assignQuestion(SurveyQuestion question) {
        this.question = question;
    }
}
