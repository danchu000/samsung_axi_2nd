

// 과목 추가/수정 모달 열기/닫기 및 저장
let subjectModalMode = 'add'; // 'add' or 'edit'
let editingSubjectCode = null;

function openSubjectModal(mode = 'add', subjectData = null) {
subjectModalMode = mode;
document.getElementById('subjectModalOverlay').style.display = 'flex';
const title = document.getElementById('subjectModalTitle');
const nameInput = document.getElementById('modalSubjectName');
const codeDiv = document.getElementById('modalSubjectCode');
const startDate = document.getElementById('modalSubjectStartDate');
const endDate = document.getElementById('modalSubjectEndDate');
const statusRadios = document.getElementsByName('modalSubjectStatus');

if (mode === 'add') {
    title.textContent = '과목 추가';
    nameInput.value = '';
    codeDiv.textContent = '';
    startDate.value = '';
    endDate.value = '';
    statusRadios[0].checked = true;
    editingSubjectCode = null;
} else if (mode === 'edit' && subjectData) {
    title.textContent = '과목 수정';
    nameInput.value = subjectData.name || '';
    codeDiv.textContent = subjectData.code || '';
    startDate.value = subjectData.startDate || '';
    endDate.value = subjectData.endDate || '';
    editingSubjectCode = subjectData.code;
    // 상태 라디오 버튼 설정
    for (const radio of statusRadios) {
        radio.checked = (radio.value === (subjectData.status || 'active'));
    }
}
}

function closeSubjectModal() {
document.getElementById('subjectModalOverlay').style.display = 'none';
}

