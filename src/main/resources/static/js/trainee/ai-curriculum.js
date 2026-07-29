/**
 * 맞춤 커리큘럼 추천 화면 (훈련생).
 *
 * 추천 대상은 **원내 개설 과정**뿐이다 — 외부 강의를 섞지 않는다.
 * 서버 연동 시 window._serverCurriculum 을 이 스크립트보다 앞에서 대입한다.
 */
(function () {
    'use strict';

    var DUMMY = {
        analyzedAt: '2026-07-29',
        stats: [
            { label: '평균 진도율', value: '78%', sub: '수강 과정 2개 기준' },
            { label: '평균 평가 점수', value: '72점', sub: '응시 5회' },
            { label: '출석률', value: '94%', sub: '최근 8주' }
        ],
        weak: [
            { label: '데이터베이스', value: 58 },
            { label: '인프라·배포', value: 32 },
            { label: '테스트·품질', value: 41 },
            { label: '알고리즘', value: 76 }
        ],
        recommend: [
            {
                name: '컨테이너 기반 배포 실무',
                round: '2026년 3차',
                period: '2026-09-01 ~ 2026-10-15',
                seats: '정원 30명 / 신청 12명',
                fit: 92,
                reasons: [
                    '인프라·배포 영역 점수가 32%로 가장 낮아요.',
                    '수강 중인 "클라우드 기반 풀스택" 과정의 후속 과정이에요.',
                    '목표 직무(백엔드 개발자) 공고의 74%가 Docker 를 요구해요.'
                ]
            },
            {
                name: '데이터베이스 설계와 성능 튜닝',
                round: '2026년 3차',
                period: '2026-09-08 ~ 2026-10-20',
                seats: '정원 25명 / 신청 19명',
                fit: 81,
                reasons: [
                    '데이터베이스 평가 점수가 58%로 평균보다 낮아요.',
                    '최근 과제에서 쿼리 작성 문항의 오답률이 높았어요.'
                ]
            },
            {
                name: '테스트 코드 작성 실무',
                round: '2026년 4차',
                period: '2026-11-03 ~ 2026-12-05',
                seats: '정원 20명 / 신청 4명',
                fit: 74,
                reasons: [
                    '테스트·품질 영역이 41%로 보완이 필요해요.',
                    '선수 과정을 모두 이수하셔서 바로 들으실 수 있어요.'
                ]
            }
        ],
        later: [
            {
                name: 'AI 모델 서빙 심화',
                meta: '2026년 4차 · 2026-11-10 시작',
                reason: '"머신러닝 기초" 과정을 먼저 이수해야 해요. 현재 미수강 상태예요.'
            },
            {
                name: '대규모 트래픽 아키텍처',
                meta: '2027년 1차 · 2027-01-05 시작',
                reason: '인프라·배포 영역 점수가 60% 이상일 때 권장돼요. 현재 32%예요.'
            }
        ]
    };

    var data = window._serverCurriculum || DUMMY;

    document.addEventListener('DOMContentLoaded', function () {
        var stamp = document.querySelector('.js-analyzed-at');
        if (stamp) stamp.textContent = '분석 기준: ' + data.analyzedAt;

        renderStats();
        renderWeak();
        renderRecommend();
        renderLater();
    });

    function renderStats() {
        document.getElementById('statGrid').innerHTML = data.stats.map(function (s) {
            return '<div class="ai-card" style="margin:0;">' +
                '<p class="ai-card-sub" style="margin:0 0 6px;">' + esc(s.label) + '</p>' +
                '<p class="ai-card-title" style="font-size:26px;">' + esc(s.value) + '</p>' +
                '<p class="ai-card-sub">' + esc(s.sub) + '</p></div>';
        }).join('');
    }

    function renderWeak() {
        document.getElementById('weakMeters').innerHTML =
            '<p class="ai-item-title" style="margin-bottom:12px;">영역별 이해도</p>' +
            data.weak.map(function (m) {
                var cls = m.value >= 70 ? '' : (m.value >= 45 ? 'warn' : 'danger');
                return '<div class="ai-meter-row">' +
                    '<span class="label">' + esc(m.label) + '</span>' +
                    '<span class="ai-bar ' + cls + '"><span style="width:' + m.value + '%"></span></span>' +
                    '<span class="value">' + m.value + '%</span></div>';
            }).join('');
    }

    function renderRecommend() {
        var box = document.getElementById('recommendList');
        if (!data.recommend.length) {
            box.innerHTML = '<div class="ai-empty">추천할 과정이 아직 없어요.<br>학습을 더 진행하면 추천이 나와요.</div>';
            return;
        }
        box.innerHTML = data.recommend.map(function (c, i) {
            return '<div class="ai-card" style="margin-bottom:14px;">' +
                '<div class="ai-card-head" style="align-items:flex-start;">' +
                    '<div style="display:flex; gap:12px; align-items:flex-start;">' +
                        '<span class="ai-rank">' + (i + 1) + '</span>' +
                        '<div>' +
                            '<p class="ai-item-title" style="font-size:15px;">' + esc(c.name) + '</p>' +
                            '<p class="ai-item-meta">' + esc(c.round) + ' · ' + esc(c.period) + '<br>' + esc(c.seats) + '</p>' +
                        '</div>' +
                    '</div>' +
                    '<div style="text-align:right; flex:none;">' +
                        '<p class="ai-card-sub" style="margin:0 0 4px;">적합도</p>' +
                        '<p class="ai-card-title" style="font-size:22px;">' + c.fit + '%</p>' +
                    '</div>' +
                '</div>' +
                '<div class="ai-reason"><b>이 과정을 추천한 이유</b><br>' +
                    c.reasons.map(function (r) { return '· ' + esc(r); }).join('<br>') +
                '</div>' +
                '<div style="margin-top:14px; text-align:right;">' +
                    '<button type="button" class="btn btn-gray btn-sm js-detail">과정 상세</button> ' +
                    '<button type="button" class="btn btn-primary btn-sm js-apply">수강 신청</button>' +
                '</div>' +
            '</div>';
        }).join('');

        // 서버 연동 전이므로 이동 대신 안내만 — 조용히 아무 일도 안 일어나면 고장으로 보인다
        box.addEventListener('click', function (e) {
            if (e.target.classList.contains('js-apply')) {
                alert('수강 신청 화면으로 연결될 예정이에요. (서버 연동 준비 중)');
            } else if (e.target.classList.contains('js-detail')) {
                alert('과정 상세 화면으로 연결될 예정이에요. (서버 연동 준비 중)');
            }
        });
    }

    function renderLater() {
        var box = document.getElementById('laterList');
        if (!data.later.length) {
            box.innerHTML = '<li><div class="ai-empty">해당하는 과정이 없어요.</div></li>';
            return;
        }
        box.innerHTML = data.later.map(function (c) {
            return '<li>' +
                '<span class="ai-rank dim">–</span>' +
                '<div style="flex:1;">' +
                    '<p class="ai-item-title">' + esc(c.name) + '</p>' +
                    '<p class="ai-item-meta">' + esc(c.meta) + '</p>' +
                    '<div class="ai-reason">' + esc(c.reason) + '</div>' +
                '</div></li>';
        }).join('');
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
