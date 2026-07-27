package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.entity.UserStatus;
import com.ssa.lms.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /* ===== 훈련생/강사 목록·상세 ===== */

    /** 훈련생 목록(+선택 상세). */
    @GetMapping("/trainees")
    public String trainees(@RequestParam(required = false) Long selected, Model model) {
        return listByRole(Role.TRAINEE, "admin/admin-02-user/admin-user-trainee", selected, model);
    }

    /** 강사 목록(+선택 상세). */
    @GetMapping("/instructors")
    public String instructors(@RequestParam(required = false) Long selected, Model model) {
        return listByRole(Role.INSTRUCTOR, "admin/admin-02-user/admin-user-instructor", selected, model);
    }

    private String listByRole(Role role, String view, Long selectedId, Model model) {
        model.addAttribute("users", userAdminService.findByRole(role));
        User selected = null;
        if (selectedId != null) {
            User u = userAdminService.get(selectedId);
            if (u.getRole() == role) {
                selected = u;
            }
        }
        model.addAttribute("selected", selected);
        return view;
    }

    /* ===== 프로필 수정 ===== */

    /** 수정 폼 — 실제 User 필드만 편집. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userAdminService.get(id);
        model.addAttribute("user", user);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", UserEditForm.from(user));
        }
        return "admin/admin-02-user/admin-user-edit";
    }

    /** 수정 저장. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserEditForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        User user = userAdminService.get(id);
        if (binding.hasErrors()) {
            model.addAttribute("user", user);
            return "admin/admin-02-user/admin-user-edit";
        }
        userAdminService.updateProfile(id, form.getName(), form.getEmail(), form.getPhone(),
                form.getBirthDate(), form.getGender(), form.getEducation(), form.getStatus());
        ra.addFlashAttribute("message", "정보를 저장했습니다.");
        return "redirect:/admin/users/" + listPathFor(user.getRole()) + "?selected=" + id;
    }

    /** 목록에서 계정 상태 변경(활성/정지). */
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam UserStatus status,
                               @RequestParam(required = false) String from, RedirectAttributes ra) {
        userAdminService.changeStatus(id, status);
        ra.addFlashAttribute("message", "계정 상태를 변경했습니다.");
        return "redirect:" + (from != null ? from : "/admin/users/trainees");
    }

    /** 계정 삭제(soft delete). */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) String from,
                         RedirectAttributes ra) {
        userAdminService.delete(id);
        ra.addFlashAttribute("message", "계정을 삭제했습니다.");
        return "redirect:" + (from != null ? from : "/admin/users/trainees");
    }

    private String listPathFor(Role role) {
        return role == Role.INSTRUCTOR ? "instructors" : "trainees";
    }

    /* ===== 접속 이력 (access_log — 내역서 보안 증빙) ===== */

    @GetMapping("/{id}/access-history")
    public String accessHistory(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int page, Model model) {
        User user = userAdminService.get(id);
        model.addAttribute("user", user);
        model.addAttribute("logs", userAdminService.findAccessLogs(id, PageRequest.of(page, 20)));
        return "admin/admin-02-user/admin-user-access-log";
    }
}
