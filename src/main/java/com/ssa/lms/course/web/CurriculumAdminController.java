package com.ssa.lms.course.web;

import com.ssa.lms.course.service.CurriculumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 과정 구성(과목/차시) 관리. 모든 처리 후 과정 상세로 돌아간다.
 * 경로 {@code /admin/courses/**} 는 SecurityConfig 의 {@code /admin/**}(ADMIN) 로 커버.
 */
@Controller
@RequestMapping("/admin/courses/{courseId}")
@RequiredArgsConstructor
public class CurriculumAdminController {

    private final CurriculumService curriculumService;

    /* ===== 과목 ===== */

    @PostMapping("/subjects")
    public String addSubject(@PathVariable Long courseId,
                             @Valid @ModelAttribute SubjectForm subjectForm,
                             BindingResult bindingResult, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("curriculumError", "과목명을 입력하세요.");
        } else {
            curriculumService.addSubject(courseId, subjectForm);
        }
        return redirect(courseId);
    }

    @PostMapping("/subjects/{subjectId}")
    public String updateSubject(@PathVariable Long courseId, @PathVariable Long subjectId,
                                @Valid @ModelAttribute SubjectForm subjectForm,
                                BindingResult bindingResult, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("curriculumError", "과목명을 입력하세요.");
        } else {
            curriculumService.updateSubject(subjectId, subjectForm);
        }
        return redirect(courseId);
    }

    @PostMapping("/subjects/{subjectId}/delete")
    public String deleteSubject(@PathVariable Long courseId, @PathVariable Long subjectId) {
        curriculumService.deleteSubject(subjectId);
        return redirect(courseId);
    }

    /* ===== 차시 ===== */

    @PostMapping("/subjects/{subjectId}/sessions")
    public String addSession(@PathVariable Long courseId, @PathVariable Long subjectId,
                             @Valid @ModelAttribute SessionForm sessionForm,
                             BindingResult bindingResult, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("curriculumError", "차시명을 입력하세요.");
        } else {
            curriculumService.addSession(subjectId, sessionForm);
        }
        return redirect(courseId);
    }

    @PostMapping("/sessions/{sessionId}")
    public String updateSession(@PathVariable Long courseId, @PathVariable Long sessionId,
                                @Valid @ModelAttribute SessionForm sessionForm,
                                BindingResult bindingResult, RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("curriculumError", "차시명을 입력하세요.");
        } else {
            curriculumService.updateSession(sessionId, sessionForm);
        }
        return redirect(courseId);
    }

    @PostMapping("/sessions/{sessionId}/delete")
    public String deleteSession(@PathVariable Long courseId, @PathVariable Long sessionId) {
        curriculumService.deleteSession(sessionId);
        return redirect(courseId);
    }

    private String redirect(Long courseId) {
        return "redirect:/admin/courses/" + courseId;
    }
}
