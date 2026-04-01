/**
 * Scroll-reveal observer — adds `.is-visible` to elements with [data-animate]
 * when they enter the viewport. Respects prefers-reduced-motion.
 */

export function initScrollReveal(): void {
  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (prefersReduced) {
    // Immediately show all animated elements
    document.querySelectorAll<HTMLElement>('[data-animate]').forEach((el) => {
      el.classList.add('is-visible');
    });
    return;
  }

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.1, rootMargin: '0px 0px -40px 0px' },
  );

  document.querySelectorAll('[data-animate]').forEach((el) => {
    observer.observe(el);
  });
}
