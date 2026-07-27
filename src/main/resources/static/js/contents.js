// 탭 클릭 이벤트 및 등록 버튼 문구 변경
document.addEventListener('DOMContentLoaded', function() {

    // data-hide-on-contents 속성이 있는 버튼 숨기기 (URL과 무관하게 적용)
    var hideBtns = document.querySelectorAll('[data-hide-on-contents]');
    hideBtns.forEach(function(btn) {
        btn.style.display = 'none';
    });

    // 서버(Thymeleaf)가 window._serverContentRows 를 내려주면 그것을 쓰고,
    // 아직 컨트롤러가 없는 페이지는 아래 더미 배열로 동작한다 (하위호환).
    //   - 문제은행: /admin/evaluation/questions 가 문제(type='문제') 행을 채운다
    //   - 영상/문서/강의/과제: A의 콘텐츠 컨트롤러가 붙으면 같은 배열에 합쳐진다
    const questionList = window._serverContentRows || [
        { id: 'Q-001', type: '영상', title: '예시 영상 1', difficulty: 'easy', category: 'KDT', usedCourseCount: 3, avgAchievement: '85%', createdAt: '2024-01-01', status: 'Active', subject: '프로그래밍', topic: '웹', subtopic: 'HTML', authorId: 'user1' },
        { id: 'Q-002', type: '문서', title: '예시 문서 1', difficulty: 'medium', category: '고교위탁', usedCourseCount: 1, avgAchievement: '72%', createdAt: '2024-01-10', status: 'Archived', subject: '수학', topic: '통계', subtopic: '평균', authorId: 'user2' },
        { id: 'Q-003', type: '강의', title: '예시 강의 1', difficulty: 'hard', category: 'KDT', usedCourseCount: 2, avgAchievement: '90%', createdAt: '2024-01-15', status: 'Active', subject: '프로그래밍', topic: '백엔드', subtopic: 'Node.js', authorId: 'user1' },
        { id: 'Q-004', type: '문제', title: '예시 퀴즈 1', difficulty: 'easy', category: '고교위탁', usedCourseCount: 4, avgAchievement: '60%', createdAt: '2024-01-20', status: 'Active', subject: '영어', topic: '문법', subtopic: '시제', authorId: 'user3' },
        { id: 'Q-005', type: '과제', title: '예시 과제 1', difficulty: 'medium', category: 'KDT', usedCourseCount: 1, avgAchievement: '75%', createdAt: '2024-01-22', status: 'Archived', subject: '프로그래밍', topic: '프론트엔드', subtopic: 'React', authorId: 'user2' },
        { id: 'Q-006', type: '영상', title: '예시 영상 2', difficulty: 'medium', category: 'KDT', usedCourseCount: 2, avgAchievement: '88%', createdAt: '2024-01-23', status: 'Active', subject: '수학', topic: '기하', subtopic: '삼각형', authorId: 'user1' },
        { id: 'Q-007', type: '문서', title: '예시 문서 2', difficulty: 'hard', category: '고교위탁', usedCourseCount: 3, avgAchievement: '70%', createdAt: '2024-01-24', status: 'Archived', subject: '프로그래밍', topic: '알고리즘', subtopic: '정렬', authorId: 'user3' },
        { id: 'Q-008', type: '강의', title: '예시 강의 2', difficulty: 'easy', category: 'KDT', usedCourseCount: 2, avgAchievement: '95%', createdAt: '2024-01-25', status: 'Active', subject: '영어', topic: '회화', subtopic: '여행', authorId: 'user2' },
        { id: 'Q-009', type: '문제', title: '예시 퀴즈 2', difficulty: 'medium', category: '고교위탁', usedCourseCount: 1, avgAchievement: '65%', createdAt: '2024-01-26', status: 'Archived', subject: '수학', topic: '확률', subtopic: '주사위', authorId: 'user1' },
        { id: 'Q-010', type: '과제', title: '예시 과제 2', difficulty: 'hard', category: 'KDT', usedCourseCount: 2, avgAchievement: '80%', createdAt: '2024-01-27', status: 'Active', subject: '프로그래밍', topic: '데이터', subtopic: 'SQL', authorId: 'user3' },
        { id: 'Q-011', type: '시험', title: '예시 시험 1', difficulty: 'hard', category: 'KDT', usedCourseCount: 1, avgAchievement: '78%', createdAt: '2024-01-28', status: 'Active', subject: '수학', topic: '미적분', subtopic: '적분', authorId: 'user2' }
    ];
    // window._questionList를 항상 초기화
    window._questionList = questionList;
    // 탭 버튼 눌렀을때 필터링
    // 전역 변수에 저장 (탭/필터링용)
    const tabBtns = document.querySelectorAll('.tab-btn');
    const addBtn = document.getElementById('addBtn');
    let typeSelectModal = null;
    // addBtn 클릭 시 동작: 전체면 모달(동적 생성), 탭별이면 해당 등록 페이지 이동
    if (addBtn) {
        addBtn.addEventListener('click', function() {
            const activeTab = document.querySelector('.tab-btn.active');
            const type = activeTab ? activeTab.dataset.type : '전체';
            if (type === '전체') {
                // 유형 선택 모달 동적 생성
                if (typeSelectModal) return; // 이미 열려있으면 중복생성 방지
                typeSelectModal = document.createElement('div');
                typeSelectModal.id = 'typeSelectModal';
                typeSelectModal.className = 'question-modal show';
                typeSelectModal.innerHTML = `
                    <div class="modal-content" style="max-width:520px;">
                        <div class="modal-header">
                            <h3 style="font-size:18px;">추가할 콘텐츠 유형 선택</h3>
                            <button class="modal-close" id="closeTypeSelectModalBtn">
                                <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="CurrentColor"><path d="m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z"/></svg>
                            </button>
                        </div>
                        <div class="modal-body" style="padding:32px 24px;">
                            <div class="type-card-list">
                                <div class="type-card" data-type="영상">
                                    <img src="/static/icons/content-video.png" alt="영상 아이콘">
                                    <span>영상 등록</span>
                                </div>
                                <div class="type-card" data-type="문서">
                                    <img src="/static/icons/content-document.png" alt="문서 아이콘">
                                    <span>문서 등록</span>
                                </div>
                                <div class="type-card" data-type="강의">
                                    <img src="/static/icons/content-class.png" alt="강의 아이콘">
                                    <span>강의 등록</span>
                                </div>
                                <div class="type-card" data-type="과제">
                                    <img src="/static/icons/content-practice.png" alt="과제 아이콘">
                                    <span>과제 등록</span>
                                </div>
                                <div class="type-card" data-type="문제">
                                    <img src="/static/icons/content-test.png" alt="문제 아이콘">
                                    <span>문제 등록</span>
                                </div>
                                <div class="type-card" data-type="시험">
                                    <img src="/static/icons/content-grading.png" alt="시험 아이콘">
                                    <span>시험 등록</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                document.body.appendChild(typeSelectModal);
                // 카드 클릭 이벤트
                typeSelectModal.querySelectorAll('.type-card').forEach(card => {
                    card.addEventListener('click', function() {
                        const type = card.getAttribute('data-type');
                        let page = '';
                        switch(type) {
                            case '영상': page = 'contents-video.html'; break;
                            case '문서': page = 'contents-document.html'; break;
                            case '강의': page = 'contents-class.html'; break;
                            case '문제': page = 'contents-test.html'; break;
                            case '과제': page = 'contents-practice.html'; break;
                            case '시험': page = 'contents-grading.html'; break;
                            default: page = 'contents-video.html'; break;
                        }
                        document.body.removeChild(typeSelectModal);
                        typeSelectModal = null;
                        window.location.href = page;
                    });
                });
                // 닫기 버튼
                typeSelectModal.querySelector('#closeTypeSelectModalBtn').addEventListener('click', function(e) {
                    document.body.removeChild(typeSelectModal);
                    typeSelectModal = null;
                });
                // 모달 바깥 클릭 시 닫기
                typeSelectModal.addEventListener('click', function(e) {
                    if (e.target === typeSelectModal) {
                        document.body.removeChild(typeSelectModal);
                        typeSelectModal = null;
                    }
                });
            } else {
                // 각 유형별 등록 페이지로 이동
                let page = '';
                switch(type) {
                    case '영상': page = 'contents-video.html'; break;
                    case '문서': page = 'contents-document.html'; break;
                    case '강의': page = 'contents-class.html'; break;
                    // 문제은행은 컨트롤러 전환 완료 — 페이지가 URL 을 내려주면 그쪽으로 간다
                    case '문제': page = (window._questionUrls && window._questionUrls.add) || 'contents-test.html'; break;
                    case '과제': page = 'contents-practice.html'; break;
                    case '시험': page = 'contents-grading.html'; break;
                    default: page = 'contents-video.html'; break;
                }
                window.location.href = page;
            }
        });
    }

            // 기존 typeSelectModal 관련 코드 제거 (동적 생성/제거로 대체)
    const tabTextMap = {
        '전체': '등록하기',
        '영상': '영상 등록',
        '문서': '문서 등록',
        '강의': '강의 등록',
        '퀴즈': '퀴즈 등록',
        '과제': '과제 등록',
        '문제': '문제 등록',
        '시험': '시험 등록'
    };
    
    // 탭 클릭 시 필터링 (window._questionList 기준으로 항상 테이블 갱신)
    const excelUploadBtn = document.getElementById('excelUploadBtn');
    const excelDownloadBtn = document.getElementById('excelDownloadBtn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            // 등록 버튼 문구 변경 및 disable 처리
            if (addBtn) {
                addBtn.innerHTML = '+ ' + (tabTextMap[btn.dataset.type] || '문제 등록');
                if (btn.dataset.type === '전체') {
                    addBtn.disabled = true;
                    addBtn.classList.add('disabled');
                } else {
                    addBtn.disabled = false;
                    addBtn.classList.remove('disabled');
                }
            }
            // 엑셀 버튼 disable 처리
            if (excelUploadBtn && excelDownloadBtn) {
                if (btn.dataset.type === '전체') {
                    excelUploadBtn.disabled = true;
                    excelUploadBtn.classList.add('disabled');
                    excelDownloadBtn.disabled = true;
                    excelDownloadBtn.classList.add('disabled');
                } else {
                    excelUploadBtn.disabled = false;
                    excelUploadBtn.classList.remove('disabled');
                    excelDownloadBtn.disabled = false;
                    excelDownloadBtn.classList.remove('disabled');
                }
            }
            // 탭 필터링 동작
            const type = btn.dataset.type;
            // 필터 select의 유형도 동기화
            if (filterType) {
                filterType.value = type;
            }
            let filtered = [];
            if (questionList) {
                if (type === '전체') {
                    filtered = questionList;
                } else {
                    filtered = questionList.filter(q => q.type === type);
                }
                renderTable(filtered);
            }
        });
    });
    // 페이지 로드 시 기본값
    if (addBtn) {
        addBtn.innerHTML = '+ 등록하기';
    }
    // 엑셀 버튼도 동일하게 처리(초기 로드 시)
    if (excelUploadBtn && excelDownloadBtn) {
        const activeTab = document.querySelector('.tab-btn.active');
        if (activeTab && activeTab.dataset.type === '전체') {
            excelUploadBtn.disabled = true;
            excelUploadBtn.classList.add('disabled');
            excelDownloadBtn.disabled = true;
            excelDownloadBtn.classList.add('disabled');
        } else {
            excelUploadBtn.disabled = false;
            excelUploadBtn.classList.remove('disabled');
            excelDownloadBtn.disabled = false;
            excelDownloadBtn.classList.remove('disabled');
        }
    }

    // 필터 섹션 동작 구현
    const filterSection = document.querySelector('.filter-section');
    const filterType = filterSection.querySelector('select[name="type"]');
    const filterTitle = filterSection.querySelector('input[type="text"]');
    const filterDifficulty = filterSection.querySelector('select[name="difficulty"]');
    const filterCategory = filterSection.querySelector('select[name="category"]');
    const filterStatus = filterSection.querySelector('select[name="status"]');
    const filterBtn = document.getElementById('searchBtn');

    function applyFilters() {
        let filtered = questionList;
        // 각 필터 요소가 null이 아니면 value 사용, 아니면 '전체' 기본값
        const typeVal = filterType && filterType.value ? filterType.value : '전체';
        const titleVal = filterTitle && filterTitle.value ? filterTitle.value.trim().toLowerCase() : '';
        const diffVal = filterDifficulty && filterDifficulty.value ? filterDifficulty.value : '전체';
        const catVal = filterCategory && filterCategory.value ? filterCategory.value : '전체';
        const statusVal = filterStatus && filterStatus.value ? filterStatus.value : '전체';

        filtered = filtered.filter(q => {
            // 유형
            if (typeVal !== '전체' && q.type !== typeVal) return false;
            // 제목 (부분일치, 대소문자 무시)
            if (titleVal && (!q.title || !q.title.toLowerCase().includes(titleVal))) return false;
            // 난이도
            if (diffVal !== '전체' && q.difficulty !== diffVal) return false;
            // 카테고리
            if (catVal !== '전체' && q.category !== catVal) return false;
            // 상태
            if (statusVal !== '전체' && q.status !== statusVal) return false;
            return true;
        });

        // 탭 active와 유형 select 동기화
        tabBtns.forEach(b => {
            if (b.dataset.type === typeVal) {
                b.classList.add('active');
            } else if (typeVal === '전체' && b.dataset.type === '전체') {
                b.classList.add('active');
            } else {
                b.classList.remove('active');
            }
        });
        renderTable(filtered);
    }

    if (filterBtn) {
        filterBtn.addEventListener('click', applyFilters);
    }
    filterTitle.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') applyFilters();
    });

    // 테이블 바인딩 함수
    function renderTable(data) {
        const tbody = document.querySelector('.question-table-area tbody');
        tbody.innerHTML = '';
        // data가 undefined/null이면 빈 배열로 처리
        if (!Array.isArray(data)) data = [];
        // 유형별 아이콘 매핑
        const typeIconMap = {
            '영상': 'content-video.png',
            '문서': 'content-document.png',
            '강의': 'content-class.png',
            '문제': 'content-test.png',
            '과제': 'content-practice.png',
            '시험': 'content-grading.png'
        };
        data.forEach(q => {
            const tr = document.createElement('tr');
            tr.dataset.authorId = q.authorId || '';
            const iconFile = typeIconMap[q.type] || 'content-etc.png';
            // 상태 뱃지 분기
            let statusClass = '';
            let statusText = '';
            if (q.status === 'Active') {
                statusClass = 'status-in-progress';
                statusText = '활성화';
            } else if (q.status === 'Archived') {
                statusClass = 'status-disabled';
                statusText = '비활성화';
            } else {
                statusClass = '';
                statusText = q.status;
            }
            tr.innerHTML = `
                <td style="text-align:center;"><input type="checkbox"></td>
                <td style="color:#999;"><img src="/static/icons/${iconFile}" alt="${q.type}" style="width:36px;vertical-align:middle;margin-right:6px;">${q.type}</td>
                <td>${q.title}</td>
                <td>${q.difficulty}</td>
                <td>${q.category}</td>
                <td>${q.usedCourseCount}</td>
                <td>${q.avgAchievement}</td>
                <td>${q.createdAt}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td><button class="btn btn-secondary btn-sm" data-id="${q.id}" data-user-role="instructor">수정하기</button></td>
            `;
            tbody.appendChild(tr);
        });
    }
    // 최초 로드 시 전체 데이터로 테이블 렌더링
    renderTable(window._questionList);

    // 전체선택 체크박스 기능
    const selectAll = document.getElementById('select_all');
    const tbody = document.querySelector('.question-table-area tbody');
    selectAll.addEventListener('change', function() {
        const checkboxes = tbody.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(cb => {
            cb.checked = selectAll.checked;
        });
    });
    tbody.addEventListener('change', function(e) {
        if (e.target.type === 'checkbox') {
            const checkboxes = Array.from(tbody.querySelectorAll('input[type="checkbox"]'));
            selectAll.checked = checkboxes.every(cb => cb.checked);
        }
    });
    
    // 수정 버튼 클릭 시 update 페이지로 이동
    // 유형에 따라 수정 페이지 경로 반환 함수
    function getUpdatePageByType(type) {
        switch(type) {
            case '영상': return 'contents-video.html';
            case '문서': return 'contents-document.html';
            case '강의': return 'contents-class.html';
            case '문제': return 'contents-test.html';
            case '과제': return 'contents-practice.html';
            case '시험': return 'contents-grading.html';
            default: return 'contents.html';
        }
    }


    // tr 클릭 시 모달로 상세 보기 (수정 버튼 제외)
    const modal = document.getElementById('questionModal');
    const modalContent = document.getElementById('modalContent');
    const closeModalBtn = document.getElementById('closeModalBtn');
    tbody.addEventListener('click', function(e) {
        if (e.target.classList.contains('btn-secondary')) {
            // 수정하기 버튼 클릭 시 모달 열리지 않도록 이벤트 중단
            e.stopPropagation();
            const qid = e.target.getAttribute('data-id');
            let tr = e.target.closest('tr');
            let type = '';
            if (tr) {
                const typeTd = tr.querySelectorAll('td')[1];
                if (typeTd) {
                    type = typeTd.textContent.trim();
                }
            }
            // 문제은행은 REST 스타일 URL 이라 ?id= 규칙과 다르다
            if (type === '문제' && window._questionUrls && window._questionUrls.edit) {
                window.location.href = window._questionUrls.edit.replace('{id}', encodeURIComponent(qid));
                return;
            }
            const page = getUpdatePageByType(type);
            window.location.href = `${page}?id=${encodeURIComponent(qid)}`;
            return;
        }
        let tr = e.target.closest('tr');
        if (!tr) return;
        const trs = Array.from(tbody.querySelectorAll('tr'));
        const idx = trs.indexOf(tr);
        if (idx < 0) return;
        // window._questionList에서 원본 데이터 사용
        const q = window._questionList.find(item => item.id === tr.querySelector('button.btn-secondary').getAttribute('data-id'));
        if (!q) return;
        let html = '';
            html += `<table class="history-table" style="width:100%; border-collapse:collapse; border:1px solid var(--color-line-gray);">`;
            html += `<tr><th style='width:120px;'>유형</th><td>${q.type}</td><th>상태</th><td>${q.status}</td></tr>`;
            html += `<tr><th>제목</th><td colspan="3">${q.title}</td></tr>`;
            html += `<tr><th>난이도</th><td>${q.difficulty}</td><th>카테고리</th><td>${q.category}</td></tr>`;
            html += `<tr><th>사용중인 과정 수</th><td>${q.usedCourseCount}</td><th>평균 성취도</th><td>${q.avgAchievement}</td></tr>`;
            html += `<tr><th>생성일</th><td>${q.createdAt}</td><th>소요시간</th><td>${q.duration || '정보 없음'}</td></tr>`;
            html += `<tr><th>문제분류</th><td colspan="3">${q.subject || ''}${q.subject && q.topic ? ' > ' : ''}${q.topic || ''}${q.topic && q.subtopic ? ' > ' : ''}${q.subtopic || ''}</td></tr>`;
            html += `</table>`;
        // 미리보기 영역 및 상세내용 추가 (영상/문서)
        if (q.type === '영상') {
            if (q.previewUrl) {
                html += `<div style='margin:24px 0; text-align:center;'>
                    <video src='${q.previewUrl}' controls style='max-width:100%; height:240px; border-radius:8px; background:#000;'></video>
                    <div style='margin-top:8px; color:#888;'>영상 미리보기</div>
                </div>`;
            }
            // 영상 상세내용
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>영상 설명</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.description || '영상 설명이 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>영상 길이</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.duration || '정보 없음'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>영상 자료</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.materials || '미리보기'}</div>
                        ${q.materialsPreviewUrl ? `
                        <div style='margin-top:16px; text-align:center;'>
                            ${q.materialsPreviewUrl.endsWith('.mp4') ? `<video src='${q.materialsPreviewUrl}' controls style='max-width:100%; height:180px; border-radius:8px; background:#000;'></video>` : ''}
                            ${(q.materialsPreviewUrl.endsWith('.jpg') || q.materialsPreviewUrl.endsWith('.png') || q.materialsPreviewUrl.endsWith('.jpeg')) ? `<img src='${q.materialsPreviewUrl}' alt='자료 미리보기' style='max-width:100%; height:180px; object-fit:contain; border-radius:8px; border:1px solid #eee;'>` : ''}
                            ${q.materialsPreviewUrl.endsWith('.pdf') ? `<iframe src='${q.materialsPreviewUrl}' style='width:100%; height:180px; border-radius:8px; border:1px solid #eee;'></iframe>` : ''}
                            <div style='margin-top:8px; color:#888;'>자료 미리보기</div>
                        </div>
                        ` : ''}
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-video.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        } else if (q.type === '문서') {
            if (q.previewUrl) {
                // 이미지 또는 PDF 미리보기
                if (q.previewUrl.endsWith('.pdf')) {
                    html += `<div style='margin:24px 0; text-align:center;'>
                        <iframe src='${q.previewUrl}' style='width:100%; height:320px; border-radius:8px; border:1px solid #eee;'></iframe>
                        <div style='margin-top:8px; color:#888;'>문서 미리보기 (PDF)</div>
                    </div>`;
                } else {
                    html += `<div style='margin:24px 0; text-align:center;'>
                        <img src='${q.previewUrl}' alt='문서 미리보기' style='max-width:100%; height:320px; object-fit:contain; border-radius:8px; border:1px solid #eee;'>
                        <div style='margin-top:8px; color:#888;'>문서 미리보기</div>
                    </div>`;
                }
            }
            // 문서 상세내용
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문서 설명</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.description || '문서 설명이 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문서 유형</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.documentType || '문서 유형 정보 없음'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문서 자료</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.materials || '미리보기'}</div>
                        ${q.materialsPreviewUrl ? `
                        <div style='margin-top:16px; text-align:center;'>
                            ${(q.materialsPreviewUrl.endsWith('.jpg') || q.materialsPreviewUrl.endsWith('.png') || q.materialsPreviewUrl.endsWith('.jpeg')) ? `<img src='${q.materialsPreviewUrl}' alt='자료 미리보기' style='max-width:100%; height:180px; object-fit:contain; border-radius:8px; border:1px solid #eee;'>` : ''}
                            ${q.materialsPreviewUrl.endsWith('.pdf') ? `<iframe src='${q.materialsPreviewUrl}' style='width:100%; height:180px; border-radius:8px; border:1px solid #eee;'></iframe>` : ''}
                            ${q.materialsPreviewUrl.endsWith('.mp4') ? `<video src='${q.materialsPreviewUrl}' controls style='max-width:100%; height:180px; border-radius:8px; background:#000;'></video>` : ''}
                            <div style='margin-top:8px; color:#888;'>자료 미리보기</div>
                        </div>
                        ` : ''}
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-document.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        } else if (q.type === '강의') {
            // 강의 상세: 수업개요, 학습자료, 진행안내(설명)
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>수업개요</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.overview || '수업개요 정보가 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>학습자료</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.materials || '학습자료 정보가 없습니다.'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>진행안내(설명)</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.guide || '진행안내 정보가 없습니다.'}</div>
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-class.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        } else if (q.type === '과제') {
            // 문제 상세: 주관식/객관식, 문제 설명, 답, 해설
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>과제 유형</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.questionType || '정보 없음'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>과제 설명</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.description || '과제 설명이 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>정답</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.answer || '정답 정보가 없습니다.'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>해설</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.explanation || '해설 정보가 없습니다.'}</div>
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-practice.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        } else if ( q.type === '문제') {
            // 문제 상세: 주관식/객관식, 문제 설명, 답, 해설
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문제 유형</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.questionType || '정보 없음'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문제 설명</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.description || '문제 설명이 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>정답</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.answer || '정답 정보가 없습니다.'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>해설</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.explanation || '해설 정보가 없습니다.'}</div>
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-document.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        } else if (q.type === '시험') {
            // 시험 상세: 시험 설명, 문제 수, 배점 등
            html += `<div style='margin:24px 0;'>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>시험 설명</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.description || '시험 설명이 없습니다.'}</div>
                </div>
                <div style='margin-bottom:18px;'>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>문제 수</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'>${q.numberOfQuestions || '정보 없음'}</div>
                </div>
                <div>
                    <h4 style='font-size:16px; color:#333; margin-bottom:8px;'>미리보기</h4>
                    <div style='background:#f8f9fa; padding:12px 16px; border-radius:8px; color:#555;'><button class="btn-xs btn-gray">미리보기</button></div>
                </div>
            </div>`;
            html += `<button style="margin-top: 16px; margin-left: auto; display: block;" class="btn btn-secondary" onclick="window.location.href='contents-grading.html?id=${encodeURIComponent(q.id)}'">수정하기</button>`;
        }
        modal.classList.add('show');
        modalContent.innerHTML = html;
        // 모달 제목 동적 변경
        const modalTitle = document.getElementById('modalTitle');
        if (modalTitle) {
            let titleText = '문제 상세';
            if (q.type === '영상') titleText = '영상 상세';
            else if (q.type === '문서') titleText = '문서 상세';
            else if (q.type === '강의') titleText = '강의 상세';
            else if (q.type === '과제') titleText = '과제 상세';
            modalTitle.textContent = titleText;
        }
    });
    if (closeModalBtn) {
        closeModalBtn.addEventListener('click', function() {
            modal.classList.remove('show');
        });
    }
    modal.addEventListener('click', function(e) {
        // 모달 바깥 영역 클릭 시 닫기 (modal-content 이외 클릭)
        if (e.target === modal || e.target.classList.contains('question-modal')) {
            modal.classList.remove('show');
        }
    });
});


// 시험탭 호버시 툴팁 표시
document.addEventListener('DOMContentLoaded', function() {
    const examBtn = document.querySelector('button.tab-btn[data-type="시험"]');
    if (examBtn) {
        let tooltip;
        examBtn.addEventListener('mouseenter', function(e) {
            tooltip = document.createElement('div');
            tooltip.className = 'tab-tooltip';
            tooltip.textContent = `시험은 문제들을 묶어 만든 평가 콘텐츠입니다.
            문제 구성은 이곳에서 관리되며, 운영 방식은 과정 배정 시 설정합니다.`;
            document.body.appendChild(tooltip);
            const rect = examBtn.getBoundingClientRect();
            tooltip.style.left = (rect.left + window.scrollX + rect.width/2 - tooltip.offsetWidth/2) + 'px';
            tooltip.style.top = (rect.bottom + window.scrollY + 8) + 'px';
            setTimeout(() => { tooltip.style.opacity = 1; }, 10);
            // 위치 재조정 (offsetWidth가 생성 후에만 정확)
            setTimeout(() => {
                tooltip.style.left = (rect.left + window.scrollX + rect.width/2 - tooltip.offsetWidth/2) + 'px';
            }, 20);
        });
        examBtn.addEventListener('mouseleave', function() {
            if (tooltip) {
                tooltip.style.opacity = 0;
                setTimeout(() => { if (tooltip && tooltip.parentNode) tooltip.parentNode.removeChild(tooltip); }, 150);
            }
        });
    }
});