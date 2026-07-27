// PDF.js 워커 설정
pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';

// PDF 파일 경로 (테스트용 공개 PDF)
const pdfUrl = 'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf';

// 뷰어 상태
let pdfDoc = null;
let currentPageIndex = 0; // 현재 보고 있는 첫 번째 페이지 (0-based)
let maxPageReached = 0; // 사용자가 도달한 최대 페이지 (진행도 추적)
let totalPages = 0;
let viewMode = 'two'; // 'single' 또는 'two'

// PDF 로드
async function loadPDF() {
    try {
        const loadingTask = pdfjsLib.getDocument(pdfUrl);
        pdfDoc = await loadingTask.promise;
        totalPages = pdfDoc.numPages;
        
        // 초기 렌더링
        renderPages();
        updateControls();
        updateProgress();
    } catch (error) {
        console.error('PDF 로드 실패:', error);
        document.getElementById('pagesContainer').innerHTML = 
            '<div class="loading-message">문서를 불러올 수 없습니다. 파일 경로를 확인해주세요.</div>';
    }
}

// 보기 모드 변경
function setViewMode(mode) {
    viewMode = mode;
    const container = document.getElementById('pagesContainer');
    const singleBtn = document.getElementById('singlePageBtn');
    const twoBtn = document.getElementById('twoPageBtn');

    if (mode === 'single') {
        container.classList.add('single-page');
        singleBtn.classList.add('active');
        twoBtn.classList.remove('active');
    } else {
        container.classList.remove('single-page');
        singleBtn.classList.remove('active');
        twoBtn.classList.add('active');
    }

    renderPages();
    updateControls();
}

// 페이지 렌더링
async function renderPages() {
    const container = document.getElementById('pagesContainer');
    container.innerHTML = '<div class="loading-message">페이지를 불러오는 중...</div>';

    const page1Num = currentPageIndex + 1; // PDF 페이지는 1-based
    container.innerHTML = '';

    // 첫 번째 페이지는 항상 표시
    if (page1Num <= totalPages) {
        const page1 = await renderPDFPage(page1Num);
        container.appendChild(page1);
    }

    // 두쪽 보기 모드일 때만 두 번째 페이지 표시
    if (viewMode === 'two') {
        const page2Num = currentPageIndex + 2;
        if (page2Num <= totalPages) {
            const page2 = await renderPDFPage(page2Num);
            container.appendChild(page2);
        }
    }

    // 진행도 업데이트
    updateMaxPageReached();
}

// PDF 페이지 렌더링
async function renderPDFPage(pageNum) {
    const page = await pdfDoc.getPage(pageNum);
    
    // 캔버스 생성
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    
    // 스케일 설정 (해상도)
    const scale = 1.5;
    const viewport = page.getViewport({ scale: scale });
    
    canvas.height = viewport.height;
    canvas.width = viewport.width;
    canvas.className = 'pdf-canvas';
    
    // 페이지 렌더링
    const renderContext = {
        canvasContext: context,
        viewport: viewport
    };
    await page.render(renderContext).promise;
    
    // 페이지 컨테이너 생성
    const pageDiv = document.createElement('div');
    pageDiv.className = 'document-page';
    pageDiv.appendChild(canvas);
    
    const pageNumber = document.createElement('div');
    pageNumber.className = 'page-number';
    pageNumber.textContent = `- ${pageNum} -`;
    pageDiv.appendChild(pageNumber);
    
    return pageDiv;
}

// 다음 페이지
function nextPage() {
    const increment = viewMode === 'single' ? 1 : 2;
    if (currentPageIndex + increment < totalPages) {
        currentPageIndex += increment;
        renderPages();
        updateControls();
        updateProgress();
    }
}

// 이전 페이지
function previousPage() {
    const decrement = viewMode === 'single' ? 1 : 2;
    if (currentPageIndex > 0) {
        currentPageIndex -= decrement;
        renderPages();
        updateControls();
        updateProgress();
    }
}

// 첫 페이지로
function firstPage() {
    if (currentPageIndex !== 0) {
        currentPageIndex = 0;
        renderPages();
        updateControls();
        updateProgress();
    }
}

// 컨트롤 버튼 상태 업데이트
function updateControls() {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const increment = viewMode === 'single' ? 1 : 2;

    prevBtn.disabled = currentPageIndex === 0;
    nextBtn.disabled = currentPageIndex + increment >= totalPages;

    // 현재 페이지 표시
    const page1 = currentPageIndex + 1;
    if (viewMode === 'single') {
        document.getElementById('currentPages').textContent = `${page1} / ${totalPages}`;
    } else {
        const page2 = Math.min(currentPageIndex + 2, totalPages);
        document.getElementById('currentPages').textContent = `${page1}-${page2} / ${totalPages}`;
    }
}

// 최대 도달 페이지 업데이트
function updateMaxPageReached() {
    const increment = viewMode === 'single' ? 1 : 2;
    const currentMax = Math.min(currentPageIndex + increment, totalPages);
    if (currentMax > maxPageReached) {
        maxPageReached = currentMax;
        updateProgress();
    }
}

// 진행률 업데이트
function updateProgress() {
    const progressPercent = Math.round((maxPageReached / totalPages) * 100);
    document.getElementById('progressPercent').textContent = progressPercent;
    document.getElementById('maxPageReached').textContent = maxPageReached;
}

// 페이지 컨테이너 클릭으로 페이지 넘기기
function setupPageNavigation() {
    const container = document.getElementById('pagesContainer');
    container.addEventListener('click', function(e) {
        const rect = container.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        const containerWidth = rect.width;
        
        // 왼쪽 절반 클릭 시 이전 페이지
        if (clickX < containerWidth / 2) {
            previousPage();
        } else {
            // 오른쪽 절반 클릭 시 다음 페이지
            nextPage();
        }
    });
}

// 페이지 로드 시 PDF 로드
window.addEventListener('DOMContentLoaded', () => {
    loadPDF();
    setupPageNavigation();
    // "학습진도 저장하고 나가기" 버튼 기능 연결
    var saveExitBtn = document.querySelector('.big-btn.btn-secondary');
    if (saveExitBtn) {
        saveExitBtn.addEventListener('click', function() {
            // 진도 저장 로직 (실제 저장은 서버 연동 필요)
            alert('학습 진도가 저장되었습니다.\n수업 화면으로 이동합니다.');
            // 창 닫기 또는 메인/목록 이동
            if (window.opener) {
                window.close();
            } else {
                window.location.href = 'courses-detail.html'; // 필요시 경로 수정
            }
        });
    }
});