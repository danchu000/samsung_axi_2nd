/**
 * 관리자 소통 > Q&A 탭의 AI 학습 도우미 현황.
 *
 * 훈련생 Q&A 가 AI 채팅으로 바뀌면서, 관리자가 봐야 할 것도 달라졌다.
 *   예전: 등록된 질문에 답을 달았는지
 *   지금: AI 가 얼마나 걸러주는지 + **AI 가 못 푼 것**이 방치되지 않는지
 *
 * 아래 값은 아직 서버가 없어 더미다. 같은 화면의 "기존 Q&A 목록"은 실제 데이터이므로
 * 화면에서 두 영역을 확실히 구분해 뒀다. 서버 연동 시 window._serverAiQna 를
 * 이 스크립트보다 앞에서 대입한다.
 */
(function () {
    'use strict';

    var DUMMY = {
        cards: [
            { label: '전체 질문', value: '186건', sub: '최근 2주 · 훈련생 24명' },
            { label: 'AI 자체 해결', value: '156건', sub: '해결률 84% · 평균 응답 3초' },
            { label: '강사 전달', value: '30건', sub: '미답변 4건 — 확인이 필요합니다' }
        ],
        escalated: [
            { q: '3주차 실습에서 커넥션 풀 설정값을 어떻게 정해야 하나요?', trainee: '김훈련', teacher: '김강사', status: '답변완료', date: '2026-07-26' },
            { q: '과제 제출 형식이 zip 이어도 되나요?', trainee: '이수강', teacher: '김강사', status: '대기중', date: '2026-07-28' },
            { q: '배포 서버에서만 한글이 깨지는데 원인이 뭘까요?', trainee: '최교육', teacher: '박강사', status: '대기중', date: '2026-07-28' },
            { q: '팀 프로젝트 주제를 바꿔도 되나요?', trainee: '정연수', teacher: '김강사', status: '대기중', date: '2026-07-29' },
            { q: '수료 요건에서 출석률은 어떻게 계산되나요?', trainee: '한지원', teacher: '박강사', status: '대기중', date: '2026-07-29' }
        ]
    };

    document.addEventListener('DOMContentLoaded', function () {
        var box = document.getElementById('aiQnaCards');
        if (!box) return;   // Q&A 탭이 아닌 화면에서는 아무 일도 하지 않는다

        var data = window._serverAiQna || DUMMY;

        box.innerHTML = data.cards.map(function (c) {
            return '<div class="ai-card" style="margin:0;">' +
                '<p class="ai-card-sub" style="margin:0 0 6px;">' + esc(c.label) + '</p>' +
                '<p class="ai-card-title" style="font-size:24px;">' + esc(c.value) + '</p>' +
                '<p class="ai-card-sub">' + esc(c.sub) + '</p></div>';
        }).join('');

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
