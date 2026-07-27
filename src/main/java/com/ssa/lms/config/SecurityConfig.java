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
                        // 역할별 URL 경계 — 구체 경로가 먼저 와야 한다 (권한정의서 기준, a-requests.md P1-6)
                        // B 도메인: 관리자 모듈 중 강사도 접근 가능한 영역
                        .requestMatchers("/admin/evaluation/**", "/admin/support/**",
                                "/admin/notice/**", "/admin/survey/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/instructor/proctor/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                        .requestMatchers("/instructor/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                        // B 도메인: 훈련생 전용 (응시/제출은 본인만 — ADMIN 도 불가)
                        .requestMatchers("/trainee/exam/**", "/trainee/assignment/**",
                                "/trainee/survey/**", "/trainee/qna/**").hasRole("TRAINEE")
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
