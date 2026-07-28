package com.ssa.lms.survey.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.survey.dto.SurveyForm;
import com.ssa.lms.survey.dto.SurveyListRow;
import com.ssa.lms.survey.dto.SurveyQuestionForm;
import com.ssa.lms.survey.dto.SurveySearchCond;
import com.ssa.lms.survey.dto.SurveyDetailView;
import com.ssa.lms.survey.dto.SurveySubmitForm;
import com.ssa.lms.survey.dto.TraineeSurveyRow;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyChoice;
import com.ssa.lms.survey.entity.SurveyAnswer;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.entity.SurveyResponse;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.repository.SurveyResponseRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * 관리자 설문 서비스.
 *
 * 접근 통제는 SecurityConfig 의 /admin/survey/** (ADMIN, INSTRUCTOR) 로 처리한다.
 *
 * 응답률 분모(대상 과정 수강생 수)는 A 소유 리포지토리를 직접 쓰지 않고
 * {@link CourseQueryService#findUserIdsByCourseId(Long)} 만 호출해 구한다 (a-requests.md P0-4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyRepository surveyRepository;
    // 엔티티 참조용 — A 가 "과정 1건 조회" 를 제공하지 않아 남겨둔다 (요청 목록에 추가)
    private final CourseRepository courseRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final CourseQueryService courseQueryService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    /**
     * 목록 조회.
     *
     * 문항 수·응답 건수는 페이지에 담긴 id 묶음으로 한 번씩만 집계한다 (행마다 조회하면 N+1).
     * 응답률 분모만은 과정 단위라 courseId 별로 캐시해 중복 호출을 막는다.
     */
    /**
     * 설문 대상 과정 셀렉트 옵션.
     * 임시로 A 소유 CourseRepository 를 읽기 전용으로 쓴다.
     * A 가 CourseQueryService 에 과정 목록 조회를 추가하면 그쪽으로 옮긴다 (docs/a-requests.md).
     */
    /**
     * 설문 대상 과정 셀렉트 옵션.
     *
     * <p>A 가 {@code CourseQueryService.findAllCourseOptions()} 를 제공하기 전에는
     * {@code CourseRepository} 를 직접 주입해 쓰고 있었다(도메인 경계 위반).
     * 이제 공식 조회로 위임한다.</p>
     */
    public List<CourseOption> courseOptions() {
        return courseQueryService.findAllCourseOptions().stream()
                .map(c -> new CourseOption(c.id(), c.courseCode(), c.courseName(), c.cohort()))
                .toList();
    }

    /** 화면용 과정 셀렉트 한 줄. A 의 레코드를 화면 표기 규칙에 맞춰 감싼다. */
    public record CourseOption(Long id, String code, String name, String cohort) {
        public String label() {
            return cohort == null || cohort.isBlank() ? name : name + " (" + cohort + ")";
        }
    }

    /**
     * 서버 페이징 목록.
     *
     * <p>설문 검색은 리포지토리가 List 를 돌려주고 서비스에서 응답률을 붙이는 구조라,
     * 자르는 지점을 서비스에 둔다. 응답률 집계는 잘라낸 뒤의 id 묶음으로만 하므로
     * 페이지가 커져도 집계 쿼리는 한 번이다.</p>
     */
    public org.springframework.data.domain.Page<SurveyListRow> search(
            SurveySearchCond cond, org.springframework.data.domain.Pageable pageable) {
        List<SurveyListRow> all = search(cond);
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return new org.springframework.data.domain.PageImpl<>(
                all.subList(from, to), pageable, all.size());
    }

    public List<SurveyListRow> search(SurveySearchCond cond) {
        List<Survey> surveys = surveyRepository.search(
                cond.courseId(), cond.statusOrNull(), cond.keywordOrNull());

        List<Long> ids = surveys.stream().map(Survey::getId).toList();
        Map<Long, Long> questionCounts = toLongMap(
                ids.isEmpty() ? Collections.emptyList() : surveyRepository.countQuestions(ids));
        Map<Long, Long> responseCounts = toLongMap(
                ids.isEmpty() ? Collections.emptyList() : surveyResponseRepository.countBySurveyIds(ids));

        Map<Long, Long> targetCache = new HashMap<>();
        List<SurveyListRow> rows = new ArrayList<>();
        for (Survey s : surveys) {
            rows.add(SurveyListRow.of(
                    s,
                    responseCounts.getOrDefault(s.getId(), 0L),
                    targetCount(s, targetCache),
                    questionCounts.getOrDefault(s.getId(), 0L)));
        }
        return rows;
    }

    public Survey getOrThrow(Long id) {
        return surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + id));
    }

    public SurveyForm loadForm(Long id) {
        Survey survey = surveyRepository.findWithQuestions(id)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + id));
        // 보기(choices)는 bag 두 개를 동시에 fetch 할 수 없어 여기서 한 번 더 채운다
        surveyRepository.fetchChoices(id);
        return SurveyForm.from(survey);
    }

    @Transactional
    public Long create(SurveyForm form) {
        validate(form);
        LocalDateTime now = LocalDateTime.now();

        Survey survey = Survey.builder()
                .title(form.getTitle().strip())
                .surveyType(form.toSurveyType())
                .course(findCourse(form.getCourseId()))
                .session(findSession(form.getSessionId()))
                .required(form.isRequired())
                .reflectCompletion(form.isReflectCompletion())
                .anonymous(form.isAnonymous())
                .startAt(form.startAtValue())
                .endAt(form.endAtValue())
                .status(form.toStatus(now))
                .build();

        applyQuestions(survey, form);
        return surveyRepository.save(survey).getId();
    }

    @Transactional
    public void update(Long id, SurveyForm form) {
        validate(form);
        Survey survey = surveyRepository.findWithQuestions(id)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + id));
        // 보기(choices)는 bag 두 개를 동시에 fetch 할 수 없어 여기서 한 번 더 채운다
        surveyRepository.fetchChoices(id);

        survey.update(
                form.getTitle().strip(), form.toSurveyType(),
                findCourse(form.getCourseId()), findSession(form.getSessionId()),
                form.isRequired(), form.isReflectCompletion(), form.isAnonymous(),
                form.startAtValue(), form.endAtValue(), form.toStatus(LocalDateTime.now()));

        // 문항·보기를 통째로 교체한다.
        //
        // QuestionService.update() 와 같은 함정인데 설문은 2단 중첩이라 더 크게 터진다.
        // clear() 만 하고 바로 add() 하면 Hibernate 가 orphan DELETE 보다 INSERT 를 먼저
        // 내보내 uk_survey_question_seq(survey_id, seq) 와 uk_survey_choice_seq(question_id, seq)
        // 양쪽 유니크 제약에 걸린다. 그래서 삭제를 flush 로 확정한 뒤에 새 문항을 넣는다.
        //
        // 자식(보기)은 문항이 지워지면 cascade + orphanRemoval 로 함께 지워지므로
        // 문항만 clear 하면 된다. 다만 flush 는 반드시 그 사이에 한 번 들어가야 한다.
        // 응답이 이미 들어왔으면 문항을 바꿀 수 없다.
        //
        // 두 가지 이유다.
        //  (1) 데이터 무결성 — "예/아니오" 에 응답한 뒤 문항 내용을 바꾸면 이미 수집한
        //      답변이 다른 질문의 답으로 둔갑한다. 설문 결과 자체가 못 쓰게 된다.
        //  (2) FK — survey_answer 가 survey_question/survey_choice 를 참조하고 있어
        //      물리 삭제 시 "Referential integrity constraint violation" 으로 500 이 난다.
        //
        // 제목 오타 수정·마감일 연장 같은 메타 정보 변경은 위에서 이미 반영됐으므로 그대로 통과한다.
        // 문항을 바꿔야 하면 설문을 새로 만들어야 한다 (시험의 ensureNotAttempted 와 같은 정책).
        if (surveyResponseRepository.existsBySurveyId(id)) {
            if (questionsChanged(survey, form)) {
                throw new IllegalStateException(
                        "이미 응답이 있는 설문은 문항을 변경할 수 없습니다. "
                                + "수집된 답변이 다른 질문의 답으로 바뀌기 때문입니다. "
                                + "문항을 바꾸려면 설문을 새로 등록하세요. "
                                + "(제목·기간·상태는 변경할 수 있습니다)");
            }
            return;   // 문항이 그대로면 메타 정보만 바뀐 것이므로 교체하지 않는다
        }

        survey.clearQuestions();
        surveyRepository.flush();
        applyQuestions(survey, form);
    }

    /** 폼으로 들어온 문항 구성이 저장된 것과 다른지 — 순서·유형·내용·보기까지 본다. */
    private boolean questionsChanged(Survey survey, SurveyForm form) {
        List<SurveyQuestionForm> incoming = form.getQuestions() == null
                ? List.of() : form.getQuestions();
        List<SurveyQuestion> stored = survey.getQuestions();
        if (incoming.size() != stored.size()) {
            return true;
        }
        for (int i = 0; i < stored.size(); i++) {
            SurveyQuestion q = stored.get(i);
            SurveyQuestionForm f = incoming.get(i);
            if (q.getQuestionType() != f.toQuestionType()
                    || !Objects.equals(q.getContent(), f.getContent())
                    || q.isRequired() != f.isRequired()) {
                return true;
            }
            List<String> storedChoices = q.getChoices().stream()
                    .map(SurveyChoice::getContent).toList();
            List<String> formChoices = f.getChoices() == null ? List.of() : f.getChoices();
            if (!storedChoices.equals(formChoices)) {
                return true;
            }
        }
        return false;
    }

    /** 선택 비활성화 — 화면의 "선택한 설문 비활성화". 비활성화는 DRAFT 로 내린다. */
    @Transactional
    public void deactivate(List<Long> ids) {
        surveyRepository.findAllById(ids)
                .forEach(s -> s.changeStatus(Survey.SurveyStatus.DRAFT));
    }

    /** 화면의 "빠른 상태변경" — 응답중/응답대기/마감 일괄 변경. */
    @Transactional
    public void changeStatus(List<Long> ids, String status) {
        Survey.SurveyStatus target = new SurveySearchCond(null, status, null).statusOrNull();
        if (target == null) {
            throw new IllegalArgumentException("변경할 상태를 선택하세요.");
        }
        surveyRepository.findAllById(ids).forEach(s -> s.changeStatus(target));
    }

    /** 선택 삭제. Survey 및 자식 엔티티는 @SQLDelete로 보존 삭제된다. */
    @Transactional
    public void delete(List<Long> ids) {
        surveyRepository.deleteAll(surveyRepository.findAllById(ids));
    }

    /** 로그인한 훈련생에게 배포된 설문만 보여 준다. */
    public List<TraineeSurveyRow> findForTrainee(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Survey> visible = surveyRepository.findVisibleToTrainee(Survey.SurveyStatus.DRAFT).stream()
                .filter(s -> s.getCourse() == null
                        || courseQueryService.findUserIdsByCourseId(s.getCourse().getId()).contains(userId))
                .toList();
        List<Long> ids = visible.stream().map(Survey::getId).toList();
        Map<Long, Long> questionCounts = toLongMap(ids.isEmpty() ? Collections.emptyList()
                : surveyRepository.countQuestions(ids));
        List<Long> submitted = surveyResponseRepository.findRespondedSurveyIds(userId);
        return visible.stream().map(s -> TraineeSurveyRow.of(s, submitted.contains(s.getId()),
                questionCounts.getOrDefault(s.getId(), 0L), now)).toList();
    }

    /** 훈련생 상세. 대상자가 아니거나 작성중 설문은 노출하지 않는다. */
    public SurveyDetailView detailForTrainee(Long surveyId, Long userId) {
        Survey survey = surveyRepository.findWithQuestions(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + surveyId));
        // 보기(choices)는 bag 두 개를 동시에 fetch 할 수 없어 여기서 한 번 더 채운다
        surveyRepository.fetchChoices(surveyId);
        requireAudience(survey, userId);
        boolean submitted = surveyResponseRepository.existsBySurveyIdAndUserId(surveyId, userId);
        return SurveyDetailView.of(survey, submitted, !submitted && survey.isOpenAt(LocalDateTime.now()));
    }

    /** 서버 시간과 DB의 응답 유니크 제약을 기준으로 제출을 확정한다. */
    @Transactional
    public void submit(Long surveyId, Long userId, SurveySubmitForm form) {
        Survey survey = surveyRepository.findWithQuestions(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + surveyId));
        // 보기(choices)는 bag 두 개를 동시에 fetch 할 수 없어 여기서 한 번 더 채운다
        surveyRepository.fetchChoices(surveyId);
        requireAudience(survey, userId);
        if (!survey.isOpenAt(LocalDateTime.now())) {
            throw new IllegalArgumentException("현재 응답할 수 없는 설문입니다.");
        }
        if (!survey.isAnonymous() && surveyResponseRepository.existsBySurveyIdAndUserId(surveyId, userId)) {
            throw new IllegalArgumentException("이미 제출한 설문입니다.");
        }

        Map<Long, SurveySubmitForm.AnswerInput> inputs = new HashMap<>();
        if (form.getAnswers() != null) {
            for (SurveySubmitForm.AnswerInput input : form.getAnswers()) {
                if (input != null && input.getQuestionId() != null) inputs.put(input.getQuestionId(), input);
            }
        }
        User respondent = survey.isAnonymous() ? null : userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        SurveyResponse response = SurveyResponse.builder().survey(survey).user(respondent)
                .submittedAt(LocalDateTime.now()).build();
        for (SurveyQuestion question : survey.getQuestions()) {
            SurveySubmitForm.AnswerInput input = inputs.get(question.getId());
            List<SurveyAnswer> answers = answersFor(question, input);
            if (question.isRequired() && answers.isEmpty()) {
                throw new IllegalArgumentException("필수 문항에 응답하세요: " + question.getContent());
            }
            answers.forEach(response::addAnswer);
        }
        surveyResponseRepository.save(response);
    }

    /* ===== 내부 ===== */

    /**
     * 익명 설문과 이수 반영은 동시에 성립할 수 없다.
     *
     * 익명이면 SurveyResponse.user 를 저장하지 않는데, 이수 반영은 "누가 냈는지"로
     * 판정한다. 둘 다 켜면 이수 판정이 조용히 전원 미제출로 떨어진다 —
     * SurveyResponse 엔티티 주석에 적힌 조합이다.
     */
    private void validate(SurveyForm form) {
        if (form.isAnonymous() && form.isReflectCompletion()) {
            throw new IllegalArgumentException(
                    "익명 설문은 이수에 반영할 수 없습니다. 익명을 끄거나 이수 반영을 끄세요.");
        }
        if (form.getDueDate() != null && form.getStartDate() != null
                && form.getDueDate().isBefore(form.getStartDate())) {
            throw new IllegalArgumentException("마감일은 시작일보다 빠를 수 없습니다.");
        }
        if (form.getQuestions() == null || form.getQuestions().stream()
                .noneMatch(q -> q.getContent() != null && !q.getContent().isBlank())) {
            throw new IllegalArgumentException("문항을 1개 이상 추가하세요.");
        }
    }

    /** 빈 문항은 걸러내고 1부터 순번을 다시 매긴다 (화면에서 중간 문항을 지울 수 있어서). */
    private void applyQuestions(Survey survey, SurveyForm form) {
        int seq = 0;
        for (SurveyQuestionForm qf : form.getQuestions()) {
            if (qf == null || qf.getContent() == null || qf.getContent().isBlank()) {
                continue;
            }
            seq++;
            SurveyQuestion question = SurveyQuestion.builder()
                    .seq(seq)
                    .questionType(qf.toQuestionType())
                    .content(qf.getContent().strip())
                    .required(qf.isRequired())
                    .scaleMax(qf.resolvedScaleMax())
                    .build();

            if (qf.needsChoices()) {
                List<String> choices = qf.cleanChoices();
                if (choices.size() < 2) {
                    throw new IllegalArgumentException(
                            "객관식 문항은 보기가 2개 이상이어야 합니다: " + qf.getContent());
                }
                int cseq = 0;
                for (String content : choices) {
                    cseq++;
                    question.addChoice(SurveyChoice.builder().seq(cseq).content(content).build());
                }
            }
            survey.addQuestion(question);
        }
    }

    private void requireAudience(Survey survey, Long userId) {
        if (survey.getStatus() == Survey.SurveyStatus.DRAFT
                || (survey.getCourse() != null
                && !courseQueryService.findUserIdsByCourseId(survey.getCourse().getId()).contains(userId))) {
            throw new IllegalArgumentException("응답 대상 설문이 아닙니다.");
        }
    }

    private List<SurveyAnswer> answersFor(SurveyQuestion question, SurveySubmitForm.AnswerInput input) {
        if (input == null) return Collections.emptyList();
        return switch (question.getQuestionType()) {
            case SINGLE, MULTI -> {
                Map<Long, SurveyChoice> choices = new HashMap<>();
                question.getChoices().forEach(c -> choices.put(c.getId(), c));
                List<Long> choiceIds = input.cleanChoiceIds();
                if (question.getQuestionType() == SurveyQuestion.SurveyQuestionType.SINGLE && choiceIds.size() > 1) {
                    throw new IllegalArgumentException("단일 선택 문항은 하나만 선택하세요.");
                }
                List<SurveyAnswer> result = new ArrayList<>();
                for (Long choiceId : choiceIds) {
                    SurveyChoice choice = choices.get(choiceId);
                    if (choice == null) throw new IllegalArgumentException("유효하지 않은 보기입니다.");
                    result.add(SurveyAnswer.builder().question(question).choice(choice).build());
                }
                yield result;
            }
            case SCALE -> {
                if (input.getScaleValue() == null) yield Collections.emptyList();
                int max = question.getScaleMax() == null ? 5 : question.getScaleMax();
                if (input.getScaleValue() < 1 || input.getScaleValue() > max) {
                    throw new IllegalArgumentException("척도 응답 범위가 올바르지 않습니다.");
                }
                yield List.of(SurveyAnswer.builder().question(question).scaleValue(input.getScaleValue()).build());
            }
            case TEXT -> input.hasText()
                    ? List.of(SurveyAnswer.builder().question(question).answerText(input.getAnswerText().strip()).build())
                    : Collections.emptyList();
        };
    }

    /** 응답률 분모 — 대상 과정의 수강생 수. 전체 대상 설문(course=null)은 0(=화면에 "-"). */
    private long targetCount(Survey survey, Map<Long, Long> cache) {
        if (survey.getCourse() == null) {
            return 0L;
        }
        Long courseId = survey.getCourse().getId();
        return cache.computeIfAbsent(courseId,
                id -> (long) courseQueryService.findUserIdsByCourseId(id).size());
    }

    private Course findCourse(Long courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("과정을 찾을 수 없습니다. id=" + courseId));
    }

    private Session findSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("차시를 찾을 수 없습니다. id=" + sessionId));
    }

    private Map<Long, Long> toLongMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
