function handleMenuClick(element, page) {
    // 기존 active 클래스 제거
    const menuItems = document.querySelectorAll('.gnb li');
    menuItems.forEach(item => item.classList.remove('active'));

    // 클릭된 메뉴 항목에 active 클래스 추가
    element.classList.add('active');

    // 페이지 이동
    window.location.href = page;
} 
