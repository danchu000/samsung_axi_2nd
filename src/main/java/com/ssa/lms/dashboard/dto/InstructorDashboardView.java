package com.ssa.lms.dashboard.dto;

import java.util.List;

/**
 * 강사 대시보드 모델 — {@code static/js/dashboard.js} 의 {@code dashboardData} 더미와 같은 shape.
 *
 * <p>dashboard.js 의 렌더 함수(renderCourseList / renderTodayEvalList / renderSupportList /
 * renderProctorList / renderNoticeList)를 그대로 두고 데이터만 갈아끼운다.</p>
 *
 * <p><b>담당 과정 한정</b> — 이 모델에 담기는 과정·시험·과제·문의는 전부
 * {@code CourseQueryService.isInstructorOf} 로 걸러진 것이다. 담당하지 않는 과정의
 * 응시자/성적이 섞이면 권한 사고다(권한정의서 △).</p>
 */
public record InstructorDashboardView(
        String userName,
        String today,
        Kpi kpi,
        List<CourseCard> courses,
        List<EvalItem> todayEvals,
        List<SupportItem> supports,
        List<ProctorItem> proctors,
        List<NoticeItem> notices
) {

    public record Kpi(String pendingAssignments, String pendingExams,
                      String pendingSupport, String proctoringExams) {
    }

    public record CourseCard(String id, String name, String status, String progress,
                             String todaySession, String missingCompletion, String href) {
    }

    /** kind = 시험 / 과제. href 는 해당 채점 화면. */
    public record EvalItem(String kind, String title, String meta, String actionText, String href) {
    }

    /** category = 튜터링 / QnA. */
    public record SupportItem(String category, String title, String meta, String href) {
    }

    public record ProctorItem(String title, String meta, String monitorHref, String recordingsHref) {
    }

    public record NoticeItem(String title, String href) {
    }
}
