/**
 * 지도형 로드맵 렌더러.
 *
 * 단계 목록을 받아 **구불구불한 길 + 정거장**으로 그린다.
 * 노드 좌표를 지그재그로 계산하고 그 점들을 지나는 부드러운 곡선(Catmull-Rom → 베지어)을
 * SVG 로 그린다. 단계 수가 3개든 8개든 같은 코드로 그려진다.
 *
 * 왜 목록이 아니라 지도인가 — "지금 어디까지 왔고 다음이 무엇인지"가 한눈에 보여야
 * 학습 동기가 생긴다. 목록은 순서는 보여주지만 진행 상황이 안 보인다.
 *
 * 전역 함수 하나만 노출한다: renderRoadmapMap(steps, onSelect)
 */
(function (global) {
    'use strict';

    var STEP_GAP = 210;   // 정거장 사이 세로 간격
    var TOP_PAD = 90;
    var BOTTOM_PAD = 110;

    /**
     * @param steps [{ title, meta, reason, status: 'done'|'current'|'locked', icon }]
     * @param onSelect 정거장을 눌렀을 때 호출 (index)
     */
    global.renderRoadmapMap = function (steps, onSelect) {
        var canvas = document.getElementById('mapCanvas');
        if (!canvas || !steps || !steps.length) return;

        var width = canvas.clientWidth || 900;
        var height = TOP_PAD + STEP_GAP * (steps.length - 1) + BOTTOM_PAD;
        canvas.style.height = height + 'px';

        var pts = layout(steps.length, width, height);

        canvas.innerHTML =
            svg(pts, steps, width, height) +
            flags(pts, width) +
            steps.map(function (s, i) { return nodeHtml(s, i, pts[i]); }).join('');

        canvas.querySelectorAll('.map-node').forEach(function (el) {
            el.addEventListener('click', function () {
                onSelect(Number(el.dataset.idx));
            });
        });
    };

    /** 좌우로 번갈아 놓아 길이 구부러지게 한다. 가장자리는 라벨이 잘리지 않게 여백을 둔다. */
    function layout(n, width, height) {
        var leftX = Math.max(140, width * 0.2);
        var rightX = Math.min(width - 140, width * 0.8);
        var pts = [];
        for (var i = 0; i < n; i++) {
            pts.push({
                x: (i % 2 === 0) ? leftX : rightX,
                y: TOP_PAD + i * STEP_GAP
            });
        }
        return pts;
    }

    function svg(pts, steps, width, height) {
        var d = curve(pts);

        // 이미 지나온 구간만 따로 진하게 — 진행률이 길 색으로 보인다
        var doneCount = 0;
        for (var i = 0; i < steps.length; i++) {
            if (steps[i].status === 'done') doneCount = i + 1;
        }
        var donePath = doneCount > 1 ? curve(pts.slice(0, doneCount)) : '';

        return '<svg viewBox="0 0 ' + width + ' ' + height + '" preserveAspectRatio="none" aria-hidden="true">' +
            '<path class="map-path-base" d="' + d + '"/>' +
            '<path class="map-path-fill" d="' + d + '"/>' +
            (donePath ? '<path class="map-path-done" d="' + donePath + '"/>' : '') +
            '<path class="map-path-dots" d="' + d + '"/>' +
        '</svg>';
    }

    /**
     * 점들을 지나는 부드러운 곡선.
     * Catmull-Rom 스플라인을 3차 베지어로 변환한다 — 점을 반드시 지나므로
     * 정거장이 길 위에 정확히 얹힌다.
     */
    function curve(p) {
        if (p.length < 2) return '';
        var d = 'M ' + p[0].x + ' ' + p[0].y;
        for (var i = 0; i < p.length - 1; i++) {
            var p0 = p[i - 1] || p[i];
            var p1 = p[i];
            var p2 = p[i + 1];
            var p3 = p[i + 2] || p2;

            var c1x = p1.x + (p2.x - p0.x) / 6;
            var c1y = p1.y + (p2.y - p0.y) / 6;
            var c2x = p2.x - (p3.x - p1.x) / 6;
            var c2y = p2.y - (p3.y - p1.y) / 6;

            d += ' C ' + c1x + ' ' + c1y + ', ' + c2x + ' ' + c2y + ', ' + p2.x + ' ' + p2.y;
        }
        return d;
    }

    function flags(pts, width) {
        var first = pts[0];
        var last = pts[pts.length - 1];
        return '<div class="map-flag" style="left:' + first.x + 'px; top:' + (first.y - 78) + 'px;">' +
                    '<span class="icon">🚩</span>출발' +
               '</div>' +
               '<div class="map-flag" style="left:' + last.x + 'px; top:' + (last.y + 86) + 'px;">' +
                    '<span class="icon">🏁</span>목표 직무' +
               '</div>';
    }

    function nodeHtml(s, i, pt) {
        var badge = s.status === 'done' ? '✓' : (s.status === 'current' ? '★' : (i + 1));
        // 상태를 색뿐 아니라 글자로도 알린다 (색만으로 구분하면 안 된다)
        var stateText = s.status === 'done' ? '완료' : (s.status === 'current' ? '지금 할 차례' : '앞으로 할 것');

        return '<button type="button" class="map-node ' + s.status + '" data-idx="' + i + '"' +
                    ' style="left:' + pt.x + 'px; top:' + pt.y + 'px;"' +
                    ' aria-label="' + esc(s.title) + ' — ' + stateText + '">' +
                    '<span class="map-node-circle">' + (s.icon || '📘') +
                        '<span class="map-node-badge">' + badge + '</span>' +
                    '</span>' +
                    '<span class="map-node-label">' + esc(s.title) + '</span>' +
               '</button>';
    }

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})(window);
