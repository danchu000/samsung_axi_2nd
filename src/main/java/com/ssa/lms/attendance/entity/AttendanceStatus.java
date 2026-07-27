package com.ssa.lms.attendance.entity;

/**
 * 차시별 출결 상태. 접속(access_log) 기반 자동 산정은 PRESENT/ABSENT 만 부여하고,
 * LATE(지각)/EXCUSED(공결) 는 관리자 수동 보정으로만 지정한다.
 */
public enum AttendanceStatus {
    PRESENT("출석"),
    LATE("지각"),
    ABSENT("결석"),
    EXCUSED("공결");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 출석으로 인정되는 상태(출석/지각/공결) — 출석률 계산에 사용. */
    public boolean isCredited() {
        return this != ABSENT;
    }
}
