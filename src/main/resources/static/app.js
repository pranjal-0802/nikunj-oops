/**
 * ============================================================================
 * Nimbus FinServe :: Intelligent IT Service Management (I-ITSM)
 * Modern Single-Page Application Client (Linear / Vercel Aesthetic)
 * Self-Contained Vanilla ES6+ (Zero External CDN Dependencies, CSP Compliant)
 * ============================================================================
 */

// Application Global State
const STATE = {
  currentUser: null,
  demoMode: false,
  demoPassword: 'Password123!',
  activeTab: 'console-employee',
  activeAdminSubtab: 'admin-catalog-panel',
  simulation: {
    virtualTime: null,
    speedMultiplier: 1
  },
  clockTimer: null,
  notificationsTimer: null
};

// ============================================================================
// 1. Toast Notification System
// ============================================================================
function showToast(message, type = 'info', duration = 3500) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let iconSvg = '';
  if (type === 'success') {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="icon text-emerald"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>`;
  } else if (type === 'error') {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="icon text-red"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>`;
  } else if (type === 'warning') {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="icon text-amber"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>`;
  } else {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="icon text-cyan"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`;
  }

  toast.innerHTML = `
    <div style="flex-shrink: 0;">${iconSvg}</div>
    <div style="flex: 1; font-size: 0.8125rem; line-height: 1.4;">${escapeHtml(message)}</div>
    <button type="button" style="background: none; border: none; color: #94a3b8; cursor: pointer; font-size: 1rem; line-height: 1;">&times;</button>
  `;

  toast.querySelector('button').addEventListener('click', () => toast.remove());
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

// Utility: HTML Escaping
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// ============================================================================
// 2. HTTP Client with Automatic JWT Handling
// ============================================================================
async function apiCall(endpoint, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (STATE.currentUser && STATE.currentUser.token) {
    headers['Authorization'] = `Bearer ${STATE.currentUser.token}`;
  }

  try {
    const res = await fetch(endpoint, { ...options, headers });
    if (res.status === 401) {
      console.warn('Authentication token expired or unauthorized.');
      handleSignOut(false);
      showToast('Session expired. Please sign in again.', 'warning');
      throw new Error('Unauthorized');
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText || 'Server Error' }));
      throw new Error(err.message || `Request failed with status ${res.status}`);
    }
    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await res.json();
    }
    return null;
  } catch (err) {
    if (err.message !== 'Unauthorized') {
      console.error(`API Error on ${endpoint}:`, err);
    }
    throw err;
  }
}

// ============================================================================
// 3. Application Initialization & Authentication Flow
// ============================================================================
document.addEventListener('DOMContentLoaded', async () => {
  setupEventListeners();
  await loadPublicConfig();
  checkExistingSession();
});

async function loadPublicConfig() {
  try {
    const config = await fetch('/api/public/config').then(r => r.json());
    STATE.demoMode = !!config.demoMode;
    if (STATE.demoMode) {
      document.getElementById('demo-accounts-container').classList.remove('hidden');
      document.getElementById('persona-switcher-bar').classList.remove('hidden');
    } else {
      document.getElementById('demo-accounts-container').classList.add('hidden');
      document.getElementById('persona-switcher-bar').classList.add('hidden');
    }
  } catch (err) {
    console.warn('Failed to load /api/public/config, defaulting demoMode to false');
  }
}

function checkExistingSession() {
  const stored = sessionStorage.getItem('iitsm_session');
  if (stored) {
    try {
      const user = JSON.parse(stored);
      if (user && user.token) {
        setAuthenticatedUser(user);
        return;
      }
    } catch (e) {
      sessionStorage.removeItem('iitsm_session');
    }
  }
  showAuthView();
}

function showAuthView() {
  document.getElementById('auth-view').classList.remove('hidden');
  document.getElementById('app-view').classList.add('hidden');
  stopBackgroundPolling();
}

function showAppView() {
  document.getElementById('auth-view').classList.add('hidden');
  document.getElementById('app-view').classList.remove('hidden');
  startBackgroundPolling();
}

function setAuthenticatedUser(user, switchDefaultTab = true) {
  STATE.currentUser = user;
  sessionStorage.setItem('iitsm_session', JSON.stringify(user));

  // Update navbar user profile
  document.getElementById('user-display-name').textContent = user.name || user.email;
  document.getElementById('user-display-role').textContent = user.role || 'ROLE_USER';
  document.getElementById('user-avatar-badge').textContent = (user.name ? user.name.charAt(0) : 'U').toUpperCase();

  // Enforce persona isolation: Show ONLY the console(s) belonging to this specific user role
  updateNavTabsForRole(user.role);

  showAppView();

  // Switch natural starting tab based on role if requested
  if (switchDefaultTab) {
    if (user.role === 'ROLE_EMPLOYEE') {
      switchTab('console-employee');
    } else if (user.role === 'ROLE_TECHNICIAN') {
      switchTab('console-technician');
    } else if (user.role === 'ROLE_TEAM_LEAD') {
      switchTab('console-teamlead');
    } else if (user.role === 'ROLE_ADMIN') {
      switchTab('console-admin');
    } else {
      switchTab('console-employee');
    }
  }

  showToast(`Signed in: ${user.name || user.email} (${user.role})`, 'info');
}

function updateNavTabsForRole(role) {
  const tabs = document.querySelectorAll('.nav-tab');
  tabs.forEach(tab => {
    const target = tab.getAttribute('data-target');
    let visible = false;
    if (role === 'ROLE_EMPLOYEE') {
      visible = (target === 'console-employee');
    } else if (role === 'ROLE_TECHNICIAN') {
      visible = (target === 'console-technician');
    } else if (role === 'ROLE_TEAM_LEAD') {
      visible = (target === 'console-technician' || target === 'console-teamlead');
    } else if (role === 'ROLE_ADMIN') {
      visible = (target === 'console-admin' || target === 'console-simulation' || target === 'console-reports');
    }
    if (visible) {
      tab.classList.remove('hidden');
      tab.style.display = 'inline-flex';
    } else {
      tab.classList.add('hidden');
      tab.style.display = 'none';
    }
  });

  const personaBar = document.getElementById('persona-switcher-bar');
  if (personaBar) {
    personaBar.classList.add('hidden');
    personaBar.style.display = 'none';
  }
}

async function loginUser(email, password, switchDefaultTab = true) {
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.trim(), password: password })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Invalid credentials or account locked' }));
      throw new Error(err.message || 'Login failed');
    }
    const data = await res.json();
    setAuthenticatedUser({
      token: data.token,
      id: data.id,
      name: data.name,
      email: data.email,
      role: data.role
    }, switchDefaultTab);
  } catch (err) {
    showToast(err.message || 'Authentication error', 'error');
  }
}

function handleSignOut(notify = true) {
  STATE.currentUser = null;
  sessionStorage.removeItem('iitsm_session');
  showAuthView();
  if (notify) showToast('Signed out successfully.', 'info');
}

// ============================================================================
// 4. Tab Navigation & View Management
// ============================================================================
async function switchTab(targetId) {
  document.querySelectorAll('.nav-tab').forEach(tab => {
    if (tab.getAttribute('data-target') === targetId) {
      tab.classList.add('active');
    } else {
      tab.classList.remove('active');
    }
  });

  document.querySelectorAll('.tab-panel').forEach(panel => {
    if (panel.id === targetId) {
      panel.classList.add('active');
    } else {
      panel.classList.remove('active');
    }
  });

  STATE.activeTab = targetId;
  refreshActiveTab();
}
  });

  STATE.activeTab = targetId;
  refreshActiveTab();
}

function refreshActiveTab() {
  if (!STATE.currentUser) return;
  switch (STATE.activeTab) {
    case 'console-employee':
      loadEmployeeTickets();
      break;
    case 'console-technician':
      loadTechnicianQueue();
      loadTechnicianDashboard();
      break;
    case 'console-teamlead':
      loadTeamLeadView();
      break;
    case 'console-admin':
      loadAdminView();
      break;
    case 'console-simulation':
      loadSimulationStatus();
      break;
    case 'console-reports':
      loadExecutiveReports();
      break;
  }
}

