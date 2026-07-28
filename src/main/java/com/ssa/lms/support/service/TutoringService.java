package com.ssa.lms.support.service;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.repository.CourseRepository;
import com.ssa.lms.support.dto.CourseOption;
import com.ssa.lms.support.dto.ResponseListRow;
import com.ssa.lms.support.dto.StaffOption;
import com.ssa.lms.support.dto.SupportFormat;
import com.ssa.lms.support.dto.SupportStats;
import com.ssa.lms.support.dto.TutoringRoomDetail;
import com.ssa.lms.support.dto.TutoringRoomForm;
import com.ssa.lms.support.dto.TutoringRoomListRow;
import com.ssa.lms.support.entity.TutoringMessage;
import com.ssa.lms.support.entity.TutoringRoom;
import com.ssa.lms.support.repository.TutoringMessageRepository;
import com.ssa.lms.support.repository.TutoringRoomRepository;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 1:1 튜터링 서비스.
 *
 * <p><b>권한 (권한정의서(1) 26행 / (2) 18행)</b><br>
 * 1:1 채팅 자체는 관리자·강사·훈련생 모두 사용하지만,
 * <b>채팅 메시지는 관리자 R / 강사 R / 훈련생 C·R</b> 이다.
 * 즉 메시지를 <b>생성</b>할 수 있는 것은 훈련생과 (대화 상대인) 담당 강사뿐이고,
 * 관리자는 열람만 한다. 메시지 수정·삭제 기능은 어느 역할에도 두지 않는다
 * (그래서 이 서비스에 update/delete 메시지 메서드가 없다).</p>
 *
 * <p><b>검색 제약:</b> {@code TutoringMessage.content} 는 AES-256 암호문으로 저장돼
 * 본문 검색이 불가능하다. 검색은 방 제목과 훈련생 이름으로만 한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutoringService {

    private static final int NO_RESPONSE_HOURS = 24;
    private static final int RECENT_DAYS = 7;

    private final TutoringRoomRepository roomRepository;
    private final TutoringMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    /* ===== 조회 ===== */

    public Page<TutoringRoomListRow> search(String keyword, String status, Long courseId,
                                            Long instructorId, Pageable pageable) {
        Page<TutoringRoom> page = roomRepository.search(
                normalize(keyword), toStatus(status), courseId, instructorId, pageable);
        Map<Long, Long> counts = messageCounts(page.getContent());
        return page.map(r -> TutoringRoomListRow.of(r, counts.getOrDefault(r.getId(), 0L)));
    }

    public List<TutoringRoomListRow> searchAll(String keyword, String status, Long courseId, Long instructorId) {
        return search(keyword, status, courseId, instructorId, Pageable.unpaged()).getContent();
    }

    public List<TutoringRoomListRow> findByTrainee(Long traineeId) {
        List<TutoringRoom> rooms = roomRepository.findByTraineeId(traineeId);
        Map<Long, Long> counts = messageCounts(rooms);
        return rooms.stream()
                .map(r -> TutoringRoomListRow.of(r, counts.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    public List<TutoringRoomListRow> findByInstructor(Long instructorId) {
        List<TutoringRoom> rooms = roomRepository.findByInstructorId(instructorId);
        Map<Long, Long> counts = messageCounts(rooms);
        return rooms.stream()
                .map(r -> TutoringRoomListRow.of(r, counts.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    /**
     * 튜터 배정 셀렉트용 목록 (훈련생 제외).
     * {@code UserRepository} 가 A 소유라 findAll() 후 자바에서 거른다 — QnaService 와 같은 이유.
     */
    public List<StaffOption> instructorOptions() {
        return userRepository.findAll().stream()
                .filter(StaffOption::isStaff)
                .map(StaffOption::of)
                .toList();
    }

    /** 과정 셀렉트 옵션 — trainee/tutoring.html 의 학습 범위 선택. */
    public List<CourseOption> courseOptions() {
        return courseRepository.findAll().stream().map(CourseOption::of).toList();
    }

    /** 응답현황(admin-support-response.html) 의 튜터링 행. */
    public List<ResponseListRow> responseRows() {
        LocalDateTime now = LocalDateTime.now();
        return roomRepository.findAll().stream()
                .map(r -> ResponseListRow.ofTutoring(r, firstInstructorReplyAt(r.getId()), now))
                .toList();
    }

    public TutoringRoom getOrThrow(Long id) {
        return roomRepository.findDetailById(id)
                .orElseThrow(() -> new IllegalArgumentException("튜터링 방을 찾을 수 없습니다. id=" + id));
    }

    /** 방 상세 + 대화 로그. 읽음 처리는 하지 않는다 (관리자 모니터링용). */
    public TutoringRoomDetail getDetail(Long roomId, Long viewerId) {
        TutoringRoom room = getOrThrow(roomId);
        return TutoringRoomDetail.of(room, messageRepository.findByRoomId(roomId), viewerId);
    }

    /**
     * 방 상세를 열면서 상대가 보낸 메시지를 읽음 처리한다 (대화 당사자용).
     *
     * <p>관리자 모니터링 화면에서는 이 메서드를 쓰면 안 된다. 관리자가 열었다고
     * 훈련생↔강사 사이의 읽음 상태가 바뀌면 안 되기 때문. 그래서 당사자 여부를
     * 먼저 확인하고, 당사자가 아니면 읽음 처리를 건너뛴다.</p>
     */
    @Transactional
    public TutoringRoomDetail openDetail(Long roomId, Long viewerId) {
        TutoringRoom room = getOrThrow(roomId);
        if (isParticipant(room, viewerId)) {
            LocalDateTime now = LocalDateTime.now();
            messageRepository.findUnread(roomId, viewerId).forEach(m -> m.markRead(now));
        }
        return TutoringRoomDetail.of(room, messageRepository.findByRoomId(roomId), viewerId);
    }

    public long countUnread(Long roomId, Long readerId) {
        return messageRepository.countUnread(roomId, readerId);
    }

    /* ===== 생성/배정/메시지 ===== */

    /** 훈련생이 튜터링 방을 만든다. 강사는 아직 미배정(WAITING). */
    @Transactional
    public Long createRoom(Long traineeId, TutoringRoomForm form) {
        User trainee = userRepository.findById(traineeId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + traineeId));

        Course course = form.getCourseId() == null ? null
                : courseRepository.findById(form.getCourseId()).orElse(null);

        TutoringRoom room = roomRepository.save(TutoringRoom.builder()
                .course(course)
                .trainee(trainee)
                .title(form.getTitle())
                .status(TutoringRoom.RoomStatus.WAITING)
                .build());

        if (form.getFirstMessage() != null && !form.getFirstMessage().isBlank()) {
            sendMessage(room.getId(), traineeId, form.getFirstMessage());
        }
        return room.getId();
    }

    /** 강사 배정. WAITING 이면 ACTIVE 로 전이된다. */
    @Transactional
    public void assignInstructor(Long roomId, Long instructorId) {
        TutoringRoom room = getOrThrow(roomId);
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("강사를 찾을 수 없습니다. id=" + instructorId));
        if (instructor.getRole() == Role.TRAINEE) {
            throw new IllegalArgumentException("훈련생은 튜터로 배정할 수 없습니다.");
        }
        room.assignInstructor(instructor);
    }

    @Transactional
    public void closeRoom(Long roomId) {
        getOrThrow(roomId).close();
    }

    /**
     * 메시지 전송.
     *
     * <p>보낼 수 있는 사람은 방의 훈련생과 배정된 강사뿐이다.
     * 관리자는 권한정의서(2) 18행에 따라 R 만 가능하므로 여기서 막는다 —
     * URL 인가(SecurityConfig)만으로는 관리자가 /admin/support/** 를 통해
     * 들어올 수 있어서, 데이터 수준에서 한 번 더 검사해야 한다.</p>
     */
    @Transactional
    public Long sendMessage(Long roomId, Long senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용을 입력하세요.");
        }
        TutoringRoom room = getOrThrow(roomId);
        if (room.getStatus() == TutoringRoom.RoomStatus.CLOSED) {
            throw new IllegalStateException("종료된 튜터링 방에는 메시지를 보낼 수 없습니다.");
        }
        if (!isParticipant(room, senderId)) {
            // 당사자 아닌 사용자(특히 관리자는 권한정의서(2) 18행 R 전용)의 전송 시도는 '인가 실패'다.
            // raw IllegalStateException 으로 던지면 어떤 advice 도 안 잡아 whitelabel 500 이 됐다 —
            // getOrThrow 열람 경로가 AccessDeniedException(→403 안내화면)을 쓰는 것과 같게 맞춘다.
            throw new AccessDeniedException(
                    "이 튜터링 방의 대화 당사자만 메시지를 보낼 수 있습니다. (관리자는 열람만 가능)");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + senderId));

        LocalDateTime now = LocalDateTime.now();
        TutoringMessage message = messageRepository.save(TutoringMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .sentAt(now)
                .build());
        room.touch(now);
        return message.getId();
    }

    /** 방의 대화 당사자인지 — 훈련생 본인 또는 배정된 강사. */
    public boolean isParticipant(TutoringRoom room, Long userId) {
        if (userId == null) {
            return false;
        }
        boolean trainee = room.getTrainee() != null && room.getTrainee().getId().equals(userId);
        boolean instructor = room.getInstructor() != null && room.getInstructor().getId().equals(userId);
        return trainee || instructor;
    }

    /* ===== 요약 카드 ===== */

    public SupportStats stats() {
        LocalDateTime now = LocalDateTime.now();

        long waiting = roomRepository.countByStatus(TutoringRoom.RoomStatus.WAITING);
        long active = roomRepository.countByStatus(TutoringRoom.RoomStatus.ACTIVE);
        long unassigned = roomRepository.countByInstructorIsNull();

        List<TutoringRoom> all = roomRepository.findAll();
        long recent = all.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(now.minusDays(RECENT_DAYS)))
                .count();

        // 첫 강사 응답 시각을 방마다 구해 평균/무응답 건수를 낸다.
        long responded = 0;
        long totalMinutes = 0;
        long noResponse = 0;
        for (TutoringRoom room : all) {
            LocalDateTime first = firstInstructorReplyAt(room.getId());
            if (first != null) {
                responded++;
                totalMinutes += Math.max(0, Duration.between(room.getCreatedAt(), first).toMinutes());
            } else if (room.getCreatedAt() != null
                    && room.getCreatedAt().isBefore(now.minusHours(NO_RESPONSE_HOURS))) {
                noResponse++;
            }
        }
        String avg = responded == 0 ? "-" : SupportFormat.hours((double) totalMinutes / responded);

        String tutorSummary = roomRepository.countByInstructorSince(now.minusDays(RECENT_DAYS)).stream()
                .map(row -> row[0] + " " + row[1] + "건")
                .collect(Collectors.joining(" / "));
        if (tutorSummary.isBlank()) {
            tutorSummary = "-";
        }

        return new SupportStats(waiting + unassigned, avg, active, recent,
                tutorSummary, "(최근 " + RECENT_DAYS + "일 기준)", noResponse);
    }

    /**
     * 방의 첫 강사 응답 시각 — 응답현황의 경과시간 계산에 쓴다.
     * 훈련생이 보낸 메시지는 제외하고, 강사가 처음 답한 시각을 찾는다.
     */
    public LocalDateTime firstInstructorReplyAt(Long roomId) {
        TutoringRoom room = getOrThrow(roomId);
        if (room.getTrainee() == null) {
            return null;
        }
        Long traineeId = room.getTrainee().getId();
        return messageRepository.findByRoomId(roomId).stream()
                .filter(m -> m.getSender() != null && !m.getSender().getId().equals(traineeId))
                .map(TutoringMessage::getSentAt)
                .findFirst()
                .orElse(null);
    }

    /* ===== 내부 ===== */

    private Map<Long, Long> messageCounts(List<TutoringRoom> rooms) {
        Map<Long, Long> map = new HashMap<>();
        if (rooms.isEmpty()) {
            return map;
        }
        List<Long> ids = rooms.stream().map(TutoringRoom::getId).toList();
        for (Object[] row : messageRepository.countByRoomIds(ids)) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private String normalize(String v) {
        return (v == null || v.isBlank() || "전체".equals(v)) ? null : v.trim();
    }

    private TutoringRoom.RoomStatus toStatus(String status) {
        if (status == null || status.isBlank() || "전체".equals(status) || status.startsWith("전체")) {
            return null;
        }
        return switch (status) {
            case "대기", "WAITING" -> TutoringRoom.RoomStatus.WAITING;
            case "진행중", "ACTIVE" -> TutoringRoom.RoomStatus.ACTIVE;
            case "종료", "답변완료", "CLOSED" -> TutoringRoom.RoomStatus.CLOSED;
            default -> throw new IllegalArgumentException("알 수 없는 상태 값: " + status);
        };
    }
}
