package com.ssa.lms.course.web;

import com.ssa.lms.course.service.CourseInstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 강사-과정 매핑. 처리 후 과정 상세로 돌아간다.
 */
@Controller
@RequestMapping("/admin/courses/{courseId}/instructors")
@RequiredArgsConstructor
public class InstructorAdminController {

    private final CourseInstructorService courseInstructorService;

    @PostMapping
    public String assign(@PathVariable Long courseId,
                         @RequestParam Long userId,
                         @RequestParam(defaultValue = "false") boolean primary,
                         RedirectAttributes ra) {
        try {
            courseInstructorService.assign(courseId, userId, primary);
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("instructorError", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }

    @PostMapping("/{mappingId}/delete")
    public String unassign(@PathVariable Long courseId, @PathVariable Long mappingId) {
        courseInstructorService.unassign(mappingId);
        return "redirect:/admin/courses/" + courseId;
    }
}
