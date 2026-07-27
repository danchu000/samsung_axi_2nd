package com.ssa.lms.user.web;

import com.ssa.lms.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 사용자 관리 컨트롤러 — {@code /admin/users/**} (SecurityConfig 의 {@code /admin/**}=ADMIN 로 커버).
 *
 * <p>첫 슬라이스: <b>가입 승인</b>(PENDING→ACTIVE). 승인 전까지 신규 가입 계정은 로그인 불가.</p>
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    /** 승인 대기 목록. */
    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("pendingUsers", userAdminService.findPending());
        return "admin/admin-02-user/admin-user-approval";
    }

    /** 가입 승인 — PENDING→ACTIVE. */
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userAdminService.approve(id);
            ra.addFlashAttribute("message", "가입을 승인했습니다. 해당 계정으로 로그인할 수 있습니다.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/pending";
    }

    /** 가입 반려 — PENDING 계정 soft delete. */
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userAdminService.reject(id);
            ra.addFlashAttribute("message", "가입을 반려했습니다.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/pending";
    }
}
