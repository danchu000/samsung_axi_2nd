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
    private final boolean enabled;

    public LoginUser(User user) {
        this.id = user.getId();
        this.loginId = user.getLoginId();
        this.password = user.getPassword();
        this.name = user.getName();
        this.role = user.getRole();
        this.enabled = user.getStatus() == com.ssa.lms.user.entity.UserStatus.ACTIVE;
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
