package com.ssa.lms.notice.repository;

import com.ssa.lms.notice.entity.NotificationRecipient;
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
}
