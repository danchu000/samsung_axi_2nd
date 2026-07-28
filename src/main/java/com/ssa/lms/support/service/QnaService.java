package com.ssa.lms.support.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.course.repository.SessionRepository;
import com.ssa.lms.support.dto.QnaDetail;
import com.ssa.lms.support.dto.QnaForm;
import com.ssa.lms.support.dto.QnaListRow;
import com.ssa.lms.support.dto.QnaSearchCond;
import com.ssa.lms.support.dto.ResponseListRow;
import com.ssa.lms.support.dto.StaffOption;
import com.ssa.lms.support.dto.SupportFormat;
import com.ssa.lms.support.dto.SupportStats;
import com.ssa.lms.support.entity.Qna;
import com.ssa.lms.support.entity.QnaAnswer;
import com.ssa.lms.support.repository.QnaAnswerRepository;
import com.ssa.lms.support.repository.QnaRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Q&A 서비스.
 *
 * <p>권한: 권한정의서(1) 26행 기준 Q&A 는 관리자/강사/훈련생 모두 사용한다.
 * URL 경계는 SecurityConfig 가 처리하고(/admin/support/** → ADMIN·INSTRUCTOR,
 * /trainee/qna/** → TRAINEE), 여기서는 "본인 글인지" 같은 데이터 수준 검사만 한다.</p>
 *
 * <p><b>검색 제약:</b> {@code Qna.content} 는 AES-256 암호문으로 저장되므로
 * 본문 검색을 지원하지 않는다. 검색은 title(및 작성자명)로만 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    /** 화면 카드의 "최근 24시간 무응답 건" 기준. */
    private static final int NO_RESPONSE_HOURS = 24;
    /** 화면 카드의 "최근 7일 신규 질문 수" 기준. */
    private static final int RECENT_DAYS = 7;

    private final QnaRepository qnaRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SessionRepository sessionRepository;

    /* ===== 조회 ===== */

    public Page<QnaListRow> search(QnaSearchCond cond, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return qnaRepository.search(
                        cond.keywordOrNull(),
                        cond.statusOrNull(),
                        cond.categoryOrNull(),
                        cond.courseIdOrNull(),
                        cond.assigneeIdOrNull(),
                        cond.unassignedOnly(),
                        pageable)
                .map(q -> QnaListRow.of(q, now));
    }

    /** 목록 전체 (화면이 클라이언트 페이징을 하는 경우). */
    public List<QnaListRow> searchAll(QnaSearchCond cond) {
        return search(cond, Pageable.unpaged()).getContent();
    }

    /** 훈련생 본인 질문 목록. */
    public List<QnaListRow> findMine(Long userId, String keyword) {
        LocalDateTime now = LocalDateTime.now();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return qnaRepository.findByUserId(userId, kw).stream()
                .map(q -> QnaListRow.of(q, now))
                .toList();
    }

    /**
     * 담당자 배정 셀렉트용 목록 (훈련생 제외).
     *
     * <p>{@code UserRepository} 는 A 소유라 조회 메서드를 추가할 수 없어
     * findAll() 후 자바에서 거른다. 사용자 수가 늘면 A에게 {@code findByRoleIn} 을
     * 요청할 것 (a-requests.md 대상).</p>
     */
    public List<StaffOption> staffOptions() {
        return userRepository.findAll().stream()
                .filter(StaffOption::isStaff)
                .map(StaffOption::of)
                .toList();
    }

    /** 응답현황(admin-support-response.html) 의 Q&A 행. */
    public List<ResponseListRow> responseRows() {
        LocalDateTime now = LocalDateTime.now();
        return qnaRepository.search(null, null, null, null, null, false, Pageable.unpaged())
                .getContent().stream()
                .map(q -> ResponseListRow.ofQna(q, now))
                .toList();
    }

    public Qna getOrThrow(Long id) {
        return qnaRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다. id=" + id));
    }

    /**
     * 상세 조회 (조회수 증가 없음) — 목록 미리보기나 권한 검사 후 재조회용.
     */
    public QnaDetail getDetail(Long id, Long viewerId, boolean staff) {
        Qna qna = getOrThrow(id);
        assertReadable(qna, viewerId, staff);
        return QnaDetail.of(qna, qnaAnswerRepository.findByQnaId(id), LocalDateTime.now());
    }

    /**
     * 상세 조회 + 조회수 1 증가.
     *
     * <p>본인 글을 자기가 열 때는 올리지 않는다 (조회수 뻥튀기 방지).</p>
     */
    @Transactional
    public QnaDetail readDetail(Long id, Long viewerId, boolean staff) {
        Qna qna = getOrThrow(id);
        assertReadable(qna, viewerId, staff);
        boolean own = qna.getUser() != null && qna.getUser().getId().equals(viewerId);
        if (!own) {
            qna.increaseViewCount();
        }
        return QnaDetail.of(qna, qnaAnswerRepository.findByQnaId(id), LocalDateTime.now());
    }

    /** 비밀글 접근 통제 — 작성자 본인 / 관리자 / 강사만. */
    private void assertReadable(Qna qna, Long viewerId, boolean staff) {
        if (!qna.isReadableBy(viewerId, staff)) {
            // 권한 실패 → AccessDeniedException(403). IllegalStateException 은 매핑되는 advice 가
            // 없어 비밀글 URL 직접 접근 시 whitelabel 500 이 됐다.
            throw new AccessDeniedException("비밀글은 작성자와 담당자만 열람할 수 있습니다.");
        }
    }

    /* ===== 등록/수정 ===== */

    /** 훈련생 질문 등록. */
    @Transactional
    public Long create(Long userId, QnaForm form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        Course course = form.getCourseId() == null ? null
                : courseRepository.findById(form.getCourseId()).orElse(null);
        Session session = form.getSessionId() == null ? null
                : sessionRepository.findById(form.getSessionId()).orElse(null);

        Qna qna = Qna.builder()
                .user(user)
                .course(course)
                .session(session)
                .category(form.toCategory())
                .title(form.getTitle())
                .content(form.getContent())
                .status(Qna.QnaStatus.WAITING)
                .secret(form.toSecret())
                .build();

        return qnaRepository.save(qna).getId();
    }

    /** 작성자 본인 수정. */
    @Transactional
    public void update(Long id, Long userId, QnaForm form) {
        Qna qna = getOrThrow(id);
        if (qna.getUser() == null || !qna.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인이 작성한 질문만 수정할 수 있습니다.");
        }
        qna.update(form.getTitle(), form.getContent(), form.toCategory(), form.toSecret());
    }

    /* ===== 답변/배정/상태 전이 ===== */

    /**
     * 답변 등록.
     *
     * <p>{@link Qna#addAnswer} 안에서 firstResponseAt(최초 1회만)과
     * status → ANSWERED 전이가 함께 일어난다. 경과시간 계산의 기준이 되는 값이라
     * 서비스에서 따로 만지지 않는다.</p>
     */
    @Transactional
    public Long answer(Long qnaId, Long responderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("답변 내용을 입력하세요.");
        }
        Qna qna = getOrThrow(qnaId);
        User responder = userRepository.findById(responderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + responderId));

        QnaAnswer answer = QnaAnswer.builder()
                .responder(responder)
                .content(content)
                .build();

        qna.addAnswer(answer, LocalDateTime.now());
        qnaRepository.flush();
        return answer.getId();
    }

    /** 담당자 배정. 미배정 → 배정 시 상태가 WAITING 이면 IN_PROGRESS 로 넘어간다. */
    @Transactional
    public void assign(Long qnaId, Long assigneeId) {
        Qna qna = getOrThrow(qnaId);
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다. id=" + assigneeId));
        if (assignee.getRole() == Role.TRAINEE) {
            throw new IllegalArgumentException("훈련생은 Q&A 담당자로 배정할 수 없습니다.");
        }
        qna.assign(assignee);
    }

    /** 종료 처리. */
    @Transactional
    public void close(Long qnaId) {
        getOrThrow(qnaId).close(LocalDateTime.now());
    }

    /** 선택 삭제 (soft delete — BaseEntity 규칙). */
    @Transactional
    public void delete(List<Long> ids) {
        qnaRepository.deleteAllById(ids);
    }

    /* ===== 요약 카드 ===== */

    public SupportStats stats() {
        LocalDateTime now = LocalDateTime.now();

        long waiting = qnaRepository.countByStatus(Qna.QnaStatus.WAITING);
        long inProgress = qnaRepository.countByStatus(Qna.QnaStatus.IN_PROGRESS);
        long recent = qnaRepository.countByCreatedAtAfter(now.minusDays(RECENT_DAYS));
        long noResponse = qnaRepository.countByFirstResponseAtIsNullAndCreatedAtBefore(
                now.minusHours(NO_RESPONSE_HOURS));

        String avg = averageFirstResponse();

        // 조회수 TOP Q&A — 제목은 공개글만 노출한다 (비밀글 제목이 카드로 새면 안 됨).
        String topTitle = "-";
        String topDesc = "";
        List<Qna> top = qnaRepository.findAllByOrderByViewCountDesc(PageRequest.of(0, 5)).getContent();
        for (Qna q : top) {
            if (!q.isSecret()) {
                topTitle = "\"" + q.getTitle() + "\"";
                topDesc = "조회수 " + q.getViewCount() + "회";
                break;
            }
        }

        return new SupportStats(waiting, avg, inProgress, recent, topTitle, topDesc, noResponse);
    }

    /**
     * 평균 첫 응답 시간.
     *
     * <p>DB 함수로 시간 차를 구하면 H2(MySQL 모드)와 MySQL 사이에 표현이 갈려서,
     * (createdAt, firstResponseAt) 쌍만 받아 자바에서 평균을 낸다.
     * 건수가 커지면 집계 테이블로 옮길 것.</p>
     */
    private String averageFirstResponse() {
        List<Object[]> pairs = qnaRepository.findResponseTimePairs();
        if (pairs.isEmpty()) {
            return "-";
        }
        long totalMinutes = 0;
        for (Object[] pair : pairs) {
            LocalDateTime created = (LocalDateTime) pair[0];
            LocalDateTime responded = (LocalDateTime) pair[1];
            totalMinutes += Math.max(0, Duration.between(created, responded).toMinutes());
        }
        return SupportFormat.hours((double) totalMinutes / pairs.size());
    }
}
