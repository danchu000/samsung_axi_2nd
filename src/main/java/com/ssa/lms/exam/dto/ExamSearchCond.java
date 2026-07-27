package com.ssa.lms.exam.dto;

import com.ssa.lms.exam.entity.Exam;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 시험 목록 검색 조건.
 *
 * 화면 admin-evaluation-test.html 의 필터에 대응한다.
 *  courseFilter / statusFilter(활성화·비활성화) / instructorFilter / searchInput
 * 셀렉트가 "전체"를 빈 문자열로 넘기므로 여기서 null 로 정규화한다 (null = 조건 미적용).
 */
@Getter
@Setter
public class ExamSearchCond {

    private Long courseId;
    /** 화면 값: "" / active / inactive */
    private String status;
    /** 강사명 부분일치. */
    private String instructor;
    /** 시험명 부분일치. */
    private String keyword;

    private static boolean isAll(String v) {
        return v == null || v.isBlank() || "전체".equals(v);
    }

    public String keywordOrNull() {
        return isAll(keyword) ? null : keyword.trim();
    }

    public String instructorOrNull() {
        return isAll(instructor) ? null : instructor.trim();
    }

    /** 활성화 = 임시저장·예정·진행중, 비활성화 = 종료·보관. null 이면 전체. */
    public List<Exam.ExamStatus> statusesOrNull() {
        if (isAll(status)) {
            return null;
        }
        return switch (status) {
            case "active", "활성화" -> List.of(
                    Exam.ExamStatus.DRAFT, Exam.ExamStatus.SCHEDULED, Exam.ExamStatus.OPEN);
            case "inactive", "비활성화" -> List.of(
                    Exam.ExamStatus.CLOSED, Exam.ExamStatus.ARCHIVED);
            default -> throw new IllegalArgumentException("알 수 없는 상태 값: " + status);
        };
    }
}
