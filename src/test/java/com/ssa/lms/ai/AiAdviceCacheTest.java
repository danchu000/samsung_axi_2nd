package com.ssa.lms.ai;

import com.ssa.lms.ai.service.AiAdviceCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 분석 결과 캐시를 고정한다.
 *
 * <p>여기서 막으려는 사고는 <b>비용이 아니라 기능 정지</b>다.
 * 커리큘럼·로드맵·진단이 화면을 열 때마다 모델을 부르면, 훈련생이 추천 화면을
 * 스무 번 새로고침하는 것만으로 1인 하루 한도(20회)를 소진해 <b>그날 Q&amp;A 가 막힌다.</b></p>
 */
class AiAdviceCacheTest {

    private AiAdviceCache cache() {
        return new AiAdviceCache(12);
    }

    @Test
    @DisplayName("같은 키로 여러 번 불러도 모델은 한 번만 부른다 — 여기가 핵심이다")
    void 두번째부터는_만들지_않는다() {
        AiAdviceCache c = cache();
        AtomicInteger calls = new AtomicInteger();

        for (int i = 0; i < 20; i++) {
            c.get("u1:CURRICULUM:", () -> { calls.incrementAndGet(); return "결과"; }, v -> true);
        }

        assertThat(calls.get())
                .as("새로고침 스무 번이 호출 스무 번이 되면 그날 Q&A 가 막힌다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("실패한 결과는 캐시하지 않는다 — 복구돼도 계속 실패 화면이 나온다")
    void 실패는_담지_않는다() {
        AiAdviceCache c = cache();
        AtomicInteger calls = new AtomicInteger();

        // 처음 두 번은 실패, 세 번째에 성공
        java.util.function.Supplier<String> loader =
                () -> calls.incrementAndGet() < 3 ? null : "성공";

        c.get("u1:X:", loader, v -> v != null);
        c.get("u1:X:", loader, v -> v != null);
        String third = c.get("u1:X:", loader, v -> v != null);

        assertThat(third).isEqualTo("성공");
        assertThat(calls.get()).as("실패를 캐시하면 3번째 시도 자체가 없다").isEqualTo(3);

        // 성공한 뒤에는 더 부르지 않는다
        c.get("u1:X:", loader, v -> v != null);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("조건이 다르면 다른 결과 — 직무를 바꿨는데 앞 직무 로드맵이 나오면 안 된다")
    void 조건별로_따로_담는다() {
        AiAdviceCache c = cache();

        String backend = c.get(AiAdviceCache.key(1L, "ROADMAP", "백엔드 개발자"),
                () -> "백엔드 로드맵", v -> true);
        String frontend = c.get(AiAdviceCache.key(1L, "ROADMAP", "프론트엔드 개발자"),
                () -> "프론트 로드맵", v -> true);

        assertThat(backend).isEqualTo("백엔드 로드맵");
        assertThat(frontend).isEqualTo("프론트 로드맵");
    }

    @Test
    @DisplayName("사용자가 다르면 결과가 섞이지 않는다 — 남의 추천이 보이면 안 된다")
    void 사용자별로_분리된다() {
        AiAdviceCache c = cache();

        String a = c.get(AiAdviceCache.key(1L, "CURRICULUM", null), () -> "A 추천", v -> true);
        String b = c.get(AiAdviceCache.key(2L, "CURRICULUM", null), () -> "B 추천", v -> true);

        assertThat(a).isEqualTo("A 추천");
        assertThat(b).isEqualTo("B 추천");
    }

    @Test
    @DisplayName("다시 분석을 누르면 그 사람 것만 지워진다 — 남의 캐시까지 날리면 안 된다")
    void 사용자별_갱신() {
        AiAdviceCache c = cache();
        AtomicInteger mine = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        c.get(AiAdviceCache.key(1L, "CURRICULUM", null),
                () -> { mine.incrementAndGet(); return "A"; }, v -> true);
        c.get(AiAdviceCache.key(2L, "CURRICULUM", null),
                () -> { other.incrementAndGet(); return "B"; }, v -> true);

        c.evictUser(1L);

        c.get(AiAdviceCache.key(1L, "CURRICULUM", null),
                () -> { mine.incrementAndGet(); return "A"; }, v -> true);
        c.get(AiAdviceCache.key(2L, "CURRICULUM", null),
                () -> { other.incrementAndGet(); return "B"; }, v -> true);

        assertThat(mine.get()).as("갱신한 사람은 새로 만든다").isEqualTo(2);
        assertThat(other.get()).as("남의 캐시는 그대로여야 한다").isEqualTo(1);
    }
}
