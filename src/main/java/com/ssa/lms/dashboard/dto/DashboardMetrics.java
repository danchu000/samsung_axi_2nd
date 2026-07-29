package com.ssa.lms.dashboard.dto;

import java.util.List;

/**
 * 대시보드 차트가 쓰는 지표.
 *
 * <p>관리자는 전체 과정, 강사는 담당 과정으로 <b>범위만 다르고 모양은 같다</b>.
 * 그래서 한 DTO 를 양쪽이 함께 쓴다 — 화면도 같은 렌더러를 쓴다.</p>
 *
 * <p>화면(JS)이 그대로 읽을 수 있도록 필드 이름을 차트 입력과 일치시켰다.</p>
 */
public record DashboardMetrics(
        List<CourseProgress> courses,
        List<Requirement> completion
) {

    /**
     * 과정별 진행 현황.
     *
     * @param progress 평균 진도율(%). 수강생의 진도 평균
     * @param elapsed  기간 경과율(%). 오늘이 훈련 기간의 몇 %인지
     *                 <b>진도와 나란히 봐야 "지연"이 보인다</b> — 기간은 70% 지났는데
     *                 진도가 40%면 늦은 것이다
     */
    public record CourseProgress(String name, int progress, int elapsed, int trainees) {
    }

    /**
     * 이수 요건별 충족 인원.
     *
     * @param label 진도 충족 / 출석 충족 / 성적 충족 / 전체 충족
     * @param done  충족 인원
     * @param total 판정 대상 인원
     */
    public record Requirement(String label, int done, int total) {
    }

    public static DashboardMetrics empty() {
        return new DashboardMetrics(List.of(), List.of());
    }
}
