/**
 * 오늘의 여정 — 훈련생 대시보드의 "오늘 할 일"을 가로 지도로 그린다.
 *
 * 목록은 "무엇이 있는지"는 보여주지만 "지금 어디쯤인지"가 안 보인다.
 * 마감이 급한 순서대로 길 위에 늘어놓으면 오늘의 흐름이 한눈에 들어온다.
 *
 * **더미를 쓰지 않는다.** 아래 목록(hpTodoList)과 똑같은 서버 데이터를 읽는다 —
 * 지도와 목록이 다른 값을 보여주면 둘 다 못 믿게 된다.
 *
 * 실행 순서: trainee/index.js 가 window._serverTraineeDashboard 로 목록을 그린 뒤
 * 이 스크립트가 같은 값을 읽어 지도를 그린다. 둘 다 defer 라 순서가 보장된다.
 */
(function () {
    'use strict';

    // 마감이 지난 것 / 가장 급한 것 / 나머지 — 지도의 세 상태로 옮긴다
    function statusOf(dday, isFirstPending) {
        if (typeof dday === 'number' && dday < 0) return 'done';      // 마감 지남
        if (isFirstPending) return 'current';                          // 가장 급한 항목
        return 'locked';
    }

    var ICON = {
        TASK: '📝',
        EXAM: '🧪',
        SURVEY: '🗳️',
        CONTENT: '🎬',
        DEFAULT: '📌'
    };

    document.addEventListener('DOMContentLoaded', function () {
        var box = document.getElementById('todayJourney');
        var canvas = document.getElementById('todayMapCanvas');
        if (!box || !canvas || typeof window.renderRoadmapMap !== 'function') return;

        var dash = window._serverTraineeDashboard;
        var todos = (dash && dash.todos) || [];

        // 할 일이 없으면 지도도 없다 — 빈 길만 그리면 고장처럼 보인다
        if (!todos.length) {
            box.style.display = 'none';
            return;
        }

        // 목록과 같은 기준(마감 임박순)으로 정렬한다
        var sorted = todos.slice().sort(function (a, b) {
            var ad = (a.dday === null || a.dday === undefined) ? 9999 : a.dday;
            var bd = (b.dday === null || b.dday === undefined) ? 9999 : b.dday;
            return ad - bd;
        });

        // 화면이 좁으면 원이 겹치므로 앞의 6개만 — 나머지는 아래 목록에 다 있다
        var shown = sorted.slice(0, 6);

        var firstPendingIdx = -1;
        shown.forEach(function (t, i) {
            if (firstPendingIdx < 0 && !(typeof t.dday === 'number' && t.dday < 0)) firstPendingIdx = i;
        });

        var steps = shown.map(function (t, i) {
            return {
                title: t.title,
                meta: (t.meta ? t.meta + ' · ' : '') + '마감 ' + (t.due || '-') + ddayText(t.dday),
                reason: reasonOf(t),
                status: statusOf(t.dday, i === firstPendingIdx),
                icon: ICON[t.type] || ICON.DEFAULT,
                href: t.href
            };
        });

        box.style.display = '';
        draw();

        // 폭이 바뀌면 정거장 좌표가 달라지므로 다시 그린다
        var timer;
        window.addEventListener('resize', function () {
            clearTimeout(timer);
            timer = setTimeout(draw, 200);
        });

        function draw() {
            window.renderRoadmapMap(steps, function (idx) {
                // 정거장을 누르면 그 항목으로 바로 이동 — 지도가 장식이 아니라 길이 되게
                var href = steps[idx] && steps[idx].href;
                if (href) window.location.href = href;
            }, { el: 'todayMapCanvas', orientation: 'horizontal' });
        }
    });

    function ddayText(dday) {
        if (typeof dday !== 'number') return '';
        if (dday < 0) return ' (마감 지남)';
        if (dday === 0) return ' (오늘 마감)';
        return ' (D-' + dday + ')';
    }

    function reasonOf(t) {
        if (typeof t.dday === 'number' && t.dday < 0) return '마감이 지났어요. 아직 제출할 수 있는지 확인해 보세요.';
        if (t.dday === 0) return '오늘 마감이에요. 가장 먼저 처리하는 게 좋아요.';
        return '아직 여유가 있어요.';
    }
})();
