/**
 * 대시보드 AI 위젯 (관리자·강사·훈련생 공용).
 *
 * 각 대시보드 화면에 아래 컨테이너 중 **자기 역할의 것 하나만** 두면 되고,
 * 이 스크립트가 그것을 찾아 그린다. 없으면 아무 일도 하지 않는다.
 *
 *   #hpAiCards     훈련생
 *   #instAiCards   강사
 *   #adminAiCards  관리자
 *
 * 서버 연동 시 window._serverAiWidget 을 이 스크립트보다 **앞에서** 대입한다.
 * (뒤에 두면 서버 값이 영원히 더미에 가려진다 — CLAUDE.md 규칙 4)
 */
(function () {
    'use strict';

    var DUMMY = {
        /*
         * 훈련생 카드는 **숫자가 아니라 행동**을 말한다.
         * 예전엔 "내 목표 직무 충족도 62%" 처럼 값만 있어서, 보고 나서 뭘 해야 할지
         * 알 수 없었다. 이 기능이 나에게 무엇을 해주는지 한 줄로 말하고,
         * 누를 버튼 하나를 붙인다.
         */
        trainee: [
            {
                label: '직무 로드맵',
                value: '목표 직무까지 62%',
                sub: '어떤 역량이 더 필요한지 단계별로 알려드려요',
                href: '/trainee/ai/roadmap',
                cta: '내 로드맵 보기'
            },
            {
                label: '맞춤 커리큘럼 추천',
                value: '나에게 맞는 과정 3개',
                sub: '내 학습 기록을 분석해 다음에 들을 과정을 골라드려요',
                href: '/trainee/ai/curriculum',
                cta: '추천 이유 보기'
            },
            {
                label: 'AI 학습 도우미',
                value: '바로 답해드려요',
                sub: '강사님 답변을 기다리지 않고 지금 물어볼 수 있어요',
                href: '/trainee/ai/qna',
                cta: '질문하기'
            }
        ],
        instructor: [
            {
                label: '보완 필요 훈련생',
                value: '7명',
                sub: '시급도 높음 3명',
                href: '/instructor/ai/diagnosis#secTrainees',
                cta: '목록 보기'
            },
            {
                label: 'AI 질문이 몰린 주제',
                value: '트랜잭션·동시성',
                sub: '최근 2주 38건 — 수업 보완 검토',
                href: '/instructor/ai/diagnosis#secTopics',
                cta: '주제 목록 보기'
            },
            {
                label: '추천 과제 미배부',
                value: '5건',
                sub: '확인 후 배부해 주세요',
                href: '/instructor/ai/diagnosis#secTasks',
                cta: '배부 목록 보기'
            }
        ],
        admin: [
            {
                label: 'AI 도우미 이용',
                value: '186건',
                sub: '최근 2주 · 훈련생 24명 (1인 평균 7.8건)',
                href: '#',
                cta: null
            },
            {
                label: 'AI 자체 해결률',
                value: '84%',
                sub: '강사 전달 30건 · 지난주 대비 +6%p',
                href: '#',
                cta: null
            },
            {
                label: '보완 필요 훈련생',
                value: '7명',
                sub: '전체 24명 중 · 시급도 높음 3명',
                href: '/instructor/ai/diagnosis#secTrainees',
                cta: '목록 보기'
            }
        ],

        /* 관리자 화면 전용 상세 — 과정 개편·수업 보완의 근거가 되는 값들 */
        adminDetail: {
            collectedAt: '2026-07-27 03:00',
        }
    };

    document.addEventListener('DOMContentLoaded', function () {
        var data = window._serverAiWidget || DUMMY;
        draw('hpAiCards', data.trainee);
        draw('instAiCards', data.instructor);
        draw('adminAiCards', data.admin);
        drawAdminDetail(data.adminDetail);
    });

    /**
     * 관리자 화면의 수집 시각 표시.
     *
     * 예전에는 여기서 요구역량·막힌주제 막대와 수집현황 표까지 그렸는데,
     * 대시보드에 박스가 너무 많아져 차트(admin/dashboard-charts.js)로 옮겼다.
     * 남은 것은 "로드맵 데이터가 언제 수집됐나" 한 줄 — 오래되면 훈련생 화면이
     * 옛 정보로 남으므로 관리자가 상시 봐야 한다.
     */
    function drawAdminDetail(d) {
        if (!d) return;
        var stamp = document.getElementById('aiCollectedAt');
        if (stamp) stamp.textContent = '직무 로드맵 수집: ' + d.collectedAt;
    }

    function draw(id, items) {
        var box = document.getElementById(id);
        if (!box) return;   // 이 역할의 대시보드가 아니면 아무 것도 하지 않는다

        if (!items || !items.length) {
            box.innerHTML = '<div class="ai-empty">표시할 분석 결과가 없어요.</div>';
            return;
        }

        box.innerHTML = items.map(function (c) {
            var cta = c.cta
                ? '<div style="margin-top:12px;"><a class="btn btn-gray btn-sm" href="' + esc(c.href) + '">' + esc(c.cta) + '</a></div>'
                : '';
            return '<div class="ai-card" style="margin:0;">' +
                '<p class="ai-card-sub" style="margin:0 0 6px;">' + esc(c.label) + '</p>' +
                '<p class="ai-card-title" style="font-size:22px;">' + esc(c.value) + '</p>' +
                '<p class="ai-card-sub">' + esc(c.sub) + '</p>' +
                cta +
            '</div>';
        }).join('');
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
