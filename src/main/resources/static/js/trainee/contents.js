// 더미 데이터 예시 (실제 사용시 fetch로 대체)
const dummyContents = [
  {
    contentId: 'CNT-001', order: 1, title: '변수와 데이터 타입', type: 'VIDEO', typeLabel: '동영상', durationMinutes: 45, progressRate: 100, isCompleted: true,
    courseId: 'COURSE-01', courseTitle: '풀스택 웹 개발', lessonId: 'LESSON-01', lessonTitle: '1차시: JavaScript 기본 문법'
  },
  {
    contentId: 'CNT-002', order: 2, title: '연산자와 표현식', type: 'DOCUMENT', typeLabel: '문서', durationMinutes: 30, progressRate: 60, isCompleted: false,
    courseId: 'COURSE-01', courseTitle: '풀스택 웹 개발', lessonId: 'LESSON-01', lessonTitle: '1차시: JavaScript 기본 문법'
  },
  {
    contentId: 'CNT-003', order: 3, title: '조건문과 반복문', type: 'TEST', typeLabel: '테스트', durationMinutes: 20, progressRate: 0, isCompleted: false,
    courseId: 'COURSE-01', courseTitle: '풀스택 웹 개발', lessonId: 'LESSON-01', lessonTitle: '1차시: JavaScript 기본 문법'
  },
  {
    contentId: 'CNT-004', order: 1, title: '함수와 스코프', type: 'VIDEO', typeLabel: '동영상', durationMinutes: 50, progressRate: 80, isCompleted: false,
    courseId: 'COURSE-02', courseTitle: 'AI 엔지니어링', lessonId: 'LESSON-02', lessonTitle: '2차시: 함수와 객체'
  },
  {
    contentId: 'CNT-005', order: 2, title: '클래스와 객체', type: 'PRACTICE', typeLabel: '실습', durationMinutes: 35, progressRate: 40, isCompleted: false,
    courseId: 'COURSE-02', courseTitle: 'AI 엔지니어링', lessonId: 'LESSON-02', lessonTitle: '2차시: 함수와 객체'
  }
  ,
  {
    contentId: 'CNT-006', order: 3, title: '객체지향 프로그래밍', type: 'CLASS', typeLabel: '수업', durationMinutes: 60, progressRate: 0, isCompleted: false,
    courseId: 'COURSE-02', courseTitle: 'AI 엔지니어링', lessonId: 'LESSON-03', lessonTitle: '3차시: 객체지향'
  }
];

function getIconFile(type) {
  switch(type) {
    case 'DOCUMENT': return 'content-document.png';
    case 'PRACTICE': return 'content-practice.png';
    case 'TEST': return 'content-test.png';
    case 'VIDEO': return 'content-video.png';
    case 'CLASS': return 'content-class.png';
    default: return 'content-document.png';
  }
}

function getDaysLeft(lessonTitle) {
  // 예시: '1차시: JavaScript 기본 문법'에서 임의로 2026-01-10을 추출한다고 가정
  // 실제로는 lesson 객체에 endDate가 있어야 함
  const today = new Date();
  // 더미: lessonTitle에 따라 날짜 매핑
  const lessonDateMap = {
    '1차시: JavaScript 기본 문법': '2026-01-10',
    '2차시: 함수와 객체': '2026-05-15'
  };
  const endDateStr = lessonDateMap[lessonTitle] || '2026-01-10';
  const endDate = new Date(endDateStr);
  const diffMs = endDate - today;
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  return diffDays < 0 ? '종료' : `D-${String(diffDays).padStart(3, '0')}`;
}

function renderContentList(contents) {
  // Sort contents by getDaysLeft value (ascending)
  const sorted = [...contents].sort((a, b) => {
    // Extract numeric days from getDaysLeft (e.g., D-010 => 10, 종료 => Infinity)
    const getDays = (lessonTitle) => {
      const val = getDaysLeft(lessonTitle);
      if (val === '종료') return Infinity;
      const match = val.match(/D-(\d+)/);
      return match ? parseInt(match[1], 10) : Infinity;
    };
    return getDays(a.lessonTitle) - getDays(b.lessonTitle);
  });
  return sorted.map(content => {
    let url = '';
    switch(content.type) {
      case 'DOCUMENT': url = '/templates/trainee/play-document.html'; break;
      case 'PRACTICE': url = '/templates/trainee/play-practice.html'; break;
      case 'TEST': url = '/templates/trainee/play-grading.html'; break;
      case 'VIDEO': url = '/templates/trainee/play-video.html'; break;
      case 'CLASS': url = '/templates/trainee/play-class.html'; break;
      default: url = '#';
    }
    return `
    <li style="display:flex;flex-direction:column;gap:8px;padding:10px;border-bottom:1px solid #eee;">
        <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:#666;">
        <span style="font-weight:600;">${content.courseTitle}</span>
        <span style="color:#bbb;">&gt;</span>
        <span>${content.lessonTitle}</span>
        </div>
      <div style="display:flex;align-items:center;gap:12px;">
      <img src="/static/icons/${getIconFile(content.type)}" alt="${content.typeLabel}" style="width:42px;height:42px;object-fit:contain;"/>
      <span style="color:#888;">${content.order}.</span>
      <span style="flex:1;">${content.title}<span style="font-size:14px;color:#999;margin-left:8px;"> ${Math.floor(content.durationMinutes/60) > 0 ? Math.floor(content.durationMinutes/60)+"시간 " : ''}${content.durationMinutes%60}분</span></span>
      <span style="font-size:14px;color:#007bff;">${content.progressRate}%</span>
        <span class="status-badge ${content.isCompleted ? 'status-completed' : 'status-in-progress'}">${content.isCompleted ? '완료' : `${getDaysLeft(content.lessonTitle)}`}</span>
        <a
          class="btn ${content.isCompleted ? 'btn-gray' : 'btn-secondary'}"
          data-status="${content.isCompleted ? 'completed' : 'progress'}"
          aria-label="${content.isCompleted ? '강의 다시보기' : '강의 학습하기'}"
          href="${url}"
        >
          ${content.isCompleted ? '다시보기' : '학습하기'}
        </a>
      </div>
    </li>
    `;
  }).join('');
}

document.addEventListener('DOMContentLoaded', function() {
  const listEl = document.getElementById('contentList');
  if (listEl) {
    listEl.innerHTML = renderContentList(dummyContents);
  }
});
// js/trainee/contents.js