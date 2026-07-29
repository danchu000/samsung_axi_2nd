package com.ssa.lms.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /** 수집처가 하나라도 준비됐는지. 켜져 있어도 키가 하나도 없으면 못 부른다. */
    public boolean isUsable() {
        return enabled && (hasSaramin() || hasWorknet());
    }

    public boolean hasSaramin() { return notBlank(saraminKey); }
    public boolean hasWorknet() { return notBlank(worknetKey); }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /** 어느 수집처가 켜졌는지 — 기동 로그·관리자 화면 안내에 쓴다. 키 값은 절대 노출하지 않는다. */
    public String enabledSources() {
        if (hasSaramin() && hasWorknet()) return "워크넷 + 사람인";
        if (hasWorknet()) return "워크넷";
        if (hasSaramin()) return "사람인";
        return "없음";
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
}
