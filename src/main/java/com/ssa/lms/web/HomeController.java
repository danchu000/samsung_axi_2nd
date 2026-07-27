package com.ssa.lms.web;

import com.ssa.lms.auth.LoginUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 진입점 — 로그인한 역할에 따라 각 모듈로 분기.
 * 각 모듈의 index 컨트롤러가 생기기 전까지는 임시 안내 페이지(home.html)를 보여준다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return "redirect:/login";
        }
        return "home";
    }
}
