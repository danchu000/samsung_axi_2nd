/**
 * 서버 페이지네이션 (개발자 B).
 *
 * 기존 `pagination.js` 는 화면에 그려진 행을 JS 가 숨겼다 보였다 하는 방식이라,
 * 서버가 전체 행을 다 내려줘야 했다. 데이터가 쌓이면 목록 한 번 여는 데
 * 수천 행이 오간다.
 *
 * 이 스크립트는 서버가 한 페이지 분량만 내려주는 화면에서 쓴다.
 * 페이지 이동은 링크(GET 파라미터)로 하고, JS 는 링크를 그리기만 한다.
 *
 * 사용법 — 화면에서 아래를 내려주면 된다:
 *   window._serverPage = {
 *     page: 1,          // 현재 페이지 (1부터)
 *     totalPages: 5,
 *     totalCount: 47,
 *     size: 10
 *   };
 *
 * 기존 페이지네이션 DOM(#prevPage, #nextPage, #pageNumbers, #currentPageInfo,
 * #totalPagesInfo)을 그대로 재사용하므로 화면 구조를 바꾸지 않는다.
 */
(function () {
    var p = window._serverPage;
    if (!p) {
        return;   // 서버 페이징을 쓰지 않는 화면 — 기존 pagination.js 가 담당
    }

    document.addEventListener('DOMContentLoaded', function () {
        render(p);
    });

    /** 현재 URL 에서 page 만 바꾼 주소. 검색 조건(필터)은 그대로 유지된다. */
    function urlForPage(n) {
        var url = new URL(window.location.href);
        url.searchParams.set('page', n);
        return url.pathname + '?' + url.searchParams.toString();
    }

    function go(n) {
        window.location.href = urlForPage(n);
    }

    function render(page) {
        var total = Math.max(page.totalPages || 1, 1);
        var cur = Math.min(Math.max(page.page || 1, 1), total);

        var info = document.getElementById('currentPageInfo');
        if (info) info.textContent = page.totalCount === 0 ? 0 : cur;
        var totalInfo = document.getElementById('totalPagesInfo');
        if (totalInfo) totalInfo.textContent = total;

        var prev = document.getElementById('prevPage');
        if (prev) {
            prev.disabled = cur <= 1;
            prev.onclick = function () { if (cur > 1) go(cur - 1); };
        }
        var next = document.getElementById('nextPage');
        if (next) {
            next.disabled = cur >= total;
            next.onclick = function () { if (cur < total) go(cur + 1); };
        }

        var box = document.getElementById('pageNumbers');
        if (!box) return;
        box.innerHTML = '';

        // 현재 페이지 기준 앞뒤 2개씩 — 기존 pagination.js 와 같은 폭
        var start = Math.max(1, cur - 2);
        var end = Math.min(total, start + 4);
        start = Math.max(1, end - 4);

        for (var i = start; i <= end; i++) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = String(i);
            if (i === cur) {
                btn.classList.add('active');
                btn.disabled = true;
            } else {
                btn.addEventListener('click', (function (n) {
                    return function () { go(n); };
                })(i));
            }
            box.appendChild(btn);
        }
    }

    /**
     * 기존 pagination.js 의 전역 changePage(±1) 을 서버 이동으로 바꾼다.
     * 일부 화면이 onclick="changePage(-1)" 를 인라인으로 갖고 있어서,
     * 그대로 두면 서버 페이징 화면에서 행이 사라진다.
     */
    window.changePage = function (direction) {
        var total = Math.max(p.totalPages || 1, 1);
        var next = Math.min(Math.max((p.page || 1) + direction, 1), total);
        if (next !== p.page) {
            go(next);
        }
    };
})();
