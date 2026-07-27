package com.ssa.lms.web.admin.support;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.support.dto.QnaSearchCond;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.support.service.TutoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자·강사 공용 지원 운영 화면. 실제 데이터 권한은 principal ID로 다시 검사한다. */
@Controller
@RequestMapping("/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {
    private final QnaService qnaService;
    private final TutoringService tutoringService;

    @GetMapping("/qna")
    public String qnaList(@ModelAttribute QnaSearchCond cond, Model model) {
        model.addAttribute("rows", qnaService.searchAll(cond));
        model.addAttribute("stats", qnaService.stats());
        return "admin/support-qna";
    }
    @GetMapping("/qna/{id}")
    public String qnaDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("detail", qnaService.readDetail(id, user.getId(), true));
        return "admin/support-qna-detail";
    }
    @PostMapping("/qna/{id}/answer")
    public String answer(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, @RequestParam String content) {
        qnaService.answer(id, user.getId(), content);
        return "redirect:/admin/support/qna/" + id;
    }
    @PostMapping("/qna/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long assigneeId, RedirectAttributes ra) {
        qnaService.assign(id, assigneeId); ra.addFlashAttribute("message", "담당자를 배정했습니다.");
        return "redirect:/admin/support/qna/" + id;
    }
    @PostMapping("/qna/{id}/close")
    public String closeQna(@PathVariable Long id) { qnaService.close(id); return "redirect:/admin/support/qna/" + id; }

    @GetMapping("/tutoring")
    public String tutoringList(@RequestParam(required = false) String keyword, @RequestParam(required = false) String status, Model model) {
        model.addAttribute("rooms", tutoringService.searchAll(keyword, status, null, null));
        model.addAttribute("stats", tutoringService.stats());
        return "admin/support-tutoring";
    }
    @GetMapping("/tutoring/{id}")
    public String tutoringDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("detail", tutoringService.getDetail(id, user.getId()));
        return "admin/support-tutoring-detail";
    }
    @PostMapping("/tutoring/{id}/assign")
    public String assignTutor(@PathVariable Long id, @RequestParam Long instructorId) { tutoringService.assignInstructor(id, instructorId); return "redirect:/admin/support/tutoring/" + id; }
    @PostMapping("/tutoring/{id}/close")
    public String closeTutor(@PathVariable Long id) { tutoringService.closeRoom(id); return "redirect:/admin/support/tutoring/" + id; }
}
