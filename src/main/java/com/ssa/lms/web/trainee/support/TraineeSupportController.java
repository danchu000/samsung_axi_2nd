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

/**
 * 훈련생 본인 데이터만 노출하는 Q&A·튜터링 진입점.
 *
 * <p><b>화면 매핑 (기존 정적 화면 전환 — CLAUDE.md 규칙)</b>
 * <ul>
 *   <li>{@code /trainee/qna} → {@code trainee/qna.html} (목록 + 질문하기 모달)</li>
 *   <li>{@code /trainee/qna/{id}} → {@code trainee/qna-detail.html}</li>
 *   <li>{@code /trainee/qna/tutoring}, {@code /trainee/qna/tutoring/{id}} → {@code trainee/tutoring.html}
 *       (원본이 좌측 목록 + 우측 채팅이 한 화면인 구조라 목록/상세가 같은 뷰다)</li>
 * </ul>
 */
@Controller
@RequestMapping("/trainee/qna")
@RequiredArgsConstructor
public class TraineeSupportController {

    private static final String VIEW_QNA = "trainee/qna";
    private static final String VIEW_QNA_DETAIL = "trainee/qna-detail";
    private static final String VIEW_TUTORING = "trainee/tutoring";

    private final QnaService qnaService;
    private final TutoringService tutoringService;

    /* ===== Q&A ===== */

    @GetMapping
    public String qnaList(@AuthenticationPrincipal LoginUser loginUser,
                          @RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", qnaService.findMine(loginUser.getId(), keyword));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new QnaForm());
        }
        return VIEW_QNA;
    }

    @PostMapping
    public String createQna(@AuthenticationPrincipal LoginUser loginUser,
                            @Valid @ModelAttribute("form") QnaForm form,
                            BindingResult bindingResult, RedirectAttributes ra, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rows", qnaService.findMine(loginUser.getId(), null));
            model.addAttribute("keyword", "");
            model.addAttribute("errorMessage", firstError(bindingResult));
            return VIEW_QNA;
        }
        qnaService.create(loginUser.getId(), form);
        ra.addFlashAttribute("message", "질문을 등록했습니다.");
        return "redirect:/trainee/qna";
    }

    @GetMapping("/{id}")
    public String qnaDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("detail", qnaService.readDetail(id, loginUser.getId(), false));
        return VIEW_QNA_DETAIL;
    }

    /* ===== 튜터링 ===== */

    @GetMapping("/tutoring")
    public String tutoringList(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        fillTutoring(loginUser, model);
        return VIEW_TUTORING;
    }

    @PostMapping("/tutoring")
    public String createRoom(@AuthenticationPrincipal LoginUser loginUser,
                             @Valid @ModelAttribute("form") TutoringRoomForm form,
                             BindingResult bindingResult, Model model, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            fillTutoring(loginUser, model);
            model.addAttribute("errorMessage", firstError(bindingResult));
            return VIEW_TUTORING;
        }
        Long roomId = tutoringService.createRoom(loginUser.getId(), form);
        ra.addFlashAttribute("message", "튜터링 요청을 등록했습니다.");
        return "redirect:/trainee/qna/tutoring/" + roomId;
    }

    @GetMapping("/tutoring/{id}")
    public String tutoringDetail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser, Model model) {
        // 본인 방인지 먼저 확인한다 — openDetail() 은 읽음 처리를 하므로 권한 검사 뒤에 부른다.
        var room = tutoringService.getOrThrow(id);
        if (room.getTrainee() == null || !room.getTrainee().getId().equals(loginUser.getId())) {
            throw new IllegalStateException("본인 튜터링만 열람할 수 있습니다.");
        }
        fillTutoring(loginUser, model);
        model.addAttribute("detail", tutoringService.openDetail(id, loginUser.getId()));
        return VIEW_TUTORING;
    }

    @PostMapping("/tutoring/{id}/messages")
    public String sendMessage(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser,
                              @RequestParam String content) {
        tutoringService.sendMessage(id, loginUser.getId(), content);
        return "redirect:/trainee/qna/tutoring/" + id;
    }

    /** 화면의 .alert 박스에 띄울 첫 검증 메시지. th:object 를 쓰면 th:field 가 기존 id 를 덮어써서 쓰지 않는다. */
    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("입력값을 확인해주세요.");
    }

    /** 좌측 패널(방 목록·과정 선택)은 목록/상세 어느 쪽으로 들어와도 같이 필요하다. */
    private void fillTutoring(LoginUser loginUser, Model model) {
        model.addAttribute("rooms", tutoringService.findByTrainee(loginUser.getId()));
        model.addAttribute("courses", tutoringService.courseOptions());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new TutoringRoomForm());
        }
    }
}
