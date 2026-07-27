package com.ssa.lms.course.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 과목 추가/수정 폼. orderNo(정렬순서)는 서비스가 자동 부여한다. */
@Getter
@Setter
public class SubjectForm {

    @NotBlank(message = "과목명을 입력하세요.")
    @Size(max = 200)
    private String name;

    private String description;
}
