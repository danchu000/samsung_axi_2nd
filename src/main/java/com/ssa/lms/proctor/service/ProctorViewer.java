package com.ssa.lms.proctor.service;

import com.ssa.lms.user.entity.Role;

/**
 * 감독 화면을 보는 사람. 컨트롤러가 {@code LoginUser} 에서 만들어 서비스로 넘긴다.
 *
 * <p>서비스가 {@code LoginUser}(=Spring Security principal)를 직접 받지 않는 이유는
 * 도메인 서비스가 웹 인증 타입에 묶이면 테스트에서 principal 을 통째로 만들어야 하기 때문이다.</p>
 */
public record ProctorViewer(Long userId, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }
}
