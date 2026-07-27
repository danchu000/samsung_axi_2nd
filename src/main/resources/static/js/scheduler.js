
// 달력 데이터
let currentYear = 2026;
let currentMonth = 0; // 0 = January
let selectedDate = null;

// 샘플 수업 데이터 (장기 과정 포함)
const courses = [
    { 
        name: '풀스택 웹 개발 (React & Node.js)', 
        startDate: '2026-01-02', 
        endDate: '2026-06-30',
        time: '09:00 - 12:00', 
        instructor: '김민수 강사', 
        type: 'kdt',
        schedule: '월, 수, 금'
    },
    { 
        name: 'Python 기초 프로그래밍', 
        startDate: '2026-01-07', 
        endDate: '2026-03-31',
        time: '14:00 - 17:00', 
        instructor: '박지영 강사', 
        type: 'highschool',
        schedule: '화, 목'
    },
    { 
        name: '머신러닝 & AI 심화과정', 
        startDate: '2026-01-07', 
        endDate: '2026-08-31',
        time: '18:00 - 21:00', 
        instructor: '이준호 강사', 
        type: 'advanced',
        schedule: '월, 수'
    },
    { 
        name: 'Java 기반 백엔드 개발', 
        startDate: '2026-01-15', 
        endDate: '2026-05-15',
        time: '13:00 - 16:00', 
        instructor: '최수진 강사', 
        type: 'kdt',
        schedule: '월~금'
    },
    { 
        name: 'HTML & CSS 기초', 
        startDate: '2026-01-20', 
        endDate: '2026-02-28',
        time: '10:00 - 12:00', 
        instructor: '김민수 강사', 
        type: 'kdt',
        schedule: '매일'
    },
    { 
        name: 'TensorFlow 실전 프로젝트', 
        startDate: '2026-02-01', 
        endDate: '2026-07-31',
        time: '10:00 - 13:00', 
        instructor: '이준호 강사', 
        type: 'advanced',
        schedule: '화, 목, 금'
    }
];

// 특정 날짜에 진행 중인 과정들을 반환
function getCoursesForDate(dateStr) {
    const date = new Date(dateStr);
    const dayOfWeek = date.getDay(); // 0=일, 1=월, 2=화, 3=수, 4=목, 5=금, 6=토
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    const currentDayName = dayNames[dayOfWeek];
    
    return courses.filter(course => {
        // 날짜 범위 체크
        if (dateStr < course.startDate || dateStr > course.endDate) {
            return false;
        }
        
        // 요일 체크
        const schedule = course.schedule;
        
        // '매일'인 경우 모든 요일 허용
        if (schedule === '매일') {
            return true;
        }
        
        // '월~금'인 경우 월요일부터 금요일까지만
        if (schedule === '월~금') {
            return dayOfWeek >= 1 && dayOfWeek <= 5;
        }
        
        // 특정 요일들이 나열된 경우 (예: '월, 수, 금')
        return schedule.includes(currentDayName);
    });
}

// 달력 초기화
function initCalendar() {
    const today = new Date();
    currentYear = today.getFullYear();
    currentMonth = today.getMonth();
    renderCalendar();
}

