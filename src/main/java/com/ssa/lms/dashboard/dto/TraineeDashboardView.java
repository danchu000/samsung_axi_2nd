package com.ssa.lms.dashboard.dto;

import java.util.List;

/**
 * 훈련생 대시보드 모델 — {@code static/js/trainee/index.js} 의 USER/COURSES/TODOS 더미와 같은 shape.
 *
 * <p><b>본인 데이터만</b> 담긴다. 과정·과제·시험·설문·성적·알림 전부 loginUser.id 로 조회한 것이고,
 * 공지는 {@code NoticeVisibilityService.traineeCourseIds} 로 "전체 공지 + 본인 수강 과정 공지"만 남긴다.</p>
 *
 * @param notices 정부 제출 문서(양식3) "훈련생 유의사항 등재(정보제공)" 요건 — 초기화면에서
 *                실제 공지 텍스트를 보여줘야 한다. 기존 화면은 하드코딩 이미지 5장이었다.
 */
public record TraineeDashboardView(
        String userName,
        String today,
        List<Stat> stats,
        List<Todo> todos,
        List<CourseCard> courses,
        List<NoticeItem> notices,
        String noticeMoreHref
) {

    /** hero 영역 요약 타일. k=라벨 / v=값 / s=보조설명. */
    public record Stat(String k, String v, String s) {
    }

    /**
     * 오늘 할 일 한 건.
     *
     * @param type TASK / EXAM / SURVEY — 화면 JS 가 배지 색과 버튼 문구를 이 값으로 고른다
     * @param dday 남은 일수. null 이면 D-day 배지를 그리지 않는다
     */
    public record Todo(String id, String type, String title, String meta,
                       String due, Integer dday, String href) {
    }

    public record CourseCard(String id, String name, String cohort, String startAt, String endAt,
                             String progressRate, String attendanceRate, String dday,
                             String continueHref, String noticeHref) {
    }

    /**
     * 초기화면 공지 미리보기 한 건.
     *
     * @param scope "전체" 또는 과정명. 본인이 볼 수 있는 범위만 내려온다
     */
    public record NoticeItem(String id, String title, String category, String date,
                             boolean pinned, String scope, String href) {
    }
}
