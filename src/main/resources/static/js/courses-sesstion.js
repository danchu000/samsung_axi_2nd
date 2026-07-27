// 과목별 콘텐츠 목록 (실제 환경에서는 DB에서 받아옴)
let subjectContentMap = {
'subject-1': [
{ value: 'content-1', label: '콘텐츠 1' },
{ value: 'content-2', label: '콘텐츠 2' }
],
'subject-2': [
{ value: 'content-3', label: '콘텐츠 3' },
{ value: 'content-4', label: '콘텐츠 4' }
]
};
let allSubjects = [
{ value: 'subject-1', label: '과목 1' },
{ value: 'subject-2', label: '과목 2' }
];

// Session rows and content rows (matrix)
// Initialize sessionCounter based on existing session header rows (th in tbody)
let sessionCounter = document.querySelectorAll('#contentRows tr th').length || 0;

// Adds a new session as a row under the table (tr with a th spanning the table)
function addSessionColumn() {
    sessionCounter++;
    const tbody = document.querySelector('#contentRows');
    const theadRow = document.querySelector('#sessionMatrix thead tr');
    const colCount = theadRow.children.length;

    const tr = document.createElement('tr');
    tr.setAttribute('data-session-id', sessionCounter);

    const th = document.createElement('th');
    th.style.textAlign = 'center';
    th.colSpan = colCount;
    th.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:space-between;">
            <span style="font-weight:700;">${sessionCounter} 차시</span>
            <button class="btn-small btn-gray" onclick="removeSessionColumn(${sessionCounter})">삭제</button>
        </div>
    `;

    tr.appendChild(th);
    tbody.appendChild(tr);
}

// Removes a session row by its id
function removeSessionColumn(sessionId) {
    const tr = document.querySelector(`#contentRows tr[data-session-id="${sessionId}"]`);
    if (tr) tr.remove();
}

// 콘텐츠 중복 방지: 모든 select 옵션을 동적으로 갱신
function addContentRow() {
    const tbody = document.getElementById('contentRows');
    const tr = document.createElement('tr');

    // subject column (select)
    const tdSubject = document.createElement('td');
    const subjectSelect = document.createElement('select');
    subjectSelect.innerHTML = '<option value="" disabled selected>과목 선택</option>' +
        allSubjects.map(s => `<option value="${s.value}">${s.label}</option>`).join('');
    subjectSelect.onchange = function() { handleSubjectChange(tr); };
    tdSubject.appendChild(subjectSelect);
    tr.appendChild(tdSubject);

    // content name column (select)
    const tdContent = document.createElement('td');
    const contentSelect = document.createElement('select');
    contentSelect.disabled = true;
    tdContent.appendChild(contentSelect);
    tr.appendChild(tdContent);

    // attendance column
    const tdAttendance = document.createElement('td');
    tdAttendance.innerHTML = `<input type="text" placeholder="출결기준">`;
    tr.appendChild(tdAttendance);

    // dynamic session columns: count of session ths = total ths - 4 (base 3 + action)
    const headerRow = document.querySelector('#sessionMatrix thead tr');
    const dynamicCount = Math.max(0, headerRow.children.length - 4);
    for (let i = 0; i < dynamicCount; i++) {
        const td = document.createElement('td');
        td.style.textAlign = 'center';
        td.innerHTML = 
        `<div style="display:flex; flex-direction:column; gap:4px; align-items:center;">
            <button class="btn-small" onclick="moveRowUp(this)" title="위로 이동">▲</button>    
            <button class="btn-small" onclick="moveRowDown(this)" title="아래로 이동">▼</button>    
        </div>`;
        tr.appendChild(td);
    }

    // action column
    const actionTd = document.createElement('td');
    actionTd.style.textAlign = 'center';
    actionTd.innerHTML = '<button class="btn-small btn-gray" onclick="removeContentRow(this)">삭제</button>';
    tr.appendChild(actionTd);

    tbody.appendChild(tr);
    updateAllContentSelectOptions();
}

// 모든 콘텐츠 목록 (실제 환경에서는 DB에서 받아옴)
let allContents = [
    { value: 'content-1', label: '콘텐츠 1' },
    { value: 'content-2', label: '콘텐츠 2' },
    { value: 'content-3', label: '콘텐츠 3' },
    { value: 'content-4', label: '콘텐츠 4' }
];

