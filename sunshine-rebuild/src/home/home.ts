/**
 * Home page entry point
 */

/* --- Styles --- */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../shared/styles/shared.css';
import './home.css';

/* --- Bootstrap JS --- */
import * as bootstrap from 'bootstrap';

/* --- Shared modules --- */
import { initI18n } from '../shared/js/i18n.ts';
import { injectNavbar } from '../shared/js/inject-navbar.ts';
import { injectFooter } from '../shared/js/footer.ts';
import { initNavbar } from '../shared/js/navbar.ts';

/* --- Home-specific modules --- */
import { initCourses } from './courses.ts';
import { initScrollReveal } from '../shared/js/scroll-reveal.ts';

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initCourses();
  initScrollReveal();

  // Navbar scroll effect
  const navbar = document.querySelector('nav.navbar');
  if (navbar) {
    window.addEventListener('scroll', () => {
      navbar.classList.toggle('scrolled', window.scrollY > 50);
    });
  }

  // Hero carousel auto-play
  const heroEl = document.getElementById('mainCarousel');
  if (heroEl) {
    new bootstrap.Carousel(heroEl, { interval: 5000, ride: 'carousel' });
  }

  // Smooth scroll for anchor links
  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener('click', (e) => {
      const id = link.getAttribute('href');
      if (!id || id === '#') return;
      const target = document.querySelector(id);
      if (target) {
        e.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  });
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
