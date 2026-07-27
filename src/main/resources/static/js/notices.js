// 예시 공지사항 데이터
const noticeData = Array.from({length: 37}, (_, i) => {
    const day = (i%30+1).toString().padStart(2,'0');
    const editDay = ((i%30+2) > 30 ? 1 : (i%30+2)).toString().padStart(2,'0');
    return {
        id: 37-i,
        category: (i % 3 === 0 ? 'kdt' : i % 3 === 1 ? '고교위탁' : '심화'),
        title: `공지사항 제목 ${37-i}`,
        author: i % 2 === 0 ? '관리자' : '운영팀',
        date: `2026-01-${day}`,
        editDate: `2026-01-${editDay}`,
        views: Math.floor(Math.random()*100+10)
    }
});

const pageSize = 10;
let currentPage = 1;

function renderNoticeTable() {
    const tbody = document.getElementById('noticeTableBody');
    tbody.innerHTML = '';
    const startIdx = (currentPage-1)*pageSize;
    const pageItems = noticeData.slice(startIdx, startIdx+pageSize);
    pageItems.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${item.id}</td>
            <td>${item.category}</td>
            <td style="text-align:left;padding-left:16px;">${item.title}</td>
            <td>${item.author}</td>
            <td>${item.date}</td>
            <td>${item.editDate}</td>
            <td>${item.views}</td>
        `;
        tr.addEventListener('click', () => {
            window.location.href = 'notice-detail.html';
        });
        tbody.appendChild(tr);
    });
}

function renderPagination() {
    const totalPages = Math.ceil(noticeData.length/pageSize);
    document.getElementById('currentPageInfo').textContent = currentPage;
    document.getElementById('totalPagesInfo').textContent = totalPages;
    // prev/next 버튼 상태
    document.getElementById('prevPage').disabled = currentPage === 1;
    document.getElementById('nextPage').disabled = currentPage === totalPages;
    // 페이지 번호 버튼
    const pageNumbers = document.getElementById('pageNumbers');
    pageNumbers.innerHTML = '';
    for(let i=1;i<=totalPages;i++){
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (i===currentPage ? ' active' : '');
        btn.textContent = i;
        btn.onclick = () => {
            currentPage = i;
            renderNoticeTable();
            renderPagination();
        };
        pageNumbers.appendChild(btn);
    }
}

function changePage(delta) {
    const totalPages = Math.ceil(noticeData.length/pageSize);
    let newPage = currentPage + delta;
    if (newPage < 1) newPage = 1;
    if (newPage > totalPages) newPage = totalPages;
    if (newPage !== currentPage) {
        currentPage = newPage;
        renderNoticeTable();
        renderPagination();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    renderNoticeTable();
    renderPagination();

    // 카테고리 select 동적 구성 (DB에서 받아온 데이터로 대체)
    const categoryList = [
        { value: '', text: '전체' },
        { value: 'kdt', text: 'KDT' },
        { value: '고교위탁', text: '고교위탁' },
        { value: '심화', text: '심화' },
        // DB에서 받아온 카테고리 추가 가능
    ];
    const courseFilter = document.getElementById('courseFilter');
    courseFilter.innerHTML = '';
    categoryList.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat.value;
        option.textContent = cat.text;
        courseFilter.appendChild(option);
    });

    // 등록일 직접선택 시 date 입력창 표시
    const dateFilter = document.getElementById('dateFilter');
    const customDateRange = document.getElementById('customDateRange');
    function handleDateFilterChange() {
        if (dateFilter.value === 'custom') {
            customDateRange.style.display = 'inline-flex';
        } else {
            customDateRange.style.display = 'none';
        }
    }
    dateFilter.addEventListener('change', handleDateFilterChange);
    // 페이지 로드 시 초기화
    handleDateFilterChange();
});
