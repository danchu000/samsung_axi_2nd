package com.ssa.lms.web.admin.assignment;

import com.ssa.lms.assignment.dto.AssignmentForm;
import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentForm;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.service.AssignmentService;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 과제 관리 화면 (평가 관리 &gt; 과제 채점).
 *
 * 컨트롤러 위치는 CLAUDE.md 규칙(com.ssa.lms.web.{admin|instructor|trainee}.{도메인})을 따르고,
 * 서비스·리포지토리는 도메인 패키지(com.ssa.lms.assignment.*)에 있다.
 *
 * 접근 권한: SecurityConfig 의 /admin/evaluation/** → ADMIN, INSTRUCTOR
 * (권한정의서(1) 과제 등록/배정: 관리자 O, 강사 O)
 */
@Controller
@RequestMapping("/admin/evaluation/assignments")
@RequiredArgsConstructor
public class AdminAssignmentController {

    static final String LIST_VIEW = "admin/admin-04-evaluation/admin-evaluation-assignment";
    static final String FORM_VIEW = "admin/admin-04-evaluation/admin-evaluation-assignment-add";
    static final String GRADING_URL_PREFIX = "/admin/evaluation/assignments/";

    private final CourseAssignmentService courseAssignmentService;
    private final AssignmentService assignmentService;
    private final AssignmentLookupRepository lookupRepository;

    /** 배정된 과제 목록. 행 렌더링·페이징은 기존 assignments.js 가 클라이언트에서 담당한다. */
    @GetMapping
    public String list(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Long graderId,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        AssignmentSearchCond cond = new AssignmentSearchCond(courseId, status, graderId, keyword);
        List<CourseAssignmentRow> rows = courseAssignmentService.search(cond, GRADING_URL_PREFIX);

        model.addAttribute("rows", rows);
        model.addAttribute("cond", cond);
        model.addAttribute("courseOptions", lookupRepository.findCourseOptions());
        model.addAttribute("instructorOptions", lookupRepository.findInstructorOptions());
        return LIST_VIEW;
    }

    /** 배정 폼 (Step1 과정 → Step2 과제 정의 선택 → Step3 운영 → Step4 평가방식). */
    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new CourseAssignmentForm());
        model.addAttribute("assignmentForm", new AssignmentForm());
        addFormReferences(model);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CourseAssignmentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("assignmentForm", new AssignmentForm());
            addFormReferences(model);
            return FORM_VIEW;
        }
        try {
            courseAssignmentService.create(form);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("create.failed", e.getMessage());
            model.addAttribute("assignmentForm", new AssignmentForm());
            addFormReferences(model);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "과제를 과정에 배정했습니다.");
        return "redirect:/admin/evaluation/assignments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", courseAssignmentService.loadForm(id));
        model.addAttribute("assignmentForm", new AssignmentForm());
        addFormReferences(model);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CourseAssignmentForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("assignmentForm", new AssignmentForm());
            addFormReferences(model);
            return FORM_VIEW;
        }
        try {
            courseAssignmentService.update(id, form);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("update.failed", e.getMessage());
            model.addAttribute("assignmentForm", new AssignmentForm());
            addFormReferences(model);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "배정 내용을 수정했습니다.");
        return "redirect:/admin/evaluation/assignments";
    }

    /**
     * 과제 "정의" 등록 — 배정 화면 Step 2 안의 인라인 폼에서 호출한다.
     *
     * 콘텐츠 은행에 정의가 하나도 없으면 배정 자체를 못 하기 때문에 같은 화면에서 만들 수 있게 했다.
     * (새 화면을 만들지 않는다는 규칙에 따라 기존 배정 화면 안에 넣었다.)
     */
    @PostMapping("/definitions")
    public String createDefinition(@Valid @ModelAttribute("assignmentForm") AssignmentForm form,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", new CourseAssignmentForm());
            addFormReferences(model);
            return FORM_VIEW;
        }
        assignmentService.create(form);
        redirectAttributes.addFlashAttribute("message", "과제를 콘텐츠 은행에 등록했습니다.");
        return "redirect:/admin/evaluation/assignments/new";
    }

    /** 선택한 과제 비활성화(마감). */
    @PostMapping("/close")
    public String close(@RequestParam("ids") List<Long> ids, RedirectAttributes redirectAttributes) {
        courseAssignmentService.close(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 마감 처리했습니다.");
        return "redirect:/admin/evaluation/assignments";
    }

    /** 선택 삭제 (soft delete — 제출물은 3년 보존 요건에 따라 그대로 남는다). */
    @PostMapping("/delete")
    public String delete(@RequestParam("ids") List<Long> ids, RedirectAttributes redirectAttributes) {
        courseAssignmentService.delete(ids);
        redirectAttributes.addFlashAttribute("message", ids.size() + "건을 삭제했습니다.");
        return "redirect:/admin/evaluation/assignments";
    }

    /* ===== 내부 ===== */

    private void addFormReferences(Model model) {
        model.addAttribute("courseOptions", lookupRepository.findCourseOptions());
        model.addAttribute("instructorOptions", lookupRepository.findInstructorOptions());
        model.addAttribute("assignmentOptions",
                assignmentService.searchOptions(null, null, null, null));
    }
}
