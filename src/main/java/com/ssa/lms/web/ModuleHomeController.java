package com.ssa.lms.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 역할별 모듈 진입점 — 로그인 성공 시 {@code RoleBasedAuthenticationSuccessHandler} 가 여기로 보낸다.
 *
 * <p><b>임시 연결</b>: 각 모듈의 실제 대시보드/index 컨트롤러가 생기기 전까지
 * 기존 정적 화면(templates/{admin,instructor,trainee}/index.html)을 그대로 렌더링한다.
 * 각 도메인 소유자가 컨트롤러를 만들면 이 매핑은 제거/이관된다.</p>
 */
@Controller
public class ModuleHomeController {

    @GetMapping("/admin")
    public String adminHome() {
        return "admin/index";
    }

    @GetMapping("/instructor")
    public String instructorHome() {
        return "instructor/index";
    }

    @GetMapping("/trainee")
    public String traineeHome() {
        return "trainee/index";
    }
}
