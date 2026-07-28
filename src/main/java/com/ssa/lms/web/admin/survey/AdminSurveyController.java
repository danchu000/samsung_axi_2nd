package com.ssa.lms.web.admin.survey;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.export.ExcelDownload;
import com.ssa.lms.survey.dto.SurveyForm;
import com.ssa.lms.survey.dto.SurveyQuestionForm;
import com.ssa.lms.survey.dto.SurveySearchCond;
import com.ssa.lms.survey.service.SurveyReportService;
import com.ssa.lms.survey.service.SurveyService;
import com.ssa.lms.user.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 관리자 설문 등록·배포·보존삭제 화면. */
@Controller
@RequestMapping("/admin/survey")
@RequiredArgsConstructor
public class AdminSurveyController {

    private static final int PAGE_SIZE = 10;
    private final SurveyService surveyService;
    private final SurveyReportService surveyReportService;

    @GetMapping
    public String list(@ModelAttribute("cond") SurveySearchCond cond,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       Model model) {
        org.springframework.data.domain.Page<com.ssa.lms.survey.dto.SurveyListRow> result =
                surveyService.search(cond, org.springframework.data.domain.PageRequest.of(
                        Math.max(page - 1, 0), PAGE_SIZE));
        model.addAttribute("rows", result.getContent());
        model.addAttribute("page", com.ssa.lms.web.PageView.of(result));
        return "admin/admin-05-attendance/admin-attendance-survey";
    }

    /**
     * 설문 결과 리포트 다운로드 (문항별 집계).
     *
     * <p>목록 화면의 "결과" 버튼이 부른다. 별도 리포트 화면을 만들지 않고 파일로만 내리는 이유는,
     * 이 화면 계열에 결과용 정적 화면이 원래 없었고 새 화면을 만들면 안 되기 때문이다(CLAUDE.md).</p>
     *
     * <p>결과는 응답 내용이라 민감하다 — SecurityConfig 의 ADMIN·INSTRUCTOR 위에
     * {@code SurveyReportService} 가 "강사는 담당 과정만"을 한 번 더 본다. URL 의 id 만 바꿔서
     * 남의 과정 응답을 받아갈 수 없어야 한다.</p>
     */
    @GetMapping("/{id}/report.xlsx")
    @ResponseBody
    public ResponseEntity<byte[]> report(@PathVariable Long id,
                                         @AuthenticationPrincipal LoginUser loginUser) {
        boolean admin = loginUser != null && loginUser.getRole() == Role.ADMIN;
        byte[] body = surveyReportService.reportExcel(id, loginUser.getId(), admin);
        return ExcelDownload.attachment("설문결과_" + surveyReportService.surveyTitle(id), body);
    }

    @GetMapping("/new")
    public String form(Model model) {
        SurveyForm form = new SurveyForm();
        form.getQuestions().add(defaultQuestion());
        model.addAttribute("form", form);
        model.addAttribute("courses", surveyService.courseOptions());
        return "admin/admin-05-attendance/admin-attendance-survey-add";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") SurveyForm form, BindingResult errors, Model model,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) { model.addAttribute("courses", surveyService.courseOptions()); return "admin/admin-05-attendance/admin-attendance-survey-add"; }
        surveyService.create(form);
        redirect.addFlashAttribute("message", "설문을 등록하고 배포 상태를 설정했습니다.");
        return "redirect:/admin/survey";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", surveyService.loadForm(id));
        model.addAttribute("courses", surveyService.courseOptions());
        return "admin/admin-05-attendance/admin-attendance-survey-add";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SurveyForm form, Model model,
                         BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) { model.addAttribute("courses", surveyService.courseOptions()); return "admin/admin-05-attendance/admin-attendance-survey-add"; }
        try {
            surveyService.update(id, form);
            redirect.addFlashAttribute("message", "설문을 수정했습니다.");
        } catch (IllegalStateException e) {
            // 응답이 있는 설문의 문항 변경 시도 — 관리자에게 500 대신 사유를 알려준다
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/survey/" + id + "/edit";
        }
        return "redirect:/admin/survey";
    }

    @PostMapping("/status")
    public String status(@RequestParam List<Long> ids, @RequestParam String status,
                         RedirectAttributes redirect) {
        surveyService.changeStatus(ids, status);
        redirect.addFlashAttribute("message", "상태를 변경했습니다.");
        return "redirect:/admin/survey";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam List<Long> ids, RedirectAttributes redirect) {
        surveyService.delete(ids);
        redirect.addFlashAttribute("message", "설문을 보존 삭제했습니다.");
        return "redirect:/admin/survey";
    }

    private SurveyQuestionForm defaultQuestion() {
        SurveyQuestionForm question = new SurveyQuestionForm();
        question.setQuestionType("text");
        question.setRequired(true);
        return question;
    }
}
