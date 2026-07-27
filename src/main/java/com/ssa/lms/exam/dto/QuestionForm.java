package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 문제 등록/수정 폼.
 *
 * 화면 admin-evaluation-question-bank-add.html 의 입력 필드에 대응한다.
 * HTML 의 id 속성(question_text 등)은 그대로 두고 name 만 camelCase 로 맞췄다 —
 * 해당 페이지 JS 가 getElementById 로만 접근하기 때문에 동작에 영향이 없다.
 *
 * 보기 4칸(choice1~4)은 화면이 고정 4칸이라 그대로 받고, 저장할 때
 * 빈 칸을 걸러 QuestionChoice 행으로 정규화한다.
 */
@Getter
@Setter
public class QuestionForm {

    private Long id;

    /** 표시용 코드. 신규 등록 시 비우면 서버가 채번한다. */
    private String questionCode;

    @NotBlank(message = "문제 유형을 선택하세요.")
    private String questionType;

    @NotBlank(message = "문제 내용을 입력하세요.")
    private String questionText;

    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;

    @NotBlank(message = "정답을 입력하세요.")
    private String correctAnswer;

    private String explanation;

    @NotBlank(message = "난이도를 선택하세요.")
    private String difficulty;

    @NotNull(message = "배점을 입력하세요.")
    @Min(value = 0, message = "배점은 0 이상이어야 합니다.")
    private Integer score;

    /** 화면은 카테고리 단일 입력. 대분류로 저장한다. */
    private String category;

    private String categoryM;
    private String categoryS;
    private String tags;

    @Min(value = 0, message = "제한 시간은 0 이상이어야 합니다.")
    private Integer timeLimit;

    private boolean caseSensitive;
    private boolean allowPartial;

    /** 화면 값: 사용중 / 미사용 */
    private String status = "사용중";

    /* ===== 변환 ===== */

    public Question.QuestionType toQuestionType() {
        return switch (questionType) {
            case "객관식", "MULTIPLE_CHOICE" -> Question.QuestionType.MULTIPLE_CHOICE;
            case "주관식", "SHORT_ANSWER" -> Question.QuestionType.SHORT_ANSWER;
            case "코딩", "CODING" -> Question.QuestionType.CODING;
            default -> throw new IllegalArgumentException("알 수 없는 문제 유형: " + questionType);
        };
    }

    public Difficulty toDifficulty() {
        return Difficulty.valueOf(difficulty.toUpperCase());
    }

    public Question.QuestionStatus toStatus() {
        return "미사용".equals(status) || "INACTIVE".equals(status)
                ? Question.QuestionStatus.INACTIVE
                : Question.QuestionStatus.ACTIVE;
    }

    /** 입력된 보기만 순번을 매겨 반환. 정답 번호와 일치하는 보기에 correct 를 세운다. */
    public List<QuestionChoice> toChoices() {
        List<QuestionChoice> result = new ArrayList<>();
        String[] raw = {choice1, choice2, choice3, choice4};
        int seq = 0;
        for (String content : raw) {
            seq++;
            if (content == null || content.isBlank()) {
                continue;
            }
            result.add(QuestionChoice.builder()
                    .seq(seq)
                    .content(content.strip())
                    .correct(String.valueOf(seq).equals(correctAnswer == null ? null : correctAnswer.strip()))
                    .build());
        }
        return result;
    }

    public static QuestionForm from(Question q) {
        QuestionForm form = new QuestionForm();
        form.id = q.getId();
        form.questionCode = q.getQuestionCode();
        form.questionType = switch (q.getQuestionType()) {
            case MULTIPLE_CHOICE -> "객관식";
            case SHORT_ANSWER -> "주관식";
            case CODING -> "코딩";
        };
        form.questionText = q.getQuestionText();
        form.correctAnswer = q.getCorrectAnswer();
        form.explanation = q.getExplanation();
        form.difficulty = q.getDifficulty().name().toLowerCase();
        form.score = q.getScore();
        form.category = q.getCategoryL();
        form.categoryM = q.getCategoryM();
        form.categoryS = q.getCategoryS();
        form.tags = q.getTags();
        form.timeLimit = q.getTimeLimit();
        form.caseSensitive = q.isCaseSensitive();
        form.allowPartial = q.isAllowPartial();
        form.status = q.getStatus() == Question.QuestionStatus.ACTIVE ? "사용중" : "미사용";

        List<QuestionChoice> choices = q.getChoices();
        for (QuestionChoice c : choices) {
            switch (c.getSeq()) {
                case 1 -> form.choice1 = c.getContent();
                case 2 -> form.choice2 = c.getContent();
                case 3 -> form.choice3 = c.getContent();
                case 4 -> form.choice4 = c.getContent();
                default -> { /* 화면은 4칸까지만 표시 */ }
            }
        }
        return form;
    }
}
