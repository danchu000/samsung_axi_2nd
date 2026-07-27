package com.ssa.lms.web.trainee.support;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.support.dto.QnaForm;
import com.ssa.lms.support.dto.TutoringRoomForm;
import com.ssa.lms.support.service.QnaService;
import com.ssa.lms.support.service.TutoringService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 훈련생 본인 데이터만 노출하는 Q&A·튜터링 진입점. */
@Controller
@RequestMapping("/trainee/qna")
@RequiredArgsConstructor
public class TraineeSupportController {
    private final QnaService qnaService;
    private final TutoringService tutoringService;

    @GetMapping
    public String qnaList(@AuthenticationPrincipal LoginUser loginUser, @RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", qnaService.findMine(loginUser.getId(), keyword));
        model.addAttribute("form", new QnaForm());
        return "trainee/support-qna";
    }

    @PostMapping
    public String createQna(@AuthenticationPrincipal LoginUser loginUser, @Valid @ModelAttribute("form") QnaForm form,
                            BindingResult bindingResult, RedirectAttributes ra, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rows", qnaService.findMine(loginUser.getId(), null));
            return "trainee/support-qna";
        }
        qnaService.create(loginUser.getId(), form);
        ra.addFlashAttribute("message", "질문을 등록했습니다.");
        return "redirect:/trainee/qna";
    }

    @GetMapping("/{id}")
    public String qnaDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("detail", qnaService.readDetail(id, loginUser.getId(), false));
        return "trainee/support-qna-detail";
    }

    @GetMapping("/tutoring")
    public String tutoringList(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("rooms", tutoringService.findByTrainee(loginUser.getId()));
        model.addAttribute("form", new TutoringRoomForm());
        return "trainee/support-tutoring";
    }

    @PostMapping("/tutoring")
    public String createRoom(@AuthenticationPrincipal LoginUser loginUser, @Valid @ModelAttribute("form") TutoringRoomForm form,
                             BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", tutoringService.findByTrainee(loginUser.getId()));
            return "trainee/support-tutoring";
        }
        tutoringService.createRoom(loginUser.getId(), form);
        ra.addFlashAttribute("message", "튜터링 요청을 등록했습니다.");
        return "redirect:/trainee/qna/tutoring";
    }

    @GetMapping("/tutoring/{id}")
    public String tutoringDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser, Model model) {
        var detail = tutoringService.openDetail(id, loginUser.getId());
        if (!detail.traineeId().equals(loginUser.getId())) throw new IllegalStateException("본인 튜터링만 열람할 수 있습니다.");
        model.addAttribute("detail", detail);
        return "trainee/support-tutoring-detail";
    }

    @PostMapping("/tutoring/{id}/messages")
    public String sendMessage(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser,
                              @RequestParam String content, RedirectAttributes ra) {
        tutoringService.sendMessage(id, loginUser.getId(), content);
        return "redirect:/trainee/qna/tutoring/" + id;
    }
}