function saveSubject() {
// 저장 로직 구현 필요
if (subjectModalMode === 'add') {
    alert('과목이 추가되었습니다.');
} else if (subjectModalMode === 'edit') {
    alert('과목이 수정되었습니다.');
}
closeSubjectModal();
}
// 텍스트 토글 동작
function setEditMode(mode) {
const txtContent = document.getElementById('toggleContentEdit');
const txtLesson = document.getElementById('toggleLessonEdit');
if (mode === 'content') {
    txtContent.classList.add('active');
    txtLesson.classList.remove('active');
} else {
    txtContent.classList.remove('active');
    txtLesson.classList.add('active');
}
// 실제 편집 모드 전환 로직은 여기에 추가
}
// 페이지 로드 시 메타 정보 설정 및 모달 이벤트 바인딩
document.addEventListener('DOMContentLoaded', function() {
    // #addContentModalOverlay 내 type-card 클릭 시 data-type별 동작 분기
    var addContentModal = document.getElementById('addContentModalOverlay');
    if (addContentModal) {
        addContentModal.addEventListener('click', function(e) {
            var card = e.target.closest('.type-card');
            if (!card) return;
            var type = card.dataset.type;
            if (!type) return;
            // 예시 분기: type별로 동작 다르게
            if (type === 'practice') {
                // practice-add-modal.html을 새 창(탭)으로 직접 오픈하기 전에 모달 닫기
                closeAddContentModal();
                window.open('practice-add-modal.html', '_blank', 'width=900,height=800,resizable=yes,scrollbars=yes');
            } else if (type === 'grading') {
                // grading-add-modal.html을 새 창(탭)으로 직접 오픈하기 전에 모달 닫기
                closeAddContentModal();
                window.open('grading-add-modal.html', '_blank', 'width=900,height=800,resizable=yes,scrollbars=yes');
            }
        });
    }
    // body가 instructor일 때 button-group 숨김
    if (document.body.dataset.userRole === 'instructor') {
        document.querySelectorAll('.button-group').forEach(function(el) {
            el.style.display = 'none';
        });
        document.querySelectorAll('.form-section[data-user-role="admin"]').forEach(function(el) {
            el.style.display = 'none';
        });
        document.querySelectorAll('.btn-icon[data-user-role="admin"]').forEach(function(el) {
            el.style.display = 'none';
        });
        document.querySelectorAll('option[data-user-role="admin"]').forEach(function(el) {
            el.hidden = true;
        });
        document.querySelectorAll('button[data-user-role="admin"]').forEach(function(el) {
            el.style.display = 'none';
        });
        document.querySelectorAll('label[data-user-role="admin"]').forEach(function(el) {
            el.style.display = 'none';
        });
    }
document.getElementById('createdDate').textContent = '2026-01-01';
document.getElementById('modifiedDate').textContent = '2026-01-08';
document.getElementById('createdBy').textContent = '이지은 관리자';
document.getElementById('modifiedBy').textContent = '이지은 관리자';

// 드래그 앤 드롭 초기화
initDragAndDrop();

// 과목 추가 버튼 이벤트
const addBtn = document.getElementById('subjectAddBtn');
if (addBtn) {
    addBtn.addEventListener('click', function() {
        openSubjectModal('add');
    });
}

// 과목 수정 버튼 이벤트 (SUB-001 예시)
const editBtn001 = document.getElementById('subjectEditBtn-SUB-001');
if (editBtn001) {
    editBtn001.addEventListener('click', function(e) {
        e.stopPropagation();
        // 실제 데이터에 맞게 값 추출 필요
        openSubjectModal('edit', {
            code: 'SUB-001',
            name: 'JavaScript 기초',
            startDate: '',
            endDate: '',
            status: 'active'
        });
    });
}

// 모달 닫기 버튼
const closeBtn = document.getElementById('subjectModalCloseBtn');
if (closeBtn) {
    closeBtn.addEventListener('click', closeSubjectModal);
}

// 모달 저장 버튼
const saveBtn = document.getElementById('modalSubjectSaveBtn');
if (saveBtn) {
    saveBtn.addEventListener('click', saveSubject);
}

// 콘텐츠 추가 버튼 이벤트 (SUB-001 예시)
const addContentBtn001 = document.getElementById('addContentBtn-SUB-001');
if (addContentBtn001) {
    addContentBtn001.addEventListener('click', function(e) {
        e.stopPropagation();
        openAddContentModal('SUB-001');
    });
}

// 콘텐츠 추가 모달 닫기 버튼
const addContentModalCloseBtn = document.getElementById('closeContentModalOverlay');
if (addContentModalCloseBtn) {
    addContentModalCloseBtn.addEventListener('click', closeAddContentModal);
}

// 선택된 콘텐츠 임시 저장용
let selectedContents = [];

// 전역에서 접근 가능하도록 window에 등록
window.removeSelectedContent = function(id) {
    selectedContents = selectedContents.filter(item => item.id !== id);
    renderSelectedContentList();
    // 검색 결과 하이라이트 해제
    const el = document.querySelector(`.add-content-result-item[data-id="${id}"]`);
    if (el) el.style.border = '2px solid transparent';
};

// 콘텐츠 추가 모달 추가하기 버튼
const addContentSubmitBtn = document.getElementById('addContentSubmitBtn');
if (addContentSubmitBtn) {
    addContentSubmitBtn.addEventListener('click', function() {
        if (selectedContents.length === 0) {
            alert('추가할 콘텐츠를 선택하세요.');
            return;
        }
        alert('선택한 콘텐츠가 추가됩니다. (타임리프 연동 예정)\n' + selectedContents.map(c => c.title).join(', '));
        closeAddContentModal();
        selectedContents = [];
        renderSelectedContentList();
    });
}
});

// 콘텐츠 추가 모달 열기/닫기
function openAddContentModal(subjectCode) {
document.getElementById('addContentModalOverlay').style.display = 'flex';
}
function closeAddContentModal() {
document.getElementById('addContentModalOverlay').style.display = 'none';
}

// 드래그 앤 드롭 기능
let draggedElement = null;

