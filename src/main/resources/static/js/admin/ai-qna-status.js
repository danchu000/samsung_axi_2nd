/**
 * ════════════════════════════════════════════════════════════════════════
 *  [기능 3-관리자] AI 학습 도우미 운영 현황     화면: /admin/support/qna
 * ════════════════════════════════════════════════════════════════════════
 *
 * ■ 무엇을 하는가
 *   훈련생 Q&A 가 AI 채팅으로 바뀌면서 관리자가 봐야 할 것도 달라졌다.
 *     예전 — 등록된 질문에 답을 달았는지
 *     지금 — AI 가 얼마나 걸러주는지 + **AI 가 못 푼 것**이 방치되지 않는지
 *
 * ■ 어떻게 구현했는가
 *   1) 요약 줄 — 카드를 쌓지 않고 한 줄로. 전체 / AI 자체 해결 / 강사 전달 / 미답변.
 *      **미답변에만 색을 준다.** 조치가 필요한 유일한 값이기 때문이다.
 *   2) 전달 목록 — 대기중이 오래 남으면 훈련생이 방치된 것이라 눈에 띄게 표시한다.
 *
 * ■ 데이터
 *   ⚠ 이 패널만 시연용. 같은 화면의 "기존 Q&A 목록"은 **실제 데이터**라
 *     화면에서 두 영역을 확실히 구분해 뒀다 — 섞이면 어느 쪽이 진짜인지 모른다.
 *   ✅ 실데이터로 바꾸려면: AiUsageLog(호출 기록, 이미 저장 중) 집계 +
 *     강사 전달 기능(아직 없음)의 저장 이력.
 *
 * ■ 서버 연동 지점
 *   window._serverAiQna 를 이 스크립트보다 **앞에서** 대입한다.
 */
(function () {
    'use strict';

    /*
     * 시연 데이터 — 숫자가 아래 목록과 맞아떨어지게 맞췄다.
     * 요약이 "전달 30건"인데 목록에 5줄만 있으면 관리자가 나머지를 찾아 헤맨다.
     * 전체 건수는 강사 화면([기능 4] AI 질문 63건)과도 같은 값을 쓴다.
     */
    var DUMMY = {
        summary: { total: '63건', solved: '58건', rate: '92%', escalated: '5건', pending: '4건' },
        escalated: [
            { q: '3주차 자료에 없는 커넥션 풀 설정값은 어떻게 정하나요?', trainee: '이훈련', teacher: '김강사', status: '답변완료', date: '2026-07-26' },
            { q: '과제 제출 형식이 zip 이어도 되나요?', trainee: '김서연', teacher: '김강사', status: '대기중', date: '2026-07-28' },
            { q: '실습 환경에서만 한글이 깨지는데 원인이 뭘까요?', trainee: '박도윤', teacher: '김강사', status: '대기중', date: '2026-07-28' },
            { q: '팀 프로젝트 주제를 바꿔도 되나요?', trainee: '이하은', teacher: '김강사', status: '대기중', date: '2026-07-29' },
            { q: '수료 요건에서 출석률은 어떻게 계산되나요?', trainee: '정민준', teacher: '김강사', status: '대기중', date: '2026-07-29' }
        ]
    };

    document.addEventListener('DOMContentLoaded', function () {
        var box = document.getElementById('aiQnaSummary');
        if (!box) return;   // Q&A 탭이 아닌 화면에서는 아무 일도 하지 않는다

        var data = window._serverAiQna || DUMMY;
        var s = data.summary;

        // 카드를 쌓지 않고 한 줄로 — 미답변만 색을 준다(유일하게 조치가 필요한 값)
        box.innerHTML =
            '<span>🤖 AI 학습 도우미</span><span class="sep">|</span>' +
            '<span>전체 <b>' + esc(s.total) + '</b></span>' +
            '<span>AI 자체 해결 <b>' + esc(s.solved) + '</b> (' + esc(s.rate) + ')</span>' +
            '<span>강사 전달 <b>' + esc(s.escalated) + '</b></span>' +
            '<span class="warn">미답변 <b class="warn">' + esc(s.pending) + '</b></span>';

        var body = document.getElementById('aiEscalatedBody');
        if (!body) return;

        if (!data.escalated.length) {
            body.innerHTML = '<tr><td colspan="5" class="ai-empty">강사에게 전달된 질문이 없습니다.</td></tr>';
            return;
        }

        body.innerHTML = data.escalated.map(function (r) {
            // 대기중이 오래 남으면 훈련생이 방치된 것 — 눈에 띄어야 한다
            var cls = r.status === '답변완료' ? 'low' : 'mid';
            return '<tr>' +
                '<td>' + esc(r.q) + '</td>' +
                '<td>' + esc(r.trainee) + '</td>' +
                '<td>' + esc(r.teacher) + '</td>' +
                '<td><span class="ai-level ' + cls + '">' + esc(r.status) + '</span></td>' +
                '<td>' + esc(r.date) + '</td>' +
            '</tr>';
        }).join('');
    });

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
