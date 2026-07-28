package com.ssa.lms.survey;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.Survey.SurveyStatus;
import com.ssa.lms.survey.entity.Survey.SurveyType;
import com.ssa.lms.survey.entity.SurveyChoice;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.entity.SurveyQuestion.SurveyQuestionType;
import com.ssa.lms.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * local 프로필 전용 설문 시드.
 *
 * <p>설문 슬라이스만 시더가 없어 빈 DB 로 뜨면 목록이 JS 더미 폴백으로 채워졌고,
 * 더미 행은 실제 id 가 없어 상세보기가 동작하지 않았다 (2026-07-28 A 발견).
 * 다른 B 시더와 같은 {@code ApplicationReadyEvent} 패턴 — 실행 순서와 무관하게
 * A 의 사용자·과정 시드 이후에 동작한다. B 검토 요청: a-requests.md.</p>
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalSurveyDataInitializer {

    private final SurveyRepository surveyRepository;
    private final CourseRepository courseRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (surveyRepository.count() > 0) {
            return;
        }
        Course course = courseRepository.findAll().stream().findFirst().orElse(null);
        if (course == null) {
            log.warn("[local] 설문 시드 건너뜀 — 과정이 없다 (A의 LocalDataInitializer 확인)");
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        Survey satisfaction = Survey.builder()
                .title("2026년 1차 과정 만족도 조사")
                .surveyType(SurveyType.COURSE_SATISFACTION)
                .course(course)
                .required(true).reflectCompletion(true).anonymous(true)
                .startAt(now.minusDays(7)).endAt(now.plusDays(14))
                .status(SurveyStatus.ONGOING)
                .build();
        satisfaction.addQuestion(scale(1, "과정 전반에 대한 만족도를 평가해 주세요.", 5));
        satisfaction.addQuestion(single(2, "가장 도움이 된 활동은 무엇인가요?",
                List.of("이론 강의", "실습 과제", "튜터링/Q&A", "동료 학습")));
        satisfaction.addQuestion(multi(3, "개선이 필요하다고 느낀 영역을 모두 선택해 주세요.",
                List.of("강의 자료", "실습 환경", "과제 난이도", "학습 지원 응답 속도")));
        satisfaction.addQuestion(text(4, "과정 운영에 바라는 점을 자유롭게 적어 주세요."));
        surveyRepository.save(satisfaction);

        Survey completion = Survey.builder()
                .title("수료 전 최종 설문")
                .surveyType(SurveyType.COMPLETION)
                .course(course)
                .required(false).reflectCompletion(false).anonymous(false)
                .startAt(now.plusDays(30)).endAt(now.plusDays(37))
                .status(SurveyStatus.SCHEDULED)
                .build();
        completion.addQuestion(scale(1, "취업 준비에 이 과정이 얼마나 도움이 되었나요?", 5));
        completion.addQuestion(text(2, "후속 과정에서 배우고 싶은 주제가 있다면 적어 주세요."));
        surveyRepository.save(completion);

        log.info("[local] 설문 시드 {}건 생성", surveyRepository.count());
    }

    private static SurveyQuestion scale(int seq, String content, int max) {
        return SurveyQuestion.builder()
                .seq(seq).questionType(SurveyQuestionType.SCALE).content(content)
                .required(true).scaleMax(max).build();
    }

    private static SurveyQuestion single(int seq, String content, List<String> choices) {
        return withChoices(SurveyQuestion.builder()
                .seq(seq).questionType(SurveyQuestionType.SINGLE).content(content)
                .required(true).build(), choices);
    }

    private static SurveyQuestion multi(int seq, String content, List<String> choices) {
        return withChoices(SurveyQuestion.builder()
                .seq(seq).questionType(SurveyQuestionType.MULTI).content(content)
                .required(false).build(), choices);
    }

    private static SurveyQuestion text(int seq, String content) {
        return SurveyQuestion.builder()
                .seq(seq).questionType(SurveyQuestionType.TEXT).content(content)
                .required(false).build();
    }

    private static SurveyQuestion withChoices(SurveyQuestion question, List<String> choices) {
        for (int i = 0; i < choices.size(); i++) {
            question.addChoice(SurveyChoice.builder().seq(i + 1).content(choices.get(i)).build());
        }
        return question;
    }
}
