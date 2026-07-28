package com.ssa.lms.attendance.web;

import com.ssa.lms.attendance.entity.Attendance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 훈련생 출결현황 화면용 뷰 — 본인 출결을 과정 단위로 묶어 차시별로 나열한다.
 * 서버에서 완성해 넘기므로 템플릿은 {@code th:each} 로 렌더만 한다(open-in-view=false 대비 트랜잭션 내 생성).
 */
public record TraineeAttendanceView(
        Long courseId,
        String courseLabel,
        int attendanceRate,
        List<Row> rows
) {

    /** 차시별 출결 행. */
    public record Row(int seq, String sessionName, LocalDate date,
                      String status, String label, boolean credited) {}

    /**
     * 수강생 본인의 출결 행 전체를 과정별로 묶어 뷰 목록으로 변환한다.
     * 과정 정렬은 과정명 가나다순, 과정 내 차시는 seq 오름차순.
     */
    public static List<TraineeAttendanceView> group(List<Attendance> attendances) {
        Map<Long, List<Attendance>> byCourse = new LinkedHashMap<>();
        for (Attendance a : attendances) {
            byCourse.computeIfAbsent(a.getCourse().getId(), k -> new ArrayList<>()).add(a);
        }

        List<TraineeAttendanceView> views = new ArrayList<>();
        for (List<Attendance> group : byCourse.values()) {
            group.sort(Comparator.comparingInt(a -> a.getSession().getSeq()));
            Attendance any = group.get(0);
            String cohort = any.getCourse().getCohort();
            String label = any.getCourse().getCourseName() + (cohort != null ? " / " + cohort : "");

            List<Row> rows = new ArrayList<>();
            int credited = 0;
            for (Attendance a : group) {
                boolean c = a.getStatus().isCredited();
                if (c) credited++;
                rows.add(new Row(a.getSession().getSeq(), a.getSession().getName(), a.getAttendanceDate(),
                        a.getStatus().name(), a.getStatus().getLabel(), c));
            }
            int rate = rows.isEmpty() ? 0 : (int) Math.round(credited * 100.0 / rows.size());
            views.add(new TraineeAttendanceView(any.getCourse().getId(), label, rate, rows));
        }
        views.sort(Comparator.comparing(TraineeAttendanceView::courseLabel));
        return views;
    }
}