// ============================================================================
// 5. Background Polling (Simulation Clock & Notifications)
// ============================================================================
function startBackgroundPolling() {
  stopBackgroundPolling();
  updateSimulationClock();
  updateNotifications();
  STATE.clockTimer = setInterval(updateSimulationClock, 2500);
  STATE.notificationsTimer = setInterval(updateNotifications, 10000);
}

function stopBackgroundPolling() {
  if (STATE.clockTimer) clearInterval(STATE.clockTimer);
  if (STATE.notificationsTimer) clearInterval(STATE.notificationsTimer);
}

async function updateSimulationClock() {
  try {
    const res = await fetch('/api/simulation/status', {
      headers: STATE.currentUser && STATE.currentUser.token ? { 'Authorization': `Bearer ${STATE.currentUser.token}` } : {}
    });
    if (!res.ok) return;
    const data = await res.json();
    STATE.simulation.virtualTime = data.virtualTime;
    STATE.simulation.speedMultiplier = data.speedMultiplier;

    // Format virtual clock
    const d = new Date(data.virtualTime);
    const timeStr = isNaN(d.getTime()) ? data.virtualTime : d.toISOString().replace('T', ' ').substring(0, 19);
    
    const timeDisplay = document.getElementById('virtual-time-display');
    if (timeDisplay) timeDisplay.textContent = timeStr;

    const speedBadge = document.getElementById('virtual-speed-badge');
    if (speedBadge) speedBadge.textContent = `${data.speedMultiplier}x`;

    const simEpoch = document.getElementById('sim-clock-epoch');
    if (simEpoch) simEpoch.textContent = timeStr;
  } catch (e) {
    // Silent catch for background polling
  }
}

async function updateNotifications() {
  if (!STATE.currentUser) return;
  try {
    const notifs = await apiCall('/api/notifications');
    if (Array.isArray(notifs)) {
      const unreadCount = notifs.filter(n => !n.read && !n.delivered).length;
      const badge = document.getElementById('unread-count-badge');
      if (badge) {
        if (unreadCount > 0) {
          badge.textContent = unreadCount;
          badge.classList.remove('hidden');
        } else {
          badge.classList.add('hidden');
        }
      }
    }
  } catch (e) {
    // Non-blocking
  }
}

