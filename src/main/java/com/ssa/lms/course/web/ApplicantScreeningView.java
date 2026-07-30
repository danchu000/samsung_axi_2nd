package com.ssa.lms.course.web;

import com.ssa.lms.course.service.screening.MockApplicant;
import com.ssa.lms.course.service.screening.ScreeningInput;
import com.ssa.lms.course.service.screening.SuitabilityResult;

import java.time.LocalDateTime;

/**
 * 수강신청 승인 화면의 신청자 1행 — 신청 정보 + 적합도 산출 결과.
 *
 * @param rowId 화면 상세 패널 연결용 식별자 (DOM id)
 */
public record ApplicantScreeningView(String rowId, String name, String loginId,
                                     String courseCode, String courseName,
                                     LocalDateTime appliedAt,
                                     ScreeningInput input, SuitabilityResult result) {

    public static ApplicantScreeningView of(int index, MockApplicant m, SuitabilityResult result) {
        return new ApplicantScreeningView("app-" + index, m.name(), m.loginId(),
                m.courseCode(), m.courseName(),
                LocalDateTime.now().minusDays(m.appliedDaysAgo()).withHour(10).withMinute(30).withSecond(0).withNano(0),
                m.input(), result);
    }
}
