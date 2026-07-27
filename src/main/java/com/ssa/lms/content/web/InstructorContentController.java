package com.ssa.lms.content.web;

import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import com.ssa.lms.content.service.ContentService;
import com.ssa.lms.content.service.FileStorageException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 강사 콘텐츠 관리 — 목록/등록(업로드)/수정/상태변경/삭제 (VOD·문서).
 * 경로 {@code /instructor/contents/**} 는 SecurityConfig 의 {@code /instructor/**}(INSTRUCTOR,ADMIN) 규칙으로 커버된다.
 *
 * <p>템플릿: {@code templates/instructor/contents.html}(목록),
 * {@code contents-video.html}/{@code contents-document.html}(등록·수정 폼).</p>
 */
@Controller
@RequestMapping("/instructor/contents")
@RequiredArgsConstructor
public class InstructorContentController {

    private static final String VIEW_DIR = "instructor/";

    private final ContentService contentService;

    /** 콘텐츠 목록 (선택: ?courseId= 로 과정 필터). */
    @GetMapping
    public String list(@RequestParam(required = false) Long courseId, Model model) {
        model.addAttribute("contents", contentService.listViews(courseId));
        model.addAttribute("courses", contentService.courseOptions());
        model.addAttribute("selectedCourseId", courseId);
        return VIEW_DIR + "contents";
    }

    /** 등록 폼 — 유형별 페이지(영상/문서)로 분기. */
    @GetMapping("/new")
    public String createForm(@RequestParam ContentType type, Model model) {
        ContentForm form = new ContentForm();
        form.setType(type);
        model.addAttribute("contentForm", form);
        model.addAttribute("mode", "create");
        addFormRefs(model);
        return formView(type);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ContentForm contentForm,
                         BindingResult bindingResult,
                         @RequestParam("file") MultipartFile file,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            addFormRefs(model);
            return formView(contentForm.getType());
        }
        try {
            Long id = contentService.create(contentForm, file);
            return "redirect:/instructor/contents?courseId=" + contentForm.getCourseId() + "#c" + id;
        } catch (FileStorageException e) {
            bindingResult.reject("file", e.getMessage());
            model.addAttribute("mode", "create");
            addFormRefs(model);
            return formView(contentForm.getType());
        }
    }

    /** 수정 폼. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ContentForm form = contentService.editForm(id);
        model.addAttribute("contentForm", form);
        model.addAttribute("contentId", id);
        model.addAttribute("mode", "edit");
        addFormRefs(model);
        return formView(form.getType());
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ContentForm contentForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("contentId", id);
            model.addAttribute("mode", "edit");
            addFormRefs(model);
            return formView(contentForm.getType());
        }
        contentService.update(id, contentForm, file);
        return "redirect:/instructor/contents?courseId=" + contentForm.getCourseId() + "#c" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ContentStatus status,
                               @RequestParam(required = false) Long courseId) {
        contentService.changeStatus(id, status);
        return "redirect:/instructor/contents" + (courseId != null ? "?courseId=" + courseId : "");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) Long courseId) {
        contentService.delete(id);
        return "redirect:/instructor/contents" + (courseId != null ? "?courseId=" + courseId : "");
    }

    /** 등록/수정 폼에서 공통으로 필요한 선택지. */
    private void addFormRefs(Model model) {
        model.addAttribute("courses", contentService.courseOptions());
        model.addAttribute("sessionOptions", contentService.sessionOptions());
        model.addAttribute("statuses", ContentStatus.values());
    }

    /** 유형별 등록/수정 폼 뷰 이름. */
    private String formView(ContentType type) {
        return VIEW_DIR + (type == ContentType.DOCUMENT ? "contents-document" : "contents-video");
    }
}
