package com.ssa.lms.exam.entity;

import com.ssa.lms.common.entity.BaseEntity;
import com.ssa.lms.course.entity.Course;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 문제은행.
 *
 * 매핑 근거
 *  - IA 엑셀 "데이터 정리" 1행 (question_id ~ status)
 *  - templates/admin/admin-04-evaluation/admin-evaluation-question-bank-add.html 입력 폼
 *
 * IA의 단일 category 는 화면이 대/중/소 3단(category1~3)을 요구하므로 3컬럼으로 확장했다.
 */
@Entity
@Table(
        name = "question",
        indexes = {
                @Index(name = "idx_question_course", columnList = "course_id"),
                @Index(name = "idx_question_category", columnList = "category_l, category_m, category_s"),
                @Index(name = "idx_question_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 화면 표시용 문제 코드 (예: Q-0001). 화면의 question_id 필드. */
    @Column(name = "question_code", length = 30, nullable = false, unique = true)
    private String questionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false)
    private QuestionType questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** 객관식이면 정답 보기의 seq, 주관식/코딩이면 정답 문자열. */
    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 10, nullable = false)
    private Difficulty difficulty;

    /** 기본 배점. 시험에 편성될 때 ExamQuestion.scoreOverride 로 덮어쓸 수 있다. */
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "category_l", length = 50)
    private String categoryL;

    @Column(name = "category_m", length = 50)
    private String categoryM;

    @Column(name = "category_s", length = 50)
    private String categoryS;

    /** 자동 출제 규칙 매칭용 태그. 쉼표 구분. */
    @Column(name = "tags", length = 255)
    private String tags;

    /** 문항별 제한시간(초). null 이면 시험 전체 제한시간만 적용. */
    @Column(name = "time_limit")
    private Integer timeLimit;

    /** 주관식 채점 시 대소문자 구분 여부. */
    @Column(name = "case_sensitive", nullable = false)
    private boolean caseSensitive;

    /** 부분점수 허용 여부. */
    @Column(name = "allow_partial", nullable = false)
    private boolean allowPartial;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private QuestionStatus status;

    /** null = 전사 공용 문제, 값 존재 = 해당 과정 전용 문제. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<QuestionChoice> choices = new ArrayList<>();

    @Builder
    public Question(String questionCode, QuestionType questionType, String questionText,
                    String correctAnswer, String explanation, Difficulty difficulty, Integer score,
                    String categoryL, String categoryM, String categoryS, String tags,
                    Integer timeLimit, boolean caseSensitive, boolean allowPartial,
                    QuestionStatus status, Course course) {
        this.questionCode = questionCode;
        this.questionType = questionType;
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.score = score;
        this.categoryL = categoryL;
        this.categoryM = categoryM;
        this.categoryS = categoryS;
        this.tags = tags;
        this.timeLimit = timeLimit;
        this.caseSensitive = caseSensitive;
        this.allowPartial = allowPartial;
        this.status = status;
        this.course = course;
    }

    public void addChoice(QuestionChoice choice) {
        this.choices.add(choice);
        choice.assignQuestion(this);
    }

    /** 객관식 여부. 자동 채점 가능 여부 판단에 쓴다. */
    public boolean isAutoGradable() {
        return this.questionType == QuestionType.MULTIPLE_CHOICE
                || this.questionType == QuestionType.SHORT_ANSWER;
    }

    /** 화면 값: 객관식 / 주관식 / 코딩 */
    public enum QuestionType {
        MULTIPLE_CHOICE,
        SHORT_ANSWER,
        CODING
    }

    /** 화면 값: 사용중 / 미사용 */
    public enum QuestionStatus {
        ACTIVE,
        INACTIVE
    }
}
