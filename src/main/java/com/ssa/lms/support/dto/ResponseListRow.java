package com.ssa.lms.support.dto;

import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.TutoringRoom;

import java.time.LocalDateTime;

/**
 * 응답 현황 한 행 — Q&A 와 튜터링을 한 표에 섞어 보여준다.
 *
 * 화면 대응: admin-06-support/admin-support-response.html 의 #responseTableBody.
 * 원본 더미는 static/js/tutoring.js 의 {@code responseData} 배열이고,
 * 컬럼(구분/유형/상태/과정/요청자/담당자/경과시간/관리)이 그대로 대응된다.
 *
 * <p><b>경과시간 규칙</b> (tutoring.js 의 "26h"):
 * 첫 응답이 없으면 {@code now - createdAt}, 있으면 {@code firstResponseAt - createdAt}.
 * 담당자 미배정은 {@code assignee == null} → "미배정".</p>
 *
 * @param detailUrl 화면의 "상세 페이지로 이동" 버튼 목적지
 */
public record ResponseListRow(
        Long id,
        /** 구분: QnA / 튜터링 */
        String type,
        String category,
        String status,
        String statusClass,
        String course,
        String requester,
        String manager,
        boolean assigned,
        String elapsed,
        /** 정렬용 원시값 (분) */
        long elapsedMinutes,
        String detailUrl
) {

    public static ResponseListRow ofQna(Qna q, LocalDateTime now) {
        return new ResponseListRow(
                q.getId(),
                "QnA",
                QnaListRow.categoryLabel(q.getCategory()),
                QnaListRow.statusLabel(q.getStatus()),
                QnaListRow.statusClass(q.getStatus()),
                SupportFormat.courseSession(
                        q.getCourse() == null ? null : q.getCourse().getCourseName(),
                        q.getSession() == null ? null : q.getSession().getSeq()),
                q.getUser() == null ? "-" : q.getUser().getName(),
                q.getAssignee() == null ? "미배정" : q.getAssignee().getName(),
                q.getAssignee() != null,
                SupportFormat.elapsed(q.getCreatedAt(), q.getFirstResponseAt(), now),
                SupportFormat.elapsedMinutes(q.getCreatedAt(), q.getFirstResponseAt(), now),
                "/admin/support/qna/" + q.getId()
        );
    }

    /**
     * 튜터링 방 행.
     *
     * @param firstResponseAt 강사가 처음 답장한 시각 (없으면 null → 계속 경과)
     */
    public static ResponseListRow ofTutoring(TutoringRoom r, LocalDateTime firstResponseAt, LocalDateTime now) {
        return new ResponseListRow(
                r.getId(),
                "튜터링",
                "튜터링",
                TutoringRoomListRow.statusLabel(r.getStatus()),
                TutoringRoomListRow.statusClass(r.getStatus()),
                r.getCourse() == null ? "-" : r.getCourse().getCourseName(),
                r.getTrainee() == null ? "-" : r.getTrainee().getName(),
                r.getInstructor() == null ? "미배정" : r.getInstructor().getName(),
                r.getInstructor() != null,
                SupportFormat.elapsed(r.getCreatedAt(), firstResponseAt, now),
                SupportFormat.elapsedMinutes(r.getCreatedAt(), firstResponseAt, now),
                "/admin/support/tutoring/" + r.getId()
        );
    }
}