function initDragAndDrop() {
const subjectItems = document.querySelectorAll('.subject-item');

subjectItems.forEach(item => {
    item.addEventListener('dragstart', handleDragStart);
    item.addEventListener('dragover', handleDragOver);
    item.addEventListener('drop', handleDrop);
    item.addEventListener('dragend', handleDragEnd);
    item.addEventListener('dragenter', handleDragEnter);
    item.addEventListener('dragleave', handleDragLeave);
});
}

function handleDragStart(e) {
draggedElement = this;
this.classList.add('dragging');
e.dataTransfer.effectAllowed = 'move';
e.dataTransfer.setData('text/html', this.innerHTML);
}

function handleDragOver(e) {
if (e.preventDefault) {
    e.preventDefault();
}
e.dataTransfer.dropEffect = 'move';
return false;
}

function handleDragEnter(e) {
if (this !== draggedElement) {
    this.classList.add('drag-over');
}
}

function handleDragLeave(e) {
this.classList.remove('drag-over');
}

function handleDrop(e) {
if (e.stopPropagation) {
    e.stopPropagation();
}

if (draggedElement !== this) {
    const subjectList = document.getElementById('subjectList');
    const allItems = [...subjectList.querySelectorAll('.subject-item')];
    const draggedIndex = allItems.indexOf(draggedElement);
    const targetIndex = allItems.indexOf(this);
    
    if (draggedIndex < targetIndex) {
        this.parentNode.insertBefore(draggedElement, this.nextSibling);
    } else {
        this.parentNode.insertBefore(draggedElement, this);
    }
}

this.classList.remove('drag-over');
return false;
}

function handleDragEnd(e) {
this.classList.remove('dragging');

const subjectItems = document.querySelectorAll('.subject-item');
subjectItems.forEach(item => {
    item.classList.remove('drag-over');
});
}

// 삭제하기 버튼
function deleteCourse() {
if (confirm('이 과정을 정말 삭제하시겠습니까?')) {
    alert('과정이 삭제되었습니다.');
    window.location.href = 'admin-courses-edu.html';
}
}


// 과목 삭제
function deleteSubject(subjectCode) {
if (confirm(`과목 ${subjectCode}을(를) 삭제하시겠습니까?`)) {
    alert(`과목 ${subjectCode}이(가) 삭제되었습니다.`);
    // 실제로는 서버에 삭제 요청을 보내고 페이지를 새로고침하거나 해당 카드를 DOM에서 제거
    // location.reload();
}
}

// 콘텐츠 추가
function addContent(subjectCode) {
// 과목 코드에 해당하는 content-list 찾기
const subjectItem = event.target.closest('.subject-item');
const contentList = subjectItem.querySelector('.content-list');

// content-list가 닫혀있다면 열기
if (!contentList.classList.contains('expanded')) {
    const contentId = contentList.id;
    toggleContent(contentId);
}

// 현재 컨텐츠 개수 확인
const currentItems = contentList.querySelectorAll('.content-item');
const newOrderNumber = currentItems.length + 1;
const newContentCode = `CNT-${String(newOrderNumber).padStart(3, '0')}`;

// 새 컨텐츠 아이템 생성
const newContentItem = document.createElement('div');
newContentItem.className = 'content-item';
newContentItem.innerHTML = `
    <div class="content-order">${newOrderNumber}</div>
    <div class="content-icon">
        <img src="/static/icons/content-video.png" alt="video">
    </div>
    <div class="content-title">새 콘텐츠 ${newOrderNumber}</div>
    <div class="content-meta">동영상 / 0분</div>
    <div class="content-move-buttons">
        <button class="btn-move" onclick="moveContentUp('${contentList.id}', this.closest('.content-item'))" title="위로 이동">▲</button>
        <button class="btn-move" onclick="moveContentDown('${contentList.id}', this.closest('.content-item'))" title="아래로 이동">▼</button>
    </div>
    <div class="content-actions">
        <button class="btn-small" onclick="deleteContent('${subjectCode}', '${newContentCode}')">삭제</button>
        <button class="btn-small" onclick="editContent('${subjectCode}', '${newContentCode}')">수정</button>
        <button class="btn-small btn-start" onclick="startLearning('${newContentCode}')">학습 시작</button>
    </div>
`;

// content-list에 추가
contentList.appendChild(newContentItem);

// content-list 높이 재계산
setTimeout(() => {
    const scrollHeight = contentList.scrollHeight;
    contentList.style.maxHeight = (scrollHeight + 40) + 'px';
}, 10);
}

