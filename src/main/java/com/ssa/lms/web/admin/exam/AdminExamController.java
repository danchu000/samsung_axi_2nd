package com.ssa.lms.web.admin.exam;

import com.ssa.lms.exam.dto.ExamForm;
import com.ssa.lms.exam.dto.ExamSearchCond;
import com.ssa.lms.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 시험 생성/설정 화면.
 *
 * 패키지 위치는 CLAUDE.md 의 컨트롤러 규칙(com.ssa.lms.web.{admin|instructor|trainee}.{도메인})을 따른다.
 * 문제은행(AdminQuestionController) 과 같은 패키지에 나란히 둔다.
 *
 * 접근 권한: SecurityConfig 의 /admin/evaluation/** → ADMIN, INSTRUCTOR
 * 응시/제출(ExamAttempt, Answer)은 이 컨트롤러의 범위가 아니다.
 */
@Controller
@RequestMapping("/admin/evaluation/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private static final String LIST_VIEW = "admin/admin-04-evaluation/admin-evaluation-test";
    private static final String ADD_VIEW = "admin/admin-04-evaluation/admin-evaluation-test-add";
    private static final String EDIT_VIEW = "admin/admin-04-evaluation/admin-evaluation-test-update";

    private final ExamService examService;

    /** 시험 목록. 필터는 서버에서 처리하고 화면은 th:each 로 그린다. */
    @GetMapping
    public String list(@ModelAttribute("cond") ExamSearchCond cond, Model model) {
        model.addAttribute("rows", examService.search(cond));
        model.addAttribute("courseOptions", examService.courseOptions());
        model.addAttribute("instructorOptions", examService.instructorOptions());
        return LIST_VIEW;
    }

    /** 등록 폼. */
    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new ExamForm());
        addFormModel(model, null);
        return ADD_VIEW;
    }

    /** 등록. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") ExamForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormModel(model, form.getCourseId());
            return ADD_VIEW;
        }
        try {
            Long id = examService.create(form);
            redirectAttributes.addFlashAttribute("message", "시험을 등록했습니다.");
            return "redirect:/admin/evaluation/exams/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            // 배점 합계·합격점수 같은 도메인 검증 실패. 화면에 그대로 보여준다.
            bindingResult.reject("exam.invalid", e.getMessage());
            addFormModel(model, form.getCourseId());
            return ADD_VIEW;
        }
    }

    /** 수정 폼. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ExamForm form = examService.loadForm(id);
        model.addAttribute("form", form);
        model.addAttribute("examQuestions", examService.loadExamQuestions(id));
        model.addAttribute("targetTraineeCount", examService.countTargetTrainees(form.getCourseId()));
        addFormModel(model, form.getCourseId());
        return EDIT_VIEW;
    }

    /** 수정. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ExamForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return renderEditWithErrors(id, form, model);
        }
        try {
            examService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "시험을 수정했습니다.");
            return "redirect:/admin/evaluation/exams/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("exam.invalid", e.getMessage());
            return renderEditWithErrors(id, form, model);
        }
    }

    /**
     * 자동 출제 규칙으로 문항 확정.
     * 규칙만 저장된 상태로는 응시 문항이 재현되지 않으므로, 이 시점에 ExamQuestion(fromRule=true)으로 내린다.
     */
    @PostMapping("/{id}/materialize")
    public String materialize(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            int added = examService.materializeRules(id);
            redirectAttributes.addFlashAttribute("message",
                    "출제 규칙으로 " + added + "문항을 확정했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            // IllegalState 는 문제은행 3배수 미달 / 응시 기록 있는 시험 수정 시도.
            // 관리자에게 500 을 보여주면 안 되고, 부족분 안내를 그대로 전달해야 한다.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/evaluation/exams/" + id + "/edit";
    }

    /** 선택 비활성화 (상태를 종료로 내린다). */
    @PostMapping("/deactivate")
    public String deactivate(@RequestParam("ids") List<Long> ids,
                             RedirectAttributes redirectAttributes) {
        examService.deactivate(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 비활성화했습니다.");
        return "redirect:/admin/evaluation/exams";
    }

    /** 선택 삭제 (soft delete). */
    @PostMapping("/delete")
    public String delete(@RequestParam("ids") List<Long> ids,
                         RedirectAttributes redirectAttributes) {
        examService.delete(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 삭제했습니다.");
        return "redirect:/admin/evaluation/exams";
    }

    /* ===== 내부 ===== */

    private String renderEditWithErrors(Long id, ExamForm form, Model model) {
        model.addAttribute("examQuestions", examService.loadExamQuestions(id));
        model.addAttribute("targetTraineeCount", examService.countTargetTrainees(form.getCourseId()));
        addFormModel(model, form.getCourseId());
        return EDIT_VIEW;
    }

    /** 폼 화면 공통 모델 — 셀렉트 옵션과 문제 선택 모달용 문제 목록. */
    private void addFormModel(Model model, Long courseId) {
        model.addAttribute("courseOptions", examService.courseOptions());
        model.addAttribute("subjectOptions", examService.subjectOptions(courseId));
        model.addAttribute("sessionOptions", examService.sessionOptions(courseId));
        model.addAttribute("instructorOptions", examService.instructorOptions());
        model.addAttribute("questionPool", examService.questionPool());
    }
}
