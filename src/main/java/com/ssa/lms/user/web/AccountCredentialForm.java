package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 최고 관리자용 계정 자격증명 변경 폼 — 아이디/이메일/비밀번호.
 *
 * <p>아이디·이메일은 현재 값이 채워진 채로 열리고, <b>비밀번호는 항상 빈칸</b>이다(해시라 되돌려 보일 수 없다).
 * 비밀번호를 빈칸으로 두면 변경하지 않는다 — 길이 검증도 값이 있을 때만 컨트롤러에서 수행한다
 * (여기에 {@code @Size} 를 붙이면 "안 바꿈"이 곧바로 검증 실패가 된다).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountCredentialForm {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$",
            message = "아이디는 영문·숫자·밑줄 4~20자로 입력해주세요.")
    private String loginId;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    /** 빈칸이면 비밀번호를 변경하지 않는다. */
    private String newPassword;

    private String confirmPassword;

    public boolean hasNewPassword() {
        return newPassword != null && !newPassword.isBlank();
    }

    public boolean isPasswordMismatch() {
        return newPassword == null || !newPassword.equals(confirmPassword);
    }

    public static AccountCredentialForm from(User user) {
        AccountCredentialForm f = new AccountCredentialForm();
        f.loginId = user.getLoginId();
        f.email = user.getEmail();
        return f;
    }
}
