package com.ssa.lms.content.web;

/**
 * 진도 갱신 요청 (훈련생 시청 화면 → 서버, JSON).
 *
 * <ul>
 *   <li>VIDEO: {@code positionSeconds}(현재 재생 위치) + {@code durationSeconds}(전체 길이).</li>
 *   <li>DOCUMENT: {@code pageReached}(도달 페이지) + {@code totalPages}(전체 페이지).</li>
 * </ul>
 * 전체 길이/페이지 수가 요청에 없으면 콘텐츠에 저장된 값을 사용한다.
 */
public class ProgressRequest {

    private Integer positionSeconds;
    private Integer durationSeconds;
    private Integer pageReached;
    private Integer totalPages;

    public Integer getPositionSeconds() { return positionSeconds; }
    public void setPositionSeconds(Integer positionSeconds) { this.positionSeconds = positionSeconds; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getPageReached() { return pageReached; }
    public void setPageReached(Integer pageReached) { this.pageReached = pageReached; }

    public Integer getTotalPages() { return totalPages; }
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
}
