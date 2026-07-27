package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.entity.CourseAssignment;
import com.ssa.lms.exam.entity.Difficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 과제 "정의" 등록/수정 폼.
 *
 * 배정 화면(admin-evaluation-assignment-add.html) Step 2 안의 "새 과제 정의 등록" 블록에서 쓴다.
 * 정의는 여러 과정에 재사용되는 원본이라 기간·채점자 같은 운영 값은 여기 없다 —
 * 그건 전부 {@link CourseAssignmentForm} 쪽이다.
 */
@Getter
@Setter
public class AssignmentForm {

    private Long id;

    @NotBlank(message = "과제명을 입력하세요.")
    private String title;

    private String description;

    /** 화면 값: DOCUMENT / LINK / TEXT / DOCUMENT_TEXT */
    @NotBlank(message = "제출 유형을 선택하세요.")
    private String defaultSubmissionType = "DOCUMENT";

    @NotNull(message = "배점을 입력하세요.")
    @Min(value = 1, message = "배점은 1 이상이어야 합니다.")
    private Integer maxScore = 100;

    private String category;

    /** 화면 값: easy / medium / hard */
    private String difficulty = "medium";

    /** 화면 값: Active / Archived */
    private String status = "Active";

    public CourseAssignment.SubmissionType toSubmissionType() {
        return SubmissionTypes.parse(defaultSubmissionType);
    }

    public Difficulty toDifficulty() {
        return (difficulty == null || difficulty.isBlank())
                ? Difficulty.MEDIUM
                : Difficulty.valueOf(difficulty.toUpperCase());
    }

    public Assignment.AssignmentStatus toStatus() {
        return "Archived".equals(status) || "ARCHIVED".equals(status)
                ? Assignment.AssignmentStatus.ARCHIVED
                : Assignment.AssignmentStatus.ACTIVE;
    }

    public static AssignmentForm from(Assignment a) {
        AssignmentForm form = new AssignmentForm();
        form.id = a.getId();
        form.title = a.getTitle();
        form.description = a.getDescription();
        form.defaultSubmissionType = a.getDefaultSubmissionType().name();
        form.maxScore = a.getMaxScore();
        form.category = a.getCategory();
        form.difficulty = a.getDifficulty() == null ? "medium" : a.getDifficulty().name().toLowerCase();
        form.status = a.getStatus() == Assignment.AssignmentStatus.ACTIVE ? "Active" : "Archived";
        return form;
    }
}
