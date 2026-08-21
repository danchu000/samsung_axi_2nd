(() => {
  const form = document.querySelector('[data-application-form]');
  if (!form) return;
  const catalog = window.COURSE_CATALOG || {};
  const kind = form.dataset.applicationForm;
  const requestedKey = new URLSearchParams(location.search).get('course');
  const initialKey = requestedKey && catalog[requestedKey] ? requestedKey : '';
  const draftKey = 'tomorrow-ai-' + kind + '-draft';
  const picker = form.querySelector('[data-course-picker]');
  const pickerWrap = form.querySelector('[data-course-picker-wrap]');
  const recommendNote = form.querySelector('[data-recommend-note]');
  const routeInputs = [...form.querySelectorAll('input[name="applicationRoute"]')];
  const courseInput = form.querySelector('[data-course-value]');

  const categoryFor = course => {
    const cat = course.cat || '';
    if (cat.startsWith('KDT')) return 'KDT 신기술';
    if (cat.startsWith('AI ·')) return 'AI·디자인 실무';
    if (cat.startsWith('해외취업')) return '해외취업';
    if (cat.startsWith('일반고')) return '일반고 위탁';
    return '자격증';
  };
  ['KDT 신기술','AI·디자인 실무','해외취업','일반고 위탁','자격증'].forEach(groupName => {
    const entries = Object.entries(catalog).filter(([,course]) => categoryFor(course) === groupName);
    if (!entries.length) return;
    const group = document.createElement('optgroup');
    group.label = groupName;
    entries.forEach(([key,course]) => {
      const option = document.createElement('option');
      option.value = key;
      option.textContent = course.title;
      group.append(option);
    });
    picker.append(group);
  });

  const selectedRoute = () => form.elements.applicationRoute?.value || '';
  const selectedCourse = () => catalog[picker.value] || null;
  const displayCourse = () => {
    const recommendation = selectedRoute() === 'recommend';
    const course = selectedCourse();
    const title = recommendation ? '과정 추천이 필요해요' : (course?.title || '신청할 과정부터 선택해 주세요');
    const cat = recommendation ? '맞춤 과정 추천 상담' : (course?.cat || '아직 선택하지 않았어요');
    const meta = recommendation ? '경험 · 목표 · 가능 일정을 확인해 추천' : (course ? `${course.period || '일정 상담'} · 정원 ${course.capacity || '상담 시 안내'}` : '과정별 일정과 정원을 확인해 안내합니다.');
    courseInput.value = recommendation ? '과정 추천 요청' : (course?.title || '');
    document.querySelectorAll('[data-course-title]').forEach(el => el.textContent = title);
    document.querySelectorAll('[data-course-cat]').forEach(el => el.textContent = cat);
    document.querySelectorAll('[data-course-meta]').forEach(el => el.textContent = meta);
    const preview = form.querySelector('[data-course-preview]');
    preview.hidden = !course || recommendation;
    if (course && !recommendation) {
      preview.querySelector('[data-preview-cat]').textContent = course.cat;
      preview.querySelector('[data-preview-title]').textContent = course.title;
      preview.querySelector('[data-preview-meta]').textContent = `${course.period} · ${course.time} · 정원 ${course.capacity}`;
    }
    const summaryLabel = form.querySelector('[data-summary-label]');
    const summaryDescription = form.querySelector('[data-summary-description]');
    const submitLabel = form.querySelector('[data-submit-label]');
    if (summaryLabel) summaryLabel.textContent = recommendation ? '요청 내용' : '신청 과정';
    if (summaryDescription) summaryDescription.textContent = recommendation ? '제출 후 담당자가 현재 상황과 목표를 확인하고 가장 적합한 과정을 추천해 드립니다.' : '제출 후 담당자가 신청 내용을 확인하고 선발 절차와 준비사항을 개별 안내합니다.';
    if (submitLabel) submitLabel.textContent = recommendation ? '추천 상담 요청' : '신청서 제출';
  };
  const setRoute = route => {
    routeInputs.forEach(input => input.checked = input.value === route);
    pickerWrap.hidden = route !== 'course';
    recommendNote.hidden = route !== 'recommend';
    picker.required = route === 'course';
    displayCourse();
  };
  routeInputs.forEach(input => input.addEventListener('change', () => setRoute(input.value)));
  picker.addEventListener('change', displayCourse);

  const steps = [...form.querySelectorAll('[data-form-step]')];
  const indicators = [...document.querySelectorAll('[data-step-indicator]')];
  let current = 0;
  const show = index => {
    current = Math.max(0, Math.min(index, steps.length - 1));
    steps.forEach((step, i) => step.hidden = i !== current);
    indicators.forEach((item, i) => item.classList.toggle('is-active', i <= current));
    window.scrollTo({top:0,behavior:'smooth'});
  };
  form.addEventListener('click', event => {
    const next = event.target.closest('[data-step-next]');
    const prev = event.target.closest('[data-step-prev]');
    if (next) {
      const required = [...steps[current].querySelectorAll('[required]')];
      if (!required.every(input => input.reportValidity())) return;
      show(current + 1);
    }
    if (prev) show(current - 1);
  });
  const serialize = () => Object.fromEntries(new FormData(form).entries());
  form.querySelector('[data-save-draft]')?.addEventListener('click', () => {
    localStorage.setItem(draftKey, JSON.stringify(serialize()));
    const message = document.querySelector('[data-save-message]');
    if (message) message.textContent = '이 브라우저에 임시 저장했습니다.';
  });
  let saved = {};
  try { saved = JSON.parse(localStorage.getItem(draftKey) || '{}'); } catch (_) {}
  Object.entries(saved).forEach(([name,value]) => {
    const input = form.elements[name];
    if (!input || name === 'course' || name === 'applicationRoute') return;
    if (input instanceof RadioNodeList) [...input].forEach(item => item.checked = item.value === value);
    else if (input.type === 'checkbox') input.checked = value === input.value || value === 'on';
    else input.value = value;
  });
  const savedRoute = saved.applicationRoute;
  const savedCourseKey = Object.keys(catalog).find(key => catalog[key].title === saved.course);
  if (initialKey) { picker.value = initialKey; setRoute('course'); }
  else if (savedRoute === 'recommend') setRoute('recommend');
  else if (savedRoute === 'course' && savedCourseKey) { picker.value = savedCourseKey; setRoute('course'); }
  else { picker.value = ''; displayCourse(); }

  form.addEventListener('submit', event => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const receipt = 'AXI-' + new Date().toISOString().slice(2,10).replaceAll('-','') + '-' + String(Date.now()).slice(-4);
    localStorage.setItem('tomorrow-ai-' + kind + '-submitted', JSON.stringify({...serialize(),receipt,submittedAt:new Date().toISOString()}));
    localStorage.removeItem(draftKey);
    document.querySelector('[data-form-shell]').hidden = true;
    const done = document.querySelector('[data-form-complete]');
    done.hidden = false;
    done.querySelector('[data-receipt]').textContent = receipt;
    window.scrollTo({top:0,behavior:'smooth'});
  });
  show(0);
})();
