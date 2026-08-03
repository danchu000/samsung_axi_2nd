package com.ssa.lms.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채용공고 수집 설정. [기능 1] 직무 로드맵의 원천.
 *
 * <p><b>수집처를 두 곳 지원한다.</b> 키가 있는 곳만 실제로 부른다.
 * <ul>
 *   <li><b>워크넷</b>(고용노동부 공공데이터) — 공공데이터포털에서 <b>키가 즉시 발급</b>된다.
 *       무료·공식이라 훈련기관 프로젝트에 가장 적합하다</li>
 *   <li><b>사람인</b> 오픈 API — 민간 공고가 더 많지만 기업회원 가입과 승인이 필요하다</li>
 * </ul>
 * 둘 다 켜면 양쪽 공고가 합쳐져 표본이 커진다.</p>
 *
 * <p><b>채용 사이트를 직접 크롤링하지 않는다.</b> 사람인·잡코리아 등은 이용약관에서
 * 자동 수집을 금지하고 있어, 정부 제출 사업에서 쓰면 그 자체가 문제가 된다.
 * 공개 API 만 쓴다.</p>
 *
 * <p><b>키는 저장소에 넣지 않는다.</b> 환경변수(.env)로만 주입한다.</p>
 *
 * <p><b>왜 검색어를 설정으로 빼는가</b><br>
 * 훈련 과정이 바뀌면 볼 직무도 바뀐다. 코드를 고쳐 배포하지 않고 설정만 바꿔
 * 대응할 수 있어야 한다. 키는 직무 그룹 id(화면 탭), 값은 검색 키워드다.</p>
 */
@ConfigurationProperties(prefix = "lms.job")
public class JobProperties {

    private boolean enabled = false;

    /** 사람인 오픈 API 액세스 키. 환경변수 SARAMIN_ACCESS_KEY. 없으면 사람인은 건너뛴다. */
    private String saraminKey = "";

    private String saraminBaseUrl = "https://oapi.saramin.co.kr";

    /**
     * 워크넷(공공데이터포털) 인증키. 환경변수 WORKNET_API_KEY. 없으면 워크넷은 건너뛴다.
     * 발급: https://www.data.go.kr → "한국고용정보원 워크넷 채용정보" → 활용신청(즉시 승인)
     */
    private String worknetKey = "";

    private String worknetBaseUrl = "https://openapi.work.go.kr";

    /* ===== AI 웹 검색 수집 (보조 수집처) ===== */

    /**
     * Claude 웹 검색으로 공고를 수집할지.
     *
     * <p><b>왜 필요한가</b> — 워크넷은 즉시 발급되지만 <b>기술 키워드 필드를 주지 않는다.</b>
     * 사람인은 주지만 기업회원 승인이 필요해 바로 못 쓴다. 그 사이를 메운다.</p>
     *
     * <p><b>API 키는 여기 없다.</b> AI 기능과 같은 {@code ANTHROPIC_API_KEY} 를 쓴다.
     * 이 값이 true 인데 AI 키가 없으면 수집기는 조용히 건너뛰고,
     * <b>관리자 대시보드의 AI 배너가 "API 키가 등록되지 않았습니다"로 알려준다.</b></p>
     */
    private boolean claudeSearch = false;

    /**
     * AI 로 볼 직무 그룹 id (쉼표 구분). <b>비우면 아무 직무도 보지 않는다.</b>
     *
     * <p>일부러 화이트리스트다. 검색 한 번이 공고 원문 여러 개를 열어 읽기 때문에
     * 직무를 늘리는 만큼 비용이 그대로 늘어난다. 기본값을 "전체"로 두면
     * 켜는 순간 10개 직무가 한꺼번에 돌아 요금이 예상 밖으로 나온다.</p>
     */
    private String claudeGroups = "";

    /**
     * AI 수집 시 직무당 목표 건수.
     *
     * <p>{@code RoadmapService.MIN_SAMPLE} 이 5라 5건은 아슬아슬하다 —
     * 한 건만 원문을 못 열어도 그 직무 탭이 통째로 사라진다. 여유를 둔다.</p>
     */
    private int claudeCount = 8;

    /**
     * 검색을 허용할 도메인 (쉼표 구분). 비우면 제한하지 않는다.
     *
     * <p>이 프로젝트는 <b>채용 사이트를 직접 크롤링하지 않는다</b>는 원칙을 지켜 왔다.
     * 웹 검색은 우리가 크롤러를 돌리는 것과는 다르지만 성격상 회색지대라,
     * 필요하면 여기서 좁힐 수 있게 열어 둔다. 정부 제출 사업이므로 판단이 필요하면
     * 좁히는 쪽이 안전하다.</p>
     */
    private String claudeAllowedDomains = "";

