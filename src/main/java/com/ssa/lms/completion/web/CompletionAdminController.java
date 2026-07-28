package com.ssa.lms.completion.web;

import com.ssa.lms.completion.entity.ConfirmStatus;
import com.ssa.lms.completion.service.CertificateService;
import com.ssa.lms.completion.service.CompletionService;
import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.export.ExcelDownload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 이수 관리 — 과정별 이수 기준 설정, 자동 판정 실행, 이수 확정, 이수증 발급 진입.
 * 경로 {@code /admin/completion/**} 는 SecurityConfig 의 {@code /admin/**}(ADMIN) 규칙으로 커버된다.
 */
@Controller
@RequestMapping("/admin/completion")
@RequiredArgsConstructor
public class CompletionAdminController {

    private static final String VIEW = "admin/admin-05-attendance/admin-attendance-graduate";

    private final CompletionService completionService;
    private final CertificateService certificateService;
    private final CourseRepository courseRepository;

    @GetMapping
    public String graduate(@RequestParam(required = false) Long courseId, Model model) {
        List<Course> courses = courseRepository.findAllByOrderByStartDateDesc();
        model.addAttribute("courses", courses);

        Course selected = resolveSelected(courses, courseId);
        model.addAttribute("selectedCourse", selected);
        model.addAttribute("confirmStatuses", ConfirmStatus.values());

        if (selected != null) {
            model.addAttribute("criteria", completionService.criteriaOf(selected.getId()));
            model.addAttribute("completions", completionService.viewsByCourse(selected.getId()));
        }
        return VIEW;
    }

    @PostMapping("/criteria")
    public String saveCriteria(@RequestParam Long courseId,
                               @RequestParam int minProgressRate,
                               @RequestParam int minAttendanceRate,
                               @RequestParam(defaultValue = "false") boolean requireGradePass,
                               @RequestParam(required = false) String note,
                               RedirectAttributes ra) {
        completionService.saveCriteria(courseId, minProgressRate, minAttendanceRate, requireGradePass, note);
        ra.addFlashAttribute("message", "이수 기준을 저장했습니다.");
        return "redirect:/admin/completion?courseId=" + courseId;
    }

    @PostMapping("/evaluate")
    public String evaluate(@RequestParam Long courseId, RedirectAttributes ra) {
        int count = completionService.evaluate(courseId);
        ra.addFlashAttribute("message", "이수 자동 판정을 실행했습니다. (대상 " + count + "명)");
        return "redirect:/admin/completion?courseId=" + courseId;
    }

    @PostMapping("/{completionId}/confirm")
    public String confirm(@PathVariable Long completionId, @RequestParam Long courseId, RedirectAttributes ra) {
        completionService.confirm(completionId);
        ra.addFlashAttribute("message", "이수를 확정했습니다.");
        return "redirect:/admin/completion?courseId=" + courseId;
    }

    @PostMapping("/{completionId}/status")
    public String changeStatus(@PathVariable Long completionId,
                               @RequestParam Long courseId,
                               @RequestParam ConfirmStatus status,
                               RedirectAttributes ra) {
        completionService.changeConfirmStatus(completionId, status);
        ra.addFlashAttribute("message", "이수확정상태를 변경했습니다.");
        return "redirect:/admin/completion?courseId=" + courseId;
    }

    /** 과정별 이수 현황 xlsx 다운로드 — 이수 관리 화면 표를 그대로 내려받는다. */
    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(@RequestParam Long courseId) {
        byte[] body = completionService.completionExcel(courseId);
        return ExcelDownload.attachment("이수현황_" + completionService.courseLabel(courseId), body);
    }

    /** 이수증 PDF — 이수 확정(PASS+CONFIRMED) 수강생만. 새 탭에서 인라인 표시. */
    @GetMapping("/{completionId}/certificate")
    public ResponseEntity<byte[]> certificate(@PathVariable Long completionId) {
        byte[] pdf = certificateService.generate(completionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("certificate-" + completionId + ".pdf").build().toString())
                .body(pdf);
    }

    private Course resolveSelected(List<Course> courses, Long courseId) {
        if (courses.isEmpty()) {
            return null;
        }
        if (courseId == null) {
            return courses.get(0);
        }
        return courses.stream().filter(c -> c.getId().equals(courseId)).findFirst().orElse(courses.get(0));
    }
}
