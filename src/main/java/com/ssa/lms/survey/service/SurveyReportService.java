package com.ssa.lms.survey.service;

import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.export.ExcelWriter;
import com.ssa.lms.survey.dto.SurveyReportView;
import com.ssa.lms.survey.entity.Survey;
import com.ssa.lms.survey.entity.SurveyChoice;
import com.ssa.lms.survey.entity.SurveyQuestion;
import com.ssa.lms.survey.entity.SurveyQuestion.SurveyQuestionType;
import com.ssa.lms.survey.repository.SurveyAnswerRepository;
import com.ssa.lms.survey.repository.SurveyRepository;
import com.ssa.lms.survey.repository.SurveyResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 설문 결과 리포트 — 문항별 집계 + xlsx 다운로드.
 *
 * <p><b>왜 {@link SurveyService} 와 분리했나</b>: SurveyService 는 설문을 만들고 고치고 응답을 받는
 * 쓰기 경로다. 리포트는 순수 읽기이고 권한 규칙이 다르다(등록·수정은 SecurityConfig 의
 * ADMIN·INSTRUCTOR 로 끝나지만, <b>결과는 응답 내용이라 강사를 담당 과정으로 한 번 더 좁혀야
 * 한다</b>). 한 클래스에 섞으면 그 차이가 묻힌다 — 채점 쪽 {@code ExamGradingService.ensureCanGrade}
 * 와 같은 이유의 분리다.</p>
 *
 * <p>훈련생은 SecurityConfig 의 {@code /admin/survey/**} → ADMIN·INSTRUCTOR 규칙으로 진입 자체가
 * 막힌다. 여기서 다시 보지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String[] SUMMARY_HEADERS = {"항목", "값"};
    private static final String[] QUESTION_HEADERS = {
            "문항번호", "문항유형", "문항", "필수", "응답자수", "항목", "응답수", "비율"};
    private static final String[] TEXT_HEADERS = {"문항번호", "문항", "순번", "응답 내용"};

    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;
    private final CourseQueryService courseQueryService;

    /* ===================== 집계 ===================== */

    /**
     * 문항별 집계.
     *
     * @param admin true 면 전체 열람, false(강사)면 담당 과정의 설문만
     */
    public SurveyReportView report(Long surveyId, Long viewerId, boolean admin) {
        Survey survey = surveyRepository.findWithQuestions(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + surveyId));
        // 보기(choices)는 bag 두 개를 동시에 fetch 할 수 없어 여기서 한 번 더 채운다 (SurveyService 와 같은 이유)
        surveyRepository.fetchChoices(surveyId);
        ensureCanRead(survey, viewerId, admin);

        Map<Long, Long> respondents = toLongMap(surveyAnswerRepository.countRespondentsByQuestion(surveyId));
        Map<Long, Map<Long, Long>> choiceCounts = toNestedMap(surveyAnswerRepository.countByChoice(surveyId));
        Map<Long, Map<Long, Long>> scaleCounts = toNestedMap(surveyAnswerRepository.countByScaleValue(surveyId));
        Map<Long, List<String>> texts = toTextMap(surveyAnswerRepository.findTextAnswers(surveyId));

        List<SurveyQuestion> questions = new ArrayList<>(survey.getQuestions());
        questions.sort(Comparator.comparing(SurveyQuestion::getSeq));

        List<SurveyReportView.QuestionReport> reports = new ArrayList<>();
        for (SurveyQuestion q : questions) {
            long answered = respondents.getOrDefault(q.getId(), 0L);
            reports.add(switch (q.getQuestionType()) {
                case SINGLE, MULTI -> choiceReport(q, answered, choiceCounts.getOrDefault(q.getId(), Map.of()));
                case SCALE -> scaleReport(q, answered, scaleCounts.getOrDefault(q.getId(), Map.of()));
                case TEXT -> textReport(q, answered, texts.getOrDefault(q.getId(), List.of()));
            });
        }

        long responseCount = surveyResponseRepository.countBySurveyIds(List.of(surveyId)).stream()
                .findFirst().map(row -> ((Number) row[1]).longValue()).orElse(0L);
        long targetCount = survey.getCourse() == null
                ? 0L
                : courseQueryService.findUserIdsByCourseId(survey.getCourse().getId()).size();

        return new SurveyReportView(
                String.valueOf(survey.getId()),
                survey.getTitle(),
                typeLabel(survey.getSurveyType()),
                survey.getCourse() == null ? "전체 대상" : survey.getCourse().getCourseName(),
                survey.getSession() == null ? "-" : survey.getSession().getName(),
                statusLabel(survey.getStatus()),
                survey.getStartAt().format(DATE) + " ~ " + survey.getEndAt().format(DATE),
                survey.isAnonymous(),
                survey.isRequired(),
                survey.isReflectCompletion(),
                targetCount,
                responseCount,
                rate(responseCount, targetCount),
                reports);
    }

    /* ===================== 다운로드 ===================== */

    /**
     * 결과 리포트 xlsx — 시트 세 장.
     *
     * <ol>
     *   <li><b>설문요약</b> — 설문 메타 + 응답률</li>
     *   <li><b>문항별집계</b> — 문항 × 보기(척도값) 별 응답수·비율</li>
     *   <li><b>주관식응답</b> — 집계할 수 없는 서술 답변 원문</li>
     * </ol>
     *
     * <p>주관식을 굳이 별도 시트로 뺀 이유는, 문항별집계 시트에 섞으면 행 길이가 들쭉날쭉해져
     * 피벗·필터가 안 먹기 때문이다.</p>
     */
    public byte[] reportExcel(Long surveyId, Long viewerId, boolean admin) {
        SurveyReportView view = report(surveyId, viewerId, admin);

        try (ExcelWriter writer = ExcelWriter.create()) {
            writer.sheet("설문요약", SUMMARY_HEADERS);
            writer.row("설문명", view.title());
            writer.row("유형", view.type());
            writer.row("대상 과정", view.courseName());
            writer.row("연계 차시", view.sessionName());
            writer.row("상태", view.surveyStatus());
            writer.row("기간", view.period());
            writer.row("익명 설문", view.anonymous());
            writer.row("필수 여부", view.required());
            writer.row("이수 반영", view.reflectCompletion());
            writer.row("대상자 수", view.targetCount() == 0 ? "-" : view.targetCount());
            writer.row("응답 건수", view.responseCount());
            writer.row("응답률", view.responseRate());
            writer.row("문항 수", (long) view.questions().size());

            writer.sheet("문항별집계", QUESTION_HEADERS);
            boolean any = false;
            for (SurveyReportView.QuestionReport q : view.questions()) {
                if (q.options().isEmpty()) {
                    // 주관식 등 — 문항 행만 남기고 항목 칸은 요약으로 채운다
                    writer.row(q.seq(), q.type(), q.content(), q.required() ? "필수" : "선택",
                            q.respondentCount(), q.summary(), null, null);
                    any = true;
                    continue;
                }
                for (SurveyReportView.Option o : q.options()) {
                    writer.row(q.seq(), q.type(), q.content(), q.required() ? "필수" : "선택",
                            q.respondentCount(), o.label(), o.count(), o.ratio());
                    any = true;
                }
            }
            if (!any) {
                writer.emptyNote("문항이 없습니다.");
            }

            writer.sheet("주관식응답", TEXT_HEADERS);
            int textRows = 0;
            for (SurveyReportView.QuestionReport q : view.questions()) {
                int no = 0;
                for (String text : q.texts()) {
                    writer.row(q.seq(), q.content(), ++no, text);
                    textRows++;
                }
            }
            if (textRows == 0) {
                writer.emptyNote("주관식 응답이 없습니다.");
            }
            return writer.toBytes();
        }
    }

    /** 다운로드 파일명에 쓸 설문명. */
    public String surveyTitle(Long surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("설문을 찾을 수 없습니다. id=" + surveyId))
                .getTitle();
    }

    /* ===================== 문항 유형별 집계 ===================== */

    private SurveyReportView.QuestionReport choiceReport(
            SurveyQuestion q, long answered, Map<Long, Long> counts) {

        List<SurveyReportView.Option> options = new ArrayList<>();
        String topLabel = null;
        long topCount = -1;
        for (SurveyChoice choice : q.getChoices()) {
            long count = counts.getOrDefault(choice.getId(), 0L);
            options.add(new SurveyReportView.Option(
                    choice.getSeq() + ". " + choice.getContent(), count, rate(count, answered)));
            if (count > topCount) {
                topCount = count;
                topLabel = choice.getContent();
            }
        }
        String summary = answered == 0 || topLabel == null
                ? "응답 없음"
                : "최다 응답: " + topLabel + " (" + topCount + "건)";
        return new SurveyReportView.QuestionReport(
                q.getSeq(), typeLabel(q.getQuestionType()), q.getContent(), q.isRequired(),
                answered, summary, options, List.of());
    }

    private SurveyReportView.QuestionReport scaleReport(
            SurveyQuestion q, long answered, Map<Long, Long> counts) {

        int max = q.getScaleMax() == null ? 5 : q.getScaleMax();
        List<SurveyReportView.Option> options = new ArrayList<>();
        long total = 0;
        long weighted = 0;
        for (int value = 1; value <= max; value++) {
            long count = counts.getOrDefault((long) value, 0L);
            options.add(new SurveyReportView.Option(value + "점", count, rate(count, answered)));
            total += count;
            weighted += (long) value * count;
        }
        String summary = total == 0
                ? "응답 없음"
                : String.format("평균 %.1f / %d점", (double) weighted / total, max);
        return new SurveyReportView.QuestionReport(
                q.getSeq(), typeLabel(q.getQuestionType()), q.getContent(), q.isRequired(),
                answered, summary, options, List.of());
    }

    private SurveyReportView.QuestionReport textReport(
            SurveyQuestion q, long answered, List<String> texts) {

        String summary = texts.isEmpty() ? "응답 없음" : "주관식 " + texts.size() + "건";
        return new SurveyReportView.QuestionReport(
                q.getSeq(), typeLabel(q.getQuestionType()), q.getContent(), q.isRequired(),
                answered, summary, List.of(), texts);
    }

    /* ===================== 권한 ===================== */

    /**
     * 관리자는 전부, 강사는 담당 과정만. 판정은 A 의 계약({@link CourseQueryService}) 으로만 한다.
     *
     * <p>과정이 없는 전체 대상 설문은 "담당"을 정의할 수 없으므로 강사에게 열지 않는다 —
     * 전 기수 응답이 한 파일에 담기기 때문이다.</p>
     */
    private void ensureCanRead(Survey survey, Long viewerId, boolean admin) {
        if (admin) {
            return;
        }
        if (survey.getCourse() == null || viewerId == null
                || !courseQueryService.isInstructorOf(viewerId, survey.getCourse().getId())) {
            throw new AccessDeniedException("담당하지 않는 과정의 설문 결과입니다.");
        }
    }

    /* ===================== 내부 ===================== */

    private static String rate(long count, long denominator) {
        if (denominator <= 0) {
            return "-";
        }
        return Math.round(count * 100.0 / denominator) + "%";
    }

    private static String typeLabel(SurveyQuestionType type) {
        return switch (type) {
            case SINGLE -> "단일선택";
            case MULTI -> "복수선택";
            case SCALE -> "척도";
            case TEXT -> "주관식";
        };
    }

    private static String typeLabel(Survey.SurveyType type) {
        return switch (type) {
            case COURSE_SATISFACTION -> "만족도";
            case LECTURE_FEEDBACK -> "평가";
            case FACILITY -> "시설";
            case COMPLETION -> "수료";
            case ETC -> "기타";
        };
    }

    private static String statusLabel(Survey.SurveyStatus status) {
        return switch (status) {
            case DRAFT -> "작성중";
            case SCHEDULED -> "응답대기";
            case ONGOING -> "응답중";
            case CLOSED -> "마감";
        };
    }

    private static Map<Long, Long> toLongMap(List<Object[]> rows) {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }

    /** [questionId, key, count] → questionId → (key → count). key 는 choiceId 또는 척도값. */
    private static Map<Long, Map<Long, Long>> toNestedMap(List<Object[]> rows) {
        Map<Long, Map<Long, Long>> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.computeIfAbsent(((Number) row[0]).longValue(), k -> new LinkedHashMap<>())
                    .put(((Number) row[1]).longValue(), ((Number) row[2]).longValue());
        }
        return map;
    }

    private static Map<Long, List<String>> toTextMap(List<Object[]> rows) {
        Map<Long, List<String>> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String text = (String) row[1];
            if (text == null || text.isBlank()) {
                continue;
            }
            map.computeIfAbsent(((Number) row[0]).longValue(), k -> new ArrayList<>()).add(text);
        }
        return map;
    }
}
