package com.ssa.lms.course.web;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.CourseStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 과정 등록/수정 폼 DTO.
 * courseCode 는 등록 시에만 입력받고(수정 불가 — B 계약 컬럼), 나머지는 공통 편집 대상이다.
 */
@Getter
@Setter
public class CourseForm {

    /** 과정 코드 — 형식: COURSE-2026-001 (등록 시 필수, 수정 시 표시만) */
    @NotBlank(message = "과정 코드를 입력하세요.")
    @Pattern(regexp = "[A-Za-z0-9-]{3,30}", message = "영문/숫자/하이픈 3~30자로 입력하세요.")
    private String courseCode;

    @NotBlank(message = "과정명을 입력하세요.")
    @Size(max = 200)
    private String courseName;

    @Size(max = 20)
    private String cohort;

    @Size(max = 50)
    private String category;

    private String description;

    @NotNull(message = "시작일을 입력하세요.")
    private LocalDate startDate;

    @NotNull(message = "종료일을 입력하세요.")
    private LocalDate endDate;

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    private int capacity;

    @NotNull(message = "운영 상태를 선택하세요.")
    private CourseStatus status;

    /** 이수 기준 진도율(%) */
    @Min(0) @Max(100)
    private int completionProgressRate = 80;

    /** 종료일이 시작일 이후인지 (@AssertTrue 검증) */
    @AssertTrue(message = "종료일은 시작일과 같거나 이후여야 합니다.")
    public boolean isPeriodValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    /** 신규 엔티티로 변환 (등록용). */
    public Course toNewCourse() {
        return Course.builder()
                .courseCode(courseCode)
                .courseName(courseName)
                .cohort(cohort)
                .category(category)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .capacity(capacity)
                .status(status)
                .completionProgressRate(completionProgressRate)
                .build();
    }

    /** 기존 과정 값으로 폼을 채운다 (수정 화면 초기값). */
    public static CourseForm from(Course course) {
        CourseForm form = new CourseForm();
        form.courseCode = course.getCourseCode();
        form.courseName = course.getCourseName();
        form.cohort = course.getCohort();
        form.category = course.getCategory();
        form.description = course.getDescription();
        form.startDate = course.getStartDate();
        form.endDate = course.getEndDate();
        form.capacity = course.getCapacity();
        form.status = course.getStatus();
        form.completionProgressRate = course.getCompletionProgressRate();
        return form;
    }
}
