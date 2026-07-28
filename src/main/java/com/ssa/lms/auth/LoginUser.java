package com.ssa.lms.auth;

import com.ssa.lms.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** 인증된 사용자 principal — 컨트롤러에서 @AuthenticationPrincipal LoginUser 로 접근 */
public class LoginUser implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String password;
    private final String name;
    private final com.ssa.lms.user.entity.Role role;
    private final com.ssa.lms.user.entity.UserStatus status;

    public LoginUser(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
        this.status = user.getStatus();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public com.ssa.lms.user.entity.Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    /**
     * 계정 잠금 여부 — 정지(SUSPENDED)/탈퇴(WITHDRAWN)는 "잠김"으로 본다.
     * Spring Security 는 잠금을 활성 여부보다 먼저 검사하므로 이들은 {@code LockedException} 으로 막히고,
     * {@link LoginFailureHandler} 에서 일반 실패(?error)로 안내된다 — "승인 대기(?pending)" 오분류를 막고
     * 계정 상태(정지 사실)가 로그인 화면에 노출되지 않게 한다.
     */
    @Override
    public boolean isAccountNonLocked() {
        return status != com.ssa.lms.user.entity.UserStatus.SUSPENDED
                && status != com.ssa.lms.user.entity.UserStatus.WITHDRAWN;
    }

    /**
     * 활성 여부 — 승인 대기(PENDING)만 비활성으로 본다.
     * PENDING 은 {@code DisabledException} 으로 막혀 {@link LoginFailureHandler} 에서 "승인 대기(?pending)" 안내로 분기된다.
     * (ACTIVE 만 로그인 통과 — 잠금/비활성 어느 쪽에도 걸리지 않는 상태.)
     */
    @Override
    public boolean isEnabled() {
        return status != com.ssa.lms.user.entity.UserStatus.PENDING;
    }
}
