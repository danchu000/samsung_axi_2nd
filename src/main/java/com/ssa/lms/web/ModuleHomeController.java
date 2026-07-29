package com.ssa.lms.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.dashboard.service.AdminDashboardService;
import com.ssa.lms.dashboard.service.DashboardMetricsService;
import com.ssa.lms.dashboard.service.InstructorDashboardService;
import com.ssa.lms.dashboard.service.TraineeDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 역할별 모듈 진입점 — 로그인 성공 시 {@code RoleBasedAuthenticationSuccessHandler} 가 여기로 보낸다.
 *
 * <p>세 화면(templates/{admin,instructor,trainee}/index.html)은 그대로 두고, 정적 더미로 채워지던
 * 대시보드 값만 서버 집계로 바꿨다. <b>역할 분기와 뷰 이름은 건드리지 않는다</b> — 여기는 A가 만든
 * 공용 진입점이고, 각 도메인 컨트롤러가 생기면 매핑이 이관될 자리다.</p>
 *
 * <p>집계 로직은 전부 {@code com.ssa.lms.dashboard.service.*} 에 있다. 여기서는 모델에 담기만 한다.</p>
 */
@Controller
@RequiredArgsConstructor
public class ModuleHomeController {

    private final AdminDashboardService adminDashboardService;
    private final InstructorDashboardService instructorDashboardService;
    private final TraineeDashboardService traineeDashboardService;
    private final DashboardMetricsService dashboardMetricsService;

    @GetMapping("/admin")
    public String adminHome(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("dashboard", adminDashboardService.load(loginUser.getId()));
        // 차트용 지표 — 관리자는 전체 과정 범위
        model.addAttribute("metrics", dashboardMetricsService.forAdmin());
        return "admin/index";
    }

    @GetMapping("/instructor")
    public String instructorHome(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("dashboard",
                instructorDashboardService.load(loginUser.getId(), loginUser.getName()));
        // 같은 계산을 담당 과정 범위로만 — 관리자와 숫자가 갈리면 안 된다
        model.addAttribute("metrics", dashboardMetricsService.forInstructor(loginUser.getId()));
        return "instructor/index";
    }

    @GetMapping("/trainee")
    public String traineeHome(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("dashboard",
                traineeDashboardService.load(loginUser.getId(), loginUser.getName()));
        // 우리 반에서 내 학습 위치 (달리기 트랙). 혼자 듣는 과정이면 값이 없다
        model.addAttribute("pace", dashboardMetricsService.paceOf(loginUser.getId()).orElse(null));
        return "trainee/index";
    }
}
