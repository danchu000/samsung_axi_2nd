package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.Qna;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 질문 등록/수정 폼.
 *
 * 화면 대응: trainee/qna.html 의 질문하기 모달
 *  - aTitle   → title
 *  - aBody    → content
 *  - aVisibility(course/private) → secret
 *
 * <p>기존 JS 가 getElementById 로 참조하는 id(aTitle/aBody/aVisibility)는 유지하고
 * name 속성만 camelCase 로 맞춘다 (프로젝트 규칙).</p>
 */
@Getter
@Setter
public class QnaForm {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    /** 화면 값: 수업 / 진도 / 기타. 미선택 시 기타. */
    private String category;

    /** 화면 값: course(과정공유) / private(나만보기) */
    private String visibility;

    private Long courseId;

    private Long sessionId;

    public Qna.QnaCategory toCategory() {
        if (category == null || category.isBlank()) {
            return Qna.QnaCategory.ETC;
        }
        return switch (category) {
            case "수업", "LECTURE" -> Qna.QnaCategory.LECTURE;
            case "진도", "PROGRESS" -> Qna.QnaCategory.PROGRESS;
            default -> Qna.QnaCategory.ETC;
        };
    }

    /** "나만보기"면 비밀글. */
    public boolean toSecret() {
        return "private".equals(visibility);
    }
}
