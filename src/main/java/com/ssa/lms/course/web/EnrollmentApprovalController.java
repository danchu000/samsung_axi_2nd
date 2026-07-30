package com.ssa.lms.course.web;

import com.ssa.lms.course.service.EnrollmentException;
import com.ssa.lms.course.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 통합 수강신청 승인 — 전체 과정의 승인 대기(APPLIED) 신청을 한 화면에서 승인/반려한다.
 * 과정 상세의 개별 승인({@link EnrollmentAdminController})과 같은 서비스를 쓰고, 처리 후 이 목록으로 돌아온다.
 */
@Controller
@RequestMapping("/admin/enrollments")
@RequiredArgsConstructor
public class EnrollmentApprovalController {

    private final EnrollmentService enrollmentService;

    /** 승인 대기 목록. */
    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("pendingEnrollments", enrollmentService.pendingEnrollments());
        return "admin/admin-03-courses/enrollment-approval";
    }

    @PostMapping("/{enrollmentId}/approve")
    public String approve(@PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.approve(enrollmentId);
            ra.addFlashAttribute("message", "수강신청을 승인했습니다.");
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enrollments/pending";
    }

    @PostMapping("/{enrollmentId}/reject")
    public String reject(@PathVariable Long enrollmentId, RedirectAttributes ra) {
        try {
            enrollmentService.reject(enrollmentId);
            ra.addFlashAttribute("message", "수강신청을 반려했습니다.");
        } catch (EnrollmentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/enrollments/pending";
    }
}