// 컨텐츠 목록 토글
function toggleContent(contentId) {
const contentList = document.getElementById(contentId);
const subjectCard = contentList.previousElementSibling;

if (contentList.classList.contains('expanded')) {
    // 닫기
    contentList.style.maxHeight = '0';
    contentList.classList.remove('expanded');
    subjectCard.classList.remove('expanded');
} else {
    // 열기 - 실제 콘텐츠 높이 계산
    contentList.classList.add('expanded');
    subjectCard.classList.add('expanded');

    
    // 실제 콘텐츠 높이 계산 후 약간의 여유 공간 추가
    setTimeout(() => {
        const scrollHeight = contentList.scrollHeight;
        contentList.style.maxHeight = (scrollHeight + 40) + 'px'; // 여유 공간 40px 추가
    }, 10);
}
}

// 컨텐츠 삭제
function deleteContent(subjectCode, contentCode) {
if (confirm(`콘텐츠 ${contentCode}를 삭제하시겠습니까?`)) {
    alert(`콘텐츠 ${contentCode}가 삭제되었습니다.`);
}
}

// 컨텐츠 수정
function editContent(subjectCode, contentCode) {
alert(`콘텐츠 ${contentCode} 수정 페이지로 이동합니다.`);
// window.location.href = `admin-courses-content-edit.html?subjectCode=${subjectCode}&contentCode=${contentCode}`;
}

// 학습 시작
function startLearning(contentCode) {
alert(`콘텐츠 ${contentCode} 학습을 시작합니다.`);
// window.location.href = `learning-content.html?contentCode=${contentCode}`;
}

// 컨텐츠 위로 이동
function moveContentUp(contentListId, contentItem) {
event.stopPropagation();
const previousItem = contentItem.previousElementSibling;
if (previousItem) {
    contentItem.parentNode.insertBefore(contentItem, previousItem);
    updateContentOrderNumbers(contentListId);
}
}

// 컨텐츠 아래로 이동
function moveContentDown(contentListId, contentItem) {
event.stopPropagation();
const nextItem = contentItem.nextElementSibling;
if (nextItem) {
    contentItem.parentNode.insertBefore(nextItem, contentItem);
    updateContentOrderNumbers(contentListId);
}
}

// 컨텐츠 순서 번호 업데이트
function updateContentOrderNumbers(contentListId) {
const contentList = document.getElementById(contentListId);
const contentItems = contentList.querySelectorAll('.content-item');
contentItems.forEach((item, index) => {
    const orderElement = item.querySelector('.content-order');
    if (orderElement) {
        orderElement.textContent = index + 1;
    }
});
}

// 학생 상세 페이지 이동 함수
function goToStudentPage(studentId) {
// 새 창을 화면 정중앙에 띄우기
const width = 1200;
const height = 800;
const left = window.screenX + (window.outerWidth - width) / 2;
const top = window.screenY + (window.outerHeight - height) / 2;

// 예시: 과목 종료일을 가져오는 로직 (실제 데이터에 맞게 수정 필요)
let endDate = '';
try {
    // 예시: SUB-001 기준, 실제로는 studentId나 subjectCode로 동적으로 가져와야 함
    const subjectCard = document.getElementById('subjectEditBtn-SUB-001')?.closest('.subject-card');
    if (subjectCard) {
        endDate = subjectCard.querySelector('.subject-end-date')?.textContent?.trim();
    }
    // 또는 전역 데이터에서 가져오기
}
catch(e) {}

// 오늘 날짜
const today = new Date();
let courseEnded = false;
if (endDate) {
    // endDate가 YYYY-MM-DD 형식이라고 가정
    const end = new Date(endDate);
    if (!isNaN(end.getTime()) && end < today) {
        courseEnded = true;
    }
}

const url = courseEnded
    ? '/templates/instructor/modal-completion.html'
    : '/templates/instructor/modal-attendance.html';
window.open(
    url,
    '_blank',
    `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`
);
}


