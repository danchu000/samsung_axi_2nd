package com.ssa.lms.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로그인 실패 사유별 분기.
 * <ul>
 *   <li>계정 비활성(승인 대기/정지 등, {@link DisabledException}) → /login?pending — "승인 대기" 안내</li>
 *   <li>그 외(아이디/비밀번호 불일치) → /login?error — 일반 실패 안내</li>
 * </ul>
 * 가입 직후 상태는 PENDING(비활성)이므로 관리자 승인 전 로그인하면 pending 안내가 뜬다.
 */
@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String target = (exception instanceof DisabledException) ? "/login?pending" : "/login?error";
        setDefaultFailureUrl(target);
        super.onAuthenticationFailure(request, response, exception);
    }
}
