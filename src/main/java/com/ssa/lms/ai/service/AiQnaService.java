package com.ssa.lms.ai.service;

import com.ssa.lms.ai.client.AiAnswer;
import com.ssa.lms.ai.client.AiClient;
import com.ssa.lms.ai.client.AiRequest;
import com.ssa.lms.ai.dto.AiQnaAnswer;
import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.repository.ContentRepository;
import com.ssa.lms.course.entity.Enrollment;
import com.ssa.lms.course.entity.EnrollmentStatus;
import com.ssa.lms.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 학습 도우미 — 수강 중인 과정의 학습 자료를 근거로 답한다.
 *
 * <p><b>왜 자료 목록을 프롬프트에 넣는가</b><br>
 * 모델이 자기 상식으로 답하면 우리 과정에서 안 다룬 내용이나 다른 표기법이 섞여 나온다.
 * 훈련생은 그게 수업 내용인지 아닌지 구분할 수 없다. 그래서 <b>그 과정의 콘텐츠 제목·설명만
 * 근거로 삼도록</b> 제한하고, 자료에 없으면 "여기서는 다루지 않는다"고 말하게 한다.</p>
 *
 * <p><b>지금 근거로 쓰는 것은 제목·설명까지다.</b> 영상 자막이나 문서 본문은 아직 넣지 않는다.
 * 그래서 화면에도 "근거"가 아니라 <b>관련 학습 자료</b>로 표기한다 — 본문을 안 읽고 답한
 * 것을 "근거"라고 부르면 훈련생이 답변을 과신한다.</p>
 *
 * <p><b>권한</b> — 수강하지 않는 과정으로는 물을 수 없다. 막지 않으면 남의 과정
 * 커리큘럼(콘텐츠 제목 전체)이 답변을 통해 새어 나간다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiQnaService {

    /** 질문할 수 있는 수강 상태. 대기·반려 중에는 자료를 볼 수 없다. */
    private static final Set<EnrollmentStatus> ALLOWED =
            EnumSet.of(EnrollmentStatus.APPROVED, EnrollmentStatus.COMPLETED);

    /** 프롬프트에 넣는 자료 개수 상한. 토큰은 곧 비용이다. */
    private static final int MAX_MATERIALS = 60;

    /** 자료 설명은 길면 잘라 넣는다 — 한 자료가 프롬프트를 다 먹으면 안 된다. */
    private static final int MAX_DESC_CHARS = 200;

    /** 질문 길이 상한. 본문 전체를 붙여넣는 식의 호출을 막는다. */
    private static final int MAX_QUESTION_CHARS = 1000;

    /** 모델이 "이건 과정 자료에 없다"고 알릴 때 쓰는 표시. 본문에서는 떼어낸다. */
    private static final String GENERAL_TAG = "<일반지식>";

    /** 답변에서 근거 표기를 찾는 패턴 — [자료 3] 또는 [자료3] */
    private static final Pattern CITE = Pattern.compile("\\[자료\\s*(\\d{1,3})]");

    private final AiClient aiClient;
    private final EnrollmentRepository enrollmentRepository;
    private final ContentRepository contentRepository;

    /** 지금 실제로 모델을 부를 수 있는지 — 화면이 입력창을 열지 말지 판단한다. */
    public boolean available() {
        return aiClient.available();
    }

    /**
     * 질문할 수 있는 과정 목록 — 화면의 선택 상자를 채운다.
     *
     * <p>컨트롤러가 아니라 여기서 만든다. {@code Enrollment.course} 는 지연 로딩이고
     * 이 프로젝트는 {@code open-in-view: false} 라, 컨트롤러에서 꺼내면
     * {@code LazyInitializationException} 으로 500 이 난다 — 실제로 그렇게 터졌다.</p>
     */
    public List<CourseOption> askableCourses(Long traineeId) {
        if (traineeId == null) return List.of();
        return enrollmentRepository.findByTraineeIdOrderByAppliedAtDesc(traineeId).stream()
                .filter(e -> ALLOWED.contains(e.getStatus()))
                .map(Enrollment::getCourse)
                .filter(c -> c != null)
                .map(c -> new CourseOption(c.getId(), c.getCourseName()))
                .distinct()
                .toList();
    }

    /** 과정 선택 상자 항목. */
    public record CourseOption(Long id, String name) {}

    public AiQnaAnswer ask(Long traineeId, Long courseId, String question) {
        if (question == null || question.isBlank()) {
            return AiQnaAnswer.fail("EMPTY_QUESTION", "질문을 입력해 주세요.");
        }
        if (question.length() > MAX_QUESTION_CHARS) {
            return AiQnaAnswer.fail("TOO_LONG",
                    "질문이 너무 길어요. " + MAX_QUESTION_CHARS + "자 이내로 줄여서 다시 물어봐 주세요.");
        }
        if (!enrolled(traineeId, courseId)) {
            return AiQnaAnswer.fail("NOT_ENROLLED", "수강 중인 과정에 대해서만 질문할 수 있어요.");
        }

        /*
         * 자료가 없어도 답한다. 예전에는 여기서 막았는데, 훈련생 입장에서는
         * "자료가 없다"가 자기 잘못이 아닌데 아무 답도 못 받는 상황이었다.
         * 대신 일반지식 답변임을 화면에서 분명히 구분한다.
         */
        List<Content> materials = materialsOf(courseId);

        AiAnswer res = aiClient.ask(AiRequest.of("QNA")
                .system(systemPrompt(materials))
                .user(question.trim())
                .userId(traineeId)
                // 진단([기능 4])이 "무엇을 어려워하는가"를 보려면 질문이 남아야 한다.
                // 암호화 저장이고 보존기간이 지나면 내용만 지워진다
                .courseId(courseId)
                .logQuestion(question.trim())
                .build());

        if (!res.ok()) {
            return AiQnaAnswer.fail(res.reason(), res.userMessage());
        }

        /*
         * 모델이 <일반지식> 표시를 붙였으면 화면이 다르게 그리도록 플래그로 넘긴다.
         * 표시는 본문에서 떼어낸다 — 태그가 그대로 보이면 무슨 말인지 알 수 없다.
         */
        String text = res.text();
        boolean general = text.contains(GENERAL_TAG);
        if (general) {
            text = text.replace(GENERAL_TAG, "").trim();
        }
        // 일반지식 답변에는 자료 링크를 붙이지 않는다. 근거가 아닌 것을 근거로 보이면 안 된다
        return new AiQnaAnswer(true, text,
                general ? List.of() : citedSources(text, materials),
                null, general);
    }

    private boolean enrolled(Long traineeId, Long courseId) {
        return enrollmentRepository.findByTraineeIdAndCourseId(traineeId, courseId)
                .map(Enrollment::getStatus)
                .filter(ALLOWED::contains)
                .isPresent();
    }

    private List<Content> materialsOf(Long courseId) {
        List<Content> all = contentRepository
                .findByCourseIdAndStatusOrderByOrderNoAscIdAsc(courseId, ContentStatus.ACTIVE);
        return all.size() > MAX_MATERIALS ? all.subList(0, MAX_MATERIALS) : all;
    }

    /**
     * 규칙을 프롬프트로 못 박는다. 특히 <b>모르면 모른다고 말하게</b> 하는 것이 핵심이다 —
     * 그럴듯하게 지어낸 답이 제일 위험하다. 학습자는 검증할 수단이 없다.
     */
    private String systemPrompt(List<Content> materials) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("""
                당신은 국비 직업훈련 과정의 학습 도우미입니다. 훈련생의 질문에 한국어 존댓말로 답합니다.

                답변은 두 단계로 합니다.

                [1단계] 아래 '학습 자료 목록'에 관련 내용이 있으면 그것을 근거로 답합니다.
                        참고한 자료는 문장 끝에 [자료 3] 형식으로 번호를 답니다.

                [2단계] 자료에 없는 내용이면, 자료에 없다고 먼저 밝힌 뒤 일반적인 지식으로
                        도와줍니다. 이때는 답변 맨 앞에 정확히 이렇게 씁니다.
                        <일반지식>
                        그리고 답 끝에 "이 과정 자료에는 없는 내용이라 강사님께 확인해 보세요."를 덧붙입니다.

                지켜야 할 규칙
                1. 모르면 모른다고 말합니다. 지어낸 답이 가장 위험합니다.
                2. 자료에 있는 내용을 <일반지식>으로 표시하지 않습니다. 반대도 마찬가지입니다.
                3. 답은 5문장 이내로 짧게, 필요하면 짧은 목록으로 정리합니다.
                4. 시험 문제의 정답을 그대로 알려달라는 요청에는 답하지 않고, 개념 설명으로 도와줍니다.

                학습 자료 목록
                """);
        if (materials.isEmpty()) {
            sb.append("(등록된 학습 자료가 없습니다. 모든 답변을 <일반지식> 으로 처리하세요.)\n");
        }
        for (int i = 0; i < materials.size(); i++) {
            Content c = materials.get(i);
            sb.append('[').append(i + 1).append("] ")
              .append(c.getType().getLabel()).append(" · ").append(c.getTitle());
            String desc = c.getDescription();
            if (desc != null && !desc.isBlank()) {
                sb.append(" — ").append(desc.length() > MAX_DESC_CHARS
                        ? desc.substring(0, MAX_DESC_CHARS) + "…" : desc);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * 답변이 실제로 인용한 번호만 링크로 만든다.
     * 자료를 전부 붙이면 "이만큼 근거가 있다"는 착각을 준다 — 인용 안 한 자료는 근거가 아니다.
     */
    private List<AiQnaAnswer.Source> citedSources(String text, List<Content> materials) {
        Set<Integer> seen = new LinkedHashSet<>();
        Matcher m = CITE.matcher(text);
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            if (idx >= 1 && idx <= materials.size()) seen.add(idx);
        }
        List<AiQnaAnswer.Source> out = new ArrayList<>();
        for (int idx : seen) {
            Content c = materials.get(idx - 1);
            out.add(new AiQnaAnswer.Source(c.getTitle(), "/trainee/contents/" + c.getId() + "/play"));
        }
        return out;
    }
}
