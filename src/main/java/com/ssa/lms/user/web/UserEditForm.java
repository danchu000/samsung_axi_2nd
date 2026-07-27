package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강사/훈련생 정보 수정 폼 — <b>User 엔티티가 실제로 보유한 필드만</b> 바인딩한다.
 * (주소·국민취업지원제도·경력 등 정적 화면의 부가 필드는 스키마 동결로 보류.)
 */
@Getter
@Setter
@NoArgsConstructor
public class UserEditForm {

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

    /** 계정 상태 — 활성(ACTIVE)/정지(SUSPENDED) 전환. 승인/반려는 승인 화면에서 처리. */
    private UserStatus status;

    public static UserEditForm from(User u) {
        UserEditForm f = new UserEditForm();
        f.name = u.getName();
        f.email = u.getEmail();
        f.phone = u.getPhone();
        f.birthDate = u.getBirthDate();
        f.gender = u.getGender();
        f.education = u.getEducation();
        f.status = u.getStatus();
        return f;
    }
}
