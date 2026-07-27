package com.ssa.lms.notice.service;

import com.ssa.lms.course.service.CourseQueryService;
import com.ssa.lms.notice.dto.NotificationForm;
import com.ssa.lms.notice.dto.NotificationListRow;
import com.ssa.lms.notice.dto.NotificationSearchCond;
import com.ssa.lms.notice.entity.Notification;
import com.ssa.lms.notice.entity.NotificationRecipient;
import com.ssa.lms.notice.repository.NotificationRecipientRepository;
import com.ssa.lms.notice.repository.NotificationRepository;
import com.ssa.lms.user.entity.User;
import com.ssa.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 알림 서비스.
 *
 * 발송 시점에 수신 대상자를 행으로 펼친다(fan-out). 나중에 수강생이 추가돼도 과거 알림이
 * 소급 발송되지 않아야 하기 때문 (NotificationRecipient 주석의 설계 결정).
 *
 * 대상자 산출은 A의 CourseQueryService 계약만 호출한다 (a-requests.md P0-4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final CourseQueryService courseQueryService;
    private final UserRepository userRepository;

    /**
     * 알림 내역 목록. 수신자 수/읽음 수는 페이지에 담긴 id 묶음으로 한 번에 집계한다
     * (행마다 조회하면 N+1 — QuestionService.search 와 같은 방식).
     *
     * @param viewerId 로그인 사용자. 본인의 읽음 상태를 "읽음/읽지않음"으로 표시한다.
     */
    public Page<NotificationListRow> search(NotificationSearchCond cond, Long viewerId, Pageable pageable) {
        Page<Notification> page = notificationRepository.search(
                cond.priorityOrNull(), cond.fromOrNull(), cond.toOrNull(), cond.keywordOrNull(), pageable);

        List<Long> ids = page.getContent().stream().map(Notification::getId).toList();
        Map<Long, long[]> counts = countMap(ids);
        Set<Long> myUnread = new HashSet<>();
        Set<Long> myAny = new HashSet<>();
        if (!ids.isEmpty() && viewerId != null) {
            for (NotificationRecipient r : recipientRepository.findByNotificationIdsAndUserId(ids, viewerId)) {
                Long nid = r.getNotification().getId();
                myAny.add(nid);
                if (r.getReadAt() == null) {
                    myUnread.add(nid);
                }
            }
        }

        return page.map(n -> {
            long[] c = counts.getOrDefault(n.getId(), new long[]{0L, 0L});
            String readStatus = !myAny.contains(n.getId()) ? "-"
                    : (myUnread.contains(n.getId()) ? "읽지않음" : "읽음");
            return NotificationListRow.of(n, readStatus, c[0], c[1]);
        });
    }

    public NotificationListRow getRow(Long id, Long viewerId) {
        Notification n = getOrThrow(id);
        Map<Long, long[]> counts = countMap(List.of(id));
        long[] c = counts.getOrDefault(id, new long[]{0L, 0L});
        String readStatus = "-";
        if (viewerId != null) {
            List<NotificationRecipient> mine =
                    recipientRepository.findByNotificationIdsAndUserId(List.of(id), viewerId);
            if (!mine.isEmpty()) {
                readStatus = mine.get(0).getReadAt() == null ? "읽지않음" : "읽음";
            }
        }
        return NotificationListRow.of(n, readStatus, c[0], c[1]);
    }

    public NotificationForm loadForm(Long id) {
        return NotificationForm.from(getOrThrow(id));
    }

    public Notification getOrThrow(Long id) {
        return notificationRepository.findWithSenderById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public Long create(NotificationForm form, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다. id=" + senderId));

        Notification notification = Notification.builder()
                .title(form.getTitle())
                .content(form.getContent())
                .priority(form.toPriority())
                .targetType(form.toTargetType())
                .targetRefId(form.getTargetRefId())
                .sendAt(form.resolveSendAt())
                .dueDate(form.getDueDate())
                .sender(sender)
                .status(form.toStatus())
                .build();

        notificationRepository.save(notification);

        // 발송 상태로 저장된 것만 수신자를 펼친다. 예약/임시저장은 아직 대상 확정 전이다.
        if (notification.getStatus() == Notification.NotificationStatus.SENT) {
            fanOut(notification);
        }
        return notification.getId();
    }

    @Transactional
    public void update(Long id, NotificationForm form) {
        Notification n = getOrThrow(id);
        boolean wasSent = n.getStatus() == Notification.NotificationStatus.SENT;
        n.update(form.getTitle(), form.getContent(), form.toPriority(),
                form.toTargetType(), form.getTargetRefId(), form.resolveSendAt(), form.getDueDate());
        n.changeStatus(form.toStatus());

        // 예약 → 발송으로 넘어간 순간에만 fan-out 한다. 이미 발송된 건은 수신자를 다시 만들지 않는다
        // (유니크 제약 uk_notification_recipient 위반 + 읽음 상태 초기화 방지).
        if (!wasSent && n.getStatus() == Notification.NotificationStatus.SENT) {
            fanOut(n);
        }
    }

    /** 화면의 "읽음처리" / "확인 처리" 버튼 — 로그인 사용자 본인 수신 건만 읽음 표시. */
    @Transactional
    public int markRead(List<Long> notificationIds, Long userId) {
        if (notificationIds == null || notificationIds.isEmpty() || userId == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;
        for (NotificationRecipient r :
                recipientRepository.findByNotificationIdsAndUserId(notificationIds, userId)) {
            if (r.getReadAt() == null) {
                r.markRead(now);
                changed++;
            }
        }
        return changed;
    }

    /** 선택 삭제 (soft delete — @SQLDelete). 수신자 행은 이력이라 남긴다. */
    @Transactional
    public void delete(List<Long> ids) {
        notificationRepository.deleteAllById(ids);
    }

    /* ===== 내부 ===== */

    /** 수신 대상자를 행으로 펼친다. 이미 만들어진 수신자는 건너뛴다. */
    private void fanOut(Notification notification) {
        List<Long> targetUserIds = resolveTargets(notification);
        if (targetUserIds.isEmpty()) {
            return;
        }
        Set<Long> already = new HashSet<>();
        for (NotificationRecipient r : recipientRepository.findByNotificationIds(List.of(notification.getId()))) {
            already.add(r.getUser().getId());
        }
        for (Long userId : targetUserIds) {
            if (already.contains(userId)) {
                continue;
            }
            userRepository.findById(userId).ifPresent(u ->
                    recipientRepository.save(NotificationRecipient.builder()
                            .notification(notification).user(u).build()));
        }
    }

    private List<Long> resolveTargets(Notification n) {
        return switch (n.getTargetType()) {
            case ALL -> userRepository.findAll().stream().map(User::getId).toList();
            case COURSE -> n.getTargetRefId() == null
                    ? List.of()
                    : courseQueryService.findUserIdsByCourseId(n.getTargetRefId());
            case USER -> n.getTargetRefId() == null ? List.of() : List.of(n.getTargetRefId());
        };
    }

    /** id → [수신자 수, 읽은 수] */
    private Map<Long, long[]> countMap(List<Long> ids) {
        Map<Long, long[]> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Object[] row : notificationRepository.countRecipients(ids)) {
            long total = ((Number) row[1]).longValue();
            long read = row[2] == null ? 0L : ((Number) row[2]).longValue();
            map.put((Long) row[0], new long[]{total, read});
        }
        return Collections.unmodifiableMap(map);
    }
}
