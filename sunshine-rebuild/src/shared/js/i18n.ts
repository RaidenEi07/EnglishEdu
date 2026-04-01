/**
 * i18n - Internationalization module
 * Supports: vi (Vietnamese), en (English)
 */
const STORAGE_KEY    = 'sso_language';
const DEFAULT_LANG   = 'vi';
const SUPPORTED_LANGS: readonly string[] = ['vi', 'en'];

let currentLang  = DEFAULT_LANG;
let translations: Record<string, string> = {};

function getSavedLang(): string {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved && SUPPORTED_LANGS.includes(saved) ? saved : DEFAULT_LANG;
  } catch { return DEFAULT_LANG; }
}

function saveLang(lang: string): void {
  try { localStorage.setItem(STORAGE_KEY, lang); } catch { /* noop */ }
}

async function loadTranslations(lang: string): Promise<Record<string, string>> {
  const response = await fetch(`/locales/${lang}.json`);
  if (!response.ok) throw new Error(`Failed to load ${lang}.json`);
  return response.json();
}

function applyTranslations(): void {
  document.querySelectorAll('[data-i18n]').forEach((el) => {
    const key = el.getAttribute('data-i18n');
    if (key && translations[key]) el.textContent = translations[key];
  });
  document.documentElement.lang = currentLang;
}

export async function setLanguage(lang: string): Promise<void> {
  if (!SUPPORTED_LANGS.includes(lang)) return;
  currentLang  = lang;
  saveLang(lang);
  translations = await loadTranslations(lang);
  applyTranslations();
}

export function t(key: string): string           { return translations[key] || key; }
export function getCurrentLang(): string { return currentLang; }

export async function initI18n(): Promise<void> {
  currentLang = getSavedLang();
  try {
    translations = await loadTranslations(currentLang);
    applyTranslations();
  } catch (err) {
    console.warn('i18n: Failed to load translations', err);
  }
  document.querySelectorAll<HTMLElement>('[data-lang]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      const lang = btn.getAttribute('data-lang');
      if (lang) setLanguage(lang);
    });
  });
}
