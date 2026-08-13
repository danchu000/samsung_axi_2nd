package com.ssa.lms.user.web;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.service.AccountCredentialService;
import com.ssa.lms.user.service.DuplicateLoginIdException;
import com.ssa.lms.user.service.SuperAdminPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 계정 자격증명 변경 — {@code /admin/accounts/{id}/credentials}. <b>최고 관리자 전용</b>.
 *
 * <p>URL 은 {@code /admin/**} 이라 SecurityConfig 가 ADMIN 까지는 걸러 주지만, "최고 관리자"는
 * URL 규칙으로 표현할 수 없다. 이 프로젝트는 메서드 보안({@code @EnableMethodSecurity})을 켜지 않아
 * {@code @PreAuthorize} 가 조용히 무시되므로, 각 핸들러 진입부에서 명시적으로 검사하고
 * {@link AccessDeniedException} 을 던진다(→ 403).</p>
 *
 * <p>대상 계정이 로그인 중이어도 세션은 끊기지 않는다 — 바뀐 아이디/비밀번호는 다음 로그인부터 적용된다.</p>
 */
@Controller
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AccountCredentialController {

    private static final String VIEW = "admin/admin-02-user/admin-user-credentials";

    private final AccountCredentialService accountCredentialService;
    private final SuperAdminPolicy superAdminPolicy;

    @GetMapping("/{id}/credentials")
    public String form(@PathVariable Long id, @AuthenticationPrincipal LoginUser me, Model model) {
        requireSuperAdmin(me);
        User target = accountCredentialService.get(id);
        model.addAttribute("target", target);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", AccountCredentialForm.from(target));
        }
        model.addAttribute("loginIdLocked", superAdminPolicy.isSuperAdmin(target.getLoginId()));
        model.addAttribute("backUrl", backUrl(target));
        return VIEW;
    }

    @PostMapping("/{id}/credentials")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal LoginUser me,
                         @Valid @ModelAttribute("form") AccountCredentialForm form,
                         BindingResult binding, HttpServletRequest request,
                         Model model, RedirectAttributes ra) {
        requireSuperAdmin(me);
        User target = accountCredentialService.get(id);

        if (form.hasNewPassword()) {
            if (form.getNewPassword().length() < 8) {
                binding.rejectValue("newPassword", "size", "비밀번호는 8자 이상 입력해주세요.");
            }
            if (form.isPasswordMismatch()) {
                binding.rejectValue("confirmPassword", "mismatch", "새 비밀번호가 일치하지 않습니다.");
            }
        }

        if (!binding.hasErrors()) {
            try {
                List<String> changed = accountCredentialService.apply(id, form.getLoginId(), form.getEmail(),
                        form.getNewPassword(), me.getUsername(), clientIp(request),
                        request.getHeader("User-Agent"));
                ra.addFlashAttribute("message", changed.isEmpty()
                        ? "변경된 내용이 없습니다."
                        : target.getName() + " 계정의 " + String.join("·", changed) + "을(를) 변경했습니다.");
                return "redirect:" + backUrl(target);
            } catch (DuplicateLoginIdException e) {
                binding.rejectValue("loginId", "duplicate", "이미 사용 중인 아이디입니다.");
            } catch (IllegalStateException e) {
                binding.rejectValue("loginId", "forbidden", e.getMessage());
            }
        }

        model.addAttribute("target", target);
        model.addAttribute("loginIdLocked", superAdminPolicy.isSuperAdmin(target.getLoginId()));
        model.addAttribute("backUrl", backUrl(target));
        return VIEW;
    }

    private void requireSuperAdmin(LoginUser me) {
        if (me == null || !superAdminPolicy.isSuperAdmin(me.getUsername())) {
            throw new AccessDeniedException("최고 관리자만 사용할 수 있는 기능입니다.");
        }
    }

    /** 작업 후 돌아갈 목록 — 관리자는 관리자 목록, 강사/훈련생은 각 목록의 해당 항목 선택 상태. */
    private String backUrl(User target) {
        if (target.getRole() == Role.ADMIN) {
            return "/admin/admins";
        }
        String path = target.getRole() == Role.INSTRUCTOR ? "instructors" : "trainees";
        return "/admin/users/" + path + "?selected=" + target.getId();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
