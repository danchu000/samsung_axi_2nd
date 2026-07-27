package com.ssa.lms.assignment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 채점 입력 (grading-modal.html 오른쪽 패널).
 *
 * reason 은 <b>이미 점수가 있는 상태에서 점수를 바꿀 때만</b> 필수다.
 * 최초 채점은 정정이 아니므로 사유를 강제하지 않는다.
 * 정정 시 사유는 GradeHistory 에 그대로 남는다 (내역서 증빙 요건).
 */
@Getter
@Setter
public class GradingForm {

    @NotNull(message = "점수를 입력하세요.")
    @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    private Integer score;

    /** 학생에게 공개되는 피드백. */
    private String feedback;

    /** 관리자·강사만 보는 내부 메모. 훈련생 화면 쿼리에서 제외된다. */
    private String memo;

    /** 성적 정정 사유. */
    private String reason;

    /** 체크 시 즉시 확정(CONFIRMED)까지. 확정된 성적만 이수 판정에 쓰인다. */
    private boolean confirm;
}
