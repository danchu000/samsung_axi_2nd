package com.ssa.lms.survey.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.survey.dto.SurveyForm;
import com.ssa.lms.survey.dto.SurveyQuestionForm;
import com.ssa.lms.survey.dto.SurveySubmitForm;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 응답이 들어온 설문의 문항 변경을 막는지 고정한다.
 *
 * <p><b>왜 막는가:</b> 두 가지 이유다.</p>
 * <ol>
 *   <li><b>데이터 무결성</b> — "예/아니오"에 응답한 뒤 문항 내용을 바꾸면
 *       이미 수집한 답변이 다른 질문의 답으로 둔갑한다. 설문 결과 자체가 못 쓰게 된다.</li>
 *   <li><b>FK</b> — {@code survey_answer} 가 {@code survey_question}/{@code survey_choice} 를
 *       참조해, 물리 삭제 시 {@code Referential integrity constraint violation} 으로 500 이 났다.</li>
 * </ol>
 *
 * <p>단 제목 오타 수정·마감일 연장 같은 메타 변경은 막으면 안 된다.</p>
 */
@SpringBootTest
@Transactional
class SurveyResponseIntegrityTest {

    @Autowired SurveyService surveyService;
    @Autowired SurveyRepository surveyRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired UserRepository userRepository;

    private Course course;
    private Long traineeId;

    @BeforeEach
    void setUp() {
        course = courseRepository.findAll().stream().findFirst().orElseThrow();
        traineeId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TRAINEE)
                .findFirst().map(User::getId).orElseThrow();
    }

    private SurveyForm form(String title, String questionText, List<String> choices) {
        SurveyForm f = new SurveyForm();
        f.setTitle(title);
        f.setSurveyType("만족도");
        f.setCourseId(course.getId());
        f.setStartDate(LocalDate.now().minusDays(1));
        f.setDueDate(LocalDate.now().plusMonths(6));
        f.setRequired(true);
        f.setReflectCompletion(false);
        f.setStatus("active");

        SurveyQuestionForm q = new SurveyQuestionForm();
        q.setQuestionType("single");
        q.setContent(questionText);
        q.setRequired(true);
        q.setChoices(new ArrayList<>(choices));
        f.setQuestions(new ArrayList<>(List.of(q)));
        return f;
    }

    /** 설문 생성 후 훈련생 1명이 응답까지 남긴다. */
    private Long createWithResponse() {
        Long id = surveyService.create(form("무결성검증", "원본문항", List.of("예", "아니오")));
        surveyRepository.flush();

        Survey survey = surveyRepository.findById(id).orElseThrow();
        SurveyQuestion q = survey.getQuestions().get(0);

        SurveySubmitForm submit = new SurveySubmitForm();
        SurveySubmitForm.AnswerInput a = new SurveySubmitForm.AnswerInput();
        a.setQuestionId(q.getId());
        a.setChoiceIds(new ArrayList<>(List.of(q.getChoices().get(0).getId())));
        submit.setAnswers(new ArrayList<>(List.of(a)));

        surveyService.submit(id, traineeId, submit);
        surveyRepository.flush();
        return id;
    }

    @Test
    @DisplayName("응답이 있으면 문항 변경을 거부한다 — 수집된 답변이 다른 질문의 답으로 바뀐다")
    void 응답후_문항변경_거부() {
        Long id = createWithResponse();

        assertThatThrownBy(() ->
                surveyService.update(id, form("무결성검증", "완전히 다른 문항", List.of("좋음", "나쁨"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 응답이 있는 설문은 문항을 변경할 수 없습니다");
    }

    @Test
    @DisplayName("응답이 있어도 제목·마감일은 바꿀 수 있다 — 과하게 막으면 운영이 안 된다")
    void 응답후_메타변경_허용() {
        Long id = createWithResponse();

        SurveyForm f = form("무결성검증(제목수정)", "원본문항", List.of("예", "아니오"));
        f.setDueDate(LocalDate.now().plusYears(1));
        surveyService.update(id, f);
        surveyRepository.flush();

        Survey after = surveyRepository.findById(id).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("무결성검증(제목수정)");
        assertThat(after.getQuestions())
                .as("메타 변경 시 문항이 지워지면 응답이 끊긴다")
                .hasSize(1);
    }

    @Test
    @DisplayName("응답이 없으면 기존대로 문항을 자유롭게 바꿀 수 있다")
    void 응답전_문항변경_허용() {
        Long id = surveyService.create(form("무결성검증2", "원본문항", List.of("예", "아니오")));
        surveyRepository.flush();

        surveyService.update(id, form("무결성검증2", "바뀐문항", List.of("A", "B", "C")));
        surveyRepository.flush();

        Survey after = surveyRepository.findById(id).orElseThrow();
        assertThat(after.getQuestions().get(0).getContent()).isEqualTo("바뀐문항");
        assertThat(after.getQuestions().get(0).getChoices()).hasSize(3);
    }
}
