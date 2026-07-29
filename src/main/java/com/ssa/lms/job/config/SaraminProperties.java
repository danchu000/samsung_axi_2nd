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
     * 직무 그룹 → 검색 키워드.
     * 그룹 id 는 화면 탭 id 이므로 함부로 바꾸면 기존 데이터와 연결이 끊긴다.
     */
    private Map<String, String> groups = new LinkedHashMap<>(Map.of(
            "backend", "백엔드 자바 스프링",
            "frontend", "프론트엔드 리액트",
            "data", "데이터분석 파이썬"
    ));

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

    public Map<String, String> getGroups() { return groups; }
    public void setGroups(Map<String, String> groups) { this.groups = groups; }

    public int getCountPerGroup() { return countPerGroup; }
    public void setCountPerGroup(int countPerGroup) { this.countPerGroup = countPerGroup; }

    public int getFreshnessDays() { return freshnessDays; }
    public void setFreshnessDays(int freshnessDays) { this.freshnessDays = freshnessDays; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
