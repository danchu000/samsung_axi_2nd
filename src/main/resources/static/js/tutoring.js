
    // 동적 데이터 예시 (타임리프 연동시 서버에서 내려줄 예정)
    const responseData = [
        {
            type: 'QnA',
            category: '수업',
            status: '미답변',
            statusClass: 'status-dropout',
            course: 'AI 분석 / 3차시',
            requester: '김OO',
            manager: '미배정',
            elapsed: '26h'
        },
        {
            type: '튜터링',
            category: '진도',
            status: '진행중',
            statusClass: 'status-pending',
            course: 'AI 기초 / 2차시',
            requester: '이OO',
            manager: '홍길동',
            elapsed: '3h'
        }
    ];

    function renderResponseTable() {
        const tbody = document.getElementById('responseTableBody');
        if (!tbody) return;
        tbody.innerHTML = responseData.map(row => `
            <tr>
                <td>${row.type}</td>
                <td>${row.category}</td>
                <td><span class="status-badge ${row.statusClass}">${row.status}</span></td>
                <td>${row.course}</td>
                <td>${row.requester}</td>
                <td class="${row.manager === '미배정' ? 'text-muted' : ''}">${row.manager}</td>
                <td>${row.elapsed}</td>
                <td><button class="btn-secondary btn-sx tutoring-detail-btn">상세 페이지로 이동</button></td>
            </tr>
        `).join('');
    }

    document.addEventListener('DOMContentLoaded', renderResponseTable);

    // 상세 페이지로 이동 버튼 클릭 시 모달 창 열기
    document.addEventListener('click', function(e) {
        if (e.target && e.target.classList.contains('tutoring-detail-btn')) {
            window.open('modal-tutoring.html', 'modalTutoring', 'width=800,height=600,resizable=yes,scrollbars=yes');
        }
    });
    sortSelect.addEventListener('change', sortTable);

// 검색하기 버튼 이벤트에 정렬도 반영
const searchBtn = document.querySelector('.btn.btn-secondary[onclick="filterResponseTable()"]');
if (searchBtn) {
    searchBtn.addEventListener('click', function() {
        setTimeout(sortTable, 0); // 필터 후 정렬 적용
    });
}

function sortTable() {
    const sortField = document.getElementById('sortSelect').value;
    const table = document.querySelector('table.history-table tbody');
    if (!table) return;
    const rows = Array.from(table.querySelectorAll('tr'));
    let getValue;
    if (sortField === 'status') {
        // 상태: 미답변 < 진행중
        getValue = row => {
            const text = row.children[2].innerText.trim();
            return text === '미답변' ? 0 : 1;
        };
    } else if (sortField === 'elapsed') {
        // 경과시간: 숫자만 추출
        getValue = row => {
            const val = row.children[6].innerText.trim();
            return parseInt(val.replace(/[^0-9]/g, '')) || 0;
        };
    } else {
        getValue = () => 0;
    }
    rows.sort((a, b) => {
        const va = getValue(a);
        const vb = getValue(b);
        return sortAscending ? va - vb : vb - va;
    });
    // 테이블에 반영
    rows.forEach(row => table.appendChild(row));
}
