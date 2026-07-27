package com.ssa.lms.assignment.dto;

import com.ssa.lms.assignment.entity.AssignmentCriteria;
import com.ssa.lms.assignment.entity.CourseAssignment;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 과정에 과제 배정 폼 (admin-evaluation-assignment-add.html).
 *
 * 화면 Step 구성 그대로:
 *  Step1 courseSelect / Step2 assignmentId(콘텐츠 은행 선택)
 *  Step3 startDate·endDate·allowResubmit·allowLate·score
 *  Step4 autoGrading·grader·criteria[]
 *
 * 화면의 id 속성은 건드리지 않고 name 만 서버 필드명에 맞췄다 —
 * 해당 페이지 JS 가 getElementById 로만 접근하기 때문에 동작에 영향이 없다.
 */
@Getter
@Setter
public class CourseAssignmentForm {

    private Long id;

    @NotNull(message = "과정을 선택하세요.")
    private Long courseId;

    @NotNull(message = "콘텐츠 은행에서 과제를 선택하세요.")
    private Long assignmentId;

    /** 이 과정에서만 다르게 안내할 설명. 비우면 정의의 설명을 쓴다. */
    private String overrideDescription;

    /** 비우면 선택한 과제 정의의 기본 제출 유형을 따른다. */
    private String submissionType;

    @NotNull(message = "시작일을 입력하세요.")
    private LocalDate startDate;

    @NotNull(message = "종료일을 입력하세요.")
    private LocalDate endDate;

    private boolean allowLate;
    private boolean allowResubmit;

    /** 재제출 허용 시 최대 횟수. 미입력이면 1. 허용 안 하면 서버가 0 으로 만든다. */
    private Integer maxResubmit;

    private boolean autoGrading;

    @NotNull(message = "배점을 입력하세요.")
    @Min(value = 1, message = "배점은 1 이상이어야 합니다.")
    private Integer score;

    /** 합격 기준. 비우면 배점의 60%. */
    private Integer passScore;

    @NotNull(message = "채점자를 선택하세요.")
    private Long graderId;

    /** 화면 값: DRAFT / SCHEDULED / OPEN / CLOSED. 기본 OPEN. */
    private String status = "OPEN";

    /** 화면의 criteria[] 반복 입력. 내용과 배점이 같은 순서로 들어온다. */
    private List<String> criteriaContents = new ArrayList<>();
    private List<Integer> criteriaScores = new ArrayList<>();

    /* ===== 변환 ===== */

    /** 시작일은 그날 00:00, 종료일은 그날 23:59 로 본다 (화면이 날짜만 입력받는다). */
    public LocalDateTime startAt() {
        return startDate.atStartOfDay();
    }

    public LocalDateTime endAt() {
        return endDate.atTime(LocalTime.of(23, 59));
    }

    public CourseAssignment.CourseAssignmentStatus toStatus() {
        if (status == null || status.isBlank()) {
            return CourseAssignment.CourseAssignmentStatus.OPEN;
        }
        return CourseAssignment.CourseAssignmentStatus.valueOf(status.trim().toUpperCase());
    }

    public int normalizedMaxResubmit() {
        if (!allowResubmit) {
            return 0;
        }
        return (maxResubmit == null || maxResubmit < 1) ? 1 : maxResubmit;
    }

    /**
     * 입력된 채점 기준만 순번을 매겨 반환.
     * 배점이 비어 있으면 0 으로 본다 — 합계 검증은 서비스가 한다.
     */
    public List<AssignmentCriteria> toCriteria() {
        List<AssignmentCriteria> result = new ArrayList<>();
        int seq = 0;
        for (int i = 0; i < criteriaContents.size(); i++) {
            String content = criteriaContents.get(i);
            if (content == null || content.isBlank()) {
                continue;
            }
            Integer s = (criteriaScores != null && criteriaScores.size() > i)
                    ? criteriaScores.get(i) : null;
            seq++;
            result.add(AssignmentCriteria.builder()
                    .seq(seq)
                    .content(content.strip())
                    .score(s == null ? 0 : s)
                    .build());
        }
        return result;
    }

    public static CourseAssignmentForm from(CourseAssignment ca) {
        CourseAssignmentForm form = new CourseAssignmentForm();
        form.id = ca.getId();
        form.courseId = ca.getCourse().getId();
        form.assignmentId = ca.getAssignment().getId();
        form.overrideDescription = ca.getOverrideDescription();
        form.submissionType = ca.getSubmissionType().name();
        form.startDate = ca.getStartAt().toLocalDate();
        form.endDate = ca.getEndAt().toLocalDate();
        form.allowLate = ca.isAllowLate();
        form.allowResubmit = ca.isAllowResubmit();
        form.maxResubmit = ca.getMaxResubmit();
        form.autoGrading = ca.isAutoGrading();
        form.score = ca.getScore();
        form.passScore = ca.getPassScore();
        form.graderId = ca.getGrader() == null ? null : ca.getGrader().getId();
        form.status = ca.getStatus().name();
        ca.getCriteria().forEach(c -> {
            form.criteriaContents.add(c.getContent());
            form.criteriaScores.add(c.getScore());
        });
        return form;
    }
}
