package com.ssa.lms.web.trainee.ai;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 훈련생 AI 학습지원 화면.
 *
 * <p><b>현재 단계: 화면(프론트엔드)만 구현.</b> 서비스·엔티티는 아직 없고, 각 화면은
 * 자기 JS 안의 더미 데이터로 그린다. 서버가 붙으면 이 컨트롤러에서 모델에 값을 담고
 * 화면이 {@code window._serverRoadmap} / {@code _serverCurriculum} 등으로 내려주면 된다.</p>
 *
 * <p>대입 스크립트는 소비 스크립트보다 <b>앞에</b> 둬야 한다 — 뒤에 두면 서버 값이
 * 영원히 더미에 가려진다 (CLAUDE.md 규칙 4).</p>
 *
 * <p>담당 화면
 * <ul>
 *   <li>{@code /trainee/ai/qna} — AI 학습 도우미 (기존 글등록형 Q&amp;A 를 대체)</li>
 *   <li>{@code /trainee/ai/curriculum} — 학습 데이터 기반 원내 과정 추천</li>
 *   <li>{@code /trainee/ai/roadmap} — 채용공고 주간 수집 기반 직무 로드맵</li>
 * </ul>
 * <p>접근 권한은 SecurityConfig 의 URL 규칙이 담당한다 ({@code /trainee/**} → TRAINEE·ADMIN).
 * 이 프로젝트는 메서드 보안(@EnableMethodSecurity)을 켜지 않아 {@code @PreAuthorize} 는
 * 조용히 무시되므로 쓰지 않는다 — 보호되는 것처럼 보이는 편이 더 위험하다.</p>
 */
@Controller
@RequestMapping("/trainee/ai")
public class TraineeAiController {

    /** AI 학습 도우미 — 학습 자료 기반으로 즉시 답변하고, 안 되면 강사에게 넘긴다. */
    @GetMapping("/qna")
    public String qna() {
        return "trainee/ai-qna";
    }

    /** 맞춤 커리큘럼 추천 — 추천 대상은 원내 개설 과정으로 한정한다. */
    @GetMapping("/curriculum")
    public String curriculum() {
        return "trainee/ai-curriculum";
    }

    /** 직무 로드맵 — 주 1회 수집한 채용공고에서 요구 역량을 뽑아 내 학습과 비교한다. */
    @GetMapping("/roadmap")
    public String roadmap() {
        return "trainee/ai-roadmap";
    }
}
