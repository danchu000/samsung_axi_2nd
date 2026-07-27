package com.ssa.lms.web.admin.survey;

import com.ssa.lms.survey.dto.SurveyForm;
import com.ssa.lms.survey.dto.SurveyQuestionForm;
import com.ssa.lms.survey.dto.SurveySearchCond;
import com.ssa.lms.survey.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final SurveyService surveyService;

    @GetMapping
    public String list(@ModelAttribute("cond") SurveySearchCond cond, Model model) {
        model.addAttribute("rows", surveyService.search(cond));
        return "admin/admin-05-attendance/admin-attendance-survey";
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
        surveyService.update(id, form);
        redirect.addFlashAttribute("message", "설문을 수정했습니다.");
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
