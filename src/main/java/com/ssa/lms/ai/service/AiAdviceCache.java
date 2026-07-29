package com.ssa.lms.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * AI 분석 결과 보관 — <b>같은 결과를 다시 만들지 않는다.</b>
 *
 * <h3>왜 필요한가 — 비용보다 먼저, 기능이 막힌다</h3>
 * 커리큘럼 추천·직무 로드맵은 화면을 열 때마다 모델을 불렀다. 그런데 1인 하루 호출
 * 한도가 20회다. 훈련생이 <b>추천 화면을 스무 번 새로고침하면 그날 Q&amp;A 가 막힌다.</b>
 * 정작 중요한 질문 기능을, 새로고침만으로 못 쓰게 되는 것이다.
 *
 * <p>게다가 이 분석들은 <b>하루 사이에 답이 거의 안 바뀐다.</b> 진도·성적이 몇 시간 만에
 * 크게 달라지지 않는다. 매번 새로 만들 이유가 없다.</p>
 *
 * <h3>메모리에 둔다</h3>
 * 지금 배포는 앱 컨테이너 하나다. 재시작하면 캐시가 비지만, 그때 한 번 더 만들면 그만이다.
 * DB 테이블로 옮기는 것은 <b>여러 대로 늘릴 때</b> 하면 된다 — 지금 하면 쓰지도 않을
 * 스키마와 마이그레이션이 늘어난다.
 *
 * <p><b>실패는 캐시하지 않는다.</b> 모델이 잠깐 죽어서 실패한 결과를 하루 동안 붙들고
 * 있으면, 복구된 뒤에도 훈련생은 계속 실패 화면을 본다.</p>
 */
@Service
public class AiAdviceCache {

    private static final Logger log = LoggerFactory.getLogger(AiAdviceCache.class);

    /** 항목이 너무 많이 쌓이면 통째로 비운다. 훈련생 수 × 직무 수 규모라 단순하게 둔다. */
    private static final int MAX_ENTRIES = 2_000;

    private final Duration ttl;
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public AiAdviceCache(@Value("${lms.ai.advice-cache-hours:12}") int hours) {
        this.ttl = Duration.ofHours(Math.max(1, hours));
    }

    private record Entry(Object value, Instant expiresAt) {
        boolean alive() { return Instant.now().isBefore(expiresAt); }
    }

    /**
     * 캐시에 있으면 그대로, 없으면 만들어서 담는다.
     *
     * @param key    캐시 키. <b>사용자와 조건이 모두 들어가야 한다</b> —
     *               빠뜨리면 남의 추천 결과가 보인다
     * @param valid  만든 결과를 캐시할지 판단. 실패한 결과를 붙들고 있으면
     *               모델이 복구돼도 계속 실패 화면이 나온다
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, java.util.function.Predicate<T> valid) {
        Entry hit = store.get(key);
        if (hit != null && hit.alive()) {
            return (T) hit.value();
        }

        T made = loader.get();

        if (valid.test(made)) {
            if (store.size() >= MAX_ENTRIES) {
                log.info("[AI] 분석 캐시가 {}건을 넘어 비운다", MAX_ENTRIES);
                store.clear();
            }
            store.put(key, new Entry(made, Instant.now().plus(ttl)));
        } else {
            // 실패는 담지 않는다. 다음 요청에서 다시 시도해야 복구된 걸 알 수 있다
            store.remove(key);
        }
        return made;
    }

    /** 특정 사용자의 캐시를 버린다 — "다시 분석" 같은 동작에 쓴다. */
    public void evictUser(Long userId) {
        if (userId == null) return;
        String prefix = userId + ":";
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void evictAll() {
        store.clear();
    }

    /** 화면에 "언제 분석한 결과인지" 알리기 위한 만료 시각. 없으면 null. */
    public Instant expiresAt(String key) {
        Entry e = store.get(key);
        return (e != null && e.alive()) ? e.expiresAt() : null;
    }

    /** 사용자·기능·조건을 모두 넣은 키. 조건을 빠뜨리면 다른 조건의 결과가 나온다. */
    public static String key(Long userId, String kind, String condition) {
        return userId + ":" + kind + ":" + (condition == null ? "" : condition);
    }
}
