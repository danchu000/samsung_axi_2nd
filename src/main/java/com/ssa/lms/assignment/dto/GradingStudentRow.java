package com.ssa.lms.assignment.dto;

import java.util.List;

/**
 * 채점 화면 학생 목록 한 행 + 오른쪽 패널이 쓰는 상세.
 *
 * static/js/assignments-grading.js 의 gradingStudentData 더미 shape 을 유지한다
 * (no / name / submitStatus / gradingStatus / score / resubmit / btnType +
 *  birth / course / assignment / submitDate).
 *
 * <b>birth 자리에 생년월일을 넣지 않는다.</b> User.birthDate 는 AES 암호문이고
 * 채점 화면에 실려 나갈 이유도 없어서 loginId 로 대체했다. 화면은 "이름 (식별자)" 로 보인다.
 *
 * 미제출자도 행으로 나온다 — 미제출자 목록이 별도 화면이 아니라 이 표에서 읽히기 때문이다.
 */
public record GradingStudentRow(
        int no,
        Long userId,
        /** 미제출자는 null. */
        Long submissionId,
        String name,
        /** 화면 birth 자리 — loginId 를 쓴다 (개인정보 노출 방지). */
        String birth,
        /** 제출 / 미제출 */
        String submitStatus,
        /** 미채점 / 채점중 / 채점완료 / 확정 / "-" */
        String gradingStatus,
        String score,
        /** "0회" 형태. attemptNo - 1. */
        String resubmit,
        /** grading(채점) / edit(수정) / disabled(미제출) */
        String btnType,
        String course,
        String assignment,
        String submitDate,
        /** 지각 제출 여부 — 제출 시점에 확정 저장된 값. 감점 근거. */
        boolean late,
        /** 채점 팝업 URL. 미제출자는 빈 문자열. */
        String gradingUrl,
        List<GradeHistoryRow> history
) {
}
