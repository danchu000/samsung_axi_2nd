package com.ssa.lms.web.instructor.ai;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 강사 AI 학습진단 화면.
 *
 * <p><b>현재 단계: 화면(프론트엔드)만 구현.</b> 화면은 자기 JS 의 더미로 그리고,
 * 서버가 붙으면 {@code window._serverDiagnosis} 로 내려주면 된다.</p>
 *
 * <p>훈련생이 AI 도우미에 남긴 질문과 평가·과제 결과로 취약 영역을 진단하고
 * 보완 과제를 추천한다. <b>추천은 제안이고 배부는 강사가 확정</b>한다 —
 * AI 가 훈련생에게 직접 과제를 내리지 않는다.</p>
 * <p>접근 권한은 SecurityConfig 의 URL 규칙이 담당한다 ({@code /instructor/**} → INSTRUCTOR·ADMIN).</p>
 */
@Controller
@RequestMapping("/instructor/ai")
public class InstructorAiController {

    @GetMapping("/diagnosis")
    public String diagnosis() {
        return "instructor/ai-diagnosis";
    }
}
