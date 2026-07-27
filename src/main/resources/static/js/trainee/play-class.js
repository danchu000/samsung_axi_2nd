// 더미 데이터 예시 (실제 사용시 fetch로 대체)
const dummyClassData = {
  title: '3-1. 객체지향 프로그래밍',
  meta: 'AI 엔지니어링 · 3차시: 객체지향 · 2026-05-22 (60분)',
  course: '풀스택 웹 개발 (React & Node.js)',
  subject: '2. AI 엔지니어링',
  lesson: '3-1. 객체지향 프로그래밍',
  time: '강의 / 60분',
  overview: `본 수업에서는 객체지향 프로그래밍의 핵심 개념과 클래스, 객체, 상속, 캡슐화, 다형성에 대해 학습합니다.\n실습을 통해 자바스크립트에서 객체지향 패턴을 직접 구현해봅니다.`,
  materials: [
    { label: '강의 슬라이드', file: '객체지향 프로그래밍.pdf', url: '#' },
    { label: '예제 코드', file: 'OOP-example.js', url: '#' },
    { label: '참고 영상', file: 'OOP 개념 설명 동영상', url: '#' }
  ],
  guide: [
    '강의 자료를 참고하여 객체지향의 기본 개념을 이해합니다.',
    '예제 코드를 따라하며 직접 클래스를 작성해봅니다.',
    '실습 후, 질의응답 및 토론 시간을 가집니다.'
  ]
};

function renderClassPage(data) {

  // 헤더 영역 동적 바인딩 (정확한 클래스명으로 지정)
  const courseEl = document.querySelector('.breadcrumb-value.breadcrumb-course');
  if (courseEl) courseEl.textContent = data.course || '과정명';
  const subjectEl = document.querySelector('.breadcrumb-value.breadcrumb-subject');
  if (subjectEl) subjectEl.textContent = data.subject || '과목명';
  const lessonEl = document.querySelector('.breadcrumb-value.breadcrumb-content');
  if (lessonEl) lessonEl.textContent = data.lesson || '콘텐츠명';
  const timeEl = document.querySelector('.content-header > span');
  if (timeEl) timeEl.textContent = data.time || '시간';

  // 본문 영역 동적 바인딩
  const titleEl = document.querySelector('.class-title');
  if (titleEl) titleEl.textContent = data.title;
  const metaEl = document.querySelector('.class-meta');
  if (metaEl) metaEl.textContent = data.meta;
  const overviewEl = document.querySelector('.class-desc.overview');
  if (overviewEl) overviewEl.innerHTML = data.overview.replace(/\n/g, '<br>');

  // 자료 리스트
  const matList = data.materials.map(m => `<li>${m.label}: <a href="${m.url}" target="_blank">${m.file}</a></li>`).join('');
  const matUl = document.querySelector('.class-materials ul');
  if (matUl) matUl.innerHTML = matList;

  // 진행 안내
  const guideEl = document.querySelector('.class-desc.guide');
  if (guideEl) guideEl.innerHTML = data.guide.map((g, i) => `${i+1}. ${g}`).join('<br>');
}

// SPA 라우터에서 호출할 수 있도록 init 함수 제공
function initLearningClassPage(data = dummyClassData) {
  renderClassPage(data);
}

// 기존 방식도 동작하도록 유지
document.addEventListener('DOMContentLoaded', function() {
  renderClassPage(dummyClassData);
});
document.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-action="open-window"]');
  if (!btn) return;

  window.open(
    btn.dataset.url,
    '_blank',
    'noopener,noreferrer'
  );
});