// ============================================================================
// 6. Console 1: Employee Portal Logic
// ============================================================================
function displayTicketCreatedCallout(created) {
  const container = document.getElementById('emp-callout-container');
  if (!container || !created) return;

  let techEmail = 'tech.network@nimbusfinserve.com';
  let techRole = 'David Kim (Senior Network Engineer)';
  if (created.assigneeName) {
    const nameLower = created.assigneeName.toLowerCase();
    if (nameLower.includes('priya') || nameLower.includes('database')) {
      techEmail = 'tech.database@nimbusfinserve.com';
      techRole = `${created.assigneeName} (Core Banking & Database Systems)`;
    } else if (nameLower.includes('sarah')) {
      techEmail = 'lead.network@nimbusfinserve.com';
      techRole = `${created.assigneeName} (Network Team Lead)`;
    } else {
      techEmail = 'tech.network@nimbusfinserve.com';
      techRole = `${created.assigneeName} (Network & Infrastructure Specialist)`;
    }
  }

  container.innerHTML = `
    <div class="card" style="background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.35); padding: 1.25rem; margin-bottom: 1.5rem; animation: fadeIn 0.3s ease;">
      <div class="flex items-start gap-3">
        <div style="font-size: 1.5rem; line-height: 1;">🎫</div>
        <div class="w-full">
          <div class="flex items-center justify-between flex-wrap gap-2" style="margin-bottom: 0.35rem;">
            <h4 class="font-bold text-white text-sm">
              Support Ticket Logged: <span class="font-mono text-cyan">${escapeHtml(created.id)}</span>
            </h4>
            <span class="badge badge-status-assigned">ROUTED &amp; ASSIGNED</span>
          </div>
          <p class="text-xs text-muted" style="margin-bottom: 0.75rem;">
            Automated intelligence classified and assigned this incident to domain specialist: 
            <strong class="text-white">${escapeHtml(created.assigneeName || 'Routing Queue')}</strong>
          </p>
          <div style="background: rgba(15, 23, 42, 0.85); border-radius: 6px; padding: 0.85rem; border-left: 3px solid #10b981;">
            <div class="flex items-center gap-1.5" style="margin-bottom: 0.25rem;">
              <span class="font-semibold text-xs" style="color: #34d399;">🎬 Live Evaluation Walkthrough Step:</span>
            </div>
            <p class="text-xs text-dim" style="line-height: 1.4;">
              This ticket is now assigned to domain expert <strong>${escapeHtml(techRole)}</strong>.
              To investigate, acknowledge, add technical notes, and resolve this ticket from the technician console, click 
              <strong class="text-white">Exit / Sign Out</strong> in the top-right and sign in as:
            </p>
            <div class="flex items-center gap-2 flex-wrap" style="margin-top: 0.5rem;">
              <code class="text-cyan font-mono text-xs" style="background: rgba(0,0,0,0.4); padding: 0.25rem 0.5rem; border-radius: 4px;">${escapeHtml(techEmail)}</code>
              <button type="button" class="btn btn-xs btn-primary btn-demo-quick-switch" data-email="${escapeHtml(techEmail)}">
                Sign Out &amp; Login as Specialist &rarr;
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;

  const switchBtn = container.querySelector('.btn-demo-quick-switch');
  if (switchBtn) {
    switchBtn.addEventListener('click', () => {
      const email = switchBtn.getAttribute('data-email');
      handleSignOut(false);
      loginUser(email, STATE.demoPassword);
    });
  }
}

async function loadEmployeeTickets() {
  const container = document.getElementById('emp-tickets-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">Loading your tickets...</div>';

  try {
    const tickets = await apiCall('/api/employee/incidents');
    renderEmployeeTickets(tickets || []);
    updateEmployeeStats(tickets || []);
  } catch (err) {
    container.innerHTML = `<div class="text-center text-red" style="padding: 2rem;">Failed to load tickets: ${escapeHtml(err.message)}</div>`;
  }
}

function updateEmployeeStats(tickets) {
  const total = tickets.length;
  const active = tickets.filter(t => t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS').length;
  const pending = tickets.filter(t => t.status === 'PENDING_INFO').length;
  const resolved = tickets.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED').length;

  document.getElementById('emp-stat-total').textContent = total;
  document.getElementById('emp-stat-active').textContent = active;
  document.getElementById('emp-stat-pending').textContent = pending;
  document.getElementById('emp-stat-resolved').textContent = resolved;
}

function renderEmployeeTickets(tickets) {
  const container = document.getElementById('emp-tickets-container');
  if (!container) return;

  if (tickets.length === 0) {
    container.innerHTML = `
      <div class="text-center text-muted" style="padding: 3rem 1rem;">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="icon-xl text-dim" style="margin-bottom: 0.5rem;">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p class="font-semibold text-white">No tickets reported yet</p>
        <p class="text-xs text-muted" style="margin-top: 0.25rem;">Use the form on the left to submit an IT support request.</p>
      </div>`;
    return;
  }

  container.innerHTML = tickets.map(t => {
    // 5-Stage Stepper calculation:
    // 1: Reported (NEW/CLASSIFIED) -> 2: Assigned -> 3: Investigating (IN_PROGRESS/PENDING_INFO) -> 4: Resolved -> 5: Closed
    let step = 1;
    if (t.status === 'ASSIGNED') step = 2;
    else if (t.status === 'IN_PROGRESS' || t.status === 'PENDING_INFO') step = 3;
    else if (t.status === 'RESOLVED') step = 4;
    else if (t.status === 'CLOSED') step = 5;

    const statusBadgeClass = `badge-status-${t.status.toLowerCase().replace('_', '-')}`;

    return `
      <div class="card" style="padding: 1rem; background: rgba(15, 23, 42, 0.7);">
        <div class="flex items-center justify-between flex-wrap gap-2" style="margin-bottom: 0.5rem;">
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold text-cyan text-xs">${escapeHtml(t.id)}</span>
            <span class="badge ${statusBadgeClass}">${escapeHtml(t.status)}</span>
            <span class="badge" style="background: var(--bg-card); color: #cbd5e1;">${escapeHtml(t.category || 'GENERAL')}</span>
          </div>
          <span class="text-xs text-dim font-mono">${formatDate(t.createdAt)}</span>
        </div>

        <h4 class="font-bold text-white text-sm" style="margin-bottom: 0.35rem;">${escapeHtml(t.title)}</h4>
        <p class="text-xs text-muted" style="margin-bottom: 0.75rem;">${escapeHtml(t.description)}</p>

        <!-- 5-Stage Visual Stepper -->
        <div class="stepper">
          <div class="step ${step >= 1 ? (step === 1 ? 'active' : 'completed') : ''}">
            <div class="step-circle">${step > 1 ? '✓' : '1'}</div>
            <span class="step-label">Reported</span>
          </div>
          <div class="step ${step >= 2 ? (step === 2 ? 'active' : 'completed') : ''}">
            <div class="step-circle">${step > 2 ? '✓' : '2'}</div>
            <span class="step-label">Assigned</span>
          </div>
          <div class="step ${step >= 3 ? (step === 3 ? 'active' : 'completed') : ''}">
            <div class="step-circle">${step > 3 ? '✓' : '3'}</div>
            <span class="step-label">In Progress</span>
          </div>
          <div class="step ${step >= 4 ? (step === 4 ? 'active' : 'completed') : ''}">
            <div class="step-circle">${step > 4 ? '✓' : '4'}</div>
            <span class="step-label">Resolved</span>
          </div>
          <div class="step ${step >= 5 ? 'completed' : ''}">
            <div class="step-circle">${step >= 5 ? '✓' : '5'}</div>
            <span class="step-label">Closed</span>
          </div>
        </div>

        <!-- Action Ribbon -->
        <div class="flex items-center justify-between flex-wrap gap-2" style="margin-top: 0.75rem; padding-top: 0.5rem; border-top: 1px solid var(--border-subtle);">
          <div class="text-xs text-muted">
            <span>Specialist: </span>
            <span class="text-white font-semibold">${escapeHtml(t.assigneeName || 'Routing Queue')}</span>
          </div>
          <div class="flex items-center gap-1.5">
            ${t.status === 'PENDING_INFO' ? `
              <button type="button" class="btn btn-xs btn-warning btn-emp-provide-info" data-id="${t.id}">
                <span>Provide Requested Info</span>
              </button>` : ''}
            ${t.status === 'RESOLVED' ? `
              <button type="button" class="btn btn-xs btn-success btn-emp-verify" data-id="${t.id}">
                <span>Verify & Rate Resolution</span>
              </button>
              <button type="button" class="btn btn-xs btn-danger btn-emp-reject" data-id="${t.id}">
                <span>Reject Fix</span>
              </button>` : ''}
            <button type="button" class="btn btn-xs btn-secondary btn-emp-details" data-id="${t.id}">
              <span>View Timeline</span>
            </button>
          </div>
        </div>
      </div>
    `;
  }).join('');

  // Attach event handlers
  container.querySelectorAll('.btn-emp-provide-info').forEach(b => {
    b.addEventListener('click', () => openProvideInfoModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-emp-verify').forEach(b => {
    b.addEventListener('click', () => openVerifyModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-emp-reject').forEach(b => {
    b.addEventListener('click', () => openRejectModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-emp-details').forEach(b => {
    b.addEventListener('click', () => openEmployeeDetailsModal(b.getAttribute('data-id')));
  });
}

// ============================================================================
// 7. Console 2: Technician Console Logic
// ============================================================================
async function loadTechnicianDashboard() {
  try {
    const data = await apiCall('/api/technician/dashboard');
    if (!data) return;

    const assigned = data.assignedIncidentsCount || 0;
    const investigating = data.inProgressCount || 0;
    const pending = data.pendingInfoCount || 0;
    const atRisk = data.atRiskCount || 0;
    const maxCapacity = data.maxConcurrentTickets || 7;
    const activeTotal = assigned + investigating + pending;
    const pct = Math.min(100, Math.round((activeTotal / maxCapacity) * 100));

    document.getElementById('tech-stat-assigned').textContent = assigned;
    document.getElementById('tech-stat-investigating').textContent = investigating;
    document.getElementById('tech-stat-pending').textContent = pending;
    document.getElementById('tech-stat-atrisk').textContent = atRisk;

    document.getElementById('tech-capacity-text').textContent = `${activeTotal} / ${maxCapacity} Active Tickets (${pct}%)`;
    const fill = document.getElementById('tech-capacity-fill');
    fill.style.width = `${pct}%`;
    if (pct > 75) {
      fill.classList.add('high');
      document.getElementById('tech-capacity-status').className = 'badge badge-p1';
      document.getElementById('tech-capacity-status').textContent = 'Heavy Load';
    } else {
      fill.classList.remove('high');
      document.getElementById('tech-capacity-status').className = 'badge badge-status-resolved';
      document.getElementById('tech-capacity-status').textContent = 'Available';
    }
  } catch (e) {
    // Non-blocking
  }
}

async function loadTechnicianQueue() {
  const container = document.getElementById('tech-queue-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">Loading technician queue...</div>';

  try {
    const queue = await apiCall('/api/technician/queue');
    const filter = document.getElementById('tech-queue-filter').value;
    const filtered = (filter === 'ALL') ? queue : queue.filter(q => q.status === filter);
    renderTechnicianQueue(filtered || []);
  } catch (err) {
    container.innerHTML = `<div class="text-center text-red" style="padding: 2rem;">Error: ${escapeHtml(err.message)}</div>`;
  }
}

function renderTechnicianQueue(queue) {
  const container = document.getElementById('tech-queue-container');
  if (!container) return;

  if (queue.length === 0) {
    container.innerHTML = `
      <div class="text-center text-muted" style="padding: 3rem 1rem;">
        <p class="font-semibold text-white">Work queue is empty</p>
        <p class="text-xs text-muted" style="margin-top: 0.25rem;">No active incidents currently assigned to your queue.</p>
      </div>`;
    return;
  }

  container.innerHTML = queue.map(t => {
    const pBadge = `badge-${t.priority ? t.priority.toLowerCase() : 'p3'}`;
    const sBadge = `badge-status-${t.status.toLowerCase().replace('_', '-')}`;

    // SLA risk indicator
    let slaClass = 'sla-safe';
    let slaText = 'Within Target';
    if (t.slaBreached) {
      slaClass = 'sla-breached';
      slaText = 'BREACHED';
    } else if (t.slaWarningTriggered) {
      slaClass = 'sla-warning';
      slaText = 'Warning > 75%';
    }

    return `
      <div class="card" style="padding: 1rem; background: rgba(15, 23, 42, 0.7);">
        <div class="flex items-center justify-between flex-wrap gap-2" style="margin-bottom: 0.5rem;">
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold text-cyan text-xs">${escapeHtml(t.id)}</span>
            <span class="badge ${pBadge}">${escapeHtml(t.priority || 'P3')}</span>
            <span class="badge ${sBadge}">${escapeHtml(t.status)}</span>
            <span class="badge" style="background: var(--bg-card); color: #cbd5e1;">${escapeHtml(t.category || 'GENERAL')}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="sla-timer-pill ${slaClass}">SLA: ${slaText}</span>
            <span class="text-xs text-dim font-mono">${formatDate(t.createdAt)}</span>
          </div>
        </div>

        <h4 class="font-bold text-white text-sm" style="margin-bottom: 0.25rem;">${escapeHtml(t.title)}</h4>
        <p class="text-xs text-muted" style="margin-bottom: 0.5rem;">${escapeHtml(t.description)}</p>

        <div class="flex items-center justify-between flex-wrap gap-2 text-xs text-dim" style="margin-bottom: 0.75rem;">
          <span>Reporter: <strong class="text-white">${escapeHtml(t.reporterName || t.reporterId)}</strong> (${escapeHtml(t.reporterDepartment || 'General')})</span>
          <span>Target Deadline: <strong class="text-cyan font-mono">${formatDate(t.slaDeadline)}</strong></span>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-1.5 flex-wrap" style="padding-top: 0.5rem; border-top: 1px solid var(--border-subtle);">
          ${t.status === 'ASSIGNED' ? `
            <button type="button" class="btn btn-xs btn-primary btn-tech-ack" data-id="${t.id}">Acknowledge</button>` : ''}
          ${t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS' ? `
            <button type="button" class="btn btn-xs btn-secondary btn-tech-investigate" data-id="${t.id}">Start Investigation</button>` : ''}
          <button type="button" class="btn btn-xs btn-secondary btn-tech-note" data-id="${t.id}">Add Note</button>
          <button type="button" class="btn btn-xs btn-warning btn-tech-reqinfo" data-id="${t.id}">Request Info (Pause SLA)</button>
          <button type="button" class="btn btn-xs btn-danger btn-tech-wrongdomain" data-id="${t.id}">Flag Out-of-Scope</button>
          <button type="button" class="btn btn-xs btn-success btn-tech-resolve" data-id="${t.id}">Resolve</button>
        </div>
      </div>
    `;
  }).join('');

  // Attach event handlers
  container.querySelectorAll('.btn-tech-ack').forEach(b => {
    b.addEventListener('click', () => technicianAcknowledge(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-tech-investigate').forEach(b => {
    b.addEventListener('click', () => technicianStartInvestigation(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-tech-note').forEach(b => {
    b.addEventListener('click', () => openAddNoteModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-tech-reqinfo').forEach(b => {
    b.addEventListener('click', () => openRequestInfoModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-tech-wrongdomain').forEach(b => {
    b.addEventListener('click', () => openFlagWrongDomainModal(b.getAttribute('data-id')));
  });
  container.querySelectorAll('.btn-tech-resolve').forEach(b => {
    b.addEventListener('click', () => openResolveModal(b.getAttribute('data-id')));
  });
}

async function technicianAcknowledge(id) {
  try {
    await apiCall(`/api/technician/incidents/${id}/acknowledge`, { method: 'POST' });
    showToast(`Acknowledged incident ${id}`, 'success');
    loadTechnicianQueue();
    loadTechnicianDashboard();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function technicianStartInvestigation(id) {
  try {
    await apiCall(`/api/technician/incidents/${id}/start-investigation`, { method: 'POST' });
    showToast(`Investigation started for ${id}`, 'success');
    loadTechnicianQueue();
    loadTechnicianDashboard();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// ============================================================================
// 8. Console 3: Team Lead Console Logic
// ============================================================================
async function loadTeamLeadView() {
  const grid = document.getElementById('teamlead-roster-grid');
  if (!grid) return;
  grid.innerHTML = '<div class="text-center text-muted" style="padding: 2rem; grid-column: 1 / -1;">Loading team roster...</div>';

  try {
    const technicians = await apiCall('/api/admin/technicians');
    renderTeamRoster(technicians || []);
  } catch (err) {
    grid.innerHTML = `<div class="text-center text-red" style="padding: 2rem; grid-column: 1 / -1;">Error: ${escapeHtml(err.message)}</div>`;
  }
}

function renderTeamRoster(technicians) {
  const grid = document.getElementById('teamlead-roster-grid');
  if (!grid) return;

  if (technicians.length === 0) {
    grid.innerHTML = '<div class="text-center text-muted w-full" style="padding: 2rem; grid-column: 1 / -1;">No technicians registered.</div>';
    return;
  }

  grid.innerHTML = technicians.map(tech => {
    const active = tech.activeIncidentsCount || 0;
    const max = tech.maxCapacity || 7;
    const pct = Math.min(100, Math.round((active / max) * 100));

    return `
      <div class="card" style="padding: 1rem; background: rgba(15, 23, 42, 0.7);">
        <div class="flex items-center justify-between" style="margin-bottom: 0.5rem;">
          <h4 class="font-bold text-white text-sm">${escapeHtml(tech.name)}</h4>
          <span class="badge ${pct > 75 ? 'badge-p1' : 'badge-status-resolved'}">${pct}% Load</span>
        </div>
        <div class="text-xs text-muted" style="margin-bottom: 0.4rem;">
          <span>Domain: <strong class="text-cyan">${escapeHtml(tech.domain || 'Engineering')}</strong></span> &middot;
          <span>Tier: <strong class="text-white">${escapeHtml(tech.designation || 'L2')}</strong></span>
        </div>
        <div class="capacity-bar">
          <div class="capacity-fill ${pct > 75 ? 'high' : ''}" style="width: ${pct}%;"></div>
        </div>
        <div class="flex justify-between items-center text-xs text-dim" style="margin-top: 0.4rem;">
          <span>Active Tickets: <strong class="text-white">${active}</strong> / ${max}</span>
          <span class="font-mono text-cyan">${escapeHtml(tech.id)}</span>
        </div>
      </div>
    `;
  }).join('');
}

// ============================================================================
// 9. Console 4: Admin Command Centre Logic
// ============================================================================
async function loadAdminView() {
  loadAdminStats();
  if (STATE.activeAdminSubtab === 'admin-catalog-panel') {
    searchAdminIncidents();
  } else if (STATE.activeAdminSubtab === 'admin-triage-panel') {
    loadAdminTriageQueue();
  } else if (STATE.activeAdminSubtab === 'admin-strategy-panel') {
    loadAssignmentStrategies();
  }
}

async function loadAdminStats() {
  try {
    const stats = await apiCall('/api/admin/stats');
    if (!stats) return;
    document.getElementById('admin-kpi-total').textContent = stats.totalIncidents || 0;
    document.getElementById('admin-kpi-open').textContent = stats.activeIncidents || 0;
    document.getElementById('admin-kpi-triage').textContent = stats.pendingTriageCount || 0;
    document.getElementById('admin-kpi-compliance').textContent = `${stats.slaComplianceRate || 100}%`;
  } catch (e) {
    // Non-blocking
  }
}

async function searchAdminIncidents() {
  const tbody = document.getElementById('admin-incidents-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted" style="padding: 2.5rem 1rem;">Searching enterprise incidents...</td></tr>';

  try {
    const query = document.getElementById('admin-search-text').value.trim();
    const category = document.getElementById('admin-filter-category').value;
    const priority = document.getElementById('admin-filter-priority').value;

    const params = new URLSearchParams();
    if (query) params.append('query', query);
    if (category) params.append('category', category);
    if (priority) params.append('priority', priority);

    const incidents = await apiCall(`/api/admin/incidents?${params.toString()}`);
    renderAdminIncidents(incidents || []);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-red" style="padding: 2.5rem 1rem;">Error: ${escapeHtml(err.message)}</td></tr>`;
  }
}

function renderAdminIncidents(incidents) {
  const tbody = document.getElementById('admin-incidents-tbody');
  if (!tbody) return;

  if (incidents.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted" style="padding: 2.5rem 1rem;">No incidents found matching criteria.</td></tr>';
    return;
  }

  tbody.innerHTML = incidents.map(t => {
    const pBadge = `badge-${t.priority ? t.priority.toLowerCase() : 'p3'}`;
    const sBadge = `badge-status-${t.status.toLowerCase().replace('_', '-')}`;

    return `
      <tr>
        <td class="font-mono font-bold text-cyan text-xs">${escapeHtml(t.id)}</td>
        <td class="font-semibold text-white">${escapeHtml(t.title)}</td>
        <td><span class="badge" style="background: var(--bg-card); color: #cbd5e1;">${escapeHtml(t.category || 'GENERAL')}</span></td>
        <td><span class="badge ${pBadge}">${escapeHtml(t.priority || 'P3')}</span></td>
        <td><span class="badge ${sBadge}">${escapeHtml(t.status)}</span></td>
        <td>${escapeHtml(t.assigneeName || 'Unassigned')}</td>
        <td class="font-mono text-xs ${t.slaBreached ? 'text-red font-bold' : 'text-muted'}">${formatDate(t.slaDeadline)}</td>
        <td class="text-right">
          <button type="button" class="btn btn-xs btn-secondary btn-admin-override" data-id="${t.id}" title="Override Priority">Override</button>
          <button type="button" class="btn btn-xs btn-secondary btn-admin-reassign" data-id="${t.id}" title="Reassign">Reassign</button>
        </td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('.btn-admin-override').forEach(b => {
    b.addEventListener('click', () => openPriorityOverrideModal(b.getAttribute('data-id')));
  });
  tbody.querySelectorAll('.btn-admin-reassign').forEach(b => {
    b.addEventListener('click', () => openReassignModal(b.getAttribute('data-id')));
  });
}

async function loadAdminTriageQueue() {
  const container = document.getElementById('admin-triage-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">Loading triage queue...</div>';

  try {
    const triage = await apiCall('/api/admin/triage-queue');
    if (!triage || triage.length === 0) {
      container.innerHTML = '<div class="text-center text-muted" style="padding: 2.5rem 1rem;">Triage queue is empty. All incidents assigned with high confidence.</div>';
      return;
    }

    container.innerHTML = triage.map(item => `
      <div class="card" style="padding: 1rem; background: rgba(15, 23, 42, 0.7); border-color: rgba(245, 158, 11, 0.3);">
        <div class="flex items-center justify-between flex-wrap gap-2" style="margin-bottom: 0.35rem;">
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold text-cyan text-xs">${escapeHtml(item.incidentId)}</span>
            <span class="badge badge-p2">Flagged Out-of-Scope</span>
          </div>
          <span class="text-xs text-dim font-mono">${formatDate(item.createdAt)}</span>
        </div>
        <p class="text-xs text-white" style="margin-bottom: 0.4rem;">Reason: <em>"${escapeHtml(item.reason)}"</em></p>
        <div class="text-xs text-muted flex items-center justify-between" style="border-top: 1px solid var(--border-subtle); padding-top: 0.5rem; margin-top: 0.5rem;">
          <span>Requesting Tech: <strong class="text-white">${escapeHtml(item.requestingTechnicianId)}</strong></span>
          <button type="button" class="btn btn-xs btn-primary btn-triage-resolve" data-id="${item.incidentId}">
            <span>Reclassify &amp; Reassign</span>
          </button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('.btn-triage-resolve').forEach(b => {
      b.addEventListener('click', () => openReclassifyModal(b.getAttribute('data-id')));
    });
  } catch (err) {
    container.innerHTML = `<div class="text-center text-red" style="padding: 2rem;">Error: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadAssignmentStrategies() {
  const container = document.getElementById('strategies-grid');
  if (!container) return;
  container.innerHTML = '<div class="text-center text-muted w-full" style="padding: 2rem; grid-column: 1 / -1;">Loading strategies...</div>';

  try {
    const strategies = await apiCall('/api/admin/assignment/strategies');
    if (!strategies) return;

    container.innerHTML = strategies.map(s => `
      <div class="card flex flex-col justify-between" style="padding: 1.25rem; background: rgba(15, 23, 42, 0.7); border-color: ${s.active ? 'var(--primary)' : 'var(--border)'};">
        <div>
          <div class="flex items-center justify-between" style="margin-bottom: 0.5rem;">
            <h4 class="font-mono font-bold text-white text-sm">${escapeHtml(s.name)}</h4>
            ${s.active ? '<span class="badge badge-status-resolved">ACTIVE</span>' : ''}
          </div>
          <p class="text-xs text-muted" style="margin-bottom: 1rem;">${escapeHtml(s.description)}</p>
        </div>
        <button type="button" class="btn btn-xs ${s.active ? 'btn-secondary' : 'btn-primary'} btn-activate-strategy" data-name="${s.name}" ${s.active ? 'disabled' : ''}>
          ${s.active ? 'Currently Active' : 'Activate Strategy'}
        </button>
      </div>
    `).join('');

    container.querySelectorAll('.btn-activate-strategy:not([disabled])').forEach(b => {
      b.addEventListener('click', async () => {
        const name = b.getAttribute('data-name');
        try {
          await apiCall('/api/admin/assignment/strategies/active', {
            method: 'PUT',
            body: JSON.stringify({ strategyName: name })
          });
          showToast(`Activated strategy: ${name}`, 'success');
          loadAssignmentStrategies();
        } catch (err) {
          showToast(err.message, 'error');
        }
      });
    });
  } catch (err) {
    container.innerHTML = `<div class="text-center text-red w-full" style="padding: 2rem; grid-column: 1 / -1;">Error: ${escapeHtml(err.message)}</div>`;
  }
}

// ============================================================================
// 10. Console 5: Simulation Engine Logic
// ============================================================================
async function loadSimulationStatus() {
  await updateSimulationClock();
}

async function runScenario(scenarioId) {
  const terminal = document.getElementById('sim-terminal-logs');
  logTerminal(`[SCENARIO] Initiating execution of ${scenarioId}...`, 'info');

  try {
    if (STATE.currentUser && STATE.currentUser.role !== 'ROLE_ADMIN') {
      if (STATE.demoMode) {
        logTerminal(`[AUTH] Auto-elevating to System Admin (admin@nimbusfinserve.com) for simulation privileges...`, 'info');
        await loginUser('admin@nimbusfinserve.com', STATE.demoPassword, false);
      } else {
        throw new Error('Simulation scenarios require Administrator privileges.');
      }
    }

    const result = await apiCall(`/api/simulation/run/${scenarioId}`, { method: 'POST' });
    logTerminal(`[SCENARIO ${scenarioId}] Execution finished with status: ${result.status}`, result.status === 'SUCCESS' ? 'success' : 'warn');
    if (Array.isArray(result.logs)) {
      result.logs.forEach(l => logTerminal(l, 'info'));
    }
    showToast(`Scenario ${scenarioId} completed successfully!`, 'success');
  } catch (err) {
    logTerminal(`[ERROR ${scenarioId}] ${err.message}`, 'error');
    showToast(`Scenario ${scenarioId} failed: ${err.message}`, 'error');
  }
}

function logTerminal(message, type = 'info') {
  const terminal = document.getElementById('sim-terminal-logs');
  if (!terminal) return;
  const entry = document.createElement('span');
  entry.className = `log-entry ${type}`;
  entry.textContent = message;
  terminal.appendChild(entry);
  terminal.scrollTop = terminal.scrollHeight;
}

// ============================================================================
// 11. Console 6: Executive Reports Logic
// ============================================================================
async function loadExecutiveReports() {
  try {
    const mttr = await apiCall('/api/admin/reports/mttr');
    if (mttr) {
      document.getElementById('kpi-mttr-val').textContent = mttr.mttrMinutes !== undefined ? mttr.mttrMinutes : '--';
      document.getElementById('kpi-mttresp-val').textContent = mttr.mttRespMinutes !== undefined ? mttr.mttRespMinutes : '--';
    }

    const sla = await apiCall('/api/admin/reports/sla');
    if (sla) {
      document.getElementById('kpi-compliance-val').textContent = `${sla.complianceRatePercent || 100}%`;
    }

    const workload = await apiCall('/api/admin/reports/workload');
    const workloadContainer = document.getElementById('report-workload-container');
    if (workload && workload.teamWorkloads && workloadContainer) {
      workloadContainer.innerHTML = workload.teamWorkloads.map(w => `
        <div class="flex items-center justify-between text-xs" style="padding: 0.4rem 0; border-bottom: 1px solid var(--border-subtle);">
          <span class="font-semibold text-white">${escapeHtml(w.teamName)}</span>
          <span class="font-mono text-cyan">${w.activeTickets} active (${w.resolvedTickets} resolved)</span>
        </div>
      `).join('');
    }

    const escalations = await apiCall('/api/admin/reports/escalations');
    const escContainer = document.getElementById('report-escalations-container');
    if (escalations && escalations.escalations && escContainer) {
      escContainer.innerHTML = escalations.escalations.map(e => `
        <div class="text-xs" style="padding: 0.35rem 0; border-bottom: 1px solid var(--border-subtle);">
          <span class="badge badge-p1 font-mono">${escapeHtml(e.tier || 'TIER-1')}</span>
          <span class="text-white">${escapeHtml(e.incidentId)}</span> &middot;
          <span class="text-muted">${escapeHtml(e.reason)}</span>
        </div>
      `).join('');
    }
  } catch (e) {
    // Non-blocking
  }
}

// ============================================================================
// 12. Interactive Action Modals
// ============================================================================
function openModal(contentHtml) {
  const container = document.getElementById('modal-container');
  const content = document.getElementById('modal-content');
  content.innerHTML = contentHtml;
  container.classList.remove('hidden');

  content.querySelectorAll('[data-close-modal]').forEach(b => {
    b.addEventListener('click', closeModal);
  });
}

function closeModal() {
  const container = document.getElementById('modal-container');
  container.classList.add('hidden');
}

function openProvideInfoModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title">Respond to Information Request</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-provide-info" class="space-y-4">
      <div class="form-group">
        <label class="form-label">Workstation / Issue Details</label>
        <textarea id="modal-info-response" class="form-textarea" rows="4" required placeholder="Provide the details requested by engineering..."></textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-primary">Submit Response</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-provide-info').addEventListener('submit', async (e) => {
    e.preventDefault();
    const text = document.getElementById('modal-info-response').value.trim();
    try {
      await apiCall(`/api/employee/incidents/${incidentId}/provide-info`, {
        method: 'POST',
        body: JSON.stringify({ responseText: text })
      });
      closeModal();
      showToast('Information submitted to engineering.', 'success');
      loadEmployeeTickets();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openVerifyModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title">Verify &amp; Rate Incident Resolution</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-verify" class="space-y-4">
      <p class="text-xs text-muted">Please confirm that your technical issue has been resolved to your satisfaction and rate the support received.</p>
      <div class="form-group">
        <label class="form-label">Service Satisfaction Rating (1–5 Stars)</label>
        <select id="modal-rating" class="form-select">
          <option value="5" selected>⭐⭐⭐⭐⭐ 5 Stars - Excellent</option>
          <option value="4">⭐⭐⭐⭐ 4 Stars - Good</option>
          <option value="3">⭐⭐⭐ 3 Stars - Average</option>
          <option value="2">⭐⭐ 2 Stars - Below Expectations</option>
          <option value="1">⭐ 1 Star - Unsatisfactory</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Optional Feedback Comments</label>
        <textarea id="modal-feedback-comment" class="form-textarea" rows="2" placeholder="Great assistance, thanks!"></textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-success">Verify &amp; Close Ticket</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-verify').addEventListener('submit', async (e) => {
    e.preventDefault();
    const rating = parseInt(document.getElementById('modal-rating').value, 10);
    const comment = document.getElementById('modal-feedback-comment').value.trim();
    try {
      await apiCall(`/api/employee/incidents/${incidentId}/verify`, { method: 'POST' });
      await apiCall(`/api/employee/incidents/${incidentId}/feedback`, {
        method: 'POST',
        body: JSON.stringify({ rating, comment })
      });
      closeModal();
      showToast('Resolution verified and feedback submitted.', 'success');
      loadEmployeeTickets();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openRejectModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title text-red">Reject Resolution</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-reject" class="space-y-4">
      <div class="form-group">
        <label class="form-label">Reason for Rejection</label>
        <textarea id="modal-reject-reason" class="form-textarea" rows="3" required placeholder="Explain why the issue persists..."></textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-danger">Reject Resolution</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-reject').addEventListener('submit', async (e) => {
    e.preventDefault();
    const reason = document.getElementById('modal-reject-reason').value.trim();
    try {
      await apiCall(`/api/employee/incidents/${incidentId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason })
      });
      closeModal();
      showToast('Resolution rejected. Ticket returned to engineering.', 'warning');
      loadEmployeeTickets();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openAddNoteModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title">Add Work Note</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-note" class="space-y-4">
      <div class="form-group">
        <label class="form-label">Note Text</label>
        <textarea id="modal-note-text" class="form-textarea" rows="3" required placeholder="Document technical investigation findings..."></textarea>
      </div>
      <div class="form-group flex items-center gap-2">
        <input type="checkbox" id="modal-note-internal" checked>
        <label for="modal-note-internal" class="text-xs text-muted">Internal engineering note only (masked from employee)</label>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-primary">Add Note</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-note').addEventListener('submit', async (e) => {
    e.preventDefault();
    const noteText = document.getElementById('modal-note-text').value.trim();
    const internalOnly = document.getElementById('modal-note-internal').checked;
    try {
      await apiCall(`/api/technician/incidents/${incidentId}/notes`, {
        method: 'POST',
        body: JSON.stringify({ noteText, internalOnly })
      });
      closeModal();
      showToast('Note added successfully.', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openRequestInfoModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title text-amber">Request Additional Info</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-reqinfo" class="space-y-4">
      <p class="text-xs text-muted">Requesting information will put the ticket in <strong>PENDING_INFO</strong> status and <strong>pause the SLA clock</strong> until the user responds.</p>
      <div class="form-group">
        <label class="form-label">Information Required</label>
        <textarea id="modal-reqinfo-text" class="form-textarea" rows="3" required placeholder="Specify what information you need from the employee..."></textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-warning">Request Info &amp; Pause SLA</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-reqinfo').addEventListener('submit', async (e) => {
    e.preventDefault();
    const text = document.getElementById('modal-reqinfo-text').value.trim();
    try {
      await apiCall(`/api/technician/incidents/${incidentId}/request-info`, {
        method: 'POST',
        body: JSON.stringify({ responseText: text })
      });
      closeModal();
      showToast('Information requested. SLA clock paused.', 'warning');
      loadTechnicianQueue();
      loadTechnicianDashboard();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openFlagWrongDomainModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title text-red">Flag Out-of-Scope (Wrong Domain)</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-flag" class="space-y-4">
      <p class="text-xs text-muted">Flagging this incident will remove it from your queue and route it to the <strong>Admin Triage Queue</strong> for domain reclassification.</p>
      <div class="form-group">
        <label class="form-label">Reason / Correct Domain Suggestion</label>
        <textarea id="modal-flag-reason" class="form-textarea" rows="3" required placeholder="e.g. This is a Database issue, not a Network issue. Reassign to Database team."></textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-danger">Route to Admin Triage</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-flag').addEventListener('submit', async (e) => {
    e.preventDefault();
    const reason = document.getElementById('modal-flag-reason').value.trim();
    try {
      await apiCall(`/api/technician/incidents/${incidentId}/flag-wrong-domain`, {
        method: 'POST',
        body: JSON.stringify({ reason })
      });
      closeModal();
      showToast('Ticket routed to Admin Triage queue.', 'info');
      loadTechnicianQueue();
      loadTechnicianDashboard();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openResolveModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title text-emerald">Resolve Incident</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-resolve" class="space-y-4">
      <div class="form-group">
        <label class="form-label">Root Cause Summary</label>
        <input type="text" id="modal-root-cause" class="form-input" required placeholder="e.g. VPN tunnel gateway IP changed due to firewall update">
      </div>
      <div class="form-group">
        <label class="form-label">Steps Taken to Resolve</label>
        <textarea id="modal-steps-taken" class="form-textarea" rows="3" required placeholder="e.g. Reconfigured routing table and restarted IPsec daemon"></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Confirmed Category</label>
        <select id="modal-confirmed-category" class="form-select">
          <option value="NETWORK">NETWORK</option>
          <option value="HARDWARE">HARDWARE</option>
          <option value="SOFTWARE">SOFTWARE</option>
          <option value="DATABASE">DATABASE</option>
          <option value="SECURITY">SECURITY</option>
          <option value="CLOUD">CLOUD</option>
          <option value="ACCESS">ACCESS</option>
          <option value="APPLICATION">APPLICATION</option>
        </select>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-success">Mark as Resolved</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-resolve').addEventListener('submit', async (e) => {
    e.preventDefault();
    const rootCause = document.getElementById('modal-root-cause').value.trim();
    const stepsTaken = document.getElementById('modal-steps-taken').value.trim();
    const confirmedCategory = document.getElementById('modal-confirmed-category').value;

    try {
      await apiCall(`/api/technician/incidents/${incidentId}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ rootCause, stepsTaken, confirmedCategory })
      });
      closeModal();
      showToast('Incident resolved! Awaiting user verification.', 'success');
      loadTechnicianQueue();
      loadTechnicianDashboard();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openPriorityOverrideModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title">Override Priority</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-priority" class="space-y-4">
      <div class="form-group">
        <label class="form-label">New Priority Level</label>
        <select id="modal-new-priority" class="form-select">
          <option value="P1">P1 (Critical - 1hr SLA)</option>
          <option value="P2">P2 (High - 4hr SLA)</option>
          <option value="P3">P3 (Medium - 8hr SLA)</option>
          <option value="P4">P4 (Low - 24hr SLA)</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Justification / Audit Reason</label>
        <input type="text" id="modal-priority-reason" class="form-input" required placeholder="e.g. VIP executive impact; escalated priority">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-primary">Apply Override</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-priority').addEventListener('submit', async (e) => {
    e.preventDefault();
    const priority = document.getElementById('modal-new-priority').value;
    const reason = document.getElementById('modal-priority-reason').value.trim();
    try {
      await apiCall(`/api/admin/incidents/${incidentId}/override-priority`, {
        method: 'POST',
        body: JSON.stringify({ priority, reason })
      });
      closeModal();
      showToast(`Priority updated to ${priority}`, 'success');
      searchAdminIncidents();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openReassignModal(incidentId) {
  apiCall('/api/admin/technicians').then(technicians => {
    const options = (technicians || []).map(t => `<option value="${t.id}">${escapeHtml(t.name)} (${t.domain || 'Tech'})</option>`).join('');
    openModal(`
      <div class="modal-header">
        <h3 class="modal-title">Reassign Incident</h3>
        <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
      </div>
      <form id="form-modal-reassign" class="space-y-4">
        <div class="form-group">
          <label class="form-label">Target Technician</label>
          <select id="modal-target-tech" class="form-select">
            ${options}
          </select>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
          <button type="submit" class="btn btn-primary">Assign</button>
        </div>
      </form>
    `);

    document.getElementById('form-modal-reassign').addEventListener('submit', async (e) => {
      e.preventDefault();
      const technicianId = document.getElementById('modal-target-tech').value;
      try {
        await apiCall(`/api/admin/incidents/${incidentId}/assign`, {
          method: 'POST',
          body: JSON.stringify({ technicianId })
        });
        closeModal();
        showToast('Incident reassigned.', 'success');
        searchAdminIncidents();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }).catch(err => showToast(err.message, 'error'));
}

function openReclassifyModal(incidentId) {
  openModal(`
    <div class="modal-header">
      <h3 class="modal-title">Triage &amp; Reclassify</h3>
      <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
    </div>
    <form id="form-modal-reclassify" class="space-y-4">
      <div class="form-group">
        <label class="form-label">New Category</label>
        <select id="modal-new-category" class="form-select">
          <option value="DATABASE">DATABASE</option>
          <option value="NETWORK">NETWORK</option>
          <option value="APPLICATION">APPLICATION</option>
          <option value="SECURITY">SECURITY</option>
          <option value="CLOUD">CLOUD</option>
          <option value="HARDWARE">HARDWARE</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Audit Reason</label>
        <input type="text" id="modal-reclassify-reason" class="form-input" required value="Human triage reclassification after out-of-scope report">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
        <button type="submit" class="btn btn-primary">Reclassify &amp; Route</button>
      </div>
    </form>
  `);

  document.getElementById('form-modal-reclassify').addEventListener('submit', async (e) => {
    e.preventDefault();
    const category = document.getElementById('modal-new-category').value;
    const reason = document.getElementById('modal-reclassify-reason').value.trim();
    try {
      await apiCall(`/api/admin/incidents/${incidentId}/reclassify`, {
        method: 'POST',
        body: JSON.stringify({ category, reason })
      });
      closeModal();
      showToast('Incident reclassified and dispatched.', 'success');
      loadAdminTriageQueue();
    } catch (err) {
      showToast(err.message, 'error');
    }
  });
}

function openEmployeeDetailsModal(incidentId) {
  apiCall(`/api/employee/incidents/${incidentId}`).then(incident => {
    const auditHtml = (incident.auditRecords || []).map(r => `
      <div class="text-xs" style="padding: 0.25rem 0; border-bottom: 1px solid var(--border-subtle);">
        <span class="font-mono text-cyan">${formatDate(r.timestamp)}</span> &middot;
        <span class="text-white">${escapeHtml(r.action)}</span>
      </div>
    `).join('') || '<div class="text-xs text-dim">No historical updates recorded.</div>';

    openModal(`
      <div class="modal-header">
        <h3 class="modal-title">Incident Details &middot; ${escapeHtml(incident.id)}</h3>
        <button type="button" class="btn btn-xs btn-secondary" data-close-modal>&times;</button>
      </div>
      <div class="space-y-3">
        <div>
          <span class="text-xs text-muted">Title:</span>
          <p class="font-bold text-white text-sm">${escapeHtml(incident.title)}</p>
        </div>
        <div>
          <span class="text-xs text-muted">Description:</span>
          <p class="text-xs text-white">${escapeHtml(incident.description)}</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="badge badge-status-${incident.status.toLowerCase().replace('_', '-')}">${escapeHtml(incident.status)}</span>
          <span class="text-xs text-muted">Specialist: <strong class="text-white">${escapeHtml(incident.assigneeName || 'Routing Queue')}</strong></span>
        </div>
        <div style="border-top: 1px solid var(--border); padding-top: 0.5rem; margin-top: 0.5rem;">
          <h4 class="font-bold text-white text-xs" style="margin-bottom: 0.35rem;">Progress Timeline</h4>
          <div class="space-y-1 max-h-40 overflow-y-auto">${auditHtml}</div>
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" data-close-modal>Close</button>
      </div>
    `);
  }).catch(err => showToast(err.message, 'error'));
}

// ============================================================================
// 13. DOM Event Listeners Setup
// ============================================================================
function setupEventListeners() {
  // Login form
  const loginForm = document.getElementById('form-login');
  if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const email = document.getElementById('login-email').value;
      const pwd = document.getElementById('login-password').value;
      loginUser(email, pwd);
    });
  }

  // Register form
  const regForm = document.getElementById('form-register');
  if (regForm) {
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('reg-name').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const pwd = document.getElementById('reg-password').value;
      const dept = document.getElementById('reg-department').value;

      try {
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, email, password: pwd, department: dept })
        });
        if (!res.ok) {
          const err = await res.json().catch(() => ({ message: 'Registration failed' }));
          throw new Error(err.message);
        }
        const data = await res.json();
        showToast('Registration successful! Signing in...', 'success');
        setAuthenticatedUser({
          token: data.token,
          id: data.id,
          name: data.name,
          email: data.email,
          role: data.role
        });
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Toggle Login/Register
  document.getElementById('btn-toggle-register').addEventListener('click', () => {
    document.getElementById('login-card').classList.add('hidden');
    document.getElementById('register-card').classList.remove('hidden');
  });
  document.getElementById('btn-toggle-login').addEventListener('click', () => {
    document.getElementById('register-card').classList.add('hidden');
    document.getElementById('login-card').classList.remove('hidden');
  });

  // Demo pills in auth view
  document.querySelectorAll('.demo-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      const email = pill.getAttribute('data-email');
      document.getElementById('login-email').value = email;
      document.getElementById('login-password').value = STATE.demoPassword;
      loginUser(email, STATE.demoPassword);
    });
  });

  // Persona chips in header bar
  document.querySelectorAll('.persona-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const email = chip.getAttribute('data-email');
      loginUser(email, STATE.demoPassword);
    });
  });

  // Sign out button
  document.getElementById('btn-logout').addEventListener('click', () => handleSignOut(true));

  // Navigation tabs
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => switchTab(tab.getAttribute('data-target')));
  });

  // Admin subtabs
  document.querySelectorAll('.admin-subtab').forEach(subtab => {
    subtab.addEventListener('click', () => {
      document.querySelectorAll('.admin-subtab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));
      subtab.classList.add('active');
      const target = subtab.getAttribute('data-subtarget');
      document.getElementById(target).classList.add('active');
      STATE.activeAdminSubtab = target;
      loadAdminView();
    });
  });

  // Employee create incident
  const createForm = document.getElementById('form-create-incident');
  if (createForm) {
    createForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const title = document.getElementById('emp-inc-title').value.trim();
      const description = document.getElementById('emp-inc-desc').value.trim();

      try {
        const created = await apiCall('/api/employee/incidents', {
          method: 'POST',
          body: JSON.stringify({ title, description })
        });
        showToast('Ticket submitted successfully!', 'success');
        createForm.reset();
        document.getElementById('desc-char-count').textContent = '0 / 4000';
        displayTicketCreatedCallout(created);
        loadEmployeeTickets();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });

    const descEl = document.getElementById('emp-inc-desc');
    if (descEl) {
      descEl.addEventListener('input', () => {
        document.getElementById('desc-char-count').textContent = `${descEl.value.length} / 4000`;
        updateAiPreview(document.getElementById('emp-inc-title').value, descEl.value);
      });
    }

    const titleEl = document.getElementById('emp-inc-title');
    if (titleEl) {
      titleEl.addEventListener('input', () => {
        updateAiPreview(titleEl.value, descEl.value);
      });
    }
  }

  // Quick fill templates
  document.querySelectorAll('.template-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const type = chip.getAttribute('data-template');
      const titleEl = document.getElementById('emp-inc-title');
      const descEl = document.getElementById('emp-inc-desc');

      if (type === 'vpn') {
        titleEl.value = 'VPN tunnel disconnects during wire transfer';
        descEl.value = 'Workstation lost connection to corporate VPN while executing SWIFT batch transfer. Gateway 10.24.1.1 unreachable with TLS handshake timeout.';
      } else if (type === 'payment') {
        titleEl.value = 'Payment Gateway 502 Bad Gateway';
        descEl.value = 'Customer checkout portal throwing HTTP 502 Bad Gateway errors on credit card processing endpoint /api/v1/payments/charge.';
      } else if (type === 'display') {
        titleEl.value = 'Laptop Display Flickering and Artifacts';
        descEl.value = 'External monitor connected via Thunderbolt dock flickering continuously with horizontal line artifacts after OS update.';
      } else if (type === 'sso') {
        titleEl.value = 'SSO Active Directory Account Locked';
        descEl.value = 'Active Directory domain controller reports user account locked out after password expiration policy triggered this morning.';
      }
      descEl.dispatchEvent(new Event('input'));
    });
  });

  // Refresh buttons
  const refEmp = document.getElementById('btn-refresh-emp-tickets');
  if (refEmp) refEmp.addEventListener('click', loadEmployeeTickets);

  const refTech = document.getElementById('btn-refresh-tech-queue');
  if (refTech) refTech.addEventListener('click', () => {
    loadTechnicianQueue();
    loadTechnicianDashboard();
  });

  const refTeam = document.getElementById('btn-refresh-teamlead');
  if (refTeam) refTeam.addEventListener('click', loadTeamLeadView);

  const refTriage = document.getElementById('btn-refresh-triage');
  if (refTriage) refTriage.addEventListener('click', loadAdminTriageQueue);

  const filterTech = document.getElementById('tech-queue-filter');
  if (filterTech) filterTech.addEventListener('change', loadTechnicianQueue);

  const btnSearch = document.getElementById('btn-admin-search');
  if (btnSearch) btnSearch.addEventListener('click', searchAdminIncidents);

  // Fast forward buttons
  document.querySelectorAll('.sim-ff-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const minutes = btn.getAttribute('data-minutes');
      try {
        await apiCall(`/api/simulation/advance?minutes=${minutes}`, { method: 'POST' });
        showToast(`Clock advanced by +${minutes} minutes`, 'info');
        updateSimulationClock();
        refreshActiveTab();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });

  // Speed multiplier buttons
  document.querySelectorAll('.sim-speed-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const speed = btn.getAttribute('data-speed');
      try {
        await apiCall(`/api/simulation/speed?multiplier=${speed}`, { method: 'POST' });
        showToast(`Clock speed set to ${speed}x`, 'info');
        updateSimulationClock();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });

  // Reset clock button
  const resetBtn = document.getElementById('btn-clock-reset');
  if (resetBtn) {
    resetBtn.addEventListener('click', async () => {
      try {
        await apiCall('/api/simulation/reset', { method: 'POST' });
        showToast('Virtual clock reset to real-time.', 'info');
        updateSimulationClock();
        refreshActiveTab();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  }

  // Scenarios buttons S1-S5
  document.querySelectorAll('.btn-run-scenario').forEach(btn => {
    btn.addEventListener('click', () => {
      const s = btn.getAttribute('data-scenario');
      runScenario(s);
    });
  });

  // Terminal copy/clear
  const copyBtn = document.getElementById('btn-copy-sim-logs');
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const logs = document.getElementById('sim-terminal-logs').innerText;
      navigator.clipboard.writeText(logs).then(() => showToast('Terminal logs copied to clipboard!', 'info'));
    });
  }

  const clearBtn = document.getElementById('btn-clear-sim-logs');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      document.getElementById('sim-terminal-logs').innerHTML = '<span class="log-entry info">// Terminal cleared.</span>';
    });
  }

  // In-app notifications bell
  const bell = document.getElementById('btn-notification-bell');
  if (bell) {
    bell.addEventListener('click', async () => {
      const modal = document.getElementById('notification-modal');
      const container = document.getElementById('notifications-list-container');
      modal.classList.remove('hidden');
      container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">Loading notifications...</div>';

      try {
        const notifs = await apiCall('/api/notifications');
        if (!notifs || notifs.length === 0) {
          container.innerHTML = '<div class="text-center text-muted" style="padding: 2rem;">No notifications.</div>';
          return;
        }

        container.innerHTML = notifs.map(n => `
          <div class="card" style="padding: 0.65rem; background: rgba(15, 23, 42, 0.7); border-color: ${n.read ? 'var(--border)' : 'var(--primary)'};">
            <div class="flex items-center justify-between" style="margin-bottom: 0.2rem;">
              <span class="badge ${n.channel === 'EMAIL' ? 'badge-p3' : 'badge-status-in-progress'}">${escapeHtml(n.channel)}</span>
              <span class="text-xs text-dim font-mono">${formatDate(n.createdAt)}</span>
            </div>
            <p class="text-xs text-white" style="margin-top: 0.2rem;">${escapeHtml(n.message)}</p>
          </div>
        `).join('');
      } catch (err) {
        container.innerHTML = `<div class="text-center text-red" style="padding: 2rem;">Error: ${escapeHtml(err.message)}</div>`;
      }
    });
  }

  const closeNotifs = document.getElementById('btn-close-notifications');
  if (closeNotifs) {
    closeNotifs.addEventListener('click', () => {
      document.getElementById('notification-modal').classList.add('hidden');
    });
  }
}

function updateAiPreview(title, desc) {
  const text = `${title} ${desc}`.toLowerCase();
  let cat = 'APPLICATION';
  let srv = 'SVC-INTRANET';

  if (text.includes('vpn') || text.includes('tunnel') || text.includes('wifi') || text.includes('network') || text.includes('ipsec') || text.includes('dns')) {
    cat = 'NETWORK';
    srv = 'SVC-VPN (Corporate VPN)';
  } else if (text.includes('database') || text.includes('sql') || text.includes('postgres') || text.includes('oracle') || text.includes('deadlock')) {
    cat = 'DATABASE';
    srv = 'SVC-DB (Core Database)';
  } else if (text.includes('screen') || text.includes('display') || text.includes('laptop') || text.includes('keyboard') || text.includes('dock')) {
    cat = 'HARDWARE';
    srv = 'SVC-WORKSTATION';
  } else if (text.includes('password') || text.includes('sso') || text.includes('locked') || text.includes('mfa')) {
    cat = 'ACCESS';
    srv = 'SVC-IAM (Identity Management)';
  } else if (text.includes('payment') || text.includes('swift') || text.includes('wire') || text.includes('checkout')) {
    cat = 'APPLICATION';
    srv = 'SVC-PAYMENTS';
  }

  document.getElementById('preview-category').textContent = cat;
  document.getElementById('preview-service').textContent = srv;
}

function formatDate(isoStr) {
  if (!isoStr) return '--';
  try {
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    return d.toISOString().replace('T', ' ').substring(0, 16);
  } catch (e) {
    return isoStr;
  }
}
