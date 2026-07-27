function toggleSidebar() {
    const body = document.body;
    const sidebar = document.querySelector('.sidebar');
    const toggleBtn = document.querySelector('.toggle-btn');
    const logo = document.getElementById('sidebarLogo');
    const logoutText = document.querySelector('.logout');
    
    body.classList.toggle('sidebar-collapsed');
    sidebar.classList.toggle('collapsed');
    toggleBtn.classList.toggle('collapsed');
    
    // 아이콘 변경
    if (body.classList.contains('sidebar-collapsed')) {
        toggleBtn.innerHTML = '▶';
        logo.src = '/static/img/amblem-white.png';
        logo.style.width = '30px';
        // logout 텍스트 숨기기
        if (logoutText) {
            logoutText.childNodes[0].textContent = '';
        }
        // 상태 저장
        localStorage.setItem('sidebarCollapsed', 'true');
    } else {
        toggleBtn.innerHTML = '◀';
        logo.src = '/static/img/logo-white.png';
        logo.style.width = '130px';
        // logout 텍스트 보이기
        if (logoutText) {
            logoutText.childNodes[0].textContent = '로그아웃 ';
        }
        // 상태 저장
        localStorage.setItem('sidebarCollapsed', 'false');
    }
}

function handleMenuClick(element, page) {
    const body = document.body;
    const submenu = element.querySelector('ul');    
    
    // 사이드바가 접혀있으면 무조건 페이지 이동
    if (body.classList.contains('sidebar-collapsed')) {
        event.stopPropagation();
        // 현재 페이지와 다를 때만 이동
        if (window.location.pathname !== page && !window.location.pathname.endsWith(page)) {
            window.location.href = page;
        }
        return;
    }

    // 특정 페이지에서는 서브메뉴 상태 초기화
    if (page === '/templates/admin/admin-04-evaluation/admin-evaluation-question-bank.html') {
        localStorage.removeItem('openSubmenu');
    }
    
    // 서브메뉴가 있는 경우 토글
    if (submenu) {
        event.stopPropagation();
        
        // 다른 열린 서브메뉴 닫기
        const allMenuItems = document.querySelectorAll('.menu > ul > li');
        allMenuItems.forEach(item => {
            if (item !== element && item.querySelector('ul')) {
                item.classList.remove('open');
            }
        });
        
        // 현재 서브메뉴 토글
        element.classList.toggle('open');
        
        // 서브메뉴 상태 저장
        const menuPage = element.getAttribute('data-page');
        if (element.classList.contains('open')) {
            localStorage.setItem('openSubmenu', menuPage);
        } else {
            localStorage.removeItem('openSubmenu');
        }
    } else {
        // 서브메뉴가 없으면 페이지 이동 (현재 페이지와 다를 때만)
        event.stopPropagation();
        if (window.location.pathname !== page && !window.location.pathname.endsWith(page)) {
            window.location.href = page;
        }
    }
}

// 페이지 로드 시 사이드바 상태 복원
document.addEventListener('DOMContentLoaded', function() {
    const sidebarCollapsed = localStorage.getItem('sidebarCollapsed');
    
    if (sidebarCollapsed === 'true') {
        const body = document.body;
        const sidebar = document.querySelector('.sidebar');
        const toggleBtn = document.querySelector('.toggle-btn');
        const logo = document.getElementById('sidebarLogo');
        const logoutText = document.querySelector('.logout');
        
        body.classList.add('sidebar-collapsed');
        sidebar.classList.add('collapsed');
        toggleBtn.classList.add('collapsed');
        toggleBtn.innerHTML = '▶';
        logo.src = '/static/img/amblem-white.png';
        logo.style.width = '30px';
        // logout 텍스트 숨기기
        if (logoutText) {
            logoutText.childNodes[0].textContent = '';
        }
    }
    
    // 서브메뉴 열림 상태 복원
    const openSubmenu = localStorage.getItem('openSubmenu');
    if (openSubmenu) {
        const menuItem = document.querySelector(`.menu > ul > li[data-page="${openSubmenu}"]`);
        if (menuItem && menuItem.querySelector('ul')) {
            menuItem.classList.add('open');
        }
    }
});