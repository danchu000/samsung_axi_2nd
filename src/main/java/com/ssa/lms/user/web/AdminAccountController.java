package com.ssa.lms.user.web;

import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.service.AdminAccountService;
import com.ssa.lms.user.service.DuplicateLoginIdException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 계정 관리 — {@code /admin/admins/**} (SecurityConfig 의 {@code /admin/**}=ADMIN 로 커버).
 *
 * <p>목록·신규 등록·수정·활성/비활성. 마지막 활성 관리자는 서비스에서 비활성화를 거부한다.</p>
 */
@Controller
@RequestMapping("/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    /** 관리자 목록. */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("admins", adminAccountService.findAdmins());
        return "admin/admin-02-user/admin-user";
    }

    /** 신규 등록 폼. */
    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AdminAccountForm());
        }
        model.addAttribute("mode", "create");
        return "admin/admin-02-user/admin-user-add";
    }

    /** 신규 등록 저장. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") AdminAccountForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (!StringUtils.hasText(form.getLoginId())) {
            binding.rejectValue("loginId", "required", "아이디는 필수입니다.");
        }
        if (!StringUtils.hasText(form.getPassword())) {
            binding.rejectValue("password", "required", "초기 비밀번호는 필수입니다.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            return "admin/admin-02-user/admin-user-add";
        }
        try {
            adminAccountService.create(form.getLoginId(), form.getPassword(), form.getName(),
                    form.getEmail(), form.getPhone(), form.getBirthDate(), form.getGender(),
                    form.getEducation());
        } catch (DuplicateLoginIdException e) {
            binding.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            model.addAttribute("mode", "create");
            return "admin/admin-02-user/admin-user-add";
        }
        ra.addFlashAttribute("message", "관리자를 등록했습니다.");
        return "redirect:/admin/admins";
    }

    /** 수정 폼. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User admin = adminAccountService.get(id);
        model.addAttribute("admin", admin);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", AdminAccountForm.from(admin));
        }
        model.addAttribute("mode", "edit");
        return "admin/admin-02-user/admin-user-update";
    }

    /** 수정 저장. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") AdminAccountForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        User admin = adminAccountService.get(id);
        if (binding.hasErrors()) {
            model.addAttribute("admin", admin);
            model.addAttribute("mode", "edit");
            return "admin/admin-02-user/admin-user-update";
        }
        adminAccountService.update(id, form.getName(), form.getEmail(), form.getPhone(),
                form.getBirthDate(), form.getGender(), form.getEducation());
        ra.addFlashAttribute("message", "관리자 정보를 수정했습니다.");
        return "redirect:/admin/admins";
    }

    /** 비활성화(정지) — 마지막 활성 관리자면 거부. */
    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminAccountService.deactivate(id);
            ra.addFlashAttribute("message", "관리자를 비활성화했습니다.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/admins";
    }

    /** 활성화. */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        adminAccountService.activate(id);
        ra.addFlashAttribute("message", "관리자를 활성화했습니다.");
        return "redirect:/admin/admins";
    }
}
