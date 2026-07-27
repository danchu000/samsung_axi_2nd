package com.ssa.lms.auth.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회원가입 폼 — 훈련생/강사 공용. 화면(01-login/signup-trainee|instructor.html) 필드와 매핑.
 *
 * <p>User 엔티티가 지원하는 항목만 수집한다. 화면의 부가 항목(경력·자격·국민취업지원제도 등)은
 * 관련 엔티티가 생기는 후속 슬라이스에서 확장한다.</p>
 */
@Getter
@Setter
public class SignupForm {

    /** "TRAINEE" 또는 "INSTRUCTOR" — 컨트롤러가 경로에 따라 세팅(관리자는 self-signup 불가) */
    private String role;

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$",
            message = "아이디는 영문·숫자·밑줄 4~20자로 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 입력해주세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요.")
    private String passwordConfirm;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50)
    private String name;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;

    @NotBlank(message = "연락처를 입력해주세요.")
    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
    private String phone;

    /** yyyy-MM-dd (HTML date input) */
    @NotBlank(message = "생년월일을 입력해주세요.")
    private String birthDate;

    private String gender;

    private String education;

    /** 개인정보 수집·이용 동의 (내역서 필수) */
    @AssertTrue(message = "개인정보 수집·이용에 동의해주세요.")
    private boolean privacyConsent;

    /** 제3자 제공 동의 (내역서 필수) */
    @AssertTrue(message = "제3자 제공에 동의해주세요.")
    private boolean thirdPartyConsent;

    @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        if (password == null) {
            return false;
        }
        return password.equals(passwordConfirm);
    }
}
