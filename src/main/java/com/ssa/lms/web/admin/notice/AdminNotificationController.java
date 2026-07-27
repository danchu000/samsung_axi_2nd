package com.ssa.lms.web.admin.notice;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.notice.dto.NotificationForm;
import com.ssa.lms.notice.dto.NotificationSearchCond;
import com.ssa.lms.notice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 알림 관리 화면 (admin-alarm.html / admin-alarm-add.html).
 *
 * 클래스명은 표준어인 Notification 으로, URL 은 화면 파일명에 맞춰 /admin/notice/alarms 로 간다
 * (Notification 엔티티 주석의 alarm/alram 정리 결정).
 * 접근 권한: SecurityConfig 의 /admin/notice/** → ADMIN, INSTRUCTOR.
 */
@Controller
@RequestMapping("/admin/notice/alarms")
@RequiredArgsConstructor
public class AdminNotificationController {

    private static final int PAGE_SIZE = 10;

    private final NotificationService notificationService;

    /** 알림 내역 목록. */
    @GetMapping
    public String list(@ModelAttribute("cond") NotificationSearchCond cond,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @AuthenticationPrincipal LoginUser loginUser,
                       Model model) {
        int pageIndex = Math.max(page, 1) - 1;
        Page<?> rows = notificationService.search(cond, loginUser.getId(),
                PageRequest.of(pageIndex, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "sendAt")));

        model.addAttribute("rows", rows.getContent());
        model.addAttribute("currentPage", Math.min(Math.max(page, 1), Math.max(rows.getTotalPages(), 1)));
        model.addAttribute("totalPages", Math.max(rows.getTotalPages(), 1));
        model.addAttribute("totalCount", rows.getTotalElements());
        return "admin/admin-07-notice/admin-alarm";
    }

    /** 등록 폼 (기존 화면은 window.open 으로 뜨는 모달 페이지다). */
    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new NotificationForm());
        model.addAttribute("row", null);
        return "admin/admin-07-notice/admin-alarm-add";
    }

    /** 상세/수정 폼 — 같은 모달 화면을 재사용한다. */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model) {
        model.addAttribute("form", notificationService.loadForm(id));
        model.addAttribute("row", notificationService.getRow(id, loginUser.getId()));
        return "admin/admin-07-notice/admin-alarm-add";
    }

    /** 등록. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") NotificationForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/admin-07-notice/admin-alarm-add";
        }
        notificationService.create(form, loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", "알림을 등록했습니다.");
        return "redirect:/admin/notice/alarms";
    }

    /** 수정. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") NotificationForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/admin-07-notice/admin-alarm-add";
        }
        notificationService.update(id, form, loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", "알림을 수정했습니다.");
        return "redirect:/admin/notice/alarms";
    }

    /** 선택 삭제 (soft delete). */
    @PostMapping("/delete")
    public String delete(@RequestParam("ids") List<Long> ids,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        notificationService.delete(ids, loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 삭제했습니다.");
        return "redirect:/admin/notice/alarms";
    }

    /** 선택 읽음처리 — 본인이 수신자인 건만 반영된다. */
    @PostMapping("/read")
    public String markRead(@RequestParam("ids") List<Long> ids,
                           @AuthenticationPrincipal LoginUser loginUser,
                           RedirectAttributes redirectAttributes) {
        int changed = notificationService.markRead(ids, loginUser.getId());
        redirectAttributes.addFlashAttribute("message", changed + "건을 읽음 처리했습니다.");
        return "redirect:/admin/notice/alarms";
    }
}
