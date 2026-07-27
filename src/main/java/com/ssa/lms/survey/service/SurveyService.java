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
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyChoice;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.repository.SurveyResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private final SurveyResponseRepository surveyResponseRepository;
    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;

    /**
     * 목록 조회.
     *
     * 문항 수·응답 건수는 페이지에 담긴 id 묶음으로 한 번씩만 집계한다 (행마다 조회하면 N+1).
     * 응답률 분모만은 과정 단위라 courseId 별로 캐시해 중복 호출을 막는다.
     */
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
        survey.clearQuestions();
        surveyRepository.flush();
        applyQuestions(survey, form);
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

    /**
     * 선택 삭제. Survey 는 BaseEntity 의 보존 정책을 따르지만 @SQLDelete 가 없어
     * 물리 삭제된다. 응답 데이터 3년 보존 요건과 맞물리므로, 응답이 1건이라도 있으면
     * 삭제하지 않고 마감 처리로 대신한다.
     */
    @Transactional
    public void delete(List<Long> ids) {
        Map<Long, Long> responseCounts = toLongMap(
                ids.isEmpty() ? Collections.emptyList() : surveyResponseRepository.countBySurveyIds(ids));
        for (Survey s : surveyRepository.findAllById(ids)) {
            if (responseCounts.getOrDefault(s.getId(), 0L) > 0) {
                s.changeStatus(Survey.SurveyStatus.CLOSED);
            } else {
                surveyRepository.delete(s);
            }
        }
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
