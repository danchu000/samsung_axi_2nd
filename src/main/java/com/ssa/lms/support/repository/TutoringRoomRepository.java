package com.ssa.lms.support.repository;

import com.ssa.lms.support.entity.TutoringRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TutoringRoomRepository extends JpaRepository<TutoringRoom, Long> {

    /**
     * 튜터링 방 목록 검색.
     *
     * <p>검색은 방 제목(title)과 훈련생 이름(User.name) — 둘 다 평문 컬럼이다.
     * 메시지 본문({@code TutoringMessage.content})은 AES-256 암호문으로 저장되므로
     * 검색 조건에 넣을 수 없다.</p>
     */
    @Query("""
            select r from TutoringRoom r
            where (:keyword is null
                   or lower(r.title) like lower(concat('%', :keyword, '%'))
                   or lower(r.trainee.name) like lower(concat('%', :keyword, '%')))
              and (:status is null or r.status = :status)
              and (:courseId is null or r.course.id = :courseId)
              and (:instructorId is null or r.instructor.id = :instructorId)
            order by case when r.lastMessageAt is null then r.createdAt else r.lastMessageAt end desc
            """)
    Page<TutoringRoom> search(@Param("keyword") String keyword,
                              @Param("status") TutoringRoom.RoomStatus status,
                              @Param("courseId") Long courseId,
                              @Param("instructorId") Long instructorId,
                              Pageable pageable);

    @Query("""
            select r from TutoringRoom r
            left join fetch r.trainee
            left join fetch r.instructor
            left join fetch r.course
            where r.id = :id
            """)
    Optional<TutoringRoom> findDetailById(@Param("id") Long id);

    /** 훈련생 본인 방 목록. */
    @Query("""
            select r from TutoringRoom r
            left join fetch r.course
            left join fetch r.instructor
            where r.trainee.id = :traineeId
            order by case when r.lastMessageAt is null then r.createdAt else r.lastMessageAt end desc
            """)
    List<TutoringRoom> findByTraineeId(@Param("traineeId") Long traineeId);

    /** 강사 담당 방 목록. */
    @Query("""
            select r from TutoringRoom r
            left join fetch r.course
            left join fetch r.trainee
            where r.instructor.id = :instructorId
            order by case when r.lastMessageAt is null then r.createdAt else r.lastMessageAt end desc
            """)
    List<TutoringRoom> findByInstructorId(@Param("instructorId") Long instructorId);

    long countByStatus(TutoringRoom.RoomStatus status);

    long countByInstructorIsNull();

    /** 강사(튜터)별 처리 건수 — 화면의 "튜터별 처리 현황 요약" 카드. */
    @Query("""
            select r.instructor.name, count(r)
            from TutoringRoom r
            where r.instructor is not null
              and r.createdAt >= :from
            group by r.instructor.name
            order by count(r) desc
            """)
    List<Object[]> countByInstructorSince(@Param("from") LocalDateTime from);
}
