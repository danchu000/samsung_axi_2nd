package com.ssa.lms.job.repository;

import com.ssa.lms.job.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    /** 중복 수집 차단용. 이미 있으면 저장하지 않는다. */
    boolean existsByExternalId(String externalId);

    @Query("select p.externalId from JobPosting p where p.externalId in :ids")
    Set<String> findExistingExternalIds(@Param("ids") List<String> ids);

    /**
     * 직무 그룹의 최근 공고. 오래된 공고를 근거로 삼으면 지난 시장을 말하게 된다.
     * 게시일이 없는 공고(파싱 실패)는 수집일로 대신 판단한다 — 버리면 표본이 줄어든다.
     */
    @Query("""
            select p from JobPosting p
             where p.jobGroup = :group
               and coalesce(p.postingDate, p.collectedAt) >= :since
             order by coalesce(p.postingDate, p.collectedAt) desc, p.id desc
            """)
    List<JobPosting> findRecent(@Param("group") String group, @Param("since") LocalDate since);

    /** 화면의 "마지막 수집" 표기용. 수집한 적이 없으면 비어 있다 — 날짜를 지어내지 않는다. */
    @Query("select max(p.collectedAt) from JobPosting p")
    Optional<LocalDate> findLastCollectedAt();

    @Query("select distinct p.jobGroup from JobPosting p")
    List<String> findGroups();
}
