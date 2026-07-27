package com.ssa.lms.web.trainee.assignment;

import com.ssa.lms.assignment.dto.SubmissionForm;
import com.ssa.lms.assignment.entity.SubmissionFile;
import com.ssa.lms.assignment.service.SubmissionService;
import com.ssa.lms.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 훈련생 과제 화면.
 *
 * 접근 권한: SecurityConfig 의 /trainee/assignment/** → TRAINEE 전용
 * (제출은 본인만 가능해야 하므로 ADMIN 도 열어두지 않았다).
 */
@Controller
@RequestMapping("/trainee/assignment")
@RequiredArgsConstructor
public class TraineeAssignmentController {

    private static final String LIST_VIEW = "trainee/assignments";

    private final SubmissionService submissionService;

    /** 내 과제 목록. 카드 렌더링·필터는 기존 화면 인라인 JS 가 담당한다. */
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("tasks", submissionService.findMyAssignments(loginUser.getId()));
        return LIST_VIEW;
    }

    /**
     * 제출 / 재제출.
     * 재제출은 이전 행을 덮어쓰지 않고 attemptNo 를 올린 새 행으로 쌓인다.
     */
    @PostMapping(value = "/{id}/submit", consumes = "multipart/form-data")
    public String submit(@PathVariable Long id,
                         @ModelAttribute SubmissionForm form,
                         @AuthenticationPrincipal LoginUser loginUser,
                         RedirectAttributes redirectAttributes) {
        form.setCourseAssignmentId(id);
        try {
            submissionService.submit(loginUser.getId(), form);
            redirectAttributes.addFlashAttribute("message", "과제를 제출했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trainee/assignment";
    }

    /**
     * 내 첨부파일 다운로드.
     * 남의 제출물 id 를 넣어도 받을 수 없도록 소유자 검사를 한다 —
     * 저장 경로 자체가 무의미한 UUID 라 유추도 안 되지만, id 는 연속값이라 검사가 필요하다.
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId,
                                             @AuthenticationPrincipal LoginUser loginUser) {
        SubmissionFile file = submissionService.getFileOrThrow(fileId);
        if (!file.getSubmission().getUser().getId().equals(loginUser.getId())) {
            return ResponseEntity.status(403).build();
        }
        Resource resource = new FileSystemResource(submissionService.storage().resolve(file));
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
