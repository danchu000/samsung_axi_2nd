package com.ssa.lms.web.trainee.survey;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.survey.dto.SurveySubmitForm;
import com.ssa.lms.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 훈련생 설문 목록·상세·제출. 권한은 SecurityConfig의 /trainee/survey/** 규칙을 따른다. */
@Controller
@RequestMapping("/trainee/survey")
@RequiredArgsConstructor
public class TraineeSurveyController {
    private final SurveyService surveyService;

    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("rows", surveyService.findForTrainee(user.getId()));
        return "trainee/surveys";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user, Model model) {
        model.addAttribute("survey", surveyService.detailForTrainee(id, user.getId()));
        model.addAttribute("form", new SurveySubmitForm());
        return "trainee/survey-detail-page";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
                         @ModelAttribute("form") SurveySubmitForm form, RedirectAttributes redirect) {
        try {
            surveyService.submit(id, user.getId(), form);
            redirect.addFlashAttribute("message", "설문 응답을 제출했습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 이미 제출했거나 기간이 지난 경우. 훈련생에게 500 을 보여주면 안 된다.
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trainee/survey";
    }
}
