/**
 * 관리자 대시보드 차트.
 *
 * 외부 라이브러리를 쓰지 않는다 — 이 프로젝트의 다른 차트들과 같은 방식(순수 canvas)이다.
 * 폐쇄망 배포가 예정돼 있어 CDN 의존을 늘리지 않는 편이 안전하다.
 *
 * 그리는 것
 *  1. 과정별 진행 현황 — 진도율 vs 기간 경과율. 둘을 나란히 놔야 "지연"이 보인다
 *  2. 주간 학습 활동 추이 — 출석·제출·응시 3개 선
 *  3. 이수 요건 충족 현황 — 요건별 충족 인원
 *  4. 채용공고 요구 역량 TOP — 가로 막대
 *
 * 서버 연동 시 window._serverAdminCharts 를 이 스크립트보다 앞에서 대입한다.
 */
(function () {
    'use strict';

    var INK = '#101828', SUB = '#667085', LINE = '#eef0f6';

    /*
     * 기본은 사이드바와 같은 남색 계열 하나로 간다.
     * 색은 "구분"이 아니라 **경고**에만 쓴다 — 지연·미달일 때만 주황/빨강이 나온다.
     * (예전엔 계열마다 다른 색이라 알록달록하고 가벼워 보였다)
     */
    var C = {
        navy: '#131D41',
        navy2: '#2a3866',
        navy3: '#5a6a9a',
        amber: '#f59e0b',
        red: '#ef4444',
        green: '#10b981'
    };

    var DUMMY = {
        courses: [
            { name: '클라우드 풀스택', progress: 62, elapsed: 58 },
            { name: '데이터 분석 실무', progress: 41, elapsed: 70 },   // 지연
            { name: 'AI 서비스 개발', progress: 88, elapsed: 84 },
            { name: '백엔드 심화', progress: 25, elapsed: 46 },        // 지연
            { name: '프론트엔드 실무', progress: 73, elapsed: 71 }
        ],
        weeks: ['6/2', '6/9', '6/16', '6/23', '6/30', '7/7', '7/14', '7/21'],
        series: [
            { label: '출석률', color: C.navy, values: [92, 94, 91, 95, 93, 96, 94, 95] },
            { label: '과제 제출률', color: C.navy2, values: [78, 82, 75, 88, 84, 79, 86, 90] },
            { label: '평가 응시율', color: C.navy3, values: [65, 71, 88, 62, 74, 91, 70, 77] }
        ],
        completion: [
            { label: '진도 충족', done: 18, total: 24 },
            { label: '출석 충족', done: 21, total: 24 },
            { label: '성적 충족', done: 14, total: 24 },
            { label: '전체 충족', done: 12, total: 24 }
        ],
        demand: [
            { label: 'Docker', value: 74 },
            { label: 'AWS', value: 71 },
            { label: '테스트 코드', value: 68 },
            { label: 'CI/CD', value: 52 },
            { label: 'Redis', value: 39 }
        ]
    };

    document.addEventListener('DOMContentLoaded', function () {
        var d = window._serverAdminCharts || DUMMY;
        drawAll(d);

        // 폭이 바뀌면 canvas 픽셀 크기가 어긋나 흐려진다 — 다시 그린다
        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { drawAll(d); }, 200);
        });
    });

    function drawAll(d) {
        courseProgress('courseProgressChart', d.courses);
        weekly('weeklyActivityChart', d.weeks, d.series);
        completion('completionChart', d.completion);
        hbar('aiDemandChart', d.demand);
    }

    /** 화면 배율을 반영해 선명하게 그린다. 이게 없으면 레티나에서 흐려진다. */
    function setup(id) {
        var cv = document.getElementById(id);
        if (!cv) return null;
        var dpr = window.devicePixelRatio || 1;
        var w = cv.clientWidth || cv.parentNode.clientWidth || 600;
        var h = Number(cv.getAttribute('height')) || 240;
        cv.width = w * dpr;
        cv.height = h * dpr;
        cv.style.height = h + 'px';
        var ctx = cv.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.font = '12px Pretendard, sans-serif';
        return { ctx: ctx, w: w, h: h };
    }

    function rrect(ctx, x, y, w, h, r) {
        if (w <= 0) return;
        r = Math.min(r, h / 2, w / 2);
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + w, y, x + w, y + h, r);
        ctx.arcTo(x + w, y + h, x, y + h, r);
        ctx.arcTo(x, y + h, x, y, r);
        ctx.arcTo(x, y, x + w, y, r);
        ctx.closePath();
        ctx.fill();
    }

    /** 과정별 진행 — 진도 막대 위에 기간 경과를 실선으로 얹어 격차를 보여준다. */
    function courseProgress(id, rows) {
        var s = setup(id); if (!s || !rows) return;
        var ctx = s.ctx, padL = 110, padR = 46, top = 12;
        var barH = 14, gap = (s.h - top - 10) / rows.length;

        rows.forEach(function (r, i) {
            var y = top + i * gap;
            var full = s.w - padL - padR;

            ctx.fillStyle = INK;
            ctx.textAlign = 'right';
            ctx.fillText(cut(r.name, 9), padL - 10, y + barH - 1);

            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, padL, y, full, barH, 7);

            // 진도가 기간보다 낮으면 지연 — 색으로 즉시 구분
            var late = r.progress < r.elapsed - 5;
            ctx.fillStyle = late ? C.amber : C.navy;
            rrect(ctx, padL, y, full * r.progress / 100, barH, 7);

            // 기간 경과 지점 — 여기까지 왔어야 한다는 기준선
            var ex = padL + full * r.elapsed / 100;
            ctx.strokeStyle = SUB;
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(ex, y - 3);
            ctx.lineTo(ex, y + barH + 3);
            ctx.stroke();

            ctx.textAlign = 'left';
            ctx.fillStyle = late ? '#b45309' : INK;
            ctx.fillText(r.progress + '%', s.w - padR + 6, y + barH - 1);
        });
    }

    /** 주간 추이 — 선 3개. */
    function weekly(id, labels, series) {
        var s = setup(id); if (!s || !labels) return;
        var ctx = s.ctx, padL = 34, padR = 12, padT = 26, padB = 26;
        var gw = s.w - padL - padR, gh = s.h - padT - padB;

        for (var i = 0; i <= 4; i++) {
            var y = padT + gh * i / 4;
            ctx.strokeStyle = LINE; ctx.lineWidth = 1;
            ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + gw, y); ctx.stroke();
            ctx.fillStyle = SUB; ctx.textAlign = 'right';
            ctx.fillText((100 - i * 25) + '', padL - 6, y + 4);
        }

        ctx.textAlign = 'center';
        labels.forEach(function (l, i) {
            ctx.fillStyle = SUB;
            ctx.fillText(l, padL + gw * i / (labels.length - 1), s.h - 8);
        });

        series.forEach(function (se) {
            ctx.strokeStyle = se.color; ctx.lineWidth = 2.5;
            ctx.beginPath();
            se.values.forEach(function (v, i) {
                var x = padL + gw * i / (se.values.length - 1);
                var y = padT + gh * (1 - v / 100);
                i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
            });
            ctx.stroke();

            ctx.fillStyle = se.color;
            se.values.forEach(function (v, i) {
                var x = padL + gw * i / (se.values.length - 1);
                var y = padT + gh * (1 - v / 100);
                ctx.beginPath(); ctx.arc(x, y, 3, 0, Math.PI * 2); ctx.fill();
            });
        });

        // 범례 — 선이 3개면 무엇이 무엇인지 표시해야 한다
        var lx = padL;
        ctx.textAlign = 'left';
        series.forEach(function (se) {
            ctx.fillStyle = se.color;
            ctx.fillRect(lx, 8, 10, 4);
            ctx.fillStyle = SUB;
            ctx.fillText(se.label, lx + 15, 13);
            lx += ctx.measureText(se.label).width + 42;
        });
    }

    /** 이수 요건 — 요건별 충족 인원 세로 막대. */
    function completion(id, rows) {
        var s = setup(id); if (!s || !rows) return;
        var ctx = s.ctx, padL = 34, padR = 12, padT = 16, padB = 34;
        var gw = s.w - padL - padR, gh = s.h - padT - padB;
        var max = Math.max.apply(null, rows.map(function (r) { return r.total; }));
        var slot = gw / rows.length, bw = Math.min(52, slot * 0.5);

        for (var i = 0; i <= 4; i++) {
            var y = padT + gh * i / 4;
            ctx.strokeStyle = LINE; ctx.lineWidth = 1;
            ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(padL + gw, y); ctx.stroke();
            ctx.fillStyle = SUB; ctx.textAlign = 'right';
            ctx.fillText(Math.round(max * (1 - i / 4)) + '', padL - 6, y + 4);
        }

        rows.forEach(function (r, i) {
            var x = padL + slot * i + (slot - bw) / 2;
            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, x, padT, bw, gh, 6);

            var hh = gh * r.done / max;
            var rate = r.done / r.total;
            ctx.fillStyle = rate >= 0.75 ? C.green : (rate >= 0.5 ? C.amber : C.red);
            rrect(ctx, x, padT + gh - hh, bw, hh, 6);

            ctx.textAlign = 'center';
            ctx.fillStyle = INK;
            ctx.fillText(r.done + '/' + r.total, x + bw / 2, padT + gh - hh - 6);
            ctx.fillStyle = SUB;
            ctx.fillText(r.label, x + bw / 2, s.h - 12);
        });
    }

    /** 가로 막대 — 요구 역량 TOP. */
    function hbar(id, rows) {
        var s = setup(id); if (!s || !rows) return;
        var ctx = s.ctx, padL = 92, padR = 44, top = 8;
        var barH = 12, gap = (s.h - top - 6) / rows.length;

        rows.forEach(function (r, i) {
            var y = top + i * gap;
            var full = s.w - padL - padR;

            ctx.fillStyle = INK; ctx.textAlign = 'right';
            ctx.fillText(cut(r.label, 8), padL - 10, y + barH - 1);

            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, padL, y, full, barH, 6);
            ctx.fillStyle = C.navy;
            rrect(ctx, padL, y, full * r.value / 100, barH, 6);

            ctx.textAlign = 'left'; ctx.fillStyle = SUB;
            ctx.fillText(r.value + '%', s.w - padR + 6, y + barH - 1);
        });
    }

    function cut(s, n) {
        s = String(s == null ? '' : s);
        return s.length > n ? s.slice(0, n - 1) + '…' : s;
    }
})();
