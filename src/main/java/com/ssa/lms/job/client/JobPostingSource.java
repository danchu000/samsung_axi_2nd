package com.ssa.lms.job.client;

import com.ssa.lms.job.entity.JobPosting;

import java.time.LocalDate;
import java.util.List;

/**
 * 채용공고 수집처. [기능 1]
 *
 * <p>수집처를 인터페이스로 뺀 이유는, 한 곳에 묶이면 그 API 가 막혔을 때 기능 전체가
 * 멈추기 때문이다. 워크넷·사람인 중 <b>키가 있는 곳만</b> 실제로 부르고, 둘 다 있으면
 * 합쳐서 표본을 키운다.</p>
 *
 * <p><b>구현체가 지켜야 할 것</b>
 * <ul>
 *   <li><b>예외를 밖으로 던지지 않는다</b> — 외부 API 가 죽어도 배치가 멈추면 안 된다.
 *       실패하면 빈 목록을 주고, 지난 회차 데이터로 화면은 계속 뜬다</li>
 *   <li><b>인증키를 로그에 남기지 않는다</b></li>
 *   <li><b>응답 스펙이 바뀌어도 죽지 않는다</b> — 없는 필드는 건너뛴다</li>
 * </ul>
 */
public interface JobPostingSource {

    /** 수집처 이름 (로그·화면 표기용). */
    String name();

    /** 이 수집처를 지금 부를 수 있는지 (키가 있는지). */
    boolean usable();

    /**
     * 한 직무 그룹의 공고를 가져온다.
     *
     * @return 실패하거나 결과가 없으면 <b>빈 목록</b>. 예외를 던지지 않는다
     */
    List<JobPosting> search(String jobGroup, String keywords, LocalDate collectedAt);
}
