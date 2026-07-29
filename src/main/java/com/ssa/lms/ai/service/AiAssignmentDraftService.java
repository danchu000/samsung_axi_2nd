package com.ssa.lms.ai.service;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.AssignmentDraft;
import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.service.CourseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [기능 4] 취약 영역 → <b>보완 과제 초안</b> 생성.
 *
 * <p>예전엔 진단 화면의 {@code task: '트랜잭션 격리수준 실습 과제'} 가
 * <b>하드코딩된 글자</b>였다. 취약 영역이 뭐로 나오든 같은 제목이 붙었다.
 * 이제 실제로 모델이 만든다.</p>
 *
 * <h3>비용 — 왜 내용까지 만들어도 되는가</h3>
 * 비용은 "얼마나 긴가"가 아니라 <b>누가 얼마나 자주 부르는가</b>로 정해진다.
 * <ul>
 *   <li>[기능 3] Q&amp;A — <b>훈련생</b>이 부른다. 1인 하루 50건 × 인원 → 호출이 폭증한다</li>
 *   <li>이 기능 — <b>강사</b>가 과제를 낼 때만 부른다. 하루 몇 건 수준이다</li>
 * </ul>
 * 한 번 호출에 입력 약 300~600자, 출력 약 500토큰이다. 강사가 하루 10번 써도
 * Q&amp;A 한 명이 하루에 쓰는 양보다 적다. 그래서 제목만 만들고 내용을 손으로 쓰게 하는 건
 * 비용을 아끼는 게 아니라 강사 시간만 버리는 선택이다.
 *
 * <p>대신 상한은 둔다 — {@code maxOutputTokens} 를 명시해 응답이 길어져 비용이 늘어나는 것을 막는다.</p>
 *
 * <h3>권한</h3>
 * <b>담당 과정이 아니면 만들 수 없다.</b> 막지 않으면 남의 과정 콘텐츠 목록이
 * 프롬프트를 통해 새어 나간다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiAssignmentDraftService {

    /** 프롬프트에 넣을 자료 수. 과제 하나를 만드는 데 전 차시가 필요하지 않다. */
    private static final int MAX_MATERIALS = 30;

    /** 출력 상한. 과제 설명이 A4 몇 장이 되면 아무도 안 읽는다. */
    private static final int MAX_OUTPUT_TOKENS = 700;

    private static final String TITLE_TAG = "제목:";
    private static final String DESC_TAG = "설명:";
    private static final String CRITERIA_TAG = "평가기준:";

    private final AiClient aiClient;
    private final ContentRepository contentRepository;
    private final CourseRepository courseRepository;
    private final CourseQueryService courseQueryService;

    public boolean available() {
        return aiClient.available();
    }

    /**
     * 취약 영역을 보완할 과제 초안을 만든다.
     *
     * @param instructorId 요청한 강사 — 담당 과정인지 확인한다
     * @param weakArea     취약 영역 (예: "데이터베이스·트랜잭션")
     * @param traineeCount 대상 인원. 몇 명에게 낼 과제인지에 따라 난이도 안내가 달라진다
     */
    public AssignmentDraft draft(Long instructorId, Long courseId, String weakArea, int traineeCount) {
        if (weakArea == null || weakArea.isBlank()) {
            return AssignmentDraft.fail("보완할 영역을 선택해 주세요.");
        }
        if (courseId == null || !courseQueryService.isInstructorOf(instructorId, courseId)) {
            return AssignmentDraft.fail("담당하는 과정에 대해서만 과제를 만들 수 있어요.");
        }

        String courseName = courseRepository.findById(courseId)
                .map(c -> c.getCourseName()).orElse("");

        List<Content> materials = contentRepository
                .findByCourseIdAndStatusOrderByOrderNoAscIdAsc(courseId, ContentStatus.ACTIVE);
        if (materials.size() > MAX_MATERIALS) materials = materials.subList(0, MAX_MATERIALS);

        AiAnswer res = aiClient.ask(AiRequest.of("ASSIGNMENT_DRAFT")
                .system(systemPrompt())
                .user(userPrompt(courseName, weakArea, traineeCount, materials))
                .maxOutputTokens(MAX_OUTPUT_TOKENS)
                .userId(instructorId)
                .build());

        if (!res.ok()) {
            return AssignmentDraft.fail(res.userMessage());
        }
        return parse(res.text(), weakArea);
    }

    private String systemPrompt() {
        return """
                당신은 국비 직업훈련 과정의 교육 설계를 돕습니다.
                강사가 특정 영역을 보완할 과제를 낼 수 있도록 초안을 만듭니다.

                반드시 아래 형식으로만 답합니다. 다른 말은 붙이지 않습니다.

                제목: (한 줄, 25자 이내)
                설명: (3~5문장. 무엇을 하는 과제인지, 왜 필요한지, 어떤 형태로 제출하는지)
                평가기준: (3~4개 항목을 줄바꿈으로 구분. 각 항목 끝에 배점을 괄호로 표기하고 합이 100점)

                지켜야 할 것
                1. 제공된 '학습 자료 목록' 범위 안에서 낼 수 있는 과제만 만듭니다.
                   자료에 없는 도구나 기술을 요구하지 않습니다 — 배운 적 없는 것을 시키면 안 됩니다.
                2. 훈련생이 혼자 할 수 있는 분량으로 만듭니다. 팀 과제나 외부 계정이 필요한 과제는 피합니다.
                3. 정답을 그대로 옮겨 적는 과제가 아니라, 직접 만들어 보게 하는 과제로 냅니다.
                """;
    }

    private String userPrompt(String courseName, String weakArea, int traineeCount,
                              List<Content> materials) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("과정: ").append(courseName).append('\n');
        sb.append("보완할 영역: ").append(weakArea).append('\n');
        sb.append("대상 인원: ").append(Math.max(traineeCount, 1)).append("명\n\n");
        sb.append("학습 자료 목록\n");
        for (int i = 0; i < materials.size(); i++) {
            Content c = materials.get(i);
            sb.append('[').append(i + 1).append("] ").append(c.getTitle());
            String d = c.getDescription();
            if (d != null && !d.isBlank()) {
                sb.append(" — ").append(d.length() > 120 ? d.substring(0, 120) + "…" : d);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 형식을 지시했어도 모델이 어긋나게 답할 수 있다.
     * 파싱이 깨지면 빈 값을 주는 대신 <b>영역명으로 만든 제목</b>이라도 돌려준다 —
     * 강사가 화면에서 고쳐 쓰면 되고, 빈 칸보다는 낫다.
     */
    private AssignmentDraft parse(String text, String weakArea) {
        String title = section(text, TITLE_TAG, DESC_TAG);
        String desc = section(text, DESC_TAG, CRITERIA_TAG);
        String criteria = section(text, CRITERIA_TAG, null);

        if (title.isBlank()) title = weakArea + " 보완 과제";
        if (desc.isBlank() && criteria.isBlank()) {
            // 형식이 완전히 어긋났으면 본문 전체를 설명으로 넘긴다. 버리지 않는다
            desc = text.trim();
        }
        return AssignmentDraft.ok(title, desc, criteria);
    }

    private String section(String text, String from, String to) {
        int s = text.indexOf(from);
        if (s < 0) return "";
        s += from.length();
        int e = (to == null) ? text.length() : text.indexOf(to, s);
        if (e < 0) e = text.length();
        return text.substring(s, e).trim();
    }
}
