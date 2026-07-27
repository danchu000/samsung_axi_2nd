package com.ssa.lms.exam;

import com.ssa.lms.exam.entity.Difficulty;
import com.ssa.lms.exam.entity.Question;
import com.ssa.lms.exam.entity.QuestionChoice;
import com.ssa.lms.exam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * local 프로필 전용 문제은행 시드 (개발자 B).
 *
 * 기존 정적 화면의 더미 배열(static/js/contents.js 의 '문제' 유형 행)을 옮긴 것이다.
 * A의 {@code LocalDataInitializer} 와 파일을 나눠 두어 서로 충돌하지 않는다.
 * MySQL 환경용 data.sql 이관은 스키마 확정 후 별도로 진행한다.
 */
@Slf4j
@Component
@Profile("local")
@Order(100)
@RequiredArgsConstructor
public class LocalQuestionDataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            return;
        }

        Question q1 = Question.builder()
                .questionCode("Q-0001")
                .questionType(Question.QuestionType.MULTIPLE_CHOICE)
                .questionText("자바스크립트에서 변수를 선언하는 키워드가 아닌 것은?")
                .correctAnswer("3")
                .explanation("int 는 자바의 자료형 키워드이며 자바스크립트에는 존재하지 않는다.")
                .difficulty(Difficulty.EASY)
                .score(5)
                .categoryL("고교위탁")
                .categoryM("프로그래밍")
                .categoryS("자바스크립트")
                .tags("변수,기초문법")
                .caseSensitive(false)
                .allowPartial(false)
                .status(Question.QuestionStatus.ACTIVE)
                .build();
        addChoices(q1, List.of("var", "let", "int", "const"), 3);

        Question q2 = Question.builder()
                .questionCode("Q-0002")
                .questionType(Question.QuestionType.SHORT_ANSWER)
                .questionText("HTTP 상태코드 404 가 의미하는 바를 한 단어로 쓰시오.")
                .correctAnswer("Not Found")
                .explanation("요청한 리소스를 서버가 찾지 못했음을 뜻한다.")
                .difficulty(Difficulty.MEDIUM)
                .score(10)
                .categoryL("KDT")
                .categoryM("웹")
                .categoryS("HTTP")
                .tags("네트워크,상태코드")
                .caseSensitive(false)
                .allowPartial(true)
                .status(Question.QuestionStatus.ACTIVE)
                .build();

        Question q3 = Question.builder()
                .questionCode("Q-0003")
                .questionType(Question.QuestionType.CODING)
                .questionText("주어진 정수 배열에서 최댓값을 반환하는 함수를 작성하시오.")
                .correctAnswer("Math.max(...arr)")
                .explanation("반복문 또는 스프레드 연산자 어느 쪽이든 정답으로 인정한다.")
                .difficulty(Difficulty.HARD)
                .score(20)
                .categoryL("KDT")
                .categoryM("알고리즘")
                .categoryS("배열")
                .tags("구현,배열")
                .timeLimit(600)
                .caseSensitive(true)
                .allowPartial(true)
                .status(Question.QuestionStatus.INACTIVE)
                .build();

        questionRepository.saveAll(List.of(q1, q2, q3));
        log.info("[local] 문제은행 시드 {}건 생성", questionRepository.count());
    }

    private void addChoices(Question question, List<String> contents, int correctSeq) {
        for (int i = 0; i < contents.size(); i++) {
            int seq = i + 1;
            question.addChoice(QuestionChoice.builder()
                    .seq(seq)
                    .content(contents.get(i))
                    .correct(seq == correctSeq)
                    .build());
        }
    }
}
