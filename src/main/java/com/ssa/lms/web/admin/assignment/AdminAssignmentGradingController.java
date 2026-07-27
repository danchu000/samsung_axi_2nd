package com.ssa.lms.web.admin.assignment;

import com.ssa.lms.assignment.dto.GradingForm;
import com.ssa.lms.assignment.entity.SubmissionFile;
import com.ssa.lms.assignment.service.AssignmentGradingService;
import com.ssa.lms.assignment.service.CourseAssignmentService;
import com.ssa.lms.assignment.service.SubmissionService;
import com.ssa.lms.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 관리자 과제 채점 화면.
 *
 * 채점 팝업(grading-modal.html)은 기존 화면이 {@code window.open} 으로 여는 별도 창이라
 * 그 구조를 유지한 채 서버 렌더링으로만 바꿨다. 저장 후에는 opener 를 새로고침한다.
 */
@Controller
@RequestMapping("/admin/evaluation/assignments")
@RequiredArgsConstructor
public class AdminAssignmentGradingController {

    static final String GRADING_VIEW = "admin/admin-04-evaluation/assignment-grading";
    static final String MODAL_VIEW = "admin/admin-04-evaluation/grading-modal";

    private final AssignmentGradingService gradingService;
    private final CourseAssignmentService courseAssignmentService;
    private final SubmissionService submissionService;

    /** 채점 화면 — 학생 목록(미제출자 포함) + 변경 이력. */
    @GetMapping("/{id}/grading")
    public String grading(@PathVariable Long id, Model model) {
        model.addAttribute("summary", gradingService.loadSummary(id));
        model.addAttribute("students",
                gradingService.loadStudents(id, AdminAssignmentController.GRADING_URL_PREFIX));
        model.addAttribute("notSubmitted", courseAssignmentService.findNotSubmittedUsers(id));
        return GRADING_VIEW;
    }

    /** 채점 팝업. */
    @GetMapping("/{courseAssignmentId}/submissions/{submissionId}/grading")
    public String gradingModal(@PathVariable Long courseAssignmentId,
                               @PathVariable Long submissionId,
                               Model model) {
        var detail = gradingService.loadSubmissionDetail(submissionId);
        // 폼을 기존 값으로 채워둔다. th:field 가 value/텍스트를 직접 쓰기 때문에
        // 템플릿에서 th:value/th:text 로 덧씌우면 무시되거나 충돌한다.
        GradingForm form = new GradingForm();
        form.setScore(detail.currentScore());
        form.setFeedback(detail.currentFeedback());
        form.setMemo(detail.currentMemo());
        model.addAttribute("detail", detail);
        model.addAttribute("form", form);
        model.addAttribute("courseAssignmentId", courseAssignmentId);
        model.addAttribute("actionPrefix", AdminAssignmentController.GRADING_URL_PREFIX);
        return MODAL_VIEW;
    }

    /** 채점 저장 — Grade upsert + (정정이면) GradeHistory. */
    @PostMapping("/{courseAssignmentId}/submissions/{submissionId}/grading")
    public String saveGrading(@PathVariable Long courseAssignmentId,
                              @PathVariable Long submissionId,
                              @Valid @ModelAttribute("form") GradingForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal LoginUser loginUser,
                              Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                gradingService.grade(submissionId, form, loginUser.getId());
                model.addAttribute("saved", true);
            } catch (IllegalArgumentException e) {
                bindingResult.reject("grade.failed", e.getMessage());
            }
        }
        model.addAttribute("detail", gradingService.loadSubmissionDetail(submissionId));
        model.addAttribute("courseAssignmentId", courseAssignmentId);
        model.addAttribute("actionPrefix", AdminAssignmentController.GRADING_URL_PREFIX);
        return MODAL_VIEW;
    }

    /**
     * 첨부파일 다운로드.
     * 저장 경로는 웹 루트 밖이라 이 엔드포인트를 거치지 않으면 받을 수 없다.
     * 원본 파일명은 여기서만 되살린다.
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        SubmissionFile file = submissionService.getFileOrThrow(fileId);
        Resource resource = new FileSystemResource(submissionService.storage().resolve(file));
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
