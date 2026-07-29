package com.ssa.lms.job.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [기능 1] 직무 로드맵 화면에 내려보내는 값. 전부 수집한 공고에서 계산된 실제 값이다.
 *
 * @param collectedAt 마지막 수집일. <b>수집한 적이 없으면 null</b> — 날짜를 지어내지 않는다
 * @param jobs        직무 그룹별 분석
 */
public record RoadmapView(LocalDate collectedAt, List<Job> jobs) {

    /**
     * @param id           직무 그룹 id (화면 탭)
     * @param name         화면 표기명
     * @param postingCount 집계에 쓴 공고 수. 표본 크기를 감추면 "68%"의 무게를 알 수 없다
     * @param matchRate    내가 이미 학습한 요구 역량의 비율(%)
     * @param avgCareer    공고에서 가장 많이 요구한 경력 구간 (최빈값)
     * @param have         내 학습 자료에서 확인된 역량
     * @param lack         공고는 요구하는데 내 학습 자료에 없는 역량
     * @param demands      요구 역량 순위 (공고 대비 %)
     * @param steps        학습 단계 — 요구 빈도 순으로 채울 순서
     * @param postings     근거가 된 원문 공고 (최신 5건)
     */
    public record Job(
            String id, String name, int postingCount, int matchRate, String avgCareer,
            List<String> have, List<String> lack,
            List<Demand> demands, List<Step> steps, List<Posting> postings) {}

    /**
     * 학습 단계.
     *
     * <p><b>AI 가 계획을 세운 것이 아니다.</b> 수집한 공고에서 요구 빈도를 세어
     * <b>많이 요구되는 것부터</b> 순서를 매긴 것이다. 그래서 이유(reason)에도
     * 정확한 근거 숫자를 적는다 — "공고 68%(34건)가 요구".</p>
     *
     * @param status done(이미 학습함) / current(다음에 할 것) / locked(그 다음)
     */
    public record Step(String title, String meta, String reason, String status) {}

    /**
     * @param label   역량명
     * @param percent 이 역량을 요구한 공고 비율(%)
     * @param count   해당 공고 수 — 퍼센트만 있으면 3건 중 2건도 67%가 된다
     * @param mine    내가 이미 학습한 역량인지
     */
    public record Demand(String label, int percent, int count, boolean mine) {}

    public record Posting(String company, String title, String url,
                          String keywords, LocalDate postingDate) {}
}
