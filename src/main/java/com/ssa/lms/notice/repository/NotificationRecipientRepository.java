package com.ssa.lms.notice.repository;

import com.ssa.lms.notice.entity.NotificationRecipient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    @Query("select r from NotificationRecipient r where r.notification.id in :ids and r.user.id = :userId")
    List<NotificationRecipient> findByNotificationIdsAndUserId(@Param("ids") Collection<Long> ids,
                                                               @Param("userId") Long userId);

    @Query("select r from NotificationRecipient r where r.notification.id in :ids")
    List<NotificationRecipient> findByNotificationIds(@Param("ids") Collection<Long> ids);

    void deleteByNotificationIdIn(Collection<Long> ids);

    /** 내가 받은 알림 — 최신순. 훈련생 알림함이 쓴다. */
    @Query("""
            select r from NotificationRecipient r
              join fetch r.notification n
            where r.user.id = :userId
            order by n.sendAt desc, n.id desc
            """)
    List<NotificationRecipient> findMine(@Param("userId") Long userId, Pageable pageable);
}
