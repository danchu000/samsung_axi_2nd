package com.ssa.lms.user.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.service.MyInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 내 정보(본인 프로필) — 관리자·강사·훈련생 3역할 공통.
 *
 * <p>URL 은 역할별 접두어로 분리:
 * {@code /admin/my-info}, {@code /instructor/my-info}, {@code /trainee/my-info}
 * (SecurityConfig 의 {@code /admin|/instructor|/trainee/**} 경계로 커버).</p>
 *
 * <p>화면은 조회+수정+비밀번호 변경을 한 페이지에서 처리한다. 프로필 저장과 비밀번호 변경은
 * 서로 다른 폼(각각 별도 POST)이며, 비밀번호 변경은 현재 비밀번호 확인이 필수다.</p>
 */
@Controller
@RequiredArgsConstructor
public class MyInfoController {

    private final MyInfoService myInfoService;

    /* ===== 관리자 ===== */

    @GetMapping("/admin/my-info")
    public String adminView(@AuthenticationPrincipal LoginUser me, Model model) {
        return view("admin", me, model);
    }

    @PostMapping("/admin/my-info")
    public String adminSave(@AuthenticationPrincipal LoginUser me,
                            @Valid @ModelAttribute("form") MyInfoForm form,
                            BindingResult binding, Model model, RedirectAttributes ra) {
        return save("admin", me, form, binding, model, ra);
    }

    @PostMapping("/admin/my-info/password")
    public String adminPassword(@AuthenticationPrincipal LoginUser me,
                                @Valid @ModelAttribute("pwForm") PasswordChangeForm pwForm,
                                BindingResult binding, Model model, RedirectAttributes ra) {
        return changePassword("admin", me, pwForm, binding, model, ra);
    }

    /* ===== 강사 ===== */

    @GetMapping("/instructor/my-info")
    public String instructorView(@AuthenticationPrincipal LoginUser me, Model model) {
        return view("instructor", me, model);
    }

    @PostMapping("/instructor/my-info")
    public String instructorSave(@AuthenticationPrincipal LoginUser me,
                                 @Valid @ModelAttribute("form") MyInfoForm form,
                                 BindingResult binding, Model model, RedirectAttributes ra) {
        return save("instructor", me, form, binding, model, ra);
    }

    @PostMapping("/instructor/my-info/password")
    public String instructorPassword(@AuthenticationPrincipal LoginUser me,
                                     @Valid @ModelAttribute("pwForm") PasswordChangeForm pwForm,
                                     BindingResult binding, Model model, RedirectAttributes ra) {
        return changePassword("instructor", me, pwForm, binding, model, ra);
    }

    /* ===== 훈련생 ===== */

    @GetMapping("/trainee/my-info")
    public String traineeView(@AuthenticationPrincipal LoginUser me, Model model) {
        return view("trainee", me, model);
    }

    @PostMapping("/trainee/my-info")
    public String traineeSave(@AuthenticationPrincipal LoginUser me,
                              @Valid @ModelAttribute("form") MyInfoForm form,
                              BindingResult binding, Model model, RedirectAttributes ra) {
        return save("trainee", me, form, binding, model, ra);
    }

    @PostMapping("/trainee/my-info/password")
    public String traineePassword(@AuthenticationPrincipal LoginUser me,
                                  @Valid @ModelAttribute("pwForm") PasswordChangeForm pwForm,
                                  BindingResult binding, Model model, RedirectAttributes ra) {
        return changePassword("trainee", me, pwForm, binding, model, ra);
    }

    /* ===== 공통 로직 ===== */

    private String template(String rolePrefix) {
        return rolePrefix + "/my-info";
    }

    private String redirect(String rolePrefix) {
        return "redirect:/" + rolePrefix + "/my-info";
    }

    private String view(String rolePrefix, LoginUser me, Model model) {
        User user = myInfoService.get(me.getId());
        model.addAttribute("user", user);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", MyInfoForm.from(user));
        }
        if (!model.containsAttribute("pwForm")) {
            model.addAttribute("pwForm", new PasswordChangeForm());
        }
        return template(rolePrefix);
    }

    private String save(String rolePrefix, LoginUser me, MyInfoForm form,
                        BindingResult binding, Model model, RedirectAttributes ra) {
        User user = myInfoService.get(me.getId());
        if (binding.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("pwForm", new PasswordChangeForm());
            return template(rolePrefix);
        }
        myInfoService.updateProfile(me.getId(), form.getName(), form.getEmail(), form.getPhone(),
                form.getBirthDate(), form.getGender(), form.getEducation());
        ra.addFlashAttribute("message", "내 정보를 저장했습니다.");
        return redirect(rolePrefix);
    }

    private String changePassword(String rolePrefix, LoginUser me, PasswordChangeForm pwForm,
                                  BindingResult binding, Model model, RedirectAttributes ra) {
        if (!binding.hasErrors() && pwForm.isMismatch()) {
            binding.rejectValue("confirmPassword", "mismatch", "새 비밀번호가 일치하지 않습니다.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("user", myInfoService.get(me.getId()));
            model.addAttribute("form", MyInfoForm.from(myInfoService.get(me.getId())));
            model.addAttribute("pwOpen", true);
            return template(rolePrefix);
        }
        try {
            myInfoService.changePassword(me.getId(), pwForm.getCurrentPassword(), pwForm.getNewPassword());
            ra.addFlashAttribute("message", "비밀번호를 변경했습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("pwError", e.getMessage());
            ra.addFlashAttribute("pwOpen", true);
        }
        return redirect(rolePrefix);
    }
}
