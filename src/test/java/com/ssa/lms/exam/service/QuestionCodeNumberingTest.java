package com.ssa.lms.exam.service;

import com.ssa.lms.exam.dto.QuestionForm;
import com.ssa.lms.exam.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 문제 코드 자동 채번이 soft delete 와 충돌하지 않는지 고정한다.
 *
 * <p>Question 에는 {@code @SQLDelete} 와 {@code question_code} 유니크 제약이 함께 걸려 있다.
 * 채번을 JPQL {@code max()} 로 구하면 {@code @SQLRestriction} 때문에 삭제된 행이 조회에서
 * 빠지고, 이미 쓴 코드를 다시 발급해 유니크 제약 위반(500)이 난다.
 * 그래서 채번과 중복검사는 네이티브 쿼리로 삭제분까지 본다.</p>
 */
@SpringBootTest
@Transactional
class QuestionCodeNumberingTest {

    @Autowired QuestionService questionService;
    @Autowired QuestionRepository questionRepository;

    private QuestionForm form(String code) {
        QuestionForm f = new QuestionForm();
        f.setQuestionCode(code);
        f.setQuestionType("주관식");
        f.setQuestionText("채번 테스트 문항");
        f.setCorrectAnswer("정답");
        f.setDifficulty("easy");
        f.setScore(5);
        f.setStatus("사용중");
        return f;
    }

    @Test
    @DisplayName("삭제된 문제의 코드를 재발급하지 않는다 — soft delete 로 DB 에 행이 남아 있기 때문")
    void 삭제후_채번() {
        Long id = questionService.create(form(null));
        String deletedCode = questionRepository.findById(id).orElseThrow().getQuestionCode();

        questionService.delete(List.of(id));

        Long next = questionService.create(form(null));
        String nextCode = questionRepository.findById(next).orElseThrow().getQuestionCode();

        assertThat(nextCode)
                .as("삭제된 코드(%s)를 다시 발급하면 유니크 제약 위반이 난다", deletedCode)
                .isNotEqualTo(deletedCode);
    }

    @Test
    @DisplayName("삭제된 문제의 코드를 직접 입력하면 중복으로 거부한다")
    void 삭제된_코드_수동입력_거부() {
        Long id = questionService.create(form(null));
        String deletedCode = questionRepository.findById(id).orElseThrow().getQuestionCode();
        questionService.delete(List.of(id));

        assertThatThrownBy(() -> questionService.create(form(deletedCode)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 문제 코드");
    }
}
