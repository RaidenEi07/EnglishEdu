/**
 * Learn page – Embedded course viewer
 * URL: /pages/learn/?id=<courseId>
 *
 * Displays Moodle course content inline: sections, modules,
 * assignments, quizzes, resources, pages — all within the app.
 */
import 'bootstrap/dist/css/bootstrap.min.css';
import '../../src/shared/styles/shared.css';
import './learn.css';
import 'bootstrap';

import { initI18n } from '../../src/shared/js/i18n.ts';
import { initNavbar } from '../../src/shared/js/navbar.ts';
import { injectFooter } from '../../src/shared/js/footer.ts';
import { injectNavbar } from '../../src/shared/js/inject-navbar.ts';
import { isLoggedIn } from '../../src/shared/js/auth.ts';
import { apiGet, apiPost, apiUpload } from '../../src/shared/js/api.ts';
import { toast } from '../../src/shared/js/toast.ts';
import type { CourseResponse } from '../../src/shared/js/types.ts';

/* ── Helpers ──────────────────────────────────── */
function show(id: string) { document.getElementById(id)?.classList.remove('d-none'); }
function hide(id: string) { document.getElementById(id)?.classList.add('d-none'); }
function setText(id: string, v: string) { const el = document.getElementById(id); if (el) el.textContent = v; }
function escHtml(s: string | null | undefined): string {
  return String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/* ── State ────────────────────────────────────── */
let courseId: number;
let course: CourseResponse;
let sections: any[] = [];
let completionMap: Record<number, boolean> = {};
let activeModuleId: number | null = null;
let moodleToken: string = '';
let moodleBaseUrl: string = '';

/* ── Module type → icon mapping ───────────────── */
const modIcon: Record<string, string> = {
  resource: 'fa-file-pdf text-warning',
  url:      'fa-link text-secondary',
  assign:   'fa-file-alt text-primary',
  quiz:     'fa-question-circle text-info',
  forum:    'fa-comments text-success',
  page:     'fa-file-lines text-primary',
  label:    'fa-tag text-muted',
  lesson:   'fa-graduation-cap text-purple',
  folder:   'fa-folder text-warning',
  book:     'fa-book text-success',
  choice:   'fa-check-square text-info',
  feedback: 'fa-poll text-info',
  glossary: 'fa-spell-check text-secondary',
  wiki:     'fa-wikipedia-w text-secondary',
  workshop: 'fa-users text-primary',
  h5pactivity: 'fa-play-circle text-info',
};

function getModIcon(modname: string): string {
  return modIcon[modname] || 'fa-circle text-muted';
}

/* ── Transform Moodle file URLs with token for direct access ── */
function proxyFileUrl(url: string): string {
  if (!url) return '';
  if (url.includes('/pluginfile.php/') || url.includes('/webservice/pluginfile.php/')) {
    if (moodleToken) {
      // Direct Moodle access with user token (supports streaming for audio/video)
      let directUrl = url;
      if (!directUrl.includes('/webservice/pluginfile.php/')) {
        directUrl = directUrl.replace('/pluginfile.php/', '/webservice/pluginfile.php/');
      }
      directUrl += (directUrl.includes('?') ? '&' : '?') + 'token=' + moodleToken;
      return directUrl;
    }
    // Fallback to backend proxy if no token available
    return `/api/v1/moodle/file?url=${encodeURIComponent(url)}`;
  }
  return url;
}

/* ── Sidebar rendering ────────────────────────── */
function renderSidebar(): void {
  const container = document.getElementById('sidebarContent')!;
  if (!sections || sections.length === 0) {
    container.innerHTML = '<p class="text-muted text-center py-4">Chưa có nội dung</p>';
    return;
  }

  let html = '';
  for (const sec of sections) {
    const modules = sec.modules || [];
    if (modules.length === 0 && !sec.summary) continue;

    const sectionName = sec.name || `Phần ${sec.section || 0}`;
    html += `<div class="section-group" data-section-id="${sec.id}">
      <div class="section-header d-flex align-items-center" data-section="${sec.id}">
        <i class="fa fa-chevron-down section-toggle me-2 small"></i>
        <span class="flex-grow-1">${escHtml(sectionName)}</span>
        <span class="badge bg-secondary bg-opacity-25 text-dark small">${modules.length}</span>
      </div>
      <div class="section-modules" data-section-body="${sec.id}">`;

    for (const mod of modules) {
      if (mod.modname === 'label') continue; // Skip labels in nav
      const completed = completionMap[mod.id] === true;
      html += `<div class="module-item" data-module-id="${mod.id}" data-modname="${mod.modname}" data-section-id="${sec.id}">
        <span class="module-icon"><i class="fa ${getModIcon(mod.modname)}"></i></span>
        <span class="module-name" title="${escHtml(mod.name)}">${escHtml(mod.name)}</span>
        ${completed ? '<i class="fa fa-check-circle completion-check"></i>' : ''}
      </div>`;
    }

    html += `</div></div>`;
  }
  container.innerHTML = html;

  // Section collapse toggles
  container.querySelectorAll<HTMLElement>('.section-header').forEach(hdr => {
    hdr.addEventListener('click', () => {
      const secId = hdr.getAttribute('data-section');
      const body = container.querySelector(`[data-section-body="${secId}"]`) as HTMLElement;
      if (body) {
        body.classList.toggle('d-none');
        hdr.classList.toggle('collapsed');
      }
    });
  });

  // Module click handlers
  container.querySelectorAll<HTMLElement>('.module-item').forEach(item => {
    item.addEventListener('click', () => {
      const modId = Number(item.getAttribute('data-module-id'));
      const modname = item.getAttribute('data-modname') || '';
      selectModule(modId, modname);
    });
  });
}

/* ── Search/filter modules ────────────────────── */
function initSearch(): void {
  const input = document.getElementById('moduleSearch') as HTMLInputElement;
  input?.addEventListener('input', () => {
    const q = input.value.toLowerCase().trim();
    document.querySelectorAll<HTMLElement>('.module-item').forEach(item => {
      const name = item.querySelector('.module-name')?.textContent?.toLowerCase() || '';
      item.classList.toggle('d-none-search', q.length > 0 && !name.includes(q));
    });
    // Show/hide section headers based on visible children
    document.querySelectorAll<HTMLElement>('.section-group').forEach(group => {
      const visibleModules = group.querySelectorAll('.module-item:not(.d-none-search)');
      const header = group.querySelector('.section-header') as HTMLElement;
      if (header) header.style.display = (q.length > 0 && visibleModules.length === 0) ? 'none' : '';
    });
  });
}

/* ── Module selection & content loading ───────── */
function selectModule(modId: number, modname: string): void {
  // Highlight in sidebar
  document.querySelectorAll('.module-item').forEach(el => el.classList.remove('active'));
  document.querySelector(`.module-item[data-module-id="${modId}"]`)?.classList.add('active');
  activeModuleId = modId;

  // Close mobile sidebar
  document.getElementById('learnSidebar')?.classList.remove('show-mobile');
  document.getElementById('sidebarOverlay')?.classList.add('d-none');

  // Find the module data
  let mod: any = null;
  for (const sec of sections) {
    mod = (sec.modules || []).find((m: any) => m.id === modId);
    if (mod) break;
  }
  if (!mod) {
    showContentError('Không tìm thấy module.');
    return;
  }

  // Route to appropriate renderer
  switch (modname) {
    case 'page':     renderPageModule(mod); break;
    case 'resource':  renderResourceModule(mod); break;
    case 'url':       renderUrlModule(mod); break;
    case 'assign':    renderAssignModule(mod); break;
    case 'quiz':      renderQuizModule(mod); break;
    case 'forum':     renderForumModule(mod); break;
    case 'label':     renderLabelModule(mod); break;
    case 'book':      renderBookModule(mod); break;
    case 'folder':    renderFolderModule(mod); break;
    default:          renderGenericModule(mod); break;
  }
}

function showContentLoading(): void {
  hide('contentWelcome'); hide('contentError'); hide('contentArea');
  show('contentLoading');
}
function showContentError(msg: string): void {
  hide('contentWelcome'); hide('contentLoading'); hide('contentArea');
  setText('contentErrorMsg', msg);
  show('contentError');
}
function showContent(html: string): void {
  hide('contentWelcome'); hide('contentLoading'); hide('contentError');
  document.getElementById('contentBody')!.innerHTML = html;
  show('contentArea');
}

/* ── Module Renderers ─────────────────────────── */

/** Page module: show HTML content inline */
async function renderPageModule(mod: any): Promise<void> {
  showContentLoading();
  try {
    const pages = await apiGet<any>(`/moodle/pages?courseId=${courseId}`);
    const pageList = pages?.pages || [];
    const page = pageList.find((p: any) => p.coursemodule === mod.id);
    if (page) {
      showContent(`
        <h4 class="mb-3">${escHtml(mod.name)}</h4>
        <div class="lesson-text-content">${sanitizeHtml(page.content || '')}</div>
      `);
    } else {
      // Fallback: show from module contents
      const content = mod.contents?.[0];
      if (content?.type === 'content') {
        showContent(`
          <h4 class="mb-3">${escHtml(mod.name)}</h4>
          <div class="lesson-text-content">${sanitizeHtml(mod.description || content.content || '')}</div>
        `);
      } else {
        showContent(`
          <h4 class="mb-3">${escHtml(mod.name)}</h4>
          <div class="lesson-text-content">${sanitizeHtml(mod.description || 'Không có nội dung.')}</div>
        `);
      }
    }
  } catch {
    showContent(`
      <h4 class="mb-3">${escHtml(mod.name)}</h4>
      <div class="lesson-text-content">${sanitizeHtml(mod.description || 'Không có nội dung.')}</div>
    `);
  }
}

/** Resource module: file download / PDF preview */
function renderResourceModule(mod: any): void {
  const files = (mod.contents || []).filter((c: any) => c.type === 'file');
  let html = `<h4 class="mb-3"><i class="fa fa-file-pdf text-warning me-2"></i>${escHtml(mod.name)}</h4>`;

  if (mod.description) {
    html += `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>`;
  }

  if (files.length === 0) {
    html += '<p class="text-muted">Không có tệp nào.</p>';
  } else {
    for (const file of files) {
      const url = proxyFileUrl(file.fileurl);
      const isPdf = file.filename?.toLowerCase().endsWith('.pdf');
      const isImage = /\.(jpg|jpeg|png|gif|svg|webp)$/i.test(file.filename || '');
      const isAudio = /\.(mp3|wav|ogg|m4a|aac|flac)$/i.test(file.filename || '');
      const isVideo = /\.(mp4|webm|ogv|mov)$/i.test(file.filename || '');
      const isDoc = /\.(doc|docx)$/i.test(file.filename || '');
      const isPpt = /\.(ppt|pptx)$/i.test(file.filename || '');

      const fileIcon = isPdf ? 'fa-file-pdf text-danger'
        : isImage ? 'fa-image text-success'
        : isAudio ? 'fa-music text-info'
        : isVideo ? 'fa-video text-primary'
        : isDoc ? 'fa-file-word text-primary'
        : isPpt ? 'fa-file-powerpoint text-warning'
        : 'fa-file text-secondary';

      html += `<div class="resource-card card mb-3">
        <div class="card-body">
          <div class="d-flex align-items-center mb-2">
            <i class="fa ${fileIcon} fa-lg me-2"></i>
            <div>
              <strong>${escHtml(file.filename)}</strong>
              ${file.filesize ? `<small class="text-muted ms-2">(${formatFileSize(file.filesize)})</small>` : ''}
            </div>
          </div>`;

      if (isPdf) {
        html += `<iframe src="${url}" class="pdf-viewer mb-2"></iframe>`;
      } else if (isImage) {
        html += `<img src="${url}" alt="${escHtml(file.filename)}" class="img-fluid rounded mb-2" style="max-height:500px">`;
      } else if (isAudio) {
        html += `<audio controls class="w-100 mb-2"><source src="${url}">Trình duyệt không hỗ trợ phát audio.</audio>`;
      } else if (isVideo) {
        html += `<video controls class="w-100 mb-2" style="max-height:500px"><source src="${url}">Trình duyệt không hỗ trợ phát video.</video>`;
      }

      html += `<a href="${url}" class="btn btn-sm btn-outline-primary" download="${escHtml(file.filename)}">
          <i class="fa fa-download me-1"></i>Tải xuống
        </a>
        </div></div>`;
    }
  }
  showContent(html);
}

/** URL module */
function renderUrlModule(mod: any): void {
  const urlContent = mod.contents?.[0];
  const url = urlContent?.fileurl || '';
  showContent(`
    <h4 class="mb-3"><i class="fa fa-link text-secondary me-2"></i>${escHtml(mod.name)}</h4>
    ${mod.description ? `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>` : ''}
    <a href="${escHtml(url)}" target="_blank" rel="noopener noreferrer" class="btn btn-primary">
      <i class="fa fa-external-link-alt me-1"></i>Mở liên kết
    </a>
  `);
}

/** Label module */
function renderLabelModule(mod: any): void {
  showContent(`
    <div class="lesson-text-content">${sanitizeHtml(mod.description || '')}</div>
  `);
}

/** Book module */
function renderBookModule(mod: any): void {
  const chapters = (mod.contents || []).filter((c: any) => c.type === 'content' || c.type === 'file');
  let html = `<h4 class="mb-3"><i class="fa fa-book text-success me-2"></i>${escHtml(mod.name)}</h4>`;
  if (mod.description) {
    html += `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>`;
  }
  if (chapters.length > 0) {
    for (const ch of chapters) {
      if (ch.content) {
        html += `<div class="lesson-text-content mb-3">${sanitizeHtml(ch.content)}</div>`;
      } else if (ch.fileurl) {
        html += `<p><a href="${proxyFileUrl(ch.fileurl)}" target="_blank">${escHtml(ch.filename || 'Tệp')}</a></p>`;
      }
    }
  }
  showContent(html);
}

/** Folder module */
function renderFolderModule(mod: any): void {
  const files = mod.contents || [];
  let html = `<h4 class="mb-3"><i class="fa fa-folder text-warning me-2"></i>${escHtml(mod.name)}</h4>`;
  if (mod.description) {
    html += `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>`;
  }
  if (files.length === 0) {
    html += '<p class="text-muted">Thư mục trống.</p>';
  } else {
    html += '<div class="list-group">';
    for (const f of files) {
      html += `<a href="${proxyFileUrl(f.fileurl)}" class="list-group-item list-group-item-action d-flex align-items-center" download>
        <i class="fa fa-file me-2 text-muted"></i>
        <span>${escHtml(f.filename)}</span>
        ${f.filesize ? `<small class="text-muted ms-auto">${formatFileSize(f.filesize)}</small>` : ''}
      </a>`;
    }
    html += '</div>';
  }
  showContent(html);
}

/** Forum module: link to Moodle forum */
function renderForumModule(mod: any): void {
  showContent(`
    <h4 class="mb-3"><i class="fa fa-comments text-success me-2"></i>${escHtml(mod.name)}</h4>
    ${mod.description ? `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>` : ''}
    <div class="alert alert-info">
      <i class="fa fa-info-circle me-2"></i>
      Diễn đàn thảo luận được quản lý trên Moodle.
    </div>
  `);
}

/** Generic fallback module */
function renderGenericModule(mod: any): void {
  let html = `<h4 class="mb-3">${escHtml(mod.name)}</h4>`;
  if (mod.description) {
    html += `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>`;
  }
  const files = (mod.contents || []).filter((c: any) => c.type === 'file');
  if (files.length > 0) {
    html += '<h6 class="mt-3">Tệp đính kèm:</h6><div class="list-group">';
    for (const f of files) {
      html += `<a href="${proxyFileUrl(f.fileurl)}" class="list-group-item list-group-item-action" download>
        <i class="fa fa-file me-2"></i>${escHtml(f.filename)}
      </a>`;
    }
    html += '</div>';
  }
  showContent(html);
}

/* ── Assignment Renderer ──────────────────────── */
async function renderAssignModule(mod: any): Promise<void> {
  showContentLoading();
  try {
    const statusRes = await apiGet<any>(`/moodle/assignment-status?assignmentId=${mod.instance}`);
    const lastAttempt = statusRes?.lastattempt;
    const submission = lastAttempt?.submission;
    const submissionStatus = submission?.status || 'new';
    const grading = lastAttempt?.gradingstatus || '';
    const feedback = statusRes?.feedback;

    let html = `
      <div class="assign-card">
        <div class="assign-header">
          <h4 class="mb-1"><i class="fa fa-file-alt me-2"></i>${escHtml(mod.name)}</h4>
          <div class="d-flex gap-3 small mt-2">
            <span><i class="fa fa-info-circle me-1"></i>Trạng thái: ${getSubmissionStatusLabel(submissionStatus)}</span>
            ${grading ? `<span><i class="fa fa-check me-1"></i>Chấm điểm: ${escHtml(grading)}</span>` : ''}
          </div>
        </div>
        <div class="card-body">`;

    // Description
    if (mod.description) {
      html += `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div><hr>`;
    }

    // Feedback (if graded)
    if (feedback?.grade) {
      html += `<div class="alert alert-success">
        <strong>Điểm: ${feedback.grade.grade ?? '—'} / ${feedback.grade.maxgrade ?? '—'}</strong>
        ${feedback.gradefordisplay ? ` (${escHtml(feedback.gradefordisplay)})` : ''}
      </div>`;
    }
    if (feedback?.plugins) {
      for (const plugin of feedback.plugins) {
        if (plugin.editorfields) {
          for (const field of plugin.editorfields) {
            if (field.text) {
              html += `<div class="alert alert-info"><strong>Nhận xét:</strong> ${sanitizeHtml(field.text)}</div>`;
            }
          }
        }
      }
    }

    // Previous submission content
    if (submission?.plugins) {
      for (const plugin of submission.plugins) {
        if (plugin.type === 'onlinetext' && plugin.editorfields) {
          for (const field of plugin.editorfields) {
            if (field.text) {
              html += `<div class="card mb-3"><div class="card-header small fw-medium">Bài nộp hiện tại</div>
                <div class="card-body lesson-text-content">${sanitizeHtml(field.text)}</div></div>`;
            }
          }
        }
        if (plugin.type === 'file' && plugin.fileareas) {
          for (const area of plugin.fileareas) {
            if (area.files && area.files.length > 0) {
              html += '<div class="card mb-3"><div class="card-header small fw-medium">Tệp đã nộp</div><div class="card-body">';
              for (const f of area.files) {
                html += `<a href="${proxyFileUrl(f.fileurl)}" class="d-block mb-1"><i class="fa fa-file me-1"></i>${escHtml(f.filename)}</a>`;
              }
              html += '</div></div>';
            }
          }
        }
      }
    }

    // Submission form (if not submitted or can resubmit)
    if (submissionStatus !== 'submitted' || lastAttempt?.canedit) {
      html += `
        <div class="assign-submission-area mt-3">
          <h6><i class="fa fa-edit me-1"></i>Nộp bài</h6>

          <!-- Tab: text / file -->
          <ul class="nav nav-tabs mb-3" role="tablist">
            <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#assignText">Văn bản</a></li>
            <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#assignFile">Tệp đính kèm</a></li>
          </ul>
          <div class="tab-content">
            <div class="tab-pane fade show active" id="assignText">
              <textarea id="assignTextInput" class="form-control mb-2" rows="8"
                placeholder="Nhập nội dung bài làm..."></textarea>
              <button id="assignTextSubmitBtn" class="btn btn-primary" data-assign-id="${mod.instance}">
                <i class="fa fa-paper-plane me-1"></i>Lưu bài nộp
              </button>
            </div>
            <div class="tab-pane fade" id="assignFile">
              <input type="file" id="assignFileInput" class="form-control mb-2">
              <button id="assignFileSubmitBtn" class="btn btn-primary" data-assign-id="${mod.instance}">
                <i class="fa fa-upload me-1"></i>Tải tệp lên & nộp
              </button>
            </div>
          </div>

          <hr>
          <button id="assignFinalSubmitBtn" class="btn btn-success" data-assign-id="${mod.instance}">
            <i class="fa fa-check-circle me-1"></i>Xác nhận nộp bài chấm điểm
          </button>
        </div>`;
    }

    html += '</div></div>';
    showContent(html);

    // Bind assignment submission handlers
    bindAssignmentHandlers(mod.instance);
  } catch {
    showContent(`
      <h4 class="mb-3"><i class="fa fa-file-alt text-primary me-2"></i>${escHtml(mod.name)}</h4>
      ${mod.description ? `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>` : ''}
      <div class="alert alert-warning">Không thể tải trạng thái bài nộp.</div>
    `);
  }
}

function bindAssignmentHandlers(assignId: number): void {
  // Text submission
  document.getElementById('assignTextSubmitBtn')?.addEventListener('click', async (e) => {
    const btn = e.currentTarget as HTMLButtonElement;
    const text = (document.getElementById('assignTextInput') as HTMLTextAreaElement)?.value;
    if (!text?.trim()) { toast.error('Vui lòng nhập nội dung.'); return; }
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
    try {
      await apiPost('/moodle/assignment-submit-text', { assignmentId: assignId, text, itemId: 0 });
      toast.success('Đã lưu bài nộp!');
    } catch (err: any) {
      toast.error(err.message || 'Lưu thất bại.');
    }
    btn.disabled = false;
    btn.innerHTML = '<i class="fa fa-paper-plane me-1"></i>Lưu bài nộp';
  });

  // File submission
  document.getElementById('assignFileSubmitBtn')?.addEventListener('click', async (e) => {
    const btn = e.currentTarget as HTMLButtonElement;
    const fileInput = document.getElementById('assignFileInput') as HTMLInputElement;
    const file = fileInput?.files?.[0];
    if (!file) { toast.error('Vui lòng chọn tệp.'); return; }
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang tải lên...';
    try {
      const fd = new FormData();
      fd.append('file', file);
      fd.append('assignmentId', String(assignId));
      await apiUpload('/moodle/assignment-submit-file?assignmentId=' + assignId, fd);
      toast.success('Đã tải tệp và lưu bài nộp!');
    } catch (err: any) {
      toast.error(err.message || 'Tải tệp thất bại.');
    }
    btn.disabled = false;
    btn.innerHTML = '<i class="fa fa-upload me-1"></i>Tải tệp lên & nộp';
  });

  // Final submit for grading
  document.getElementById('assignFinalSubmitBtn')?.addEventListener('click', async (e) => {
    const btn = e.currentTarget as HTMLButtonElement;
    if (!confirm('Xác nhận nộp bài để chấm điểm? Bạn có thể không chỉnh sửa được nữa.')) return;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang nộp...';
    try {
      await apiPost(`/moodle/assignment-submit-grading?assignmentId=${assignId}`, {});
      toast.success('Đã nộp bài chấm điểm!');
      // Refresh the assignment view
      const mod = findModuleByInstance(assignId, 'assign');
      if (mod) renderAssignModule(mod);
    } catch (err: any) {
      toast.error(err.message || 'Nộp bài thất bại.');
      btn.disabled = false;
      btn.innerHTML = '<i class="fa fa-check-circle me-1"></i>Xác nhận nộp bài chấm điểm';
    }
  });
}

function getSubmissionStatusLabel(status: string): string {
  const map: Record<string, string> = {
    new: 'Chưa nộp',
    draft: 'Bản nháp',
    submitted: 'Đã nộp',
    reopened: 'Mở lại',
  };
  return map[status] || status;
}

/* ── Quiz Renderer ────────────────────────────── */
async function renderQuizModule(mod: any): Promise<void> {
  showContentLoading();
  try {
    const userRaw = localStorage.getItem('sso_user');
    const userRole = userRaw ? (JSON.parse(userRaw)?.role || '') : '';
    const isTeacherOrAdmin = userRole === 'TEACHER' || userRole === 'ADMIN';

    let html = `
      <div class="quiz-container">
        <h4 class="mb-3"><i class="fa fa-question-circle text-info me-2"></i>${escHtml(mod.name)}</h4>
        ${mod.description ? `<div class="lesson-text-content mb-3">${sanitizeHtml(mod.description)}</div>` : ''}`;

    if (isTeacherOrAdmin) {
      // Teachers and admins view quiz info only — no attempt buttons
      html += `<div class="alert alert-info">
        <i class="fa fa-info-circle me-2"></i>Bài kiểm tra dành cho học sinh. Giáo viên và quản trị viên không thể làm bài kiểm tra.
      </div>`;
      html += '</div>';
      showContent(html);
    } else {
      // Get quiz attempts for students
      const attemptsRes = await apiGet<any>(`/moodle/quiz/attempts?quizId=${mod.instance}`);
      const attempts = attemptsRes?.attempts || [];
      const inProgressAttempt = attempts.find((a: any) => a.state === 'inprogress');

      // Previous attempts
      if (attempts.length > 0) {
        html += `<h6 class="mt-3">Lần thử trước:</h6>
          <table class="table table-sm table-bordered">
            <thead><tr><th>#</th><th>Trạng thái</th><th>Điểm</th><th>Thời gian</th><th></th></tr></thead><tbody>`;
        for (const a of attempts) {
          const state = a.state === 'finished' ? 'Hoàn thành' : a.state === 'inprogress' ? 'Đang làm' : a.state;
          const grade = a.sumgrades != null ? `${Number(a.sumgrades).toFixed(1)}` : '—';
          const time = a.timefinish ? new Date(a.timefinish * 1000).toLocaleString('vi-VN') : '—';
          html += `<tr>
            <td>${a.attempt}</td>
            <td>${escHtml(state)}</td>
            <td>${grade}</td>
            <td class="small">${time}</td>
            <td>
              ${a.state === 'finished' ? `<button class="btn btn-sm btn-outline-info quiz-review-btn" data-attempt-id="${a.id}">Xem lại</button>` : ''}
              ${a.state === 'inprogress' ? `<button class="btn btn-sm btn-primary quiz-continue-btn" data-attempt-id="${a.id}">Tiếp tục</button>` : ''}
            </td>
          </tr>`;
        }
        html += '</tbody></table>';
      }

      // Actions
      if (inProgressAttempt) {
        html += `<button id="quizContinueBtn" class="btn btn-primary" data-attempt-id="${inProgressAttempt.id}">
          <i class="fa fa-play me-1"></i>Tiếp tục làm bài
        </button>`;
      } else {
        html += `<button id="quizStartBtn" class="btn btn-success" data-quiz-id="${mod.instance}">
          <i class="fa fa-play-circle me-1"></i>Bắt đầu làm bài
        </button>`;
      }

      html += '</div>';
      showContent(html);

      // Bind handlers
      document.getElementById('quizStartBtn')?.addEventListener('click', async () => {
        await startQuiz(mod.instance, mod.name);
      });
      document.getElementById('quizContinueBtn')?.addEventListener('click', async () => {
        const attemptId = Number((document.getElementById('quizContinueBtn') as HTMLElement).getAttribute('data-attempt-id'));
        await loadQuizPage(attemptId, 0, mod.name);
      });
      document.querySelectorAll('.quiz-review-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
          const attemptId = Number(btn.getAttribute('data-attempt-id'));
          await reviewQuiz(attemptId, mod.name);
        });
      });
      document.querySelectorAll('.quiz-continue-btn').forEach(btn => {
        btn.addEventListener('click', async () => {
          const attemptId = Number(btn.getAttribute('data-attempt-id'));
          await loadQuizPage(attemptId, 0, mod.name);
        });
      });
    }
  } catch {
    showContent(`
      <h4 class="mb-3">${escHtml(mod.name)}</h4>
      <div class="alert alert-warning">Không thể tải thông tin bài kiểm tra.</div>
    `);
  }
}

