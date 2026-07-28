package com.ssa.lms.web.admin.assignment;

import com.ssa.lms.assignment.dto.AssignmentForm;
import com.ssa.lms.assignment.dto.AssignmentSearchCond;
import com.ssa.lms.assignment.dto.CourseAssignmentForm;
import com.ssa.lms.assignment.dto.CourseAssignmentRow;
import com.ssa.lms.assignment.repository.AssignmentLookupRepository;
import com.ssa.lms.assignment.service.AssignmentService;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.web.PageView;
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

    /** 문제은행·시험 목록과 같은 값. 화면 페이지네이션 DOM 이 10건 기준이다. */
    private static final int PAGE_SIZE = 10;

    private final CourseAssignmentService courseAssignmentService;
    private final AssignmentService assignmentService;
    private final AssignmentLookupRepository lookupRepository;

    /**
     * 배정된 과제 목록. 행 렌더링은 기존 assignments.js 가 담당하고,
     * <b>페이징은 서버가</b> 한다 (문제은행·시험 목록과 같은 방식).
     *
     * <p><b>강사는 담당 과정 과제만 본다.</b> 이 화면은 SecurityConfig 에서
     * ADMIN·INSTRUCTOR 모두에게 열려 있는데 지금까지 아무 제한이 없어서 강사가 들어오면
     * 담당하지 않는 과정의 과제까지 전부 보였다. 권한정의서의 △(담당 과정 한정) 위반이라
     * 서버 페이징으로 바꾸면서 같이 막았다. 제한은 쿼리 조건이라 page 조작으로 못 뚫는다.</p>
     */
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser,
                       @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Long graderId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        AssignmentSearchCond cond = new AssignmentSearchCond(courseId, status, graderId, keyword);
        Page<CourseAssignmentRow> result = courseAssignmentService.searchScoped(
                cond, GRADING_URL_PREFIX,
                loginUser == null ? null : loginUser.getId(), isAdmin(loginUser),
                PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "startAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        model.addAttribute("rows", result.getContent());
        model.addAttribute("page", PageView.of(result));
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

    /** 관리자 여부 — 강사는 담당 과정으로 제한된다. */
    private static boolean isAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.getRole() == Role.ADMIN;
    }

    private void addFormReferences(Model model) {
        model.addAttribute("courseOptions", lookupRepository.findCourseOptions());
        model.addAttribute("instructorOptions", lookupRepository.findInstructorOptions());
        model.addAttribute("assignmentOptions",
                assignmentService.searchOptions(null, null, null, null));
    }
}
