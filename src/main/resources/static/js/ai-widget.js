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
        trainee: [
            {
                label: '내 목표 직무 충족도',
                value: '62%',
                sub: '백엔드 개발자 기준',
                href: '/trainee/ai/roadmap',
                cta: '로드맵 보기'
            },
            {
                label: '추천 과정',
                value: '3개',
                sub: '적합도 74% 이상',
                href: '/trainee/ai/curriculum',
                cta: '추천 보기'
            },
            {
                label: '강사님 답변 대기',
                value: '1건',
                sub: 'AI로 해결 안 된 질문',
                href: '/trainee/ai/qna',
                cta: 'AI에게 묻기'
            }
        ],
        instructor: [
            {
                label: '보완 필요 훈련생',
                value: '7명',
                sub: '시급도 높음 3명',
                href: '/instructor/ai/diagnosis',
                cta: '진단 보기'
            },
            {
                label: 'AI 질문이 몰린 주제',
                value: '트랜잭션·동시성',
                sub: '최근 2주 38건 — 수업 보완 검토',
                href: '/instructor/ai/diagnosis',
                cta: '주제 보기'
            },
            {
                label: '추천 과제 미배부',
                value: '5건',
                sub: '확인 후 배부해 주세요',
                href: '/instructor/ai/diagnosis',
                cta: '배부하기'
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
                href: '/instructor/ai/diagnosis',
                cta: '진단 보기'
            }
        ],

        /* 관리자 화면 전용 상세 — 과정 개편·수업 보완의 근거가 되는 값들 */
        adminDetail: {
            collectedAt: '2026-07-27 03:00',
            demand: [
                { label: 'Docker', value: 74 },
                { label: 'AWS', value: 71 },
                { label: '테스트 코드', value: 68 },
                { label: 'CI/CD', value: 52 },
                { label: 'Redis', value: 39 }
            ],
            stuck: [
                { label: '트랜잭션·동시성', value: 82, count: 38 },
                { label: 'Docker·배포', value: 64, count: 29 },
                { label: 'JPA 연관관계', value: 51, count: 24 },
                { label: 'REST API 설계', value: 33, count: 15 },
                { label: '테스트 코드', value: 22, count: 10 }
            ],
            collect: [
                { job: '백엔드 개발자', count: '128건', at: '2026-07-27 03:00', ok: true, note: '정상' },
                { job: '프론트엔드 개발자', count: '96건', at: '2026-07-27 03:00', ok: true, note: '정상' },
                { job: '데이터 분석가', count: '74건', at: '2026-07-27 03:00', ok: true, note: '정상' },
                { job: 'AI 엔지니어', count: '0건', at: '2026-07-13 03:00', ok: false, note: '2주째 수집 실패 — 훈련생 화면에 옛 데이터가 보여요' }
            ]
        }
    };

    document.addEventListener('DOMContentLoaded', function () {
        var data = window._serverAiWidget || DUMMY;
        draw('hpAiCards', data.trainee);
        draw('instAiCards', data.instructor);
        draw('adminAiCards', data.admin);
        drawAdminDetail(data.adminDetail);
    });

    /** 관리자 전용 상세 — 해당 자리가 없는 화면(강사·훈련생)에서는 그냥 넘어간다. */
    function drawAdminDetail(d) {
        if (!d) return;

        var stamp = document.getElementById('aiCollectedAt');
        if (stamp) stamp.textContent = '직무 로드맵 수집: ' + d.collectedAt;

        meters('aiDemandMeters', d.demand, function (m) { return m.value + '%'; });
        meters('aiStuckMeters', d.stuck, function (m) { return m.count + '건'; });

        var body = document.getElementById('aiCollectBody');
        if (body) {
            body.innerHTML = d.collect.map(function (r) {
                // 수집 실패는 눈에 띄어야 한다 — 방치하면 훈련생이 옛 정보를 보게 된다
                var badge = r.ok
                    ? '<span class="ai-level low">정상</span>'
                    : '<span class="ai-level high">실패</span>';
                return '<tr>' +
                    '<td>' + esc(r.job) + '</td>' +
                    '<td>' + esc(r.count) + '</td>' +
                    '<td>' + esc(r.at) + '</td>' +
                    '<td>' + badge + '</td>' +
                    '<td style="font-size:12.5px; color:' + (r.ok ? '#6b7280' : '#b91c1c') + ';">' + esc(r.note) + '</td>' +
                '</tr>';
            }).join('');
        }
    }

    function meters(id, list, valueOf) {
        var box = document.getElementById(id);
        if (!box || !list) return;
        box.innerHTML = list.map(function (m) {
            var cls = m.value >= 70 ? 'danger' : (m.value >= 45 ? 'warn' : '');
            return '<div class="ai-meter-row">' +
                '<span class="label">' + esc(m.label) + '</span>' +
                '<span class="ai-bar ' + cls + '"><span style="width:' + m.value + '%"></span></span>' +
                '<span class="value">' + esc(valueOf(m)) + '</span></div>';
        }).join('');
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
