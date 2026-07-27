package com.ssa.lms.support.repository;

import com.ssa.lms.support.entity.TutoringMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TutoringMessageRepository extends JpaRepository<TutoringMessage, Long> {

    /**
     * 방 대화 로그 (시간순).
     *
     * <p>메시지 본문은 암호문으로 저장되므로 여기서도 검색·정렬 조건에 쓰지 않는다.
     * 정렬은 평문 컬럼 sent_at 으로만 한다.</p>
     */
    @Query("""
            select m from TutoringMessage m
            left join fetch m.sender
            where m.room.id = :roomId
            order by m.sentAt asc
            """)
    List<TutoringMessage> findByRoomId(@Param("roomId") Long roomId);

    /** 방별 마지막 메시지 시각/건수 — 목록 렌더링 시 N+1 을 피하려고 묶어서 집계한다. */
    @Query("""
            select m.room.id, count(m)
            from TutoringMessage m
            where m.room.id in :roomIds
            group by m.room.id
            """)
    List<Object[]> countByRoomIds(@Param("roomIds") List<Long> roomIds);

    /**
     * 상대방이 보낸 안 읽은 메시지 — 읽음 처리 대상.
     * 본인이 보낸 메시지는 제외한다.
     */
    @Query("""
            select m from TutoringMessage m
            where m.room.id = :roomId
              and m.sender.id <> :readerId
              and m.readAt is null
            """)
    List<TutoringMessage> findUnread(@Param("roomId") Long roomId, @Param("readerId") Long readerId);

    /** 방별 안 읽은 건수. */
    @Query("""
            select count(m) from TutoringMessage m
            where m.room.id = :roomId
              and m.sender.id <> :readerId
              and m.readAt is null
            """)
    long countUnread(@Param("roomId") Long roomId, @Param("readerId") Long readerId);
}
