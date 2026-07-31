package com.ssa.lms.course.web;

import com.ssa.lms.course.service.EnrollmentException;
import com.ssa.lms.course.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 수강신청 승인/반려. 처리 후 과정 상세로 돌아간다.
 */
@Controller
@RequestMapping("/admin/courses/{courseId}/enrollments")
@RequiredArgsConstructor
public class EnrollmentAdminController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{enrollmentId}/approve")
    public String approve(@PathVariable Long courseId, @PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.approve(enrollmentId);
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("enrollmentError", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }

    @PostMapping("/{enrollmentId}/reject")
    public String reject(@PathVariable Long courseId, @PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.reject(enrollmentId);
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("enrollmentError", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }

    @PostMapping("/{enrollmentId}/restore")
    public String restore(@PathVariable Long courseId, @PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.restore(enrollmentId);
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("enrollmentError", e.getMessage());
        }
        return "redirect:/admin/courses/" + courseId;
    }
}