async function startQuiz(quizId: number, quizName: string): Promise<void> {
  showContentLoading();
  try {
    const res = await apiPost<any>(`/moodle/quiz/start?quizId=${quizId}`, {});
    const attemptId = res?.attempt?.id;
    if (!attemptId) throw new Error('No attempt created');
    await loadQuizPage(attemptId, 0, quizName);
  } catch (err: any) {
    // If attempt already in progress, fetch attempts and continue the existing one
    if (err.message && err.message.toLowerCase().includes('in progress')) {
      try {
        const mod = findModuleByQuizId(quizId, 'quiz');
        if (mod) {
          toast.info('Đã có bài đang làm dở, tiếp tục...');
          await renderQuizModule(mod);
          return;
        }
      } catch { /* fall through */ }
    }
    showContentError(err.message || 'Không thể bắt đầu bài kiểm tra.');
  }
}

async function loadQuizPage(attemptId: number, page: number, quizName: string): Promise<void> {
  showContentLoading();
  try {
    const data = await apiGet<any>(`/moodle/quiz/attempt-data?attemptId=${attemptId}&page=${page}`);
    const questions = data?.questions || [];
    const nextPage = data?.nextpage ?? -1;

    let html = `
      <div class="quiz-container">
        <div class="d-flex align-items-center justify-content-between mb-3">
          <h4 class="mb-0">${escHtml(quizName)}</h4>
          <span class="badge bg-info">Trang ${page + 1}</span>
        </div>
        <form id="quizForm">`;

    for (const q of questions) {
      html += `<div class="quiz-question">
        <div class="question-html">${sanitizeHtml(q.html || '')}</div>
      </div>`;
    }

    html += `<div class="d-flex gap-2 mt-3">`;
    if (page > 0) {
      html += `<button type="button" id="quizPrevBtn" class="btn btn-outline-secondary" data-page="${page - 1}">
        <i class="fa fa-arrow-left me-1"></i>Trang trước
      </button>`;
    }
    if (nextPage >= 0) {
      html += `<button type="button" id="quizNextBtn" class="btn btn-primary" data-page="${nextPage}">
        Trang tiếp<i class="fa fa-arrow-right ms-1"></i>
      </button>`;
    }
    html += `<button type="button" id="quizSubmitBtn" class="btn btn-success ms-auto">
        <i class="fa fa-check me-1"></i>Nộp bài
      </button>`;
    html += '</div></form></div>';

    showContent(html);

    // Bind quiz navigation
    document.getElementById('quizPrevBtn')?.addEventListener('click', async () => {
      const answers = collectQuizAnswers();
      await apiPost('/moodle/quiz/save', { attemptId, answers });
      await loadQuizPage(attemptId, page - 1, quizName);
    });
    document.getElementById('quizNextBtn')?.addEventListener('click', async () => {
      const answers = collectQuizAnswers();
      await apiPost('/moodle/quiz/save', { attemptId, answers });
      await loadQuizPage(attemptId, nextPage, quizName);
    });
    document.getElementById('quizSubmitBtn')?.addEventListener('click', async () => {
      if (!confirm('Bạn có chắc muốn nộp bài? Sau khi nộp sẽ không thể chỉnh sửa.')) return;
      const answers = collectQuizAnswers();
      showContentLoading();
      try {
        await apiPost('/moodle/quiz/submit', { attemptId, answers });
        toast.success('Đã nộp bài kiểm tra!');
        await reviewQuiz(attemptId, quizName);
      } catch (err: any) {
        showContentError(err.message || 'Nộp bài thất bại.');
      }
    });
  } catch (err: any) {
    showContentError(err.message || 'Không thể tải câu hỏi.');
  }
}

