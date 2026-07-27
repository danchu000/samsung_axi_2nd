function goCourseDetail(courseId) {
    // window.location.href = `/admin/courses/${courseId}`;
    window.location.href = `courses-detail.html`;
}

// 과정 데이터 배열 (타임리프 th:each로 쉽게 교체 가능)
const courseList = [
    {
        id: 1,
        category: 'KDT',
        categoryClass: 'badge-kdt',
        title: '풀스택 웹 개발 (React & Node.js)',
        instructor: '김민수 강사',
        subjectCount: 12,
        totalHours: 120,
        studentCount: 45,
        status: 'completed',
        statusText: '운영중',
        period: '2026.01.02 ~ 2026.06.30',
        progress: 42,
        toggle: 'active'
    },
    {
        id: 2,
        category: '고교위탁',
        categoryClass: 'badge-highschool',
        title: 'Python 기초 프로그래밍',
        instructor: '박지영 강사',
        subjectCount: 8,
        totalHours: 80,
        studentCount: 32,
        status: 'in-progress',
        statusText: '모집중',
        period: '2026.01.15 ~ 2026.04.30',
        progress: 68,
        toggle: 'active'
    },
    {
        id: 3,
        category: '심화',
        categoryClass: 'badge-advanced',
        title: '머신러닝 & AI 심화과정',
        instructor: '이준호 강사',
        subjectCount: 15,
        totalHours: 160,
        studentCount: 28,
        status: 'completed',
        statusText: '운영중',
        period: '2025.12.01 ~ 2026.07.31',
        progress: 25,
        toggle: 'active'
    },
    {
        id: 4,
        category: 'KDT',
        categoryClass: 'badge-kdt',
        title: 'Java 기반 백엔드 개발',
        instructor: '최수진 강사',
        subjectCount: 10,
        totalHours: 100,
        studentCount: 38,
        status: 'pending',
        statusText: '예정',
        period: '2026.02.01 ~ 2026.05.31',
        progress: 15,
        toggle: 'active'
    }
];

function renderCourseGrid() {
    const grid = document.querySelector('.course-grid');
    grid.innerHTML = '';
    courseList.forEach(course => {
        const div = document.createElement('div');
        div.className = 'course-card';
        div.onclick = function() { goCourseDetail(course.id); };
        div.innerHTML = `
            <div class="course-top-section" onclick="event.stopPropagation()">
                <span class="course-category ${course.categoryClass}">${course.category}</span>
                <div data-user-role="admin" class="toggle-status">
                    <select class="status-toggle">
                        <option value="active" ${course.toggle==='active'?'selected':''}>활성</option>
                        <option value="hidden" ${course.toggle==='hidden'?'selected':''}>비활성</option>
                        <option value="inactive" ${course.toggle==='inactive'?'selected':''}>숨김</option>
                    </select>
                </div>
            </div>
            <div class="course-header">
                <h3 class="course-title">${course.title}</h3>
                <span class="instructor-name">${course.instructor}</span>
            </div>
            <div class="course-stats">
                <div class="stat-box">
                    <span class="stat-text">과목 <strong>${course.subjectCount}</strong>개</span>
                </div>
                <div class="divider-vertical"></div>
                <div class="stat-box">
                    <span class="stat-text">총 <strong>${course.totalHours}</strong>시간</span>
                </div>
                <div class="divider-vertical"></div>
                <div class="stat-box">
                    <span class="stat-text">수강생 <strong>${course.studentCount}</strong>명</span>
                </div>
            </div>
            <div class="course-progress-info">
                <div class="progress-left">
                    <span class="status-badge status-${course.status}">${course.statusText}</span>
                    <span class="progress-period">📅 ${course.period}</span>
                </div>
                <span class="progress-percentage">${course.progress}%</span>
            </div>
            <div class="progress-bar-container">
                <div class="progress-bar" style="width: ${course.progress}%;"></div>
            </div>
        `;
        grid.appendChild(div);
    });
}

document.addEventListener('DOMContentLoaded', function() {
    renderCourseGrid();
    // body가 admin이 아닐 때 option[data-user-role="admin"] 숨김
    if (document.body.dataset.userRole !== 'admin') {
        document.querySelectorAll('div[data-user-role="admin"]').forEach(function(el) {
            el.hidden = true;
        });
    }
});
