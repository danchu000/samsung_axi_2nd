package com.ssa.lms.job.service;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import com.ssa.lms.job.config.SaraminProperties;
import com.ssa.lms.job.dto.JobCollectionSummary;
import com.ssa.lms.job.dto.RoadmapView;
import com.ssa.lms.job.entity.JobPosting;
import com.ssa.lms.job.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * [기능 1] 직무 로드맵 계산.
 *
 * <p><b>어디까지 실제 값인가 — 정확히 말한다</b>
 * <ul>
 *   <li>✅ 공고 수·요구 역량 순위·비율 — 수집한 공고에서 <b>직접 센 값</b>이다.
 *       사람인이 주는 {@code keyword} 필드를 그대로 집계한다. AI 를 쓰지 않는다 —
 *       제공되는 값이 있는데 모델을 부르면 돈만 나가고 더 부정확하다</li>
 *   <li>✅ 원문 공고 — 저장된 실제 공고. 링크로 확인할 수 있다</li>
 *   <li>⚠ 보유/부족 판정 — <b>내 학습 자료의 제목·설명에 그 단어가 있는지</b>로 본다.
 *       "Docker 가 자료 제목에 있다 = Docker 를 할 줄 안다"는 아니다.
 *       정확히 하려면 자료 본문과 평가 결과까지 봐야 한다(벡터 검색 단계)</li>
 * </ul>
 *
 * <p><b>표본이 작으면 비율을 말하지 않는다</b><br>
 * 공고 3건 중 2건도 67% 다. 그 숫자로 "이 역량이 중요하다"고 하면 거짓이다.
 * 그래서 퍼센트와 건수를 <b>항상 같이</b> 내려보내고, 표본이 {@link #MIN_SAMPLE} 미만이면
 * 그 직무를 아예 빼 버린다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadmapService {

    /** 이 미만이면 비율을 말할 수 없다. 표본이 작은 통계는 없는 것보다 나쁘다. */
    private static final int MIN_SAMPLE = 5;

    /** 화면에 보여줄 요구 역량 개수. 꼬리까지 늘어놓으면 뭘 봐야 할지 모른다. */
    private static final int TOP_DEMANDS = 12;

    /** 근거로 붙일 원문 공고 수. */
    private static final int SHOW_POSTINGS = 5;

    private static final Set<EnrollmentStatus> LEARNED =
            EnumSet.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    private final JobPostingRepository jobPostingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ContentRepository contentRepository;
    private final SaraminProperties props;

    /**
     * 훈련생의 로드맵.
     *
     * @return 수집된 공고가 없으면 {@code jobs} 가 빈 목록이고 {@code collectedAt} 이 null.
     *         화면은 "아직 수집된 공고가 없습니다"를 보여준다 — 가짜로 채우지 않는다.
     */
    public RoadmapView forTrainee(Long traineeId) {
        Optional<LocalDate> collectedAt = jobPostingRepository.findLastCollectedAt();
        if (collectedAt.isEmpty()) {
            return new RoadmapView(null, List.of());
        }

        Set<String> mySkills = skillsOf(traineeId);
        LocalDate since = LocalDate.now().minusDays(props.getFreshnessDays());

        List<RoadmapView.Job> jobs = new ArrayList<>();
        for (String group : props.getGroups().keySet()) {
            analyze(group, since, mySkills).ifPresent(jobs::add);
        }
        return new RoadmapView(collectedAt.get(), jobs);
    }

    /**
     * [기능 1-관리자] 수집 현황 요약.
     *
     * <p>훈련생 화면과 <b>같은 집계 함수</b>를 쓴다. 관리자와 훈련생이 다른 숫자를 보면
     * 어느 쪽도 못 믿는다. 다른 것은 "보유/부족" 판정뿐이다 — 그건 개인별이라
     * 관리자 화면에는 의미가 없다.</p>
     */
    public JobCollectionSummary collectionSummary() {
        Optional<LocalDate> last = jobPostingRepository.findLastCollectedAt();
        long total = jobPostingRepository.count();

        if (last.isEmpty()) {
            return new JobCollectionSummary(props.isUsable(), null, null, total,
                    List.of(), List.of());
        }

        LocalDate since = LocalDate.now().minusDays(props.getFreshnessDays());
        int stale = (int) ChronoUnit.DAYS.between(last.get(), LocalDate.now());

        List<JobCollectionSummary.GroupSummary> groups = new ArrayList<>();
        Map<String, Integer> allSkills = new LinkedHashMap<>();
        int allPostings = 0;

        for (String group : props.getGroups().keySet()) {
            List<JobPosting> postings = jobPostingRepository.findRecent(group, since);
            Map<String, Integer> counts = countKeywords(postings);
            allPostings += postings.size();
            counts.forEach((k, v) -> allSkills.merge(k, v, Integer::sum));

            groups.add(new JobCollectionSummary.GroupSummary(
                    group, props.nameOf(group), postings.size(),
                    postings.size() >= MIN_SAMPLE,
                    topSkills(counts, postings.size(), 3)));
        }

        return new JobCollectionSummary(props.isUsable(), last.get(), stale, total,
                groups, topSkills(allSkills, allPostings, 10));
    }

    /** 한 공고 안의 중복 키워드는 1건으로 센다 — 나열이 많은 공고 하나가 순위를 흔든다. */
    private Map<String, Integer> countKeywords(List<JobPosting> postings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JobPosting p : postings) {
            for (String kw : distinct(p.keywordList())) {
                counts.merge(kw, 1, Integer::sum);
            }
        }
        return counts;
    }

    private List<JobCollectionSummary.SkillCount> topSkills(Map<String, Integer> counts,
                                                            int total, int limit) {
        if (total <= 0) return List.of();
        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> new JobCollectionSummary.SkillCount(
                        e.getKey(), e.getValue(),
                        (int) Math.round(e.getValue() * 100.0 / total)))
                .toList();
    }

    private Optional<RoadmapView.Job> analyze(String group, LocalDate since, Set<String> mySkills) {
        List<JobPosting> postings = jobPostingRepository.findRecent(group, since);
        if (postings.size() < MIN_SAMPLE) {
            return Optional.empty();   // 표본이 작으면 비율을 말하지 않는다
        }

        Map<String, Integer> counts = countKeywords(postings);

        int total = postings.size();
        List<RoadmapView.Demand> demands = counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(TOP_DEMANDS)
                .map(e -> new RoadmapView.Demand(
                        e.getKey(),
                        (int) Math.round(e.getValue() * 100.0 / total),
                        e.getValue(),
                        hasSkill(mySkills, e.getKey())))
                .toList();

        List<String> have = demands.stream().filter(RoadmapView.Demand::mine)
                .map(RoadmapView.Demand::label).toList();
        List<String> lack = demands.stream().filter(d -> !d.mine())
                .map(RoadmapView.Demand::label).toList();

        int matchRate = demands.isEmpty() ? 0
                : (int) Math.round(have.size() * 100.0 / demands.size());

        List<RoadmapView.Posting> shown = postings.stream()
                .limit(SHOW_POSTINGS)
                .map(p -> new RoadmapView.Posting(p.getCompanyName(), p.getTitle(), p.getUrl(),
                        p.getKeywords(), p.getPostingDate()))
                .toList();

        return Optional.of(new RoadmapView.Job(
                group, props.nameOf(group),
                total, matchRate, topExperience(postings), have, lack,
                demands, steps(demands), shown));
    }

    /**
     * 공고가 가장 많이 요구한 경력 구간.
     *
     * <p>평균이 아니라 <b>최빈값</b>이다. "신입~3년"처럼 구간 문자열로 오기 때문에
     * 숫자로 평균을 낼 수 없다. 억지로 평균처럼 보이게 만들면 없는 정밀도를 꾸미는 것이다.</p>
     */
    private String topExperience(List<JobPosting> postings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (JobPosting p : postings) {
            String v = p.getExperienceLevel();
            if (v != null && !v.isBlank()) counts.merge(v.trim(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("정보 없음");   // 공고에 경력 표기가 없으면 지어내지 않는다
    }

    /**
     * 학습 단계 — <b>AI 가 계획을 세운 것이 아니다.</b>
     *
     * <p>요구 빈도 순으로 줄을 세운 것뿐이다. 이미 학습한 것(done)을 앞에 두고,
     * 아직 없는 것 중 <b>가장 많이 요구되는 것</b>을 다음 할 일(current)로 놓는다.
     * 그래서 이유에는 반드시 근거 숫자를 적는다 — "공고 68%(34건)가 요구".
     * 근거 없이 순서만 주면 왜 그 순서인지 알 수 없다.</p>
     */
    private List<RoadmapView.Step> steps(List<RoadmapView.Demand> demands) {
        List<RoadmapView.Step> out = new ArrayList<>();

        // 이미 갖춘 것 — 상위 3개만. 다 나열하면 정작 할 일이 안 보인다
        demands.stream().filter(RoadmapView.Demand::mine).limit(3).forEach(d ->
                out.add(new RoadmapView.Step(
                        d.label(),
                        "학습 자료 있음 · 공고 " + d.percent() + "%가 요구",
                        "수강 중인 과정 자료에서 다루고 있어요. 공고 " + d.count() + "건이 요구하는 항목이에요.",
                        "done")));

        // 채워야 할 것 — 요구 빈도가 높은 순
        List<RoadmapView.Demand> todo = demands.stream()
                .filter(d -> !d.mine()).limit(5).toList();
        for (int i = 0; i < todo.size(); i++) {
            RoadmapView.Demand d = todo.get(i);
            out.add(new RoadmapView.Step(
                    d.label(),
                    "공고 " + d.percent() + "%(" + d.count() + "건)가 요구",
                    i == 0
                        ? "아직 학습 자료가 없는 항목 중 요구 빈도가 가장 높아요. 여기부터 채우면 효과가 커요."
                        : "공고 " + d.count() + "건이 요구하는 항목이에요. 앞 단계를 마친 뒤 이어서 하면 좋아요.",
                    i == 0 ? "current" : "locked"));
        }
        return out;
    }

    /**
     * 내가 학습한 것으로 볼 역량 — <b>수강 중·수료한 과정의 콘텐츠 제목·설명</b>.
     *
     * <p>본문은 읽지 않는다. 그래서 이 판정은 근사치다.
     * 실제 이해도까지 보려면 평가 결과와 자료 본문이 필요하다.</p>
     */
    private Set<String> skillsOf(Long traineeId) {
        Set<String> text = new java.util.HashSet<>();
        for (Enrollment e : enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(traineeId)) {
            if (!LEARNED.contains(e.getStatus()) || e.getCourse() == null) continue;

            text.add(lower(e.getCourse().getCourseName()));
            for (Content c : contentRepository.findByCourseIdAndStatusOrderByOrderNoAscIdAsc(
                    e.getCourse().getId(), ContentStatus.ACTIVE)) {
                text.add(lower(c.getTitle()));
                if (c.getDescription() != null) text.add(lower(c.getDescription()));
            }
        }
        return text;
    }

    /** 대소문자·공백 차이로 놓치지 않게 정규화해서 부분 일치로 본다. */
    private boolean hasSkill(Set<String> myTexts, String keyword) {
        String k = lower(keyword).replace(" ", "");
        if (k.isEmpty()) return false;
        return myTexts.stream().anyMatch(t -> t.replace(" ", "").contains(k));
    }

    private String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private List<String> distinct(List<String> src) {
        return src.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }
}
