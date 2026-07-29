package com.ssa.lms.job.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사람인 채용정보 오픈 API 설정. [기능 1] 직무 로드맵의 수집 대상.
 *
 * <p><b>기본값은 꺼짐.</b> 키가 없으면 수집기가 돌지 않고 앱은 그대로 뜬다.
 * 외부 API 가 죽었다고 LMS 가 못 뜨면 안 된다.</p>
 *
 * <p><b>액세스 키는 저장소에 넣지 않는다.</b> 환경변수로만 주입한다:
 * <pre>SARAMIN_ACCESS_KEY=... LMS_JOB_ENABLED=true</pre></p>
 *
 * <p><b>왜 검색어를 설정으로 빼는가</b><br>
 * 훈련 과정이 바뀌면 볼 직무도 바뀐다. 코드를 고쳐 배포하지 않고 설정만 바꿔
 * 대응할 수 있어야 한다. 키는 직무 그룹 id(화면 탭), 값은 검색 키워드다.</p>
 */
@ConfigurationProperties(prefix = "lms.job")
public class SaraminProperties {

    private boolean enabled = false;

    /** 사람인 오픈 API 액세스 키. 환경변수 SARAMIN_ACCESS_KEY 로 주입한다. */
    private String accessKey = "";

    private String baseUrl = "https://oapi.saramin.co.kr";

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

    /** 실제로 부를 수 있는 상태인지. 켜져 있어도 키가 없으면 못 부른다. */
    public boolean isUsable() {
        return enabled && accessKey != null && !accessKey.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

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
