package com.ssa.lms.content.web;

import com.ssa.lms.content.entity.Content;
import com.ssa.lms.content.entity.ContentStatus;
import com.ssa.lms.content.entity.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 강사 콘텐츠 등록/수정 폼. 실제 업로드 파일(MultipartFile)은 컨트롤러에서 별도 파라미터로 받는다
 * (등록 시 필수, 수정 시 선택).
 */
@Getter
@Setter
public class ContentForm {

    /** 소속 과정 (필수) */
    @NotNull(message = "과정을 선택하세요.")
    private Long courseId;

    /** 배치 차시 (선택 — 과정 공용 콘텐츠는 비움) */
    private Long sessionId;

    @NotNull(message = "콘텐츠 유형을 선택하세요.")
    private ContentType type;

    @NotNull(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다.")
    private String title;

    private String description;

    /** VIDEO 재생 길이(초) — 진도율 기준 */
    private Integer durationSeconds;

    /** DOCUMENT 총 페이지 수 — 진도율 기준 */
    private Integer pageCount;

    private Integer orderNo;

    /** 이수 필수 콘텐츠 여부 (기본 true) */
    private Boolean required = Boolean.TRUE;

    private ContentStatus status = ContentStatus.ACTIVE;

    public static ContentForm from(Content c) {
        ContentForm f = new ContentForm();
        f.courseId = c.getCourse().getId();
        f.sessionId = c.getSession() != null ? c.getSession().getId() : null;
        f.type = c.getType();
        f.title = c.getTitle();
        f.description = c.getDescription();
        f.durationSeconds = c.getDurationSeconds();
        f.pageCount = c.getPageCount();
        f.orderNo = c.getOrderNo();
        f.required = c.isRequired();
        f.status = c.getStatus();
        return f;
    }
}
