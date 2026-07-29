package com.ssa.lms.ai.service;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.DiagnosisView;
import com.ssa.lms.ai.entity.AiUsageLog;
import com.ssa.lms.ai.repository.AiUsageLogRepository;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [기능 4] AI 학습진단 — <b>훈련생이 물어본 것</b>에서 취약 영역을 뽑는다.
 *
 * <h3>질문이 있을 때만 부른다</h3>
 * 훈련생이 오늘 아무것도 안 물었으면 어제와 달라질 게 없다. 그런데 강사가 화면을
 * 열 때마다 모델을 부르면, 아무 일도 없는 날에도 돈이 나간다.
 * 그래서 <b>기간 내 질문이 0건이면 모델을 부르지 않고</b> "아직 질문 없음"을 돌려준다.
 *
 * <h3>무엇을 근거로 하나</h3>
 * {@code AiUsageLog} 에 <b>암호화 저장된 훈련생 질문</b>이다.
 * 답변은 저장하지 않는다 — 진단에 필요한 것은 "무엇을 어려워하는가"이지 답이 아니다.
 * 질문 본문은 암호문이라 DB 에서 분류할 수 없어, 기간치를 읽어 모델에게 넘긴다.
 *
 * <h3>권한</h3>
 * <b>담당 과정의 질문만</b> 본다. 막지 않으면 남의 과정 훈련생이 뭘 물었는지가 보인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(AiDiagnosisService.class);

    /** 진단 대상 기간. 하루치만 보면 표본이 너무 작아 주제가 안 잡힌다. */
    private static final int WINDOW_DAYS = 14;

    /** 모델에 넘길 질문 수 상한. 넘치면 최근 것부터 쓴다 — 토큰은 비용이다. */
    private static final int MAX_QUESTIONS = 120;

    private static final int MAX_OUTPUT_TOKENS = 1200;

    /** 모델 답에서 훈련생 블록을 찾는 패턴. */
    private static final Pattern ROW = Pattern.compile(
            "훈련생id\\s*:\\s*(\\d+)\\s*\\r?\\n시급도\\s*:\\s*(높음|보통|낮음)\\s*\\r?\\n"
          + "취약영역\\s*:\\s*(.*?)\\s*\\r?\\n근거\\s*:\\s*(.*?)\\s*\\r?\\n추천과제\\s*:\\s*(.*?)(?=\\r?\\n훈련생id\\s*:|$)",
            Pattern.DOTALL);

    private static final Pattern TOPIC = Pattern.compile(
            "주제\\s*:\\s*(.+?)\\s*\\|\\s*건수\\s*:\\s*(\\d+)\\s*\\|\\s*인원\\s*:\\s*(\\d+)");

    private final AiClient aiClient;
    private final AiUsageLogRepository usageRepository;
    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public boolean available() {
        return aiClient.available();
    }

    /**
     * 강사의 담당 과정 진단.
     *
     * @return 질문이 없으면 {@link DiagnosisView#empty()} — <b>모델을 부르지 않는다</b>
     */
    public DiagnosisView forInstructor(Long instructorId) {
        if (instructorId == null) return DiagnosisView.empty();

        List<Long> myCourseIds = courseRepository.findAll().stream()
                .map(Course::getId)
                .filter(id -> courseQueryService.isInstructorOf(instructorId, id))
                .toList();
        if (myCourseIds.isEmpty()) return DiagnosisView.empty();

        LocalDateTime from = LocalDateTime.now().minusDays(WINDOW_DAYS);
        List<AiUsageLog> logs = usageRepository.findQuestionsSince(myCourseIds, from);

        /*
         * 여기가 비용 방어선이다. 질문이 없으면 분석할 것도 없다 —
         * 강사가 화면을 새로고침할 때마다 모델을 부르면 아무 일 없는 날에도 돈이 나간다.
         */
        if (logs.isEmpty()) {
            return DiagnosisView.empty();
        }

        if (logs.size() > MAX_QUESTIONS) {
            logs = logs.subList(logs.size() - MAX_QUESTIONS, logs.size());
        }

        Map<Long, String> traineeNames = namesOf(logs);
        Map<Long, String> courseNames = new LinkedHashMap<>();
        courseRepository.findAllById(myCourseIds)
                .forEach(c -> courseNames.put(c.getId(), c.getCourseName()));

        AiAnswer res = aiClient.ask(AiRequest.of("DIAGNOSIS")
                .system(systemPrompt())
                .user(userPrompt(logs, traineeNames, courseNames))
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .userId(instructorId)
                .build());

        if (!res.ok()) {
            log.warn("[AI] 진단 실패 reason={}", res.reason());
            return DiagnosisView.empty();
        }

        List<DiagnosisView.Row> rows = parseRows(res.text(), traineeNames, courseNames, logs);
        List<DiagnosisView.Topic> topics = parseTopics(res.text());

        if (rows.isEmpty()) {
            log.warn("[AI] 진단 — 모델 답에서 훈련생을 못 찾음. 형식 확인 필요");
            return DiagnosisView.empty();
        }
        return new DiagnosisView(LocalDate.now(), logs.size(), topics, rows);
    }

    private Map<Long, String> namesOf(List<AiUsageLog> logs) {
        List<Long> ids = logs.stream().map(AiUsageLog::getUserId).filter(v -> v != null).distinct().toList();
        Map<Long, String> out = new LinkedHashMap<>();
        userRepository.findAllById(ids).forEach(u -> out.put(u.getId(), u.getName()));
        return out;
    }

    private String systemPrompt() {
        return """
                당신은 직업훈련 과정의 학습 분석가입니다.
                훈련생들이 AI 학습 도우미에 남긴 질문을 보고, 누가 어느 영역을 어려워하는지 진단합니다.

                반드시 지킬 것
                1. 제시된 질문에서 실제로 드러난 것만 씁니다. 질문에 없는 내용을 추측하지 않습니다.
                2. 질문이 1~2건뿐인 훈련생은 시급도를 '낮음'으로 둡니다. 적은 질문으로 단정하면 안 됩니다.
                3. 근거에는 반드시 구체적인 사실을 씁니다. ("트랜잭션 관련 질문 5회" 처럼)
                4. 추천과제는 제목만 25자 이내로 씁니다. 내용은 쓰지 않습니다.
                5. 아래 형식만 출력합니다. 다른 말은 붙이지 않습니다.

                [주제]
                주제: (주제명) | 건수: (숫자) | 인원: (숫자)
                주제: ...

                [훈련생]
                훈련생id: (숫자)
                시급도: (높음|보통|낮음)
                취약영역: (쉼표로 구분, 최대 2개)
                근거: (한 문장)
                추천과제: (제목)

                훈련생id: ...
                """;
    }

    private String userPrompt(List<AiUsageLog> logs, Map<Long, String> names,
                              Map<Long, String> courses) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("최근 ").append(WINDOW_DAYS).append("일간 훈련생 질문 ")
          .append(logs.size()).append("건입니다.\n\n");

        // 훈련생별로 묶어 준다. 뒤섞어 주면 모델이 누가 뭘 물었는지 놓친다
        Map<Long, List<AiUsageLog>> byTrainee = new LinkedHashMap<>();
        for (AiUsageLog l : logs) {
            if (l.getUserId() == null) continue;
            byTrainee.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(l);
        }

        for (Map.Entry<Long, List<AiUsageLog>> e : byTrainee.entrySet()) {
            sb.append("훈련생id ").append(e.getKey())
              .append(" (").append(names.getOrDefault(e.getKey(), "이름없음")).append(")\n");
            for (AiUsageLog l : e.getValue()) {
                sb.append("  - [").append(courses.getOrDefault(l.getCourseId(), "과정미상")).append("] ")
                  .append(l.getQuestion()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 모델 답을 실제 훈련생과 대조한다.
     * 목록에 없는 id 는 버린다 — 지어낸 훈련생에게 과제를 배부할 수는 없다.
     */
    private List<DiagnosisView.Row> parseRows(String text, Map<Long, String> names,
                                              Map<Long, String> courses, List<AiUsageLog> logs) {
        // 훈련생이 주로 질문한 과정을 붙여 준다 — 과제 배부는 과정 단위로 이뤄진다
        Map<Long, Long> mainCourse = new LinkedHashMap<>();
        for (AiUsageLog l : logs) {
            if (l.getUserId() != null && l.getCourseId() != null) {
                mainCourse.putIfAbsent(l.getUserId(), l.getCourseId());
            }
        }

        List<DiagnosisView.Row> out = new ArrayList<>();
        Matcher m = ROW.matcher(text);
        while (m.find()) {
            long id;
            try {
                id = Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            String name = names.get(id);
            if (name == null) continue;   // 지어낸 훈련생은 버린다

            String levelLabel = m.group(2);
            String level = switch (levelLabel) {
                case "높음" -> "high";
                case "보통" -> "mid";
                default -> "low";
            };

            List<String> weak = new ArrayList<>();
            for (String w : m.group(3).split(",")) {
                String t = w.trim();
                if (!t.isEmpty()) weak.add(t);
            }

            Long courseId = mainCourse.get(id);
            out.add(new DiagnosisView.Row(id, name, courseId,
                    courses.getOrDefault(courseId, ""), level, levelLabel,
                    weak, m.group(4).trim(), m.group(5).trim()));
        }

        // 시급도 높은 순 — 급한 훈련생이 아래에 있으면 못 보고 지나친다
        out.sort((a, b) -> rank(a.level()) - rank(b.level()));
        return out;
    }

    private int rank(String level) {
        return switch (level) {
            case "high" -> 0;
            case "mid" -> 1;
            default -> 2;
        };
    }

    private List<DiagnosisView.Topic> parseTopics(String text) {
        List<DiagnosisView.Topic> out = new ArrayList<>();
        Matcher m = TOPIC.matcher(text);
        while (m.find()) {
            try {
                out.add(new DiagnosisView.Topic(m.group(1).trim(),
                        Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
            } catch (NumberFormatException ignored) {
                // 숫자가 아니면 그 줄만 버린다 — 한 줄 때문에 전체를 잃지 않는다
            }
        }
        out.sort((a, b) -> b.count() - a.count());
        return out;
    }
}