// 모든 콘텐츠 select 박스의 옵션을 동적으로 갱신 + 남은 콘텐츠 개수 UI 갱신 + 추가 버튼 제한
function updateAllContentSelectOptions() {
    const tbody = document.getElementById('contentRows');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    // 모든 row의 콘텐츠 select 중복 방지 및 과목별 옵션 표시
    let selectedContents = [];
    rows.forEach(tr => {
        const contentSelect = tr.querySelector('td:nth-child(2) select');
        if (contentSelect && contentSelect.value) {
            selectedContents.push(contentSelect.value);
        }
    });

    rows.forEach(tr => {
        const subjectSelect = tr.querySelector('td:nth-child(1) select');
        const contentSelect = tr.querySelector('td:nth-child(2) select');
        if (!subjectSelect || !contentSelect) return;
        const subjectValue = subjectSelect.value;
        if (!subjectValue) {
            contentSelect.innerHTML = '<option value="" disabled selected>과목을 먼저 선택하세요</option>';
            contentSelect.disabled = true;
        } else {
            const currentValue = contentSelect.value;
            const allContents = subjectContentMap[subjectValue] || [];
            // 중복 방지: 이미 선택된 콘텐츠는 옵션에서 제외(단, 현재 선택값은 유지)
            const availableContents = allContents.filter(c => !selectedContents.includes(c.value) || c.value === currentValue);
            let optionsHtml = '<option value="" disabled selected>콘텐츠 선택</option>';
            availableContents.forEach(c => {
                optionsHtml += `<option value="${c.value}"${c.value === currentValue ? ' selected' : ''}>${c.label}</option>`;
            });
            contentSelect.innerHTML = optionsHtml;
            contentSelect.disabled = false;
        }
    });

    // 남은 콘텐츠 개수 UI 갱신 (전체 콘텐츠 - 사용된 콘텐츠)
    let totalContentCount = Object.values(subjectContentMap).reduce((acc, arr) => acc + arr.length, 0);
    let usedContentCount = selectedContents.length;
    const infoSpan = document.getElementById('contentCountInfo');
    if (infoSpan) {
        infoSpan.textContent = `남은 콘텐츠 ${totalContentCount - usedContentCount} / ${totalContentCount}`;
    }
    // 콘텐츠 추가 버튼 제한 (전체 row 수 >= 전체 콘텐츠 수)
    const btnAdd = document.getElementById('btnAddContent');
    if (btnAdd) {
        btnAdd.disabled = (rows.length - 1 >= totalContentCount); // -1: 헤더 row 제외
        btnAdd.style.opacity = btnAdd.disabled ? '0.5' : '1';
        btnAdd.style.cursor = btnAdd.disabled ? 'not-allowed' : 'pointer';
    }
}
// 과목 select 변경 시 해당 row의 콘텐츠 select 옵션 갱신
function handleSubjectChange(tr) {
updateAllContentSelectOptions();
}

// 콘텐츠 select 박스 change 이벤트에서 옵션 갱신
document.addEventListener('change', function(e) {
    if (e.target && e.target.closest('td') && e.target.tagName === 'SELECT') {
        updateAllContentSelectOptions();
    }
});

// 페이지 로드시 남은 콘텐츠 UI 갱신
document.addEventListener('DOMContentLoaded', function() {
    updateAllContentSelectOptions();
});

function removeContentRow(btn) {
    const tr = btn.closest('tr');
    tr.remove();
    updateAllContentSelectOptions(); // 삭제 후 남은 콘텐츠/옵션 즉시 복원
}

// Move table row up/down within #contentRows
function moveRowUp(btn) {
    const tr = btn.closest('tr');
    if (!tr) return;
    const prev = tr.previousElementSibling;
    if (prev) {
        tr.parentNode.insertBefore(tr, prev);
    }
}

function moveRowDown(btn) {
    const tr = btn.closest('tr');
    if (!tr) return;
    const next = tr.nextElementSibling;
    if (next) {
        tr.parentNode.insertBefore(next, tr);
    }
}
// 과목 추가 모달 열기/닫기
function openSubjectModal() {
    document.getElementById('subjectModalOverlay').style.display = 'flex';
}
function closeSubjectModal() {
    document.getElementById('subjectModalOverlay').style.display = 'none';
}
function saveSubject() {
    // 저장 로직 구현 필요
    alert('과목이 저장되었습니다.');
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
// 페이지 로드 시 메타 정보 설정
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('createdDate').textContent = '2026-01-01';
    document.getElementById('modifiedDate').textContent = '2026-01-08';
    document.getElementById('createdBy').textContent = '이지은 관리자';
    document.getElementById('modifiedBy').textContent = '이지은 관리자';
    
    // 드래그 앤 드롭 초기화
    initDragAndDrop();
});

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

