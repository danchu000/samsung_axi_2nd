package com.ssa.lms.exam.service;

import com.ssa.lms.exam.dto.QuestionForm;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import com.ssa.lms.exam.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 문제 수정 시 보기 처리를 고정한다.
 *
 * <p><b>배경:</b> 예전에는 보기를 통째로 지우고 새로 넣었다. 그런데 {@code answer.choice_id}
 * 가 question_choice 를 참조하고 있어서, <b>이미 응시된 문제를 수정하면 FK 위반으로 500</b>
 * 이 났다. 관리자가 오탈자 하나 고치려다 막히는 상황이었다.</p>
 *
 * <p>지금은 같은 seq 는 내용만 바꿔 행(id)을 유지한다. 응시자가 "3번을 골랐다"는 기록이
 * 살아 있어야 채점·이의제기 대응이 된다(3년 보존 요건).</p>
 */
@SpringBootTest
@Transactional
class QuestionChoiceUpdateTest {

    @Autowired QuestionService questionService;
    @Autowired QuestionRepository questionRepository;

    private QuestionForm form(String text, String... choices) {
        QuestionForm f = new QuestionForm();
        f.setQuestionType("객관식");
        f.setQuestionText(text);
        f.setCorrectAnswer("1");
        f.setDifficulty("easy");
        f.setScore(5);
        f.setStatus("사용중");
        String[] c = new String[4];
        System.arraycopy(choices, 0, c, 0, Math.min(choices.length, 4));
        f.setChoice1(c[0]); f.setChoice2(c[1]); f.setChoice3(c[2]); f.setChoice4(c[3]);
        return f;
    }

    @Test
    @DisplayName("보기 내용을 바꿔도 행 id 가 유지된다 — answer.choice_id 가 끊기지 않아야 한다")
    void 보기_행id_유지() {
        Long id = questionService.create(form("원본", "가", "나", "다", "라"));
        questionRepository.flush();

        List<Long> before = questionRepository.findById(id).orElseThrow()
                .getChoices().stream().map(QuestionChoice::getId).toList();

        questionService.update(id, form("수정", "가-수정", "나-수정", "다-수정", "라-수정"));
        questionRepository.flush();

        Question after = questionRepository.findById(id).orElseThrow();
        assertThat(after.getChoices().stream().map(QuestionChoice::getId))
                .as("보기 행이 삭제·재생성되면 응시 기록(answer.choice_id)이 끊긴다")
                .containsExactlyElementsOf(before);
        assertThat(after.getChoices().stream().map(QuestionChoice::getContent))
                .containsExactly("가-수정", "나-수정", "다-수정", "라-수정");
    }

    @Test
    @DisplayName("보기를 줄이면 남은 것만 유지된다")
    void 보기_축소() {
        Long id = questionService.create(form("원본", "가", "나", "다", "라"));
        questionRepository.flush();

        questionService.update(id, form("축소", "가", "나"));
        questionRepository.flush();

        assertThat(questionRepository.findById(id).orElseThrow().getChoices())
                .hasSize(2)
                .extracting(QuestionChoice::getContent)
                .containsExactly("가", "나");
    }

    @Test
    @DisplayName("보기를 늘리면 새 행이 추가된다")
    void 보기_확장() {
        Long id = questionService.create(form("원본", "가", "나"));
        questionRepository.flush();

        questionService.update(id, form("확장", "가", "나", "다", "라"));
        questionRepository.flush();

        assertThat(questionRepository.findById(id).orElseThrow().getChoices())
                .hasSize(4)
                .extracting(QuestionChoice::getContent)
                .containsExactly("가", "나", "다", "라");
    }

    @Test
    @DisplayName("객관식 → 주관식으로 바꾸면 보기가 정리된다")
    void 유형_변경() {
        Long id = questionService.create(form("원본", "가", "나", "다", "라"));
        questionRepository.flush();

        QuestionForm f = form("주관식으로", "가", "나", "다", "라");
        f.setQuestionType("주관식");
        f.setCorrectAnswer("정답문자열");
        questionService.update(id, f);
        questionRepository.flush();

        Question after = questionRepository.findById(id).orElseThrow();
        assertThat(after.getQuestionType()).isEqualTo(Question.QuestionType.SHORT_ANSWER);
        assertThat(after.getChoices()).isEmpty();
    }

    @Test
    @DisplayName("정답 위치가 바뀌면 correct 플래그도 따라 바뀐다")
    void 정답_변경() {
        Long id = questionService.create(form("원본", "가", "나", "다", "라"));
        questionRepository.flush();

        QuestionForm f = form("정답변경", "가", "나", "다", "라");
        f.setCorrectAnswer("3");
        questionService.update(id, f);
        questionRepository.flush();

        assertThat(questionRepository.findById(id).orElseThrow().getChoices())
                .filteredOn(QuestionChoice::isCorrect)
                .extracting(QuestionChoice::getSeq)
                .containsExactly(3);
    }
}
