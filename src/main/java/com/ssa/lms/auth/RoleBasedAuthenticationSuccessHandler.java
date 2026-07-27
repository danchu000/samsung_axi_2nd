package com.ssa.lms.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * 로그인 성공 시 역할별 초기 화면으로 분기.
 * ADMIN → /admin, INSTRUCTOR → /instructor, TRAINEE → /trainee.
 *
 * <p>기존 defaultSuccessUrl("/", true) 를 대체한다. 여러 역할을 동시에 가진 경우
 * 권한이 넓은 순(ADMIN > INSTRUCTOR > TRAINEE)으로 우선한다.</p>
 */
@Component
public class RoleBasedAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        getRedirectStrategy().sendRedirect(request, response, targetUrl(authentication.getAuthorities()));
    }

    private String targetUrl(Collection<? extends GrantedAuthority> authorities) {
        boolean admin = hasRole(authorities, "ROLE_ADMIN");
        boolean instructor = hasRole(authorities, "ROLE_INSTRUCTOR");
        if (admin) {
            return "/admin";
        }
        if (instructor) {
            return "/instructor";
        }
        return "/trainee";
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        return authorities.stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
