package com.ssa.lms.web.trainee.ai;

import com.ssa.lms.ai.dto.AiQnaAnswer;
import com.ssa.lms.ai.service.AiQnaService;
import com.ssa.lms.auth.LoginUser;
import com.ssa.lms.job.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 훈련생 AI 학습지원 화면.
 *
 * <p>대입 스크립트는 소비 스크립트보다 <b>앞에</b> 둬야 한다 — 뒤에 두면 서버 값이
 * 영원히 더미에 가려진다 (CLAUDE.md 규칙 4).</p>
 *
 * <p>담당 화면
 * <ul>
 *   <li>{@code /trainee/ai/qna} — AI 학습 도우미. <b>모델 연동 완료</b>
 *       (과정 목록·답변 모두 서버에서 온다)</li>
 *   <li>{@code /trainee/ai/curriculum} — 학습 데이터 기반 원내 과정 추천. <b>화면만</b> —
 *       JS 안의 더미로 그린다</li>
 *   <li>{@code /trainee/ai/roadmap} — 직무 로드맵. <b>사람인 API 수집 연동 완료</b>
 *       (공고 수·요구 역량은 실제 집계값, 보유/부족 판정은 자료 제목 매칭 근사치)</li>
 * </ul>
 * <p>접근 권한은 SecurityConfig 의 URL 규칙이 담당한다 ({@code /trainee/**} → TRAINEE·ADMIN).
 * 이 프로젝트는 메서드 보안(@EnableMethodSecurity)을 켜지 않아 {@code @PreAuthorize} 는
 * 조용히 무시되므로 쓰지 않는다 — 보호되는 것처럼 보이는 편이 더 위험하다.</p>
 */
@Controller
@RequestMapping("/trainee/ai")
@RequiredArgsConstructor
public class TraineeAiController {

    private final AiQnaService aiQnaService;
    private final RoadmapService roadmapService;

    /** AI 학습 도우미 — 학습 자료 기반으로 즉시 답변하고, 안 되면 강사에게 넘긴다. */
    @GetMapping("/qna")
    public String qna(@AuthenticationPrincipal LoginUser me, Model model) {
        model.addAttribute("myCourses",
                aiQnaService.askableCourses(me == null ? null : me.getId()));
        // 키가 없거나 꺼져 있으면 화면이 미리 안내한다 — 질문을 다 쓰고 나서 실패하면 허탈하다
        model.addAttribute("aiAvailable", aiQnaService.available());
        return "trainee/ai-qna";
    }

    /**
     * 질문 → 답변. 화면이 fetch 로 부른다.
     *
     * <p><b>실패해도 200 으로 답한다.</b> 모델 장애·한도 초과는 사용자가 이해할 수 있는
     * 안내 문구로 내려주는 편이 낫다 — 채팅창에서 500 이 나면 아무 말도 못 보여준다.
     * 실패 여부는 본문의 {@code ok} 로 구분한다.</p>
     */
    @PostMapping("/qna/ask")
    @ResponseBody
    public AiQnaAnswer ask(@AuthenticationPrincipal LoginUser me, @RequestBody AskRequest req) {
        if (req == null || req.courseId() == null) {
            return AiQnaAnswer.fail("NO_COURSE", "질문할 과정을 먼저 선택해 주세요.");
        }
        return aiQnaService.ask(me.getId(), req.courseId(), req.question());
    }

    /** 화면에서 받는 요청 본문. */
    public record AskRequest(Long courseId, String question) {}

    /** 맞춤 커리큘럼 추천 — 추천 대상은 원내 개설 과정으로 한정한다. */
    @GetMapping("/curriculum")
    public String curriculum() {
        return "trainee/ai-curriculum";
    }

    /**
     * 직무 로드맵 — 주 1회 수집한 채용공고에서 요구 역량을 뽑아 내 학습과 비교한다.
     *
     * <p>수집된 공고가 없으면 {@code roadmap.jobs()} 가 빈 목록이고 {@code collectedAt} 이
     * null 이다. 화면은 그때 "아직 수집 전"을 보여준다 — 날짜를 지어내지 않는다.</p>
     */
    @GetMapping("/roadmap")
    public String roadmap(@AuthenticationPrincipal LoginUser me, Model model) {
        model.addAttribute("roadmap", roadmapService.forTrainee(me == null ? null : me.getId()));
        return "trainee/ai-roadmap";
    }
}
