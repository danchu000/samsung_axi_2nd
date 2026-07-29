package com.ssa.lms.job.service;

import com.ssa.lms.job.client.SaraminClient;
import com.ssa.lms.job.config.SaraminProperties;
import com.ssa.lms.job.entity.JobPosting;
import com.ssa.lms.job.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 채용공고 <b>주 1회</b> 수집. [기능 1] 직무 로드맵의 심장.
 *
 * <p>여기가 "주 1회 수집"이 실제로 일어나는 유일한 곳이다. 이 배치가 없으면
 * 화면의 "마지막 수집: …" 은 그냥 글자다.</p>
 *
 * <p><b>왜 월요일 새벽 3시인가</b><br>
 * 훈련생이 거의 안 쓰는 시간대다. 공고 100건 × 직무 3개를 받아 저장하는 동안
 * 외부 API 응답이 느리면 그 사이 요청이 밀린다. 주 단위 갱신이면 충분한 데이터라
 * 굳이 업무 시간에 돌릴 이유가 없다.</p>
 *
 * <p><b>중복을 반드시 막는다</b><br>
 * 같은 공고가 매주 다시 잡힌다. 그대로 쌓으면 "공고 68%가 요구" 같은 숫자가
 * 통째로 거짓이 된다 — 오래 걸려 있는 공고일수록 여러 번 세어지기 때문이다.
 * 외부 id 로 걸러 저장 전에 버린다.</p>
 *
 * <p><b>한 직무가 실패해도 나머지는 계속한다</b><br>
 * 외부 API 는 일부 키워드에서만 실패하기도 한다. 전체를 하나의 트랜잭션으로 묶으면
 * 멀쩡히 받은 공고까지 같이 버리게 된다. 그래서 <b>이 클래스에는 트랜잭션을 걸지 않고</b>
 * 그룹마다 {@code saveAll} 이 자기 트랜잭션으로 커밋되게 둔다
 * ({@code SimpleJpaRepository.saveAll} 자체가 {@code @Transactional} 이다).
 *
 * <p>여기에 {@code @Transactional} 을 붙이는 건 의미가 없다 — 같은 클래스 안에서
 * 부르면 프록시를 타지 않아 <b>아무 효과가 없는데 걸린 것처럼 보인다.</b>
 * 그게 더 위험하다.</p>
 */
@Service
@RequiredArgsConstructor
public class JobPostingCollector {

    private static final Logger log = LoggerFactory.getLogger(JobPostingCollector.class);

    private final SaraminProperties props;
    private final SaraminClient client;
    private final JobPostingRepository repository;

    /**
     * 매주 월요일 03:00.
     *
     * <p>{@code cron} 이므로 기동 시각과 무관하게 정해진 때에만 돈다.
     * {@code fixedDelay} 로 두면 앱을 재시작할 때마다 즉시 한 번 더 돌아
     * 외부 API 호출이 불필요하게 늘어난다.</p>
     */
    @Scheduled(cron = "${lms.job.collect-cron:0 0 3 * * MON}")
    public void collectWeekly() {
        collectNow();
    }

    /**
     * 수동 수집(관리자 버튼·최초 1회용). 스케줄과 같은 일을 한다.
     *
     * @return 새로 저장된 공고 수. 꺼져 있으면 -1
     */
    public int collectNow() {
        if (!props.isUsable()) {
            log.info("[공고수집] 건너뜀 — {}",
                    props.isEnabled() ? "lms.job.enabled=true 이지만 액세스 키가 비어 있음"
                                      : "lms.job.enabled=false");
            return -1;
        }

        LocalDate today = LocalDate.now();
        int saved = 0;
        for (Map.Entry<String, SaraminProperties.Group> g : props.getGroups().entrySet()) {
            try {
                saved += collectGroup(g.getKey(), g.getValue().getKeywords(), today);
            } catch (Exception e) {
                // 한 직무의 저장 실패가 나머지 직무 수집을 막으면 안 된다
                log.error("[공고수집] {} 저장 실패 — 건너뛰고 계속한다 cause={}",
                        g.getKey(), e.getClass().getSimpleName());
            }
        }
        log.info("[공고수집] 완료 — 신규 {}건 (기준일 {})", saved, today);
        return saved;
    }

    /** 직무 그룹 하나. saveAll 이 자기 트랜잭션으로 커밋하므로 여기서 실패해도 앞 그룹은 남는다. */
    private int collectGroup(String group, String keywords, LocalDate today) {
        List<JobPosting> fetched = client.search(group, keywords, today);
        if (fetched.isEmpty()) {
            log.warn("[공고수집] {} — 받은 공고 없음 (키워드: {})", group, keywords);
            return 0;
        }

        // 이미 있는 id 를 한 번에 조회한다. 건건이 exists 를 치면 100번 쿼리가 나간다
        Set<String> existing = repository.findExistingExternalIds(
                fetched.stream().map(JobPosting::getExternalId).toList());

        List<JobPosting> fresh = new ArrayList<>();
        for (JobPosting p : fetched) {
            if (!existing.contains(p.getExternalId())) fresh.add(p);
        }

        repository.saveAll(fresh);
        log.info("[공고수집] {} — 받음 {}건 / 신규 {}건", group, fetched.size(), fresh.size());
        return fresh.size();
    }
}