// 샘플 콘텐츠 데이터 (타임리프 연동 전 임시)
let contentListData = [
    {
        order: 1,
        code: 'CNT-001',
        title: '변수와 데이터 타입',
        type: '동영상',
        duration: '45분',
        meta: '동영상 / 45분'
    },
    {
        order: 2,
        code: 'CNT-002',
        title: '연산자와 표현식',
        type: '문서',
        duration: '30분',
        meta: '문서 / 30분'
    },
    {
        order: 4,
        code: 'CNT-004',
        title: '함수와 이벤트',
        type: '과제',
        duration: '40분',
        meta: '과제 / 40분'
    },
    {
        order: 5,
        code: 'CNT-005',
        title: '객체와 배열',
        type: '강의',
        duration: '50분',
        meta: '강의 / 50분'
    },
    {
        order: 6,
        code: 'CNT-006',
        title: '최종 시험',
        type: '시험',
        duration: '60분',
        meta: '시험 / 60분'
    }
];
const contentTypeIconMap = {
    '동영상': 'content-video.png',
    '문서': 'content-document.png',
    '과제': 'content-practice.png',
    '강의': 'content-class.png',
    '시험': 'content-grading.png'
};

// 콘텐츠 리스트 렌더링 함수
function renderContentList(subjectCode, contentListId) {
    const list = document.getElementById(contentListId);
    if (!list) return;
    list.innerHTML = '';
    // 타입별 페이지 매핑
    const typeToPage = {
        '동영상': 'play-video.html',
        '문서': 'play-document.html',
        '과제': 'play-practice.html',
        '강의': 'play-class.html',
        '시험': 'play-grading.html'
    };
    contentListData.forEach((item, idx) => {
        const div = document.createElement('div');
        div.className = 'content-item';
        const page = typeToPage[item.type] || '#';
        div.innerHTML = `
            <div class="content-order">${idx + 1}</div>
            <div class="content-icon">
                <img src="/static/icons/${contentTypeIconMap[item.type] || 'content-document.png'}" alt="${item.type}">
            </div>
            <div class="content-title">${item.title}</div>
            <div class="content-meta">${item.meta}</div>
            <div class="content-move-buttons">
                <button class="btn-move" onclick="moveContentUp('${contentListId}', this.closest('.content-item'))" title="위로 이동">▲</button>
                <button class="btn-move" onclick="moveContentDown('${contentListId}', this.closest('.content-item'))" title="아래로 이동">▼</button>
            </div>
            <div class="content-actions">
                <button class="btn-small" onclick="deleteContent('${subjectCode}', '${item.code}')">삭제</button>
                <button class="btn-small" onclick="editContent('${subjectCode}', '${item.code}')">수정</button>
                <button class="btn-small btn-start" onclick="startLearning('${item.code}'); location.href='${page}'">학습 시작</button>
            </div>
        `;
        list.appendChild(div);
    });
}

// 페이지 로드 시 콘텐츠 리스트 렌더링
document.addEventListener('DOMContentLoaded', function() {
    renderContentList('SUB-001', 'content-001');
    // ...기존 코드...
});

