package com.ssa.lms.web.admin.exam;

import com.ssa.lms.exam.dto.QuestionForm;
import com.ssa.lms.exam.dto.QuestionListRow;
import com.ssa.lms.exam.dto.QuestionSearchCond;
import com.ssa.lms.exam.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 문제은행 화면.
 *
 * 패키지 위치는 CLAUDE.md 의 컨트롤러 규칙(com.ssa.lms.web.{admin|instructor|trainee}.{도메인})을 따른다.
 * 서비스·리포지토리는 도메인 패키지(com.ssa.lms.exam.*)에 둔다.
 *
 * 접근 권한: SecurityConfig 의 /admin/evaluation/** → ADMIN, INSTRUCTOR
 * (권한정의서(1) 19~20행 "콘텐츠 등록/수정: 관리자 O, 강사 O")
 */
@Controller
@RequestMapping("/admin/evaluation/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    /**
     * 문제은행 목록.
     *
     * 탭 전환·페이징·상세 모달은 기존 contents.js 가 클라이언트에서 담당하므로,
     * 서버는 필터링된 행을 통째로 내려주고 화면이 window._serverContentRows 로 넘긴다.
     */
    @GetMapping
    public String list(@ModelAttribute("cond") QuestionSearchCond cond, Model model) {
        // rows 는 A의 콘텐츠(영상·문서·강의) 목록과 병합되는 지점이다.
        // A의 Content 가 들어오면 같은 QuestionListRow 모양으로 만들어 여기에 합치면 된다.
        model.addAttribute("rows", questionService.searchAll(cond));
        return "admin/admin-04-evaluation/admin-evaluation-question-bank";
    }

    /** 등록 폼. */
    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new QuestionForm());
        return "admin/admin-04-evaluation/admin-evaluation-question-bank-add";
    }

    /** 등록. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") QuestionForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/admin-04-evaluation/admin-evaluation-question-bank-add";
        }
        questionService.create(form);
        redirectAttributes.addFlashAttribute("message", "문제를 등록했습니다.");
        return "redirect:/admin/evaluation/questions";
    }

    /** 수정 폼. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", questionService.loadForm(id));
        return "admin/admin-04-evaluation/admin-evaluation-question-bank-add";
    }

    /** 수정. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") QuestionForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/admin-04-evaluation/admin-evaluation-question-bank-add";
        }
        questionService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "문제를 수정했습니다.");
        return "redirect:/admin/evaluation/questions";
    }

    /** 선택 비활성화. */
    @PostMapping("/deactivate")
    public String deactivate(@RequestParam("ids") List<Long> ids,
                             RedirectAttributes redirectAttributes) {
        questionService.deactivate(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 비활성화했습니다.");
        return "redirect:/admin/evaluation/questions";
    }

    /** 선택 삭제 (soft delete). */
    @PostMapping("/delete")
    public String delete(@RequestParam("ids") List<Long> ids,
                         RedirectAttributes redirectAttributes) {
        questionService.delete(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 삭제했습니다.");
        return "redirect:/admin/evaluation/questions";
    }
}