// 수정하기 버튼
function editCourse() {
    // 수정 페이지로 이동 (추후 구현)
    alert('수정 페이지로 이동합니다.');
    window.location.href = 'admin-courses-edu-update.html';
}

// 삭제하기 버튼
function deleteCourse() {
    if (confirm('이 과정을 정말 삭제하시겠습니까?')) {
        alert('과정이 삭제되었습니다.');
        window.location.href = 'admin-courses-edu.html';
    }
}

// 과목 추가
function addSubject() {
    // 과목 추가 페이지로 이동 또는 모달 열기
    window.location.href = 'admin-courses-edu-subject.html';
}

// 과목 수정
function editSubject(subjectCode) {
    alert(`과목 ${subjectCode} 수정 페이지로 이동합니다.`);
    // window.location.href = `admin-courses-subject-update.html?subjectCode=${subjectCode}`;
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
    // 실제 구현 시 studentId로 상세 페이지 이동
    window.location.href = `admin-user-trainee.html?student=${studentId}`;
}

// 저장 버튼 핸들러 (임시 구현)
function saveSessionMatrix() {
    // TODO: 실제 저장 로직을 서버 API 호출로 구현
    alert('차시 및 콘텐츠가 저장되었습니다.');
    console.log('saveSessionMatrix called - implement server save here');
}

// 자동 차시 생성 구현

function autoGenerateSessions() {
    const tbody = document.getElementById('contentRows');
    tbody.innerHTML = '';
    let sessionNum = 1;
    for (const subject of allSubjects) {
        // 차시 header row
        const thTr = document.createElement('tr');
        thTr.setAttribute('data-session-id', sessionNum);
        const th = document.createElement('th');
        th.colSpan = 5;
        th.style.textAlign = 'center';
        th.innerHTML = `<div style="display:flex; align-items:center; justify-content:space-between;"><span style="font-weight:700;">${sessionNum} 차시</span></div>`;
        thTr.appendChild(th);
        tbody.appendChild(thTr);
        sessionNum++;
        // 해당 과목의 콘텐츠 tr 생성
        const contents = subjectContentMap[subject.value] || [];
        for (const content of contents) {
            const tr = document.createElement('tr');
            // 과목 select
            const tdSubject = document.createElement('td');
            const subjectSelect = document.createElement('select');
            subjectSelect.innerHTML = '<option value="" disabled>과목 선택</option>' +
                allSubjects.map(s => `<option value="${s.value}"${s.value===subject.value?' selected':''}>${s.label}</option>`).join('');
            tdSubject.appendChild(subjectSelect);
            tr.appendChild(tdSubject);
            // 콘텐츠 select
            const tdContent = document.createElement('td');
            const contentSelect = document.createElement('select');
            contentSelect.innerHTML = `<option value="" disabled>콘텐츠 선택</option>` +
                contents.map(c => `<option value="${c.value}"${c.value===content.value?' selected':''}>${c.label}</option>`).join('');
            tdContent.appendChild(contentSelect);
            tr.appendChild(tdContent);
            // 출결기준
            const tdAttendance = document.createElement('td');
            tdAttendance.innerHTML = `<input type="text" placeholder="출결기준">`;
            tr.appendChild(tdAttendance);
            // 이동/삭제 버튼
            const tdMove = document.createElement('td');
            tdMove.style.textAlign = 'center';
            tdMove.innerHTML = `<div style="display:flex; flex-direction:column; gap:4px; align-items:center;"><button class="btn-small" onclick="moveRowUp(this)" title="위로 이동">▲</button><button class="btn-small" onclick="moveRowDown(this)" title="아래로 이동">▼</button></div>`;
            tr.appendChild(tdMove);
            const tdAction = document.createElement('td');
            tdAction.style.textAlign = 'center';
            tdAction.innerHTML = '<button class="btn-small btn-gray" onclick="removeContentRow(this)">삭제</button>';
            tr.appendChild(tdAction);
            tbody.appendChild(tr);
        }
    }
    updateAllContentSelectOptions();
}
