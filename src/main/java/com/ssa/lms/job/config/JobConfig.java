package com.ssa.lms.job.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * [기능 1] 채용공고 수집 설정 등록.
 *
 * <p>키가 없으면 수집기가 조용히 아무것도 안 한다. 그 상태를 기동 로그로 남긴다 —
 * "로드맵이 비어 있다"를 버그로 오해하고 원인을 찾느라 시간을 버리지 않게.</p>
 *
 * <p>액세스 키 값은 절대 로그에 남기지 않는다. 준비 여부만 남긴다.</p>
 */
@Configuration
@EnableConfigurationProperties(JobProperties.class)
public class JobConfig {

    private static final Logger log = LoggerFactory.getLogger(JobConfig.class);

    private final JobProperties props;

    public JobConfig(JobProperties props) {
        this.props = props;
    }

    @PostConstruct
    void announce() {
        if (props.isUsable()) {
            log.info("[공고수집] 활성 — 수집처 {} · 직무 {}종, 그룹당 {}건, 최근 {}일 기준",
                    props.enabledSources(), props.getGroups().size(),
                    props.getCountPerGroup(), props.getFreshnessDays());
        } else {
            log.warn("[공고수집] 비활성 — {}. 직무 로드맵은 '아직 수집 전'으로 표시됩니다.",
                    props.isEnabled() ? "lms.job.enabled=true 이지만 인증키가 하나도 없음(워크넷/사람인)"
                                      : "lms.job.enabled=false");
        }
    }
}
