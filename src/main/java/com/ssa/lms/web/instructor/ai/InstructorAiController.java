package com.ssa.lms.web.instructor.ai;

import com.ssa.lms.ai.dto.AssignmentDraft;
import com.ssa.lms.ai.service.AiAssignmentDraftService;
import com.ssa.lms.auth.LoginUser;
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
 * 강사 AI 학습진단 화면. [기능 4]
 *
 * <p>훈련생이 AI 도우미에 남긴 질문과 평가·과제 결과로 취약 영역을 진단하고
 * 보완 과제를 추천한다. <b>추천은 제안이고 배부는 강사가 확정</b>한다 —
 * AI 가 훈련생에게 직접 과제를 내리지 않는다.</p>
 *
 * <p><b>진행 상태</b>
 * <ul>
 *   <li>진단 목록(취약 영역·근거) — 아직 화면 더미. 진단 엔진이 없다</li>
 *   <li>과제 초안 생성 — <b>실제 모델 호출</b>. 예전엔 제목이 하드코딩이었다</li>
 * </ul>
 *
 * <p>접근 권한은 SecurityConfig 의 URL 규칙이 담당한다
 * ({@code /instructor/**} → INSTRUCTOR·ADMIN). 이 프로젝트는 메서드 보안을 켜지 않아
 * {@code @PreAuthorize} 는 조용히 무시되므로 쓰지 않는다 — 담당 과정 확인은
 * 서비스 안에서 직접 한다.</p>
 */
@Controller
@RequestMapping("/instructor/ai")
@RequiredArgsConstructor
public class InstructorAiController {

    private final AiAssignmentDraftService draftService;

    @GetMapping("/diagnosis")
    public String diagnosis(Model model) {
        // 키가 없으면 화면이 [AI로 과제 만들기] 버튼을 비활성으로 그린다 —
        // 눌러 놓고 기다렸다 실패하면 허탈하다
        model.addAttribute("aiAvailable", draftService.available());
        return "instructor/ai-diagnosis";
    }

    /**
     * 취약 영역 → 과제 초안. 화면이 fetch 로 부른다.
     *
     * <p>실패해도 200 으로 답한다 — 모달에서 500 이 나면 보여줄 말이 없다.
     * 성공 여부는 본문의 {@code ok} 로 구분한다.</p>
     */
    @PostMapping("/assignment-draft")
    @ResponseBody
    public AssignmentDraft assignmentDraft(@AuthenticationPrincipal LoginUser me,
                                           @RequestBody DraftRequest req) {
        if (req == null) {
            return AssignmentDraft.fail("요청이 비어 있어요.");
        }
        return draftService.draft(me.getId(), req.courseId(), req.weakArea(),
                req.traineeCount() == null ? 1 : req.traineeCount());
    }

    public record DraftRequest(Long courseId, String weakArea, Integer traineeCount) {}
}
