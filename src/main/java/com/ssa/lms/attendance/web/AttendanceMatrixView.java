package com.ssa.lms.attendance.web;

import com.ssa.lms.attendance.entity.Attendance;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.user.entity.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 출결 현황 화면용 매트릭스 뷰 — 행=수강생, 열=차시. 셀은 차시별 출결 상태.
 * 서버에서 완성해 넘기므로 템플릿은 {@code th:each} 로 렌더만 한다.
 */
public class AttendanceMatrixView {

    /** 열(차시) */
    public record Col(Long sessionId, int seq, String name, LocalDate date) {}

    /** 셀(수강생×차시 출결). recorded=false 면 아직 산정 전. */
    public record Cell(Long attendanceId, String status, String label, boolean credited, boolean recorded) {}

    /** 행(수강생) */
    public record Row(Long traineeId, String traineeName, List<Cell> cells, int attendanceRate) {}

    private final List<Col> columns;
    private final List<Row> rows;

    private AttendanceMatrixView(List<Col> columns, List<Row> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public List<Col> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public boolean isEmpty() {
        return columns.isEmpty() || rows.isEmpty();
    }

    /**
     * @param trainees    과정 수강생(이름 표기 순으로 미리 정렬해 전달)
     * @param sessions    일정이 잡힌 차시(seq 순)
     * @param attendances 과정의 출결 행 전체
     */
    public static AttendanceMatrixView of(List<User> trainees, List<Session> sessions,
                                          List<Attendance> attendances) {
        List<Col> columns = sessions.stream()
                .map(s -> new Col(s.getId(), s.getSeq(), s.getName(), s.getLessonDate()))
                .toList();

        // (traineeId, sessionId) -> Attendance
        Map<Long, Map<Long, Attendance>> byTrainee = new LinkedHashMap<>();
        for (Attendance a : attendances) {
            byTrainee.computeIfAbsent(a.getTrainee().getId(), k -> new LinkedHashMap<>())
                    .put(a.getSession().getId(), a);
        }

        List<Row> rows = new ArrayList<>();
        for (User trainee : trainees) {
            Map<Long, Attendance> row = byTrainee.getOrDefault(trainee.getId(), Map.of());
            List<Cell> cells = new ArrayList<>();
            int credited = 0;
            int recorded = 0;
            for (Col col : columns) {
                Attendance a = row.get(col.sessionId());
                if (a == null) {
                    cells.add(new Cell(null, "NONE", "-", false, false));
                } else {
                    boolean c = a.getStatus().isCredited();
                    if (c) credited++;
                    recorded++;
                    cells.add(new Cell(a.getId(), a.getStatus().name(), a.getStatus().getLabel(), c, true));
                }
            }
            int rate = recorded == 0 ? 0 : (int) Math.round(credited * 100.0 / recorded);
            rows.add(new Row(trainee.getId(), trainee.getName(), cells, rate));
        }
        return new AttendanceMatrixView(columns, rows);
    }
}
