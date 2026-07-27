package com.ssa.lms.assignment.service;

import com.ssa.lms.assignment.dto.AssignmentForm;
import com.ssa.lms.assignment.dto.AssignmentOptionRow;
import com.ssa.lms.assignment.entity.Assignment;
import com.ssa.lms.assignment.repository.AssignmentRepository;
import com.ssa.lms.exam.entity.Difficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과제 "정의" 서비스 — 재사용 원본의 CRUD.
 *
 * 정의를 지워도 이미 배정된 CourseAssignment 와 제출물은 남는다
 * (Assignment 에 @SQLDelete 가 걸려 있어 물리 삭제가 아니다. 3년 보존 요건).
 *
 * 권한정의서(1) 19~20행: 콘텐츠 등록·수정 관리자 O / 강사 O →
 * 접근 통제는 SecurityConfig 의 /admin/evaluation/** (ADMIN, INSTRUCTOR) 가 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public Assignment getOrThrow(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("과제를 찾을 수 없습니다. id=" + id));
    }

    /** 배정 화면 Step 2 후보 목록. 화면 값("전체"/빈 문자열)은 여기서 null 로 정규화한다. */
    public List<AssignmentOptionRow> searchOptions(String keyword, String difficulty,
                                                   String category, String status) {
        List<Assignment> found = assignmentRepository.search(
                blankToNull(keyword),
                parseDifficulty(difficulty),
                blankToNull(category),
                parseStatus(status));
        return found.stream().map(AssignmentOptionRow::of).toList();
    }

    /** 콘텐츠 은행 목록(문제은행 화면의 "과제" 탭)에 합칠 행. */
    public List<Assignment> findAll() {
        return assignmentRepository.search(null, null, null, null);
    }

    public AssignmentForm loadForm(Long id) {
        return AssignmentForm.from(getOrThrow(id));
    }

    @Transactional
    public Long create(AssignmentForm form) {
        Assignment assignment = Assignment.builder()
                .title(form.getTitle().strip())
                .description(form.getDescription())
                .defaultSubmissionType(form.toSubmissionType())
                .maxScore(form.getMaxScore())
                .category(blankToNull(form.getCategory()))
                .difficulty(form.toDifficulty())
                .status(form.toStatus())
                .build();
        return assignmentRepository.save(assignment).getId();
    }

    @Transactional
    public void update(Long id, AssignmentForm form) {
        getOrThrow(id).update(
                form.getTitle().strip(), form.getDescription(), form.toSubmissionType(),
                form.getMaxScore(), blankToNull(form.getCategory()),
                form.toDifficulty(), form.toStatus());
    }

    @Transactional
    public void deactivate(List<Long> ids) {
        assignmentRepository.findAllById(ids)
                .forEach(a -> a.changeStatus(Assignment.AssignmentStatus.ARCHIVED));
    }

    /** soft delete (@SQLDelete). 배정·제출물은 그대로 남는다. */
    @Transactional
    public void delete(List<Long> ids) {
        assignmentRepository.deleteAllById(ids);
    }

    /* ===== 내부 ===== */

    private static String blankToNull(String v) {
        return (v == null || v.isBlank() || "전체".equals(v)) ? null : v.trim();
    }

    private static Difficulty parseDifficulty(String v) {
        String normalized = blankToNull(v);
        return normalized == null ? null : Difficulty.valueOf(normalized.toUpperCase());
    }

    private static Assignment.AssignmentStatus parseStatus(String v) {
        String normalized = blankToNull(v);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "Active", "ACTIVE" -> Assignment.AssignmentStatus.ACTIVE;
            case "Archived", "ARCHIVED" -> Assignment.AssignmentStatus.ARCHIVED;
            default -> throw new IllegalArgumentException("알 수 없는 상태 값: " + v);
        };
    }
}
