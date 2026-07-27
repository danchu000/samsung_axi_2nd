package com.ssa.lms.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 튜터링 방 생성 폼 (훈련생이 튜터링 요청).
 * 화면 대응: trainee/tutoring.html 의 범위 선택 + 첫 질문.
 */
@Getter
@Setter
public class TutoringRoomForm {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    private String title;

    private Long courseId;

    /** 방을 만들면서 같이 보내는 첫 메시지 (선택). */
    private String firstMessage;
}
