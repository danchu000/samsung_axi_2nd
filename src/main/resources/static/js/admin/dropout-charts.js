/**
 * 이탈 예측 대시보드 차트.
 *
 * 데이터는 서버(DropoutPredictionService)가 window._serverDropout 으로 주입한다 —
 * 지금은 표본(sample=true)이고, 모델이 연동되면 같은 모양의 실제 값이 온다.
 * 이 파일은 그리기만 하므로 연동 시 바꿀 것이 없다.
 *
 * 추이 선은 dashboard-charts-common.js 의 공용 렌더러를 쓴다.
 * 과정별 위험도 막대만 여기서 그린다 — 값이 높을수록 나쁜 지표라 경고색 규칙
 * (60% 이상 빨강 · 40% 이상 주황)이 필요한데, 공용 hbar 는 "높을수록 좋은" 지표용이라
 * 남색 고정이다. 공용을 고치면 기존 화면 의미가 흔들리므로 여기서만 따로 그린다.
 *
 * 선 색 3종(#ef4444/#f59e0b/#3b4f8f)은 색각이상(CVD) 분리도·명도 검증을 통과한 조합이다.
 * 안정 계열을 기존 남색(#5a6a9a)보다 진하게 쓴 것은 취향이 아니라 검증 결과다 — 바꾸지 말 것.
 */
(function () {
    'use strict';

    var RED = '#ef4444', AMBER = '#f59e0b', NAVY = '#3b4f8f';
    var INK = '#101828', SUB = '#667085';

    document.addEventListener('DOMContentLoaded', function () {
        var d = window._serverDropout;
        if (!d) return;
        drawAll(d);

        // 폭이 바뀌면 canvas 픽셀 크기가 어긋나 흐려진다 — 다시 그린다
        var t;
        window.addEventListener('resize', function () {
            clearTimeout(t);
            t = setTimeout(function () { drawAll(d); }, 200);
        });
    });

    function drawAll(d) {
        var C = window.DashCharts;
        if (!C) return;

        C.weekly('dropoutTrendChart', d.trend && d.trend.weeks, [
            { label: '고위험', color: RED, values: d.trend ? d.trend.high : [] },
            { label: '중위험', color: AMBER, values: d.trend ? d.trend.mid : [] },
            { label: '안정', color: NAVY, values: d.trend ? d.trend.stable : [] }
        ], '추이 데이터가 없습니다');

        riskBars('courseRiskChart', d.courseRisks);
        C.hbar('factorChart', d.factors, '요인 데이터가 없습니다');
        // 데이터 단계 안내는 상단 배너(admin-dropout.html)가 담당한다 — 차트별 배지는 달지 않는다
    }

    /**
     * 과정별 이탈 위험도 가로 막대 — 위험 등급 경계(40/60%)에서 경고색으로 바뀐다.
     * 색만으로 구분하지 않는다: 값 %가 항상 함께 찍히고, 경계는 패널 부제에 글로 적혀 있다.
     * rows: [{ name, risk }]
     */
    function riskBars(id, rows) {
        var s = setup(id); if (!s) return;
        rows = rows || [];
        if (!rows.length) { empty(s, '표시할 과정이 없습니다'); return; }

        var ctx = s.ctx, padL = 108, padR = 46, top = 12;
        var barH = 14, gap = (s.h - top - 10) / rows.length;

        rows.forEach(function (r, i) {
            var y = top + i * gap, full = s.w - padL - padR;

            ctx.fillStyle = INK; ctx.textAlign = 'right';
            ctx.fillText(cut(r.name, 9), padL - 10, y + barH - 1);

            ctx.fillStyle = '#f3f4f6';
            rrect(ctx, padL, y, full, barH, 7);

            var color = r.risk >= 60 ? RED : (r.risk >= 40 ? AMBER : NAVY);
            ctx.fillStyle = color;
            rrect(ctx, padL, y, full * Math.min(r.risk, 100) / 100, barH, 7);

            ctx.textAlign = 'left';
            ctx.fillStyle = r.risk >= 40 ? '#b45309' : INK;
            if (r.risk >= 60) ctx.fillStyle = '#b91c1c';
            ctx.fillText(r.risk + '%', s.w - padR + 6, y + barH - 1);
        });
    }

    /* ===== canvas 유틸 — 공용 모듈이 내부 함수를 노출하지 않아 같은 방식으로 둔다 ===== */

    function setup(id) {
        var cv = document.getElementById(id);
        if (!cv) return null;
        var dpr = window.devicePixelRatio || 1;
        var w = cv.clientWidth || cv.parentNode.clientWidth || 420;
        // 공용 setup() 과 같은 이유 — height 속성을 매번 읽으면 HiDPI 리사이즈마다 높이가 자란다
        var h = Number(cv.dataset.baseHeight || cv.getAttribute('height')) || 240;
        cv.dataset.baseHeight = h;
        cv.width = w * dpr;
        cv.height = h * dpr;
        cv.style.height = h + 'px';
        var ctx = cv.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, w, h);
        ctx.font = '12px Pretendard, sans-serif';
        return { ctx: ctx, w: w, h: h };
    }

    function empty(s, msg) {
        s.ctx.fillStyle = '#9ca3af';
        s.ctx.textAlign = 'center';
        s.ctx.fillText(msg, s.w / 2, s.h / 2);
    }

    function rrect(ctx, x, y, w, h, r) {
        if (w <= 0 || h <= 0) return;
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

    function cut(s, n) {
        s = String(s == null ? '' : s);
        return s.length > n ? s.slice(0, n - 1) + '…' : s;
    }
})();
