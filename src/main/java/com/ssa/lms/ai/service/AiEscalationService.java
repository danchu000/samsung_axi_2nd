package com.ssa.lms.ai.service;

import com.ssa.lms.support.dto.QnaForm;
import com.ssa.lms.support.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * [기능 3→4] AI 가 못 푼 질문을 <b>강사에게 실제로 넘긴다</b>.
 *
 * <p><b>이전에는 화면이 거짓말을 했다.</b> "담당 강사님께 전달되었어요" 라고 띄우고
 * 아무 데도 저장하지 않았다. 훈련생은 답변을 기다리고, 강사에게는 아무것도 안 갔다.
 * AI 도우미에서 가장 나쁜 고장은 "AI 가 못 푼 것이 조용히 사라지는 것"이다.</p>
 *
 * <p>기존 Q&amp;A({@link QnaService})에 그대로 넣는다. 새 저장소를 만들지 않는 이유는,
 * 강사·관리자가 이미 보고 있는 목록이 거기이기 때문이다. 별도 테이블을 만들면
 * <b>아무도 안 보는 곳</b>에 쌓인다.</p>
 */
@Service
@RequiredArgsConstructor
public class AiEscalationService {

    /** 제목 길이 상한. Qna.title 컬럼과 목록 화면이 감당할 수 있는 길이. */
    private static final int MAX_TITLE = 80;

    /** 대화가 길어도 이만큼만 넘긴다. 강사가 읽을 수 있는 분량이어야 한다. */
    private static final int MAX_TURNS = 20;

    private final QnaService qnaService;

    /**
     * 대화를 Q&amp;A 글로 만들어 등록한다.
     *
     * @param turns 화면에 쌓인 대화. {@code who} 는 "me" 또는 "ai"
     * @return 만들어진 Q&amp;A id
     */
    public Long escalate(Long traineeId, Long courseId, List<Turn> turns) {
        if (turns == null || turns.isEmpty()) {
            throw new IllegalArgumentException("전달할 대화가 없습니다.");
        }

        QnaForm form = new QnaForm();
        form.setTitle(title(turns));
        form.setContent(body(turns));
        form.setCategory("학습");
        // 과정 공유 — 같은 곳에서 막히는 훈련생이 또 있을 수 있다.
        // 나만보기로 두면 같은 질문에 강사가 여러 번 답하게 된다
        form.setVisibility("course");
        form.setCourseId(courseId);

        return qnaService.create(traineeId, form);
    }

    /** 첫 질문을 제목으로. 강사가 목록에서 무슨 질문인지 바로 알아야 한다. */
    private String title(List<Turn> turns) {
        String first = turns.stream()
                .filter(t -> "me".equals(t.who()))
                .map(Turn::text)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElse("AI 도우미 질문");
        first = first.replaceAll("\\s+", " ").trim();
        return first.length() > MAX_TITLE ? first.substring(0, MAX_TITLE - 1) + "…" : first;
    }

    /**
     * 대화 전체를 본문으로.
     *
     * <p>AI 답변까지 같이 넘긴다 — 강사가 "AI 가 뭐라고 답했는지" 알아야
     * 같은 말을 반복하지 않고, AI 답이 틀렸다면 바로잡을 수 있다.</p>
     */
    private String body(List<Turn> turns) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("[AI 학습 도우미에서 전달된 질문]\n")
          .append("AI 답변으로 해결되지 않아 강사님께 전달합니다.\n\n")
          .append("──────── 대화 내용 ────────\n");

        List<Turn> shown = turns.size() > MAX_TURNS
                ? turns.subList(turns.size() - MAX_TURNS, turns.size())
                : turns;
        if (turns.size() > MAX_TURNS) {
            sb.append("(앞부분 ").append(turns.size() - MAX_TURNS).append("개 생략)\n\n");
        }

        for (Turn t : shown) {
            sb.append("me".equals(t.who()) ? "▶ 훈련생: " : "🤖 AI: ")
              .append(t.text() == null ? "" : t.text().trim())
              .append("\n\n");
        }
        return sb.toString().trim();
    }

    /** 화면에서 올라오는 대화 한 줄. */
    public record Turn(String who, String text) {}
}
