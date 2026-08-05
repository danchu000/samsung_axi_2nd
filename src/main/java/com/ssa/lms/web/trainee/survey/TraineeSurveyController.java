package com.ssa.lms.web.trainee.survey;

import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.demo.SampleScreenData;
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
    private final SampleScreenData sampleScreenData;

    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser user, Model model) {
        var filled = sampleScreenData.fill(
                surveyService.findForTrainee(user.getId()),
                sampleScreenData::surveys);
        model.addAttribute("rows", filled.rows());
        model.addAttribute("sampleData", filled.sample());
        return "trainee/surveys";
    }

    /**
     * 설문 응답 화면.
     *
     * <p>없는 설문 id 로 들어오면 서비스가 {@code IllegalArgumentException} 을 던진다.
     * 그대로 두면 훈련생에게 500(Whitelabel)이 뜨므로 목록으로 되돌리고 이유를 알려준다.</p>
     *
     * <p>예시 행에서 열었으면 예시 상세를 그대로 렌더한다 — 목록만 예시로 채우고 상세는
     * 되돌려 보내면 응답 화면 자체를 캡처할 수 없었다. 제출은 아래 {@link #submit}
     * 에서 막으므로 DB 에는 아무것도 남지 않는다. 예시 기능이 꺼져 있으면
     * ({@code LMS_DEMO_SAMPLE_DATA=false}) 예시 상세도 없으니 종전대로 안내한다.</p>
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
                         Model model, RedirectAttributes redirect) {
        if (SampleScreenData.isSampleId(id)) {
            var sample = sampleScreenData.surveyDetail(id);
            if (sample == null) {
                redirect.addFlashAttribute("error",
                        "화면 예시용 설문이라 열 수 없습니다. 실제 설문이 등록되면 응답할 수 있어요.");
                return "redirect:/trainee/survey";
            }
            model.addAttribute("survey", sample);
            model.addAttribute("sampleData", true);
            model.addAttribute("form", new SurveySubmitForm());
            return "trainee/survey-detail-page";
        }
        try {
            model.addAttribute("survey", surveyService.detailForTrainee(id, user.getId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/trainee/survey";
        }
        model.addAttribute("form", new SurveySubmitForm());
        return "trainee/survey-detail-page";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal LoginUser user,
                         @ModelAttribute("form") SurveySubmitForm form, RedirectAttributes redirect) {
        if (SampleScreenData.isSampleId(id)) {
            // 예시 설문은 실제 문항 id 가 없다. 서비스까지 내려보내면 알아보기 어려운 메시지가 나온다.
            redirect.addFlashAttribute("error",
                    "화면 예시용 설문이라 제출되지 않습니다. 실제 설문이 등록되면 응답할 수 있어요.");
            return "redirect:/trainee/survey";
        }
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