    /**
     * 직무 그룹 id → 표기명·검색어.
     *
     * <p>그룹 id 는 화면 탭 id 이자 DB 의 {@code job_group} 값이다.
     * <b>함부로 바꾸면 이미 수집한 공고와 연결이 끊긴다.</b></p>
     *
     * <p>표기명을 검색어와 <b>같은 곳</b>에 둔다. 예전엔 표기명이 자바 코드에
     * 따로 박혀 있어서, 설정에 직무를 추가해도 화면에는 id 가 그대로 나왔다.</p>
     */
    private Map<String, Group> groups = new LinkedHashMap<>();

    /**
     * @param name     화면에 보일 직무 이름
     * @param keywords 사람인 검색어. 너무 좁으면 표본이 안 모이고, 너무 넓으면 딴 직무가 섞인다
     */
    public static class Group {
        private String name;
        private String keywords;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
    }

    /** 그룹당 한 번에 가져올 공고 수. 사람인 API 상한이 110 이라 그 아래로 둔다. */
    private int countPerGroup = 100;

    /** 이 일수보다 오래된 공고는 집계에서 뺀다. 지난 시장을 근거로 삼으면 안 된다. */
    private int freshnessDays = 90;

    private Duration timeout = Duration.ofSeconds(20);

    /** 수집처가 하나라도 준비됐는지. 켜져 있어도 쓸 수 있는 수집처가 없으면 못 부른다. */
    public boolean isUsable() {
        return enabled && (hasSaramin() || hasWorknet() || hasClaudeSearch());
    }

    public boolean hasSaramin() { return notBlank(saraminKey); }
    public boolean hasWorknet() { return notBlank(worknetKey); }

    /**
     * AI 수집을 <b>설정상</b> 쓰기로 했는지.
     *
     * <p>실제로 부를 수 있는지는 AI 키까지 봐야 알 수 있고, 그건
     * {@code ClaudeSearchSource.usable()} 이 판단한다. 여기서 AI 키를 보지 않는 이유는
     * 공고 설정이 AI 설정을 알 필요가 없기 때문이다.</p>
     */
    public boolean hasClaudeSearch() { return claudeSearch && !claudeGroupIds().isEmpty(); }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /** 어느 수집처가 켜졌는지 — 기동 로그·관리자 화면 안내에 쓴다. 키 값은 절대 노출하지 않는다. */
    public String enabledSources() {
        List<String> on = new ArrayList<>();
        if (hasWorknet()) on.add("워크넷");
        if (hasSaramin()) on.add("사람인");
        if (hasClaudeSearch()) on.add("AI 웹검색");
        return on.isEmpty() ? "없음" : String.join(" + ", on);
    }

    /** AI 로 볼 직무 그룹 id. 설정이 비어 있으면 빈 집합 — 아무 직무도 보지 않는다. */
    public Set<String> claudeGroupIds() {
        return splitCsv(claudeGroups);
    }

    /** 검색 허용 도메인. 비어 있으면 제한하지 않는다. */
    public List<String> claudeAllowedDomainList() {
        return List.copyOf(splitCsv(claudeAllowedDomains));
    }

    private Set<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getSaraminKey() { return saraminKey; }
    public void setSaraminKey(String saraminKey) { this.saraminKey = saraminKey; }

    public String getSaraminBaseUrl() { return saraminBaseUrl; }
    public void setSaraminBaseUrl(String saraminBaseUrl) { this.saraminBaseUrl = saraminBaseUrl; }

    public String getWorknetKey() { return worknetKey; }
    public void setWorknetKey(String worknetKey) { this.worknetKey = worknetKey; }

    public String getWorknetBaseUrl() { return worknetBaseUrl; }
    public void setWorknetBaseUrl(String worknetBaseUrl) { this.worknetBaseUrl = worknetBaseUrl; }

    public Map<String, Group> getGroups() { return groups; }
    public void setGroups(Map<String, Group> groups) { this.groups = groups; }

    /** 표기명이 비어 있으면 id 라도 보여준다 — 빈 탭보다는 낫다. */
    public String nameOf(String groupId) {
        Group g = groups.get(groupId);
        return (g == null || g.getName() == null || g.getName().isBlank()) ? groupId : g.getName();
    }

    public int getCountPerGroup() { return countPerGroup; }
    public void setCountPerGroup(int countPerGroup) { this.countPerGroup = countPerGroup; }

    public int getFreshnessDays() { return freshnessDays; }
    public void setFreshnessDays(int freshnessDays) { this.freshnessDays = freshnessDays; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public boolean isClaudeSearch() { return claudeSearch; }
    public void setClaudeSearch(boolean claudeSearch) { this.claudeSearch = claudeSearch; }

    public String getClaudeGroups() { return claudeGroups; }
    public void setClaudeGroups(String claudeGroups) { this.claudeGroups = claudeGroups; }

    public int getClaudeCount() { return claudeCount; }
    public void setClaudeCount(int claudeCount) { this.claudeCount = claudeCount; }

    public String getClaudeAllowedDomains() { return claudeAllowedDomains; }
    public void setClaudeAllowedDomains(String v) { this.claudeAllowedDomains = v; }
}
