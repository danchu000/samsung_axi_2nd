package com.ssa.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * 공통 보안 설정 — 권한 3종(ADMIN/INSTRUCTOR/TRAINEE) URL 경계.
 * 공동 소유 파일: 수정 전 반드시 상대 개발자와 공유 (CLAUDE.md).
 *
 * <p>커스텀 로그인 화면(01-login/login.html) 연동은 개발자 A의 로그인 슬라이스에서 진행 예정.
 * 그 전까지는 Spring Security 기본 로그인 페이지(/login)로 동작 확인 가능.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스, 로그인/가입, H2 콘솔(local)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/img/**",
                                "/icons/**", "/font/**", "/favicon.ico").permitAll()
                        .requestMatchers("/", "/login", "/signup/**", "/error").permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                        // 역할별 URL 경계 (관리자는 강사/훈련생 화면 접근 허용)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        .requestMatchers("/trainee/**").hasAnyRole("TRAINEE", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // TODO(A-로그인 슬라이스): .loginPage("/login") 커스텀 화면 연동
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // H2 콘솔용 (local 프로필에서만 콘솔이 열림)
                .csrf(csrf -> csrf.ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
