package com.ssa.lms.job.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [기능 1-관리자] 채용공고 수집 현황 요약.
 *
 * <p>관리자가 알아야 할 것은 두 가지다.
 * <ol>
 *   <li><b>언제 수집됐나</b> — 오래됐으면 훈련생이 지난 시장을 보고 있는 것이다.
 *       수집이 멈춘 것을 아무도 모르면 몇 주 뒤에야 알게 된다</li>
 *   <li><b>뭐가 요구되고 있나</b> — 과정 개편·수업 보완의 근거가 된다</li>
 * </ol>
 *
 * @param enabled      수집 기능이 켜져 있는지 (키 포함)
 * @param collectedAt  마지막 수집일. <b>수집한 적이 없으면 null</b>
 * @param staleDays    마지막 수집 이후 지난 일수. 수집 전이면 null
 * @param totalCount   저장된 공고 총 건수
 * @param groups       직무별 요약
 * @param topSkills    전 직무 통합 요구 역량 상위 — 어느 과정을 열어야 할지의 근거
 */
public record JobCollectionSummary(
        boolean enabled,
        LocalDate collectedAt,
        Integer staleDays,
        long totalCount,
        List<GroupSummary> groups,
        List<SkillCount> topSkills
) {

    /**
     * @param analyzable 표본이 충분해 훈련생 화면에 실제로 노출되는지.
     *                   수집은 됐는데 표본이 모자라 안 보이는 경우를 구분해야 한다
     * @param topSkills  이 직무에서 가장 많이 요구된 역량 (요약이라 3개만)
     */
    public record GroupSummary(String id, String name, int postingCount,
                               boolean analyzable, List<SkillCount> topSkills) {}

    /** @param percent 전체 공고 대비 비율 — 건수만 있으면 많은지 적은지 알 수 없다 */
    public record SkillCount(String label, int count, int percent) {}

    /** 수집이 이 일수를 넘겨 멈춰 있으면 화면에서 경고한다. 주 1회 수집이므로 2주. */
    public static final int STALE_WARN_DAYS = 14;

    public boolean isStale() {
        return staleDays != null && staleDays > STALE_WARN_DAYS;
    }
}
