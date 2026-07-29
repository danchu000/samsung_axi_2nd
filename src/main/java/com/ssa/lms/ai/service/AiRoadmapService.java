package com.ssa.lms.ai.service;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.job.dto.RoadmapView;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [기능 1] 직무 로드맵 — <b>Claude 의 일반 지식</b>으로 만든다.
 *
 * <h3>이것이 실시간 채용공고가 아니라는 점을 분명히 한다</h3>
 * Claude 는 Messages API 로 <b>웹을 볼 수 없다.</b> 채용 사이트를 실시간으로 읽지 못한다.
 * 그래서 여기서 만드는 것은 <b>"이 직무에 일반적으로 요구되는 역량"</b>이지
 * "이번 주 공고 128건 중 68%가 요구한 항목"이 아니다.
 *
 * <p>그래서 <b>공고 건수·요구 비율을 만들지 않는다.</b> 모델에게 물으면 그럴듯한 숫자를
 * 지어내 주지만, 그건 근거가 없는 수치다. 훈련생이 그 숫자를 보고 진로를 정한다.
 * 화면에도 "일반적인 직무 요구사항"이라고 그대로 적는다.</p>
 *
 * <p>실제 공고 통계가 필요하면 워크넷·사람인 같은 <b>공고 API 를 붙여야 한다</b>
 * ({@code com.ssa.lms.job} 패키지에 수집기가 준비돼 있고 키만 넣으면 동작한다).
 * 그때는 {@code RoadmapService} 가 실제 집계를 주고 이 클래스는 쓰이지 않는다.</p>
 *
 * <h3>비용</h3>
 * 직무 하나당 1회 호출이고 결과는 자주 바뀌지 않는다. 훈련생이 화면을 열 때마다
 * 부르지 않도록, <b>고른 직무 하나만</b> 만든다. 전 직무를 한 번에 만들면
 * 화면 한 번 여는 데 10회 호출이 나간다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiRoadmapService {

    private static final Logger log = LoggerFactory.getLogger(AiRoadmapService.class);

    private static final Set<EnrollmentStatus> LEARNED =
            EnumSet.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    private static final int MAX_OUTPUT_TOKENS = 1200;

    /** 학습 이력으로 넘길 자료 제목 수. 전 차시를 다 넣을 필요는 없다. */
    private static final int MAX_MATERIAL_TITLES = 40;

    private static final Pattern STEP = Pattern.compile(
            "역량\\s*:\\s*(.+?)\\s*\\r?\\n보유\\s*:\\s*(예|아니오)\\s*\\r?\\n설명\\s*:\\s*(.+?)(?=\\r?\\n역량\\s*:|$)",
            Pattern.DOTALL);

    private final AiClient aiClient;
    private final EnrollmentRepository enrollmentRepository;
    private final ContentRepository contentRepository;

    public boolean available() {
        return aiClient.available();
    }

    /**
     * 직무 하나에 대한 로드맵.
     *
     * @param jobName 직무 이름 (예: "백엔드 개발자")
     * @return 실패하면 {@code jobs} 가 빈 목록 — 화면은 안내를 보여준다
     */
    public RoadmapView forTrainee(Long traineeId, String jobName) {
        if (traineeId == null || jobName == null || jobName.isBlank()) {
            return new RoadmapView(null, List.of());
        }

        List<String> learned = learnedTopics(traineeId);
        if (learned.isEmpty()) {
            return new RoadmapView(null, List.of());
        }

        AiAnswer res = aiClient.ask(AiRequest.of("ROADMAP")
                .system(systemPrompt())
                .user(userPrompt(jobName, learned))
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .userId(traineeId)
                .build());

        if (!res.ok()) {
            log.warn("[AI] 로드맵 생성 실패 reason={}", res.reason());
            return new RoadmapView(null, List.of());
        }

        List<RoadmapView.Step> steps = parseSteps(res.text());
        if (steps.isEmpty()) {
            log.warn("[AI] 로드맵 — 모델 답에서 역량을 못 찾음. 형식 확인 필요");
            return new RoadmapView(null, List.of());
        }

        List<String> have = new ArrayList<>();
        List<String> lack = new ArrayList<>();
        for (RoadmapView.Step s : steps) {
            (("done".equals(s.status())) ? have : lack).add(s.title());
        }

        /*
         * postingCount 0, demands 빈 목록 — 실제 공고를 안 봤으니 통계가 없다.
         * 여기에 숫자를 넣으면 훈련생이 "공고 데이터"로 오해한다.
         */
        RoadmapView.Job job = new RoadmapView.Job(
                "ai", jobName, 0, matchRate(have.size(), steps.size()),
                "정보 없음", have, lack, List.of(), steps, List.of());

        return new RoadmapView(LocalDate.now(), List.of(job));
    }

    private int matchRate(int have, int total) {
        return total == 0 ? 0 : (int) Math.round(have * 100.0 / total);
    }

    /** 내가 배운 것 — 수강·수료 과정의 자료 제목. 모델이 "이미 아는 것"을 판단할 근거다. */
    private List<String> learnedTopics(Long traineeId) {
        Set<String> out = new LinkedHashSet<>();
        for (Enrollment e : enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(traineeId)) {
            if (!LEARNED.contains(e.getStatus()) || e.getCourse() == null) continue;
            out.add(e.getCourse().getCourseName());
            for (Content c : contentRepository.findByCourseIdAndStatusOrderByOrderNoAscIdAsc(
                    e.getCourse().getId(), ContentStatus.ACTIVE)) {
                out.add(c.getTitle());
                if (out.size() >= MAX_MATERIAL_TITLES) return List.copyOf(out);
            }
        }
        return List.copyOf(out);
    }

    private String systemPrompt() {
        return """
                당신은 직업훈련 과정의 진로 상담자입니다.
                훈련생이 목표 직무로 가기 위해 갖춰야 할 역량을 순서대로 정리합니다.

                반드시 지킬 것
                1. 채용공고 건수나 "공고의 몇 %가 요구" 같은 수치를 절대 쓰지 마세요.
                   실제 공고를 조회한 것이 아니므로 그런 숫자는 지어낸 것이 됩니다.
                2. 역량은 6~8개로, 기초부터 심화 순서로 정리합니다.
                3. '이미 학습한 내용' 목록과 겹치는 역량은 보유: 예 로 표시합니다.
                4. 설명은 왜 그 역량이 필요한지를 한두 문장으로 씁니다.
                5. 아래 형식만 출력합니다. 다른 말은 붙이지 않습니다.

                역량: (이름)
                보유: (예|아니오)
                설명: (한두 문장)

                역량: ...
                """;
    }

    private String userPrompt(String jobName, List<String> learned) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("목표 직무: ").append(jobName).append("\n\n");
        sb.append("훈련생이 이미 학습한 내용\n");
        for (String t : learned) {
            sb.append("- ").append(t).append('\n');
        }
        return sb.toString();
    }

    private List<RoadmapView.Step> parseSteps(String text) {
        List<RoadmapView.Step> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        boolean currentAssigned = false;

        Matcher m = STEP.matcher(text);
        while (m.find()) {
            String title = m.group(1).trim();
            if (title.isEmpty() || !seen.add(title.toLowerCase(Locale.ROOT))) continue;

            boolean has = "예".equals(m.group(2).trim());
            String reason = m.group(3).trim().replaceAll("\\s+", " ");

            String status;
            if (has) {
                status = "done";
            } else if (!currentAssigned) {
                status = "current";     // 아직 없는 것 중 첫 번째가 "지금 할 일"
                currentAssigned = true;
            } else {
                status = "locked";
            }

            out.add(new RoadmapView.Step(title,
                    has ? "학습 자료 있음" : "다음 단계", reason, status));
        }
        return out;
    }
}