// 달력 렌더링
function renderCalendar() {
    const firstDay = new Date(currentYear, currentMonth, 1);
    const lastDay = new Date(currentYear, currentMonth + 1, 0);
    const prevLastDay = new Date(currentYear, currentMonth, 0);
    
    const firstDayIndex = firstDay.getDay();
    const lastDateNum = lastDay.getDate();
    const prevLastDateNum = prevLastDay.getDate();
    
    const monthNames = ['월', '월', '월', '월', '월', '월', '월', '월', '월', '10월', '11월', '12월'];
    document.getElementById('currentMonth').textContent = `${currentYear}년 ${currentMonth + 1}월`;
    
    let days = '';
    const today = new Date();
    const isCurrentMonth = today.getFullYear() === currentYear && today.getMonth() === currentMonth;
    const todayDate = today.getDate();
    
    // 이전 달 날짜
    for (let i = firstDayIndex; i > 0; i--) {
        const date = prevLastDateNum - i + 1;
        days += `<div class="calendar-day other-month">
            <div class="day-number">${date}</div>
        </div>`;
    }
    
    // 현재 달 날짜
    for (let i = 1; i <= lastDateNum; i++) {
        const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`;
        const dayOfWeek = new Date(currentYear, currentMonth, i).getDay();
        const classes = getCoursesForDate(dateStr);
        
        let dayClass = 'calendar-day';
        if (isCurrentMonth && i === todayDate) dayClass += ' today';
        if (dayOfWeek === 0) dayClass += ' sunday';
        if (dayOfWeek === 6) dayClass += ' saturday';
        
        let classIndicators = '';
        const maxShow = 3;
        for (let j = 0; j < Math.min(classes.length, maxShow); j++) {
            const isStartDate = classes[j].startDate === dateStr;
            const prefix = isStartDate ? '🎯 ' : '';
            classIndicators += `<div class="class-indicator class-${classes[j].type}">${prefix}${classes[j].name}</div>`;
        }
        if (classes.length > maxShow) {
            classIndicators += `<div class="more-classes">+${classes.length - maxShow}개 더보기</div>`;
        }
        
        days += `<div class="${dayClass}" onclick="selectDate('${dateStr}')">
            <div class="day-number">${i}</div>
            <div class="class-indicators">${classIndicators}</div>
        </div>`;
    }
    
    // 다음 달 날짜
    const remainingDays = 42 - (firstDayIndex + lastDateNum); // 6 weeks
    for (let i = 1; i <= remainingDays; i++) {
        days += `<div class="calendar-day other-month">
            <div class="day-number">${i}</div>
        </div>`;
    }
    
    document.getElementById('calendarGrid').innerHTML = days;
}

// 날짜 선택
function selectDate(dateStr) {
    selectedDate = dateStr;
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    
    document.getElementById('sidebarDate').textContent = `${year}년 ${month}월 ${day}일`;
    
    const classes = getCoursesForDate(dateStr);
    let classListHTML = '';
    
    if (classes.length === 0) {
        classListHTML = '<div class="no-classes">이 날짜에 예정된 수업이 없습니다.</div>';
    } else {
        classes.forEach(cls => {
            const isStartDate = cls.startDate === dateStr;
            const isEndDate = cls.endDate === dateStr;
            const formatDate = (d) => {
                const dt = new Date(d);
                return `${dt.getFullYear()}.${String(dt.getMonth() + 1).padStart(2, '0')}.${String(dt.getDate()).padStart(2, '0')}`;
            };
            
            let statusBadge = '';
            if (isStartDate) {
                statusBadge = '<span class="status-badge status-completed" style="margin-left:8px;">개강</span>';
            } else if (isEndDate) {
                statusBadge = '<span class="status-badge status-dropout" style="margin-left:8px;">종강</span>';
            } else {
                statusBadge = '<span class="status-badge status-in-progress" style="margin-left:8px;">진행중</span>';
            }
            
            classListHTML += `
                <div class="class-item ${cls.type}">
                    <div style="margin-bottom:8px; font-size:12px; color:#333;">
                        📆 과정 기간 ${formatDate(cls.startDate)} ~ ${formatDate(cls.endDate)}
                    </div>
                    <div class="class-name">${cls.name}${statusBadge}</div>
                    <div class="class-info">
                        <span>${cls.instructor}</span>
                    </div>
                        <div class="class-time">${cls.time} | ${cls.schedule}</div>
                </div>
            `;
        });
    }
    
    document.getElementById('classList').innerHTML = classListHTML;
    document.getElementById('scheduleSidebar').classList.add('active');
    
    // 선택된 날짜 하이라이트
    document.querySelectorAll('.calendar-day').forEach(day => {
        day.classList.remove('selected');
    });
    event.target.closest('.calendar-day').classList.add('selected');
}

// 사이드바 닫기
function closeSidebar() {
    document.getElementById('scheduleSidebar').classList.remove('active');
    document.querySelectorAll('.calendar-day').forEach(day => {
        day.classList.remove('selected');
    });
}

// 이전 달
function previousMonth() {
    currentMonth--;
    if (currentMonth < 0) {
        currentMonth = 11;
        currentYear--;
    }
    renderCalendar();
}

// 다음 달
function nextMonth() {
    currentMonth++;
    if (currentMonth > 11) {
        currentMonth = 0;
        currentYear++;
    }
    renderCalendar();
}

// 오늘로 이동
function goToday() {
    const today = new Date();
    currentYear = today.getFullYear();
    currentMonth = today.getMonth();
    renderCalendar();
    
    // 오늘 날짜의 수업 정보도 사이드바에 표시
    const todayStr = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    selectDate(todayStr);
}

// 페이지 로드 시 초기화
window.addEventListener('DOMContentLoaded', initCalendar);
