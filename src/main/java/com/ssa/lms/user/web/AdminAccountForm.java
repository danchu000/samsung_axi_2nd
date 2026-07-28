package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자 계정 등록/수정 폼.
 * 등록 시에는 loginId/password 가 필수, 수정 시에는 프로필 필드만 사용한다(loginId 불변).
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminAccountForm {

    /** 로그인 아이디 — 등록 시에만 사용(수정 시 불변). */
    private String loginId;

    /** 초기 비밀번호 — 등록 시에만 사용. */
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    private String phone;

    /** yyyy-MM-dd */
    private String birthDate;

    /** male / female */
    private String gender;

    private String education;

    public static AdminAccountForm from(User u) {
        AdminAccountForm f = new AdminAccountForm();
        f.loginId = u.getLoginId();
        f.name = u.getName();
        f.email = u.getEmail();
        f.phone = u.getPhone();
        f.birthDate = u.getBirthDate();
        f.gender = u.getGender();
        f.education = u.getEducation();
        return f;
    }
}
