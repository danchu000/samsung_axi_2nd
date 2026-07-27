/**
 * 문제은행 일괄 액션 (개발자 B).
 *
 * 목록 테이블은 contents.js 가 그리므로, 여기서는 체크된 행의 문제 id 를 모아
 * 서버로 POST 하는 것만 담당한다. URL 과 CSRF 토큰은 페이지가 내려준다.
 */
document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('bulkForm');
    var idsInput = document.getElementById('bulkIds');
    var urls = window._questionBulkUrls;
    if (!form || !idsInput || !urls) {
        return;
    }

    /** 체크된 행에서 문제 id 수집. id 는 행의 "수정하기" 버튼 data-id 에 들어 있다. */
    function selectedIds() {
        var tbody = document.querySelector('.question-table-area tbody');
        if (!tbody) {
            return [];
        }
        return Array.from(tbody.querySelectorAll('tr'))
            .filter(function (tr) {
                var cb = tr.querySelector('input[type="checkbox"]');
                return cb && cb.checked;
            })
            .map(function (tr) {
                var btn = tr.querySelector('button.btn-secondary');
                return btn ? btn.getAttribute('data-id') : null;
            })
            .filter(Boolean);
    }

    function submit(action, confirmMessage) {
        var ids = selectedIds();
        if (ids.length === 0) {
            alert('먼저 문제를 선택하세요.');
            return;
        }
        if (!confirm(ids.length + '건을 ' + confirmMessage)) {
            return;
        }
        idsInput.value = ids.join(',');
        form.setAttribute('action', action);
        form.submit();
    }

    var deactivateBtn = document.getElementById('bulkDeactivateBtn');
    if (deactivateBtn) {
        deactivateBtn.addEventListener('click', function () {
            submit(urls.deactivate, '비활성화할까요?');
        });
    }

    var deleteBtn = document.getElementById('bulkDeleteBtn');
    if (deleteBtn) {
        deleteBtn.addEventListener('click', function () {
            // 물리 삭제가 아니라 soft delete (내역서 3년 보존 요건)
            submit(urls.remove, '삭제할까요? 삭제된 문제는 목록에서만 사라지고 기록은 보존됩니다.');
        });
    }
});