function collectQuizAnswers(): Record<string, string> {
  const answers: Record<string, string> = {};
  const form = document.getElementById('quizForm');
  if (!form) return answers;

  // Collect all form inputs within quiz questions (Moodle renders HTML with input fields)
  form.querySelectorAll<HTMLInputElement>('input[name], select[name], textarea[name]').forEach(el => {
    if (el.type === 'radio') {
      if (el.checked) answers[el.name] = el.value;
    } else if (el.type === 'checkbox') {
      if (el.checked) answers[el.name] = el.value;
    } else {
      answers[el.name] = el.value;
    }
  });
  form.querySelectorAll<HTMLSelectElement>('select[name]').forEach(el => {
    answers[el.name] = el.value;
  });
  form.querySelectorAll<HTMLTextAreaElement>('textarea[name]').forEach(el => {
    answers[el.name] = el.value;
  });
  return answers;
}

async function reviewQuiz(attemptId: number, quizName: string): Promise<void> {
  showContentLoading();
  try {
    const review = await apiGet<any>(`/moodle/quiz/review?attemptId=${attemptId}`);
    const questions = review?.questions || [];
    const grade = review?.grade;

    let html = `
      <div class="quiz-container quiz-review">
        <h4 class="mb-2">${escHtml(quizName)} — Kết quả</h4>
        ${grade != null ? `<div class="alert alert-info"><strong>Điểm: ${Number(grade).toFixed(1)}</strong></div>` : ''}`;

    for (const q of questions) {
      const stateClass = q.state === 'gradedright' ? 'correct' : q.state === 'gradedwrong' ? 'incorrect' : 'partial';
      html += `<div class="quiz-question ${stateClass}">
        <div class="question-html">${sanitizeHtml(q.html || '')}</div>
      </div>`;
    }

    html += `<a href="javascript:void(0)" id="quizBackBtn" class="btn btn-outline-primary mt-3">
      <i class="fa fa-arrow-left me-1"></i>Quay lại
    </a></div>`;

    showContent(html);
    document.getElementById('quizBackBtn')?.addEventListener('click', () => {
      // Re-render quiz module overview
      const mod = findModuleByInstance(review?.attempt?.quiz, 'quiz');
      if (mod) renderQuizModule(mod);
    });
  } catch {
    showContentError('Không thể tải kết quả kiểm tra.');
  }
}

