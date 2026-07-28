package com.ssa.lms.dashboard.dto;

import java.util.List;

/**
 * 관리자 대시보드 모델 — {@code templates/admin/index.html} 의 {@code adminDashboardData} 더미와 같은 shape.
 *
 * <p>기존 화면의 렌더 함수(setKpi / renderRisks / renderTables / renderNotices)를 고치지 않고
 * 데이터만 서버 값으로 갈아끼우기 위해 필드 이름을 더미와 1:1 로 맞췄다.
 * 예외는 {@code completionTop.final} 하나다 — {@code final} 은 자바 예약어라
 * {@code finalResult} 로 바꾸고 화면 JS 도 같이 바꿨다.</p>
 *
 * <p>값이 없는 항목은 {@code "-"}({@link DashboardFormat#NONE})다. 0 과 구분해야 한다.</p>
 */
public record AdminDashboardView(
        String today,
        Kpi kpi,
        List<Risk> risks,
        List<CourseTop> courseTop,
        List<EvalTop> evalTop,
        List<CompletionTop> completionTop,
        List<SupportTop> supportTop,
        List<NoticeItem> notices
) {

    /** 상단 KPI 카드 5개. 모두 문자열이다 — 데이터가 없는 항목은 "-" 로 내린다. */
    public record Kpi(Users users, Courses courses, Evals evals, Attendance attendance, Support support) {
    }

    public record Users(String trainees, String instructors, String admins) {
    }

    public record Courses(String inProgress, String upcoming, String ended) {
    }

    /** 시험 + 과제를 합친 평가 진행 현황. */
    public record Evals(String inProgress, String upcoming, String ended) {
    }

    /** 출결/이수 — A 도메인({@code completion})을 읽기 전용으로 집계한다. */
    public record Attendance(String pendingConfirm, String confirmed, String certificates) {
    }

    public record Support(String unanswered, String inProgress, String slaRisk) {
    }

    public record Risk(String title, String meta, String href) {
    }

    public record CourseTop(String name, String round, String period,
                            String attendanceRate, String target, String status) {
    }

    public record EvalTop(String name, String course, String status,
                          String submitted, String grading, String href) {
    }

    /** {@code final} 이 예약어라 {@code finalResult} 로 둔다 (화면 JS 도 같은 이름을 읽는다). */
    public record CompletionTop(String course, String trainee, String rate,
                                String finalResult, String confirm, String href) {
    }

    public record SupportTop(String type, String status, String course,
                             String requester, String elapsed, String href) {
    }

    public record NoticeItem(String title, String href) {
    }
}
