package com.ssa.lms.web.admin.notice;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.notice.dto.NoticeForm;
import com.ssa.lms.notice.dto.NoticeSearchCond;
import com.ssa.lms.notice.service.NoticeService;
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
 * 관리자 공지 관리 화면.
 *
 * 패키지 위치는 CLAUDE.md 규칙(com.ssa.lms.web.{admin|instructor|trainee}.{도메인})을 따른다.
 * 접근 권한: SecurityConfig 의 /admin/notice/** → ADMIN, INSTRUCTOR.
 */
@Controller
@RequestMapping("/admin/notice")
@RequiredArgsConstructor
public class AdminNoticeController {

    /** 기존 notices.js 의 pageSize 와 동일하게 맞춘다. */
    private static final int PAGE_SIZE = 10;

    private final NoticeService noticeService;

    /** 공지 목록 — 서버 페이징. 고정 공지 먼저, 그 다음 최신순. */
    @GetMapping
    public String list(@ModelAttribute("cond") NoticeSearchCond cond,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Model model) {
        int pageIndex = Math.max(page, 1) - 1;
        Sort sort = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id"));
        var rows = noticeService.search(cond, PageRequest.of(pageIndex, PAGE_SIZE, sort));

        addPaging(model, rows, page);
        model.addAttribute("rows", rows.getContent());
        model.addAttribute("categories", noticeService.activeCategories());
        return "admin/admin-07-notice/admin-notice";
    }

    /** 등록 폼. */
    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new NoticeForm());
        model.addAttribute("categories", noticeService.activeCategories());
        model.addAttribute("courses", noticeService.selectableCourses());
        return "admin/admin-07-notice/notice-add";
    }

    /** 등록. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") NoticeForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal LoginUser loginUser,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", noticeService.activeCategories());
            model.addAttribute("courses", noticeService.selectableCourses());
            return "admin/admin-07-notice/notice-add";
        }
        Long id = noticeService.create(form, loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", "공지사항을 등록했습니다.");
        return "redirect:/admin/notice/" + id;
    }

    /** 상세. 관리자 화면에서도 조회수는 올린다(내역서 조회 이력 요건). */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getDetail(id, true));
        return "admin/admin-07-notice/notice-detail";
    }

    /** 수정 폼. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", noticeService.loadForm(id));
        model.addAttribute("categories", noticeService.activeCategories());
        model.addAttribute("courses", noticeService.selectableCourses());
        return "admin/admin-07-notice/notice-add";
    }

    /** 수정. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") NoticeForm form,
                         BindingResult bindingResult,
                         Model model,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", noticeService.activeCategories());
            model.addAttribute("courses", noticeService.selectableCourses());
            return "admin/admin-07-notice/notice-add";
        }
        noticeService.update(id, form, loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", "공지사항을 수정했습니다.");
        return "redirect:/admin/notice/" + id;
    }

    /** 삭제 (soft delete). */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        noticeService.delete(List.of(id), loginUser.getId(), loginUser.getRole());
        redirectAttributes.addFlashAttribute("message", "공지사항을 삭제했습니다.");
        return "redirect:/admin/notice";
    }

    /** 화면 하단 페이지네이션(pagination.css) 이 쓰는 값들. */
    private void addPaging(Model model, Page<?> rows, int page) {
        int totalPages = Math.max(rows.getTotalPages(), 1);
        model.addAttribute("currentPage", Math.min(Math.max(page, 1), totalPages));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", rows.getTotalElements());
    }
}
