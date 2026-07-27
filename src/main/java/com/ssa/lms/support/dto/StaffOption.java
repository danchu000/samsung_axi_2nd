package com.ssa.lms.support.dto;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;

/**
 * 담당자/튜터 배정 셀렉트 박스의 옵션 한 건.
 *
 * <p>엔티티(User)를 화면으로 그대로 넘기지 않기 위한 최소 DTO다.
 * User 의 email/phone/birthDate 는 AES-256 암호화 대상 개인정보라
 * 화면에는 id 와 name 만 내린다.</p>
 */
public record StaffOption(Long id, String name, String roleLabel) {

    public static StaffOption of(User u) {
        return new StaffOption(
                u.getId(),
                u.getName(),
                u.getRole() == null ? "-" : u.getRole().getLabel());
    }

    public static boolean isStaff(User u) {
        return u.getRole() != null && u.getRole() != Role.TRAINEE;
    }
}
