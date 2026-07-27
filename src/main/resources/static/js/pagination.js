// 페이지네이션 기능
class Pagination {
    constructor(options = {}) {
        this.itemsPerPage = options.itemsPerPage || 10;
        this.currentPage = 1;
        this.filteredItems = [];
        this.itemSelector = options.itemSelector || '.list-item';
        this.containerSelector = options.containerSelector || '.list-container';
        this.paginationSelector = options.paginationSelector || '.pagination';
    }

    // 페이지네이션 초기화
    init() {
        this.updatePagination();
        this.renderPaginationUI();
    }

    // 페이지네이션 업데이트
    updatePagination() {
        const allItems = document.querySelectorAll(this.itemSelector);
        this.filteredItems = Array.from(allItems).filter(item => {
            const displayStyle = window.getComputedStyle(item).display;
            return displayStyle !== 'none';
        });
        
        const totalPages = Math.ceil(this.filteredItems.length / this.itemsPerPage);
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;

        // 모든 항목 숨기기
        this.filteredItems.forEach(item => {
            item.style.display = 'none';
        });

        // 현재 페이지 항목만 표시
        this.filteredItems.slice(startIndex, endIndex).forEach(item => {
            item.style.display = '';
        });

        this.updatePaginationUI(totalPages);
    }

    // 페이지네이션 UI 렌더링
    renderPaginationUI() {
        const paginationContainer = document.querySelector(this.paginationSelector);
        if (!paginationContainer) return;

        paginationContainer.innerHTML = `
            <button id="prevPage" class="pagination-btn">이전</button>
            <div id="pageNumbers" class="page-numbers"></div>
            <button id="nextPage" class="pagination-btn">다음</button>
            <div class="page-info">
                <span id="currentPageInfo">1</span> / <span id="totalPagesInfo">1</span> 페이지
            </div>
        `;

        document.getElementById('prevPage').addEventListener('click', () => this.changePage(-1));
        document.getElementById('nextPage').addEventListener('click', () => this.changePage(1));
    }

    // 페이지네이션 UI 업데이트
    updatePaginationUI(totalPages) {
        const prevBtn = document.getElementById('prevPage');
        const nextBtn = document.getElementById('nextPage');
        const currentPageInfo = document.getElementById('currentPageInfo');
        const totalPagesInfo = document.getElementById('totalPagesInfo');

        if (prevBtn) prevBtn.disabled = this.currentPage === 1;
        if (nextBtn) nextBtn.disabled = this.currentPage === totalPages || totalPages === 0;
        if (currentPageInfo) currentPageInfo.textContent = totalPages === 0 ? 0 : this.currentPage;
        if (totalPagesInfo) totalPagesInfo.textContent = totalPages;

        this.updatePageNumbers(totalPages);
    }

    // 페이지 번호 버튼 업데이트
    updatePageNumbers(totalPages) {
        const pageNumbersDiv = document.getElementById('pageNumbers');
        if (!pageNumbersDiv) return;

        pageNumbersDiv.innerHTML = '';

        // 최대 5개의 페이지 번호 표시
        let startPage = Math.max(1, this.currentPage - 2);
        let endPage = Math.min(totalPages, startPage + 4);

        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }

        for (let i = startPage; i <= endPage; i++) {
            const button = document.createElement('button');
            button.textContent = i;
            button.className = 'page-number-btn';
            button.onclick = () => this.goToPage(i);
            if (i === this.currentPage) {
                button.classList.add('active');
            }
            pageNumbersDiv.appendChild(button);
        }
    }

    // 페이지 변경
    changePage(direction) {
        const totalPages = Math.ceil(this.filteredItems.length / this.itemsPerPage);
        this.currentPage += direction;
        if (this.currentPage < 1) this.currentPage = 1;
        if (this.currentPage > totalPages) this.currentPage = totalPages;
        this.updatePagination();
    }

    // 특정 페이지로 이동
    goToPage(page) {
        this.currentPage = page;
        this.updatePagination();
    }

    // 필터링 후 페이지네이션 초기화
    reset() {
        this.currentPage = 1;
        this.updatePagination();
    }

    // 페이지 당 항목 수 변경
    setItemsPerPage(count) {
        this.itemsPerPage = count;
        this.currentPage = 1;
        this.updatePagination();
    }
}

// 전역 변수로 내보내기 (ES5 방식)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = Pagination;
}
