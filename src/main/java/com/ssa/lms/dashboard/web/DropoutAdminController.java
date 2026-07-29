package com.ssa.lms.dashboard.web;

import com.ssa.lms.dashboard.service.DropoutPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 학습 분석 — 이탈(중도탈락) 예측 대시보드.
 * 경로 {@code /admin/analytics/**} 는 SecurityConfig 의 {@code /admin/**}(ADMIN) 규칙으로 커버된다.
 */
@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class DropoutAdminController {

    private static final String VIEW = "admin/admin-08-analytics/admin-dropout";

    private final DropoutPredictionService dropoutPredictionService;

    @GetMapping("/dropout")
    public String dropout(Model model) {
        model.addAttribute("prediction", dropoutPredictionService.forAdmin());
        return VIEW;
    }
}
