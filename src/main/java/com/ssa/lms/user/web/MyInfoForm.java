package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내 정보(본인 프로필) 수정 폼 — User 엔티티가 실제 보유한 필드만 바인딩.
 * (주소·부서·경력 등 정적 화면 부가 필드는 스키마 동결로 보류.)
 */
@Getter
@Setter
@NoArgsConstructor
public class MyInfoForm {

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

    public static MyInfoForm from(User u) {
        MyInfoForm f = new MyInfoForm();
        f.name = u.getName();
        f.email = u.getEmail();
        f.phone = u.getPhone();
        f.birthDate = u.getBirthDate();
        f.gender = u.getGender();
        f.education = u.getEducation();
        return f;
    }
}
