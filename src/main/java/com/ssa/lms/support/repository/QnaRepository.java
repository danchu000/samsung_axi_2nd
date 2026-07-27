package com.ssa.lms.support.repository;

import com.ssa.lms.support.entity.Qna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    /**
     * Q&A 목록 검색.
     *
     * <p><b>검색 대상은 title 뿐이다.</b> {@code Qna.content} 에는
     * {@code @Convert(converter = CryptoConverter.class)} 가 걸려 있어 DB 에는 암호문이
     * 저장된다. 암호문에 LIKE 를 걸면 언제나 0건이므로 content 는 조건에 넣지 않는다.
     * (내역서 개인정보 암호화 요건 — 컨버터를 떼면 안 된다.)</p>
     *
     * <p>작성자 검색도 평문 컬럼인 {@code User.name} 으로만 한다.</p>
     */
    @Query("""
            select q from Qna q
            where (:keyword is null
                   or lower(q.title) like lower(concat('%', :keyword, '%'))
                   or lower(q.user.name) like lower(concat('%', :keyword, '%')))
              and (:status is null or q.status = :status)
              and (:category is null or q.category = :category)
              and (:courseId is null or q.course.id = :courseId)
              and (:assigneeId is null or q.assignee.id = :assigneeId)
              and (:unassigned = false or q.assignee is null)
            order by q.createdAt desc
            """)
    Page<Qna> search(@Param("keyword") String keyword,
                     @Param("status") Qna.QnaStatus status,
                     @Param("category") Qna.QnaCategory category,
                     @Param("courseId") Long courseId,
                     @Param("assigneeId") Long assigneeId,
                     @Param("unassigned") boolean unassigned,
                     Pageable pageable);

    /** 상세 — 연관 엔티티를 한 번에 가져와 LAZY 프록시 문제를 피한다. */
    @Query("""
            select q from Qna q
            left join fetch q.user
            left join fetch q.course
            left join fetch q.session
            left join fetch q.assignee
            where q.id = :id
            """)
    Optional<Qna> findDetailById(@Param("id") Long id);

    /** 훈련생 본인 질문 목록. */
    @Query("""
            select q from Qna q
            left join fetch q.course
            left join fetch q.session
            where q.user.id = :userId
              and (:keyword is null or lower(q.title) like lower(concat('%', :keyword, '%')))
            order by q.createdAt desc
            """)
    List<Qna> findByUserId(@Param("userId") Long userId, @Param("keyword") String keyword);

    long countByStatus(Qna.QnaStatus status);

    long countByAssigneeIsNull();

    /** 최근 N일 신규 질문 수. */
    long countByCreatedAtAfter(LocalDateTime from);

    /**
     * 평균 첫 응답 소요 시간(분). 아직 응답이 없는 건은 제외한다.
     * H2/MySQL 모두 지원하는 표준 함수가 마땅치 않아 초 단위 차이를 자바에서 계산하도록
     * (createdAt, firstResponseAt) 쌍만 내려준다.
     */
    @Query("""
            select q.createdAt, q.firstResponseAt from Qna q
            where q.firstResponseAt is not null
            """)
    List<Object[]> findResponseTimePairs();

    /**
     * 무응답 경과 건수 — 첫 응답이 없고 등록된 지 기준 시각보다 오래된 질문.
     * 화면의 "최근 24시간 무응답 건" 카드에 쓴다.
     */
    long countByFirstResponseAtIsNullAndCreatedAtBefore(LocalDateTime threshold);

    /** 조회수 TOP — 화면의 "조회수 TOP Q&A" 카드. */
    Page<Qna> findAllByOrderByViewCountDesc(Pageable pageable);
}