/* ── Utility ──────────────────────────────────── */
function findModuleByInstance(instanceId: number, modname: string): any {
  for (const sec of sections) {
    for (const mod of (sec.modules || [])) {
      if (mod.instance === instanceId && mod.modname === modname) return mod;
    }
  }
  return null;
}

function findModuleByQuizId(quizId: number, modname: string): any {
  // quizId is the Moodle quiz instance id (same as mod.instance for quiz modules)
  return findModuleByInstance(quizId, modname);
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
}

function sanitizeHtml(html: string): string {
  // Rewrite Moodle pluginfile URLs to use direct token-based access
  return html.replace(
    /(?:https?:\/\/[^"'\s]*\/(?:webservice\/)?pluginfile\.php\/[^"'\s]*)/g,
    (match) => proxyFileUrl(match)
  );
}

/* ── Sidebar mobile toggle ────────────────────── */
function initSidebarToggle(): void {
  const toggle = document.getElementById('sidebarToggle');
  const sidebar = document.getElementById('learnSidebar');
  const overlay = document.getElementById('sidebarOverlay');

  toggle?.addEventListener('click', () => {
    sidebar?.classList.toggle('show-mobile');
    overlay?.classList.toggle('d-none');
  });
  overlay?.addEventListener('click', () => {
    sidebar?.classList.remove('show-mobile');
    overlay?.classList.add('d-none');
  });
}

/* ── Completion loading ───────────────────────── */
async function loadCompletion(): Promise<void> {
  try {
    const res = await apiGet<any>(`/moodle/completion?courseId=${courseId}`);
    const statuses = res?.statuses || [];
    for (const s of statuses) {
      if (s.state === 1 || s.state === 2) { // 1=complete, 2=complete_pass
        completionMap[s.cmid] = true;
      }
    }
  } catch {
    // Non-critical — skip
  }
}

/* ── Init ─────────────────────────────────────── */
async function initLearnPage(): Promise<void> {
  const params = new URLSearchParams(window.location.search);
  const idStr = params.get('id');

  if (!idStr || isNaN(Number(idStr))) {
    hide('learnLoading');
    show('learnError');
    setText('learnErrorMsg', 'Không tìm thấy ID khóa học trong URL.');
    return;
  }

  if (!isLoggedIn()) {
    window.location.replace(`/pages/login/?redirect=${encodeURIComponent(window.location.href)}`);
    return;
  }

  courseId = Number(idStr);

  try {
    // Load course info
    course = await apiGet<CourseResponse>(`/courses/${courseId}`);
    setText('courseTitle', course.name);
    document.title = `${course.name} | Học bài`;

    // Set Moodle direct link
    if (course.moodleCourseId) {
      try {
        const launch = await apiGet<{ url: string }>(`/moodle/launch?courseId=${courseId}`);
        const link = document.getElementById('moodleDirectLink') as HTMLAnchorElement;
        if (link && launch.url) {
          link.href = launch.url;
          link.classList.remove('d-none');
        }
      } catch { /* ignore */ }
    }

    // Fetch Moodle user token for direct file access (audio, PDF, etc.)
    try {
      const tokenRes = await apiGet<{ token: string; baseUrl: string }>('/moodle/user-token');
      moodleToken = tokenRes.token;
      moodleBaseUrl = tokenRes.baseUrl;
    } catch { /* fallback to proxy */ }

    // Load course contents from Moodle
    sections = await apiGet<any[]>(`/moodle/course-contents?courseId=${courseId}`) || [];

    // Load completion status
    await loadCompletion();

    // Render
    hide('learnLoading');
    show('learnContainer');
    renderSidebar();
    initSearch();
    initSidebarToggle();

    // Auto-select module from URL hash
    const hash = window.location.hash;
    if (hash && hash.startsWith('#mod-')) {
      const modId = Number(hash.replace('#mod-', ''));
      if (!isNaN(modId)) {
        const modEl = document.querySelector(`.module-item[data-module-id="${modId}"]`) as HTMLElement;
        if (modEl) {
          const modname = modEl.getAttribute('data-modname') || '';
          selectModule(modId, modname);
        }
      }
    }
  } catch (err: any) {
    hide('learnLoading');
    show('learnError');
    setText('learnErrorMsg', err.message || 'Không thể tải khóa học.');
  }
}

function initApp(): void {
  initI18n();
  injectNavbar();
  injectFooter();
  initNavbar();
  initLearnPage();
}

function boot(): void { initApp(); document.body.classList.add('ready'); }
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}