// 모달 js 추가
  // 1) DOM
  const addTypeModal = document.getElementById('addContentModalOverlay');
  const closeTypeBtn = document.getElementById('closeContentModalOverlay');

  const bankModal = document.getElementById('bankSearchModalOverlay');
  const closeBankBtn = document.getElementById('closeBankSearchModalOverlay');
  const bankBackBtn = document.getElementById('bankBackBtn');
  const bankCancelBtn = document.getElementById('bankCancelBtn');
  const bankConfirmBtn = document.getElementById('bankConfirmBtn');

  const bankModalTitle = document.getElementById('bankModalTitle');
  const bankModalDesc = document.getElementById('bankModalDesc');
  const bankTypeBadge = document.getElementById('bankTypeBadge');

  const bankKeyword = document.getElementById('bankKeyword');
  const bankCategory = document.getElementById('bankCategory');
  const bankSort = document.getElementById('bankSort');
  const bankSearchBtn = document.getElementById('bankSearchBtn');

  const bankResultList = document.getElementById('bankResultList');
  const bankSelectedPreview = document.getElementById('bankSelectedPreview');

  // 2) 상태
  let selectedType = null;        // video/document/class/test/practice
  let selectedBankItem = null;    // {id, title, meta...}

  // 3) 타입별 라벨/설명 맵
  const TYPE_META = {
    video:    { title: '영상 검색',     badge: '영상',   desc: '문제은행(콘텐츠 라이브러리)에서 영상을 검색해 선택하세요.' },
    document: { title: '문서 검색',     badge: '문서',   desc: '문제은행(콘텐츠 라이브러리)에서 문서를 검색해 선택하세요.' },
    class:    { title: '강의 검색',     badge: '강의',   desc: '문제은행(콘텐츠 라이브러리)에서 강의를 검색해 선택하세요.' },
    test:     { title: '시험 선택/생성', badge: '시험',   desc: '기존 시험을 선택하거나 새 시험을 생성해 연결할 수 있어요.' },
    practice: { title: '과제 선택/생성', badge: '과제',   desc: '기존 과제를 선택하거나 새 과제를 생성해 연결할 수 있어요.' },
  };

  // 4) 모달 열고닫기 유틸
  function openModal(el)  { el.style.display = 'flex'; }
  function closeModal(el) { el.style.display = 'none'; }

  // 5) 타입 선택 카드 클릭
  document.querySelectorAll('#addContentModalOverlay .type-card').forEach(card => {
        card.addEventListener('click', () => {
            selectedType = card.dataset.type;
            if (selectedType === 'grading' || selectedType === 'practice') {
                return;
            }
            setupBankModalByType(selectedType);
            closeModal(addTypeModal);
            openModal(bankModal);
            // 초기화
            selectedBankItem = null;
            bankConfirmBtn.disabled = true;
            bankSelectedPreview.innerHTML = '<div style="font-size:13px; color:#888;">아직 선택된 항목이 없습니다.</div>';
            bankResultList.innerHTML = '<div style="padding:14px; color:#888; font-size:13px;">검색어를 입력하고 검색하세요.</div>';
            bankKeyword.value = '';
        });
  });

  function setupBankModalByType(type) {
    const meta = TYPE_META[type] || { title: '콘텐츠 검색', badge: '콘텐츠', desc: '콘텐츠를 검색해 선택하세요.' };
    bankModalTitle.textContent = meta.title;
    bankModalDesc.textContent = meta.desc;
    bankTypeBadge.textContent = meta.badge;
  }

  // 6) 닫기 버튼들
  closeTypeBtn?.addEventListener('click', () => closeModal(addTypeModal));
  closeBankBtn?.addEventListener('click', () => closeModal(bankModal));
  bankCancelBtn?.addEventListener('click', () => closeModal(bankModal));

  // 7) "유형 다시 선택"
  bankBackBtn?.addEventListener('click', () => {
    closeModal(bankModal);
    openModal(addTypeModal);
  });

  // 8) 검색 버튼 클릭 (여기서 실제 API 호출로 바꾸면 됨)
  bankSearchBtn?.addEventListener('click', async () => {
    const keyword = bankKeyword.value.trim();
    const category = bankCategory.value;
    const sort = bankSort.value;

    // 데모 데이터 (나중에 fetch로 교체)
    // const res = await fetch(`/api/bank/search?type=${selectedType}&keyword=${encodeURIComponent(keyword)}&category=${category}&sort=${sort}`);
    // const list = await res.json();

    const list = mockSearch(selectedType, keyword, category, sort);
    renderResultList(list);
  });

  function renderResultList(list) {
    if (!list || list.length === 0) {
      bankResultList.innerHTML = '<div style="padding:14px; color:#888; font-size:13px;">검색 결과가 없습니다.</div>';
      return;
    }

    bankResultList.innerHTML = '';
    list.forEach(item => {
      const el = document.createElement('div');
      el.className = 'bank-item';
      el.style.cssText = 'padding:14px; border-bottom:1px solid #eee; cursor:pointer;';
      el.dataset.id = item.id;

      el.innerHTML = `
        <div style="display:flex; justify-content:space-between; gap:10px;">
          <div>
            <div style="font-weight:600;">${escapeHtml(item.title)}</div>
            <div style="margin-top:6px; font-size:12px; color:#666;">
              카테고리: ${escapeHtml(item.categoryText)} · 사용중 과정: ${item.usedCount} · 평균 성취도: ${item.avgScore}%
            </div>
          </div>
          <span style="font-size:12px; color:#666; white-space:nowrap;">${escapeHtml(item.createdAt)}</span>
        </div>
      `;

      el.addEventListener('click', () => {
        selectedBankItem = item;
        bankConfirmBtn.disabled = false;
        bankSelectedPreview.innerHTML = `
          <div style="font-weight:700;">${escapeHtml(item.title)}</div>
          <div style="margin-top:8px; font-size:12px; color:#666;">
            ID: ${escapeHtml(String(item.id))}<br/>
            카테고리: ${escapeHtml(item.categoryText)}<br/>
            사용중 과정: ${item.usedCount} · 평균 성취도: ${item.avgScore}%<br/>
            생성일: ${escapeHtml(item.createdAt)}
          </div>
        `;
      });

      bankResultList.appendChild(el);
    });
  }

  // 9) 선택 완료 → 기존 폼에 반영 (hidden input 추천)
  bankConfirmBtn?.addEventListener('click', () => {
    if (!selectedBankItem) return;

    // 예: hidden input에 저장해서 submit 시 서버로 넘기기
    // <input type="hidden" name="contentType" id="selectedContentType">
    // <input type="hidden" name="bankItemId" id="selectedBankItemId">
    // <input type="text" readonly id="selectedBankItemTitle">

    const typeInput = document.getElementById('selectedContentType');
    const idInput   = document.getElementById('selectedBankItemId');
    const titleView = document.getElementById('selectedBankItemTitle');

    if (typeInput) typeInput.value = selectedType;
    if (idInput) idInput.value = selectedBankItem.id;
    if (titleView) titleView.value = selectedBankItem.title;

    closeModal(bankModal);
  });

  // 유틸: mock search
  function mockSearch(type, keyword, category, sort) {
    const base = [
      { id: 101, title: 'JavaScript 기초 - 변수와 자료형', categoryText:'고교위탁', usedCount:4, avgScore:60, createdAt:'2026-01-10' },
      { id: 102, title: 'React 기초 - 컴포넌트 이해', categoryText:'K-Digital', usedCount:2, avgScore:72, createdAt:'2026-01-05' },
      { id: 103, title: 'Node.js - REST API 실습', categoryText:'기타', usedCount:1, avgScore:55, createdAt:'2025-12-20' },
    ];
    let list = base.filter(x => !keyword || x.title.toLowerCase().includes(keyword.toLowerCase()));
    if (category) list = list.filter(x => (category==='highschool' && x.categoryText==='고교위탁') || (category==='kdt' && x.categoryText==='K-Digital') || (category==='etc' && x.categoryText==='기타'));
    // sort는 데모라 생략
    return list.map(x => ({...x, type}));
  }

  function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (m) => ({
      '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'
    }[m]));
  }
