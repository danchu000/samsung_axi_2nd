package com.ssa.lms.ai.service;

import com.ssa.lms.ai.repository.AiUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [기능 5] AI 이용 현황 집계 — 대시보드 위젯의 실데이터.
 *
 * <p>이전에는 이 숫자들이 화면 JS 에 박혀 있었다("186건", "84%"). 실제로는
 * {@code AiUsageLog} 에 <b>이미 기록되고 있었는데</b> 아무도 세지 않았다.</p>
 *
 * <p><b>AI 를 부르지 않는다.</b> 세는 일에 모델을 쓰면 돈만 나가고 더 부정확하다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiUsageStatsService {

    /** 집계 기간. 하루치는 표본이 너무 작아 추세가 안 보인다. */
    private static final int WINDOW_DAYS = 14;

    private final AiUsageLogRepository repository;

    public Stats qnaStats() {
        LocalDateTime from = LocalDateTime.now().minusDays(WINDOW_DAYS);

        /*
         * 세 값을 한 쿼리로 받는다. 따로 세면 그 사이 새 호출이 들어와
         * "성공 > 전체" 같은 앞뒤 안 맞는 숫자가 나온다.
         *
         * JPQL 다중 select 는 Object[] 로 오는데, 스프링 데이터가 한 행을
         * Object[][] 로 감싸 줄 때가 있어 양쪽을 모두 받는다.
         */
        Object[] row = unwrap(repository.statsSince("QNA", from));
        if (row == null) return Stats.empty(WINDOW_DAYS);

        long total = asLong(row[0]);
        long ok = asLong(row[1]);
        long users = asLong(row[2]);

        // 0건일 때 "0%"를 보여주면 "해결률이 나쁘다"로 읽힌다. 비율을 아예 주지 않는다
        Integer rate = total == 0 ? null : (int) Math.round(ok * 100.0 / total);

        return new Stats(WINDOW_DAYS, total, ok, total - ok, users, rate);
    }

    private Object[] unwrap(Object result) {
        if (result == null) return null;
        if (result instanceof Object[] arr) {
            return (arr.length > 0 && arr[0] instanceof Object[] inner) ? inner : arr;
        }
        return null;
    }

    private long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * @param days       집계 기간(일)
     * @param total      전체 호출
     * @param solved     AI 가 답한 건수
     * @param failed     실패(한도 초과·모델 오류)
     * @param users      이용한 훈련생 수
     * @param solveRate  해결률(%). <b>0건이면 null</b> — 0%로 보이면 나쁜 성적으로 읽힌다
     */
    public record Stats(int days, long total, long solved, long failed,
                        long users, Integer solveRate) {

        static Stats empty(int days) {
            return new Stats(days, 0, 0, 0, 0, null);
        }

        /** 아직 아무도 안 썼는지 — 화면이 "이용 없음"을 보여줄지 판단한다. */
        public boolean isEmpty() {
            return total == 0;
        }

        /** 1인 평균 이용 건수. 이용자가 없으면 0. */
        public double perUser() {
            return users == 0 ? 0 : Math.round(total * 10.0 / users) / 10.0;
        }
    }
}
