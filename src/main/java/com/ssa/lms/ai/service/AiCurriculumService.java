package com.ssa.lms.ai.service;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.CurriculumAdvice;
import com.ssa.lms.attendance.service.AttendanceService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.grading.service.GradeQueryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [기능 2] 학습 데이터 기반 맞춤 커리큘럼 추천.
 *
 * <h3>추천 범위는 원내 과정뿐</h3>
 * 모델에게 <b>모집중인 원내 과정 목록을 주고 그 안에서만 고르게</b> 한다.
 * 그리고 돌아온 답을 <b>실제 과정 id 와 다시 대조해</b> 목록에 없는 과정은 버린다.
 * 프롬프트로만 제한하면 모델이 그럴듯한 외부 강의를 지어낼 수 있는데,
 * 훈련생은 그게 우리 과정인지 아닌지 구분할 수 없다.
 *
 * <h3>집계는 SQL, 판단만 모델</h3>
 * 진도·성적·출석은 <b>실제 값을 계산해서</b> 모델에게 준다. 숫자를 모델에게 세게 하면
 * 돈은 돈대로 나가고 틀린다. 모델이 하는 일은 "이 데이터면 어느 과정이 맞는가"와
 * 그 이유를 문장으로 쓰는 것뿐이다.
 *
 * <h3>비용</h3>
 * 훈련생이 화면을 열 때마다 부르면 인원 × 방문 횟수만큼 나간다.
 * 결과를 저장해 하루 1회로 묶는 것이 다음 단계다 — 지금은 호출 자체가 성립하는지부터 본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiCurriculumService {

    private static final Logger log = LoggerFactory.getLogger(AiCurriculumService.class);

    private static final Set<EnrollmentStatus> LEARNING =
            EnumSet.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    /** 신청 이력이 있는 상태 — 이미 넣은 과정을 "새로 신청하세요"라고 하면 안 된다. */
    private static final Set<EnrollmentStatus> APPLIED_ANY =
            EnumSet.of(EnrollmentStatus.APPLIED, EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    /** 추천 개수. 너무 많으면 고르지 못하고 아무것도 안 누른다. */
    private static final int MAX_RECOMMEND = 3;

    private static final int MAX_OUTPUT_TOKENS = 800;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 모델 답에서 과정 블록을 찾는 패턴. */
    private static final Pattern BLOCK = Pattern.compile(
            "과정id\\s*:\\s*(\\d+)\\s*\\r?\\n적합도\\s*:\\s*(\\d{1,3})\\s*\\r?\\n이유\\s*:\\s*([\\s\\S]*?)(?=\\n과정id\\s*:|$)");

    private final AiClient aiClient;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final GradeQueryService gradeQueryService;
    private final AttendanceService attendanceService;

    public boolean available() {
        return aiClient.available();
    }

    public CurriculumAdvice recommend(Long traineeId) {
        if (traineeId == null) return CurriculumAdvice.fail("로그인이 필요해요.");

        List<Enrollment> mine = enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(traineeId);
        List<Enrollment> learning = mine.stream()
                .filter(e -> LEARNING.contains(e.getStatus()) && e.getCourse() != null)
                .toList();

        if (learning.isEmpty()) {
            return CurriculumAdvice.fail("수강 중인 과정이 없어요. 과정을 먼저 신청해 주세요.");
        }

        // 이미 신청한 과정은 추천 후보에서 뺀다 — 신청 화면에 가도 또 신청할 수 없다
        Set<Long> alreadyApplied = mine.stream()
                .filter(e -> APPLIED_ANY.contains(e.getStatus()) && e.getCourse() != null)
                .map(e -> e.getCourse().getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        List<Course> candidates = courseRepository.findAll().stream()
                .filter(c -> c.getStatus() == CourseStatus.RECRUITING)
                .filter(c -> !alreadyApplied.contains(c.getId()))
                .toList();

        if (candidates.isEmpty()) {
            return CurriculumAdvice.fail("지금 신청할 수 있는 모집중 과정이 없어요. 새 과정이 열리면 알려드릴게요.");
        }

        List<CurriculumAdvice.Stat> stats = statsOf(traineeId, learning);

        AiAnswer res = aiClient.ask(AiRequest.of("CURRICULUM")
                .system(systemPrompt())
                .user(userPrompt(stats, learning, candidates))
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .userId(traineeId)
                .build());

        if (!res.ok()) {
            return new CurriculumAdvice(false, LocalDate.now(), stats, List.of(), res.userMessage());
        }

        List<CurriculumAdvice.Recommend> picked = parse(res.text(), candidates, alreadyApplied);
        if (picked.isEmpty()) {
            log.warn("[AI] 커리큘럼 추천 — 모델 답에서 유효한 과정을 못 찾음. 형식 확인 필요");
            return new CurriculumAdvice(false, LocalDate.now(), stats, List.of(),
                    "추천을 만들지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
        return new CurriculumAdvice(true, LocalDate.now(), stats, picked, null);
    }

    /** 판단의 입력값 — <b>실제 계산한 값</b>이다. 모델에게 세게 하지 않는다. */
    private List<CurriculumAdvice.Stat> statsOf(Long traineeId, List<Enrollment> learning) {
        List<CurriculumAdvice.Stat> out = new ArrayList<>();

        int progressSum = 0, attendSum = 0, attendCount = 0;
        double scoreSum = 0; int scoreCount = 0;

        for (Enrollment e : learning) {
            progressSum += (int) Math.round(e.getProgressRate());

            Double avg = gradeQueryService.averageConfirmedScore(traineeId, e.getCourse().getId());
            if (avg != null) { scoreSum += avg; scoreCount++; }

            int rate = attendanceService.attendanceRate(traineeId, e.getCourse().getId());
            if (rate >= 0) { attendSum += rate; attendCount++; }
        }

        out.add(new CurriculumAdvice.Stat("평균 진도율",
                (progressSum / learning.size()) + "%",
                "수강 과정 " + learning.size() + "개"));

        // 확정 성적이 없으면 "0점"이 아니라 "아직 없음"이다 — 0점으로 보이면 낙담한다
        out.add(new CurriculumAdvice.Stat("평균 평가 점수",
                scoreCount == 0 ? "—" : Math.round(scoreSum / scoreCount) + "점",
                scoreCount == 0 ? "확정된 성적 없음" : "확정 성적 " + scoreCount + "개 과정"));

        out.add(new CurriculumAdvice.Stat("출석률",
                attendCount == 0 ? "—" : (attendSum / attendCount) + "%",
                attendCount == 0 ? "출결 기록 없음" : "수강 과정 기준"));

        return out;
    }

    private String systemPrompt() {
        return """
                당신은 직업훈련기관의 학습 상담자입니다. 훈련생의 학습 데이터를 보고
                '우리 기관이 개설한 모집중 과정' 중에서 다음에 들으면 좋을 과정을 고릅니다.

                반드시 지킬 것
                1. 제시된 '신청 가능한 과정' 목록에 있는 과정만 고릅니다.
                   목록에 없는 과정이나 외부 강의는 절대 추천하지 않습니다.
                2. 최대 3개까지만 고릅니다. 맞는 과정이 하나뿐이면 하나만 고릅니다.
                3. 이유는 훈련생의 실제 수치를 근거로 씁니다. ("진도율이 55%라 ...")
                4. 아래 형식만 출력합니다. 다른 말은 붙이지 않습니다.

                과정id: (숫자)
                적합도: (0~100 숫자)
                이유: (2~3문장. 왜 이 훈련생에게 이 과정인지)

                과정id: ...
                """;
    }

    private String userPrompt(List<CurriculumAdvice.Stat> stats,
                              List<Enrollment> learning, List<Course> candidates) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("[훈련생 학습 데이터]\n");
        for (CurriculumAdvice.Stat s : stats) {
            sb.append("- ").append(s.label()).append(": ").append(s.value())
              .append(" (").append(s.sub()).append(")\n");
        }

        sb.append("\n[현재 수강 중인 과정]\n");
        for (Enrollment e : learning) {
            sb.append("- ").append(e.getCourse().getCourseName())
              .append(" (진도 ").append(Math.round(e.getProgressRate())).append("%)\n");
        }

        sb.append("\n[신청 가능한 과정 — 이 안에서만 고르세요]\n");
        for (Course c : candidates) {
            sb.append("과정id ").append(c.getId()).append(" · ").append(c.getCourseName());
            if (c.getCategory() != null) sb.append(" [").append(c.getCategory()).append(']');
            if (c.getDescription() != null && !c.getDescription().isBlank()) {
                String d = c.getDescription();
                sb.append(" — ").append(d.length() > 120 ? d.substring(0, 120) + "…" : d);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 모델 답을 실제 과정과 대조한다.
     *
     * <p><b>여기가 핵심 방어선이다.</b> 프롬프트로 "목록에서만 고르라"고 해도 모델은
     * 없는 과정을 지어낼 수 있다. id 가 후보 목록에 없으면 버린다 —
     * 없는 과정을 추천하면 훈련생이 신청하러 갔다가 못 찾는다.</p>
     */
    private List<CurriculumAdvice.Recommend> parse(String text, List<Course> candidates,
                                                   Set<Long> alreadyApplied) {
        Map<Long, Course> byId = new LinkedHashMap<>();
        candidates.forEach(c -> byId.put(c.getId(), c));

        List<CurriculumAdvice.Recommend> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        Matcher m = BLOCK.matcher(text);
        while (m.find() && out.size() < MAX_RECOMMEND) {
            Long id;
            int fit;
            try {
                id = Long.parseLong(m.group(1));
                fit = Math.max(0, Math.min(100, Integer.parseInt(m.group(2))));
            } catch (NumberFormatException e) {
                continue;
            }

            Course c = byId.get(id);
            if (c == null || !seen.add(id)) continue;   // 지어낸 과정·중복은 버린다

            List<String> reasons = new ArrayList<>();
            for (String line : m.group(3).split("\\r?\\n")) {
                String t = line.trim().replaceFirst("^[-·•]\\s*", "");
                if (!t.isEmpty()) reasons.add(t);
            }

            out.add(new CurriculumAdvice.Recommend(
                    c.getId(), c.getCourseName(), period(c), seats(c), fit,
                    reasons, alreadyApplied.contains(c.getId())));
        }
        return out;
    }

    private String period(Course c) {
        if (c.getStartDate() == null || c.getEndDate() == null) return "일정 미정";
        return c.getStartDate().format(YMD) + " ~ " + c.getEndDate().format(YMD);
    }

    private String seats(Course c) {
        return c.getCapacity() <= 0 ? "모집중" : "정원 " + c.getCapacity() + "명 · 모집중";
    }
}
