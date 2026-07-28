package com.ssa.lms.user.web;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비밀번호 변경 폼 — 현재 비밀번호 확인 필수.
 * 새 비밀번호/확인 일치 여부는 컨트롤러에서 검증한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeForm {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.")
    private String confirmPassword;

    public boolean isMismatch() {
        return newPassword == null || !newPassword.equals(confirmPassword);
    }
}
