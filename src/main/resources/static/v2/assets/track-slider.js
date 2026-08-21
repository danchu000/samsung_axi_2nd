(() => {
  const deck = document.querySelector('.track-deck');
  if (!deck) return;

  const cards = [...deck.querySelectorAll('.track-card')];
  const dots = [...document.querySelectorAll('.track-deck__nav i')];
  const prev = document.querySelector('[data-track-prev]');
  const next = document.querySelector('[data-track-next]');
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
  let index = 0;
  let timer;
  let wheelLocked = false;

  const activate = (nextIndex, behavior = 'smooth') => {
    index = (nextIndex + cards.length) % cards.length;
    cards.forEach((card, i) => card.classList.toggle('is-active', i === index));
    dots.forEach((dot, i) => dot.classList.toggle('is-active', i === index));
    const card = cards[index];
    const target = card.offsetLeft - (deck.clientWidth - card.clientWidth) / 2;
    deck.scrollTo({ left: target, behavior: reducedMotion ? 'auto' : behavior });
  };

  const stop = () => clearInterval(timer);
  const start = () => {
    stop();
    if (!reducedMotion) timer = setInterval(() => activate(index + 1), 4500);
  };

  prev?.addEventListener('click', () => { activate(index - 1); start(); });
  next?.addEventListener('click', () => { activate(index + 1); start(); });
  dots.forEach((dot, i) => dot.addEventListener('click', () => { activate(i); start(); }));

  deck.addEventListener('wheel', event => {
    if (wheelLocked || Math.abs(event.deltaY) < 8) return;
    event.preventDefault();
    wheelLocked = true;
    activate(index + (event.deltaY > 0 ? 1 : -1));
    start();
    setTimeout(() => { wheelLocked = false; }, 520);
  }, { passive: false });

  let scrollFrame;
  deck.addEventListener('scroll', () => {
    cancelAnimationFrame(scrollFrame);
    scrollFrame = requestAnimationFrame(() => {
      const center = deck.scrollLeft + deck.clientWidth / 2;
      let closest = 0;
      cards.forEach((card, i) => {
        if (Math.abs(card.offsetLeft + card.clientWidth / 2 - center) < Math.abs(cards[closest].offsetLeft + cards[closest].clientWidth / 2 - center)) closest = i;
      });
      index = closest;
      cards.forEach((card, i) => card.classList.toggle('is-active', i === index));
      dots.forEach((dot, i) => dot.classList.toggle('is-active', i === index));
    });
  });

  deck.addEventListener('mouseenter', stop);
  deck.addEventListener('mouseleave', start);
  deck.addEventListener('focusin', stop);
  deck.addEventListener('focusout', start);
  deck.addEventListener('pointerdown', stop);
  deck.addEventListener('pointerup', start);
  start();
})();
