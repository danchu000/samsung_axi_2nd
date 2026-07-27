// assignments.js - 과제 목록, 필터, 상세/제출 모달 제어

// 더미 데이터 예시 (실제 서비스에서는 서버에서 받아옴)
const ASSIGNMENTS = [
  {
    id: 'a01',
    name: 'React 실습 과제',
    course: '풀스택 웹 개발',
    session: '3차시',
    status: 'todo',
    deadline: '2026-01-30',
    type: '파일',
    retake: '가능',
    desc: 'React로 TODO 앱을 구현하세요.',
    myStatus: '미제출',
    submit: null,
    grade: null
  },
  {
    id: 'a02',
    name: '파이썬 데이터 분석',
    course: '데이터 분석 입문',
    session: '2차시',
    status: 'graded',
    deadline: '2026-01-20',
    type: '텍스트',
    retake: '불가',
    desc: 'Pandas로 데이터 통계 분석 결과를 제출하세요.',
    myStatus: '제출/채점완료',
    submit: { text: 'Pandas로 분석 결과를 정리했습니다.' },
    grade: { meta: '100점', feedback: '아주 잘했습니다!' }
  }
];

// DOM
const cardList = document.getElementById('cardList');
const listHint = document.getElementById('listHint');
const fCourse = document.getElementById('fCourse');
const fSession = document.getElementById('fSession');
const fStatus = document.getElementById('fStatus');
const fSort = document.getElementById('fSort');
const fKeyword = document.getElementById('fKeyword');
const btnSearch = document.getElementById('btnSearch');
const btnReset = document.getElementById('btnReset');

// 모달
const detailModal = document.getElementById('detailModal');
const submitModal = document.getElementById('submitModal');

// 필터/정렬/검색
function filterAssignments() {
  let filtered = [...ASSIGNMENTS];
  if (fCourse.value) filtered = filtered.filter(a => a.course === fCourse.value);
  if (fSession.value) filtered = filtered.filter(a => a.session === fSession.value);
  if (fStatus.value) filtered = filtered.filter(a => a.status === fStatus.value);
  if (fKeyword.value.trim()) {
    const kw = fKeyword.value.trim().toLowerCase();
    filtered = filtered.filter(a => a.name.toLowerCase().includes(kw) || a.course.toLowerCase().includes(kw));
  }
  // 정렬
  if (fSort.value === 'deadline_soon') filtered.sort((a,b)=>a.deadline.localeCompare(b.deadline));
  if (fSort.value === 'deadline_late') filtered.sort((a,b)=>b.deadline.localeCompare(a.deadline));
  if (fSort.value === 'recent') filtered.sort((a,b)=>b.id.localeCompare(a.id));
  if (fSort.value === 'title') filtered.sort((a,b)=>a.name.localeCompare(b.name));
  return filtered;
}

function renderList() {
  const list = filterAssignments();
  cardList.innerHTML = list.length ? list.map(a => `
    <div class="card" data-id="${a.id}">
      <div class="card-title">${a.name}</div>
      <div class="card-meta">${a.course} / ${a.session}</div>
      <div class="card-status">상태: ${a.status}</div>
      <div class="card-deadline">마감: ${a.deadline}</div>
      <button class="btn btn-primary btn-detail" data-id="${a.id}">상세보기</button>
    </div>
  `).join('') : '<div class="empty">과제가 없습니다.</div>';
  listHint.textContent = `전체 ${list.length}건`;
}

// 상세 모달 열기
function openDetailModal(id) {
  const a = ASSIGNMENTS.find(x => x.id === id);
  if (!a) return;
  document.getElementById('dName').textContent = a.name;
  document.getElementById('dMeta').textContent = `${a.course} / ${a.session}`;
  document.getElementById('dStatus').textContent = a.status;
  document.getElementById('dType').textContent = a.type;
  document.getElementById('dWindow').textContent = a.deadline;
  document.getElementById('dRetake').textContent = a.retake;
  document.getElementById('dMyStatus').textContent = a.myStatus;
  document.getElementById('dDesc').textContent = a.desc;
  // 제출/채점 정보
  document.getElementById('submitBox').hidden = !a.submit;
  document.getElementById('gradeBox').hidden = !a.grade;
  if (a.submit) {
    document.getElementById('dSubmitMeta').textContent = a.myStatus;
    document.getElementById('dSubmitTextWrap').hidden = !a.submit.text;
    document.getElementById('dSubmitText').textContent = a.submit.text || '';
  }
  if (a.grade) {
    document.getElementById('dGradeMeta').textContent = a.grade.meta;
    document.getElementById('dGradeFeedback').textContent = a.grade.feedback;
  }
  detailModal.style.display = 'flex';
  detailModal.setAttribute('aria-hidden', 'false');
}

// 모달 닫기
function closeModal(modal) {
  modal.style.display = 'none';
  modal.setAttribute('aria-hidden', 'true');
}

document.addEventListener('DOMContentLoaded', function() {
  renderList();
  // 카드 상세보기 버튼
  cardList.addEventListener('click', function(e) {
    const btn = e.target.closest('.btn-detail');
    if (btn) openDetailModal(btn.getAttribute('data-id'));
  });
  // 모달 닫기
  document.querySelectorAll('[data-close="detailModal"]').forEach(btn => {
    btn.addEventListener('click', () => closeModal(detailModal));
  });
  // 필터/검색
  btnSearch.addEventListener('click', renderList);
  btnReset.addEventListener('click', () => {
    fCourse.value = '';
    fSession.value = '';
    fStatus.value = '';
    fSort.value = 'deadline_soon';
    fKeyword.value = '';
    renderList();
  });
});
