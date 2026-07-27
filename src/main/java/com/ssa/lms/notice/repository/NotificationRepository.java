package com.ssa.lms.notice.repository;

import com.ssa.lms.notice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 알림 내역 검색 (admin-alarm.html).
     * 필터: 중요도 / 발송일 범위 / 검색어(제목·내용).
     */
    @Query(value = """
            select n from Notification n
            join fetch n.sender s
            where (:priority is null or n.priority = :priority)
              and (:from is null or n.sendAt >= :from)
              and (:to is null or n.sendAt < :to)
              and (:keyword is null
                   or lower(n.title) like lower(concat('%', :keyword, '%'))
                   or lower(n.content) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(n) from Notification n
            where (:priority is null or n.priority = :priority)
              and (:from is null or n.sendAt >= :from)
              and (:to is null or n.sendAt < :to)
              and (:keyword is null
                   or lower(n.title) like lower(concat('%', :keyword, '%'))
                   or lower(n.content) like lower(concat('%', :keyword, '%')))
            """)
    Page<Notification> search(@Param("priority") Notification.Priority priority,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    @Query("select n from Notification n join fetch n.sender where n.id = :id")
    Optional<Notification> findWithSenderById(@Param("id") Long id);

    /**
     * 알림별 수신자 수 / 읽은 수 집계.
     * 행마다 조회하면 N+1 이 되므로 id 묶음으로 한 번에 센다.
     * 반환: [notificationId(Long), total(Long), readCount(Long)]
     */
    @Query("""
            select r.notification.id, count(r.id),
                   sum(case when r.readAt is not null then 1 else 0 end)
            from NotificationRecipient r
            where r.notification.id in :ids
            group by r.notification.id
            """)
    List<Object[]> countRecipients(@Param("ids") Collection<Long> ids);
}
