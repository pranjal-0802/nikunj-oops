/**
 * ============================================================================
 * Nimbus FinServe :: Intelligent IT Service Management (I-ITSM)
 * Modern Single-Page Application Client (Linear / Vercel Aesthetic)
 * ============================================================================
 */

// Application Global State
const STATE = {
  currentUser: {
    email: 'admin@nimbusfinserve.com',
    role: 'ROLE_ADMIN',
    name: 'System Administrator',
    token: null,
    avatar: 'A'
  },
  simulation: {
    virtualTime: null,
    speedMultiplier: 1,
    paused: false
  },
  activeTab: 'console-employee',
  adminIncidentsCache: [],
  techniciansRoster: []
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
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-emerald-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg>`;
  } else if (type === 'error') {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-red-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>`;
  } else if (type === 'warning') {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-amber-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>`;
  } else {
    iconSvg = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-cyan-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`;
  }

  toast.innerHTML = `
    <div class="flex-shrink-0">${iconSvg}</div>
    <div class="flex-1 text-xs leading-snug">${message}</div>
    <button class="text-slate-400 hover:text-white font-bold text-sm ml-2">&times;</button>
  `;

  toast.querySelector('button').addEventListener('click', () => {
    toast.remove();
  });

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

// ============================================================================
// 2. API Client with Automatic JWT Handling
// ============================================================================
async function apiCall(endpoint, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  if (STATE.currentUser.token) {
    headers['Authorization'] = `Bearer ${STATE.currentUser.token}`;
  }

  try {
    const res = await fetch(endpoint, { ...options, headers });
    if (res.status === 401) {
      console.warn('Session expired or unauthorized, re-authenticating...');
      await authenticateUser(STATE.currentUser.email, false);
      return apiCall(endpoint, options);
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
    console.error(`API Error on ${endpoint}:`, err);
    throw err;
  }
}

// ============================================================================
// 3. Authentication & Persona Switching
// ============================================================================
async function authenticateUser(email, autoSwitchTab = true) {
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email, password: 'Password123!' })
    });
    if (!res.ok) throw new Error('Authentication failed for ' + email);
    const data = await res.json();
    STATE.currentUser.token = data.token;
    STATE.currentUser.email = data.email;
    STATE.currentUser.role = data.role;
    STATE.currentUser.name = data.name || email.split('@')[0];
    STATE.currentUser.avatar = (STATE.currentUser.name.charAt(0) || 'U').toUpperCase();

    // Update UI Elements
    document.getElementById('active-user-name').textContent = STATE.currentUser.name;
    document.getElementById('active-user-role').textContent = STATE.currentUser.role;
    document.getElementById('active-user-avatar').textContent = STATE.currentUser.avatar;

    // Sync persona chip active state
    document.querySelectorAll('.persona-chip').forEach(chip => {
      if (chip.getAttribute('data-email') === email) {
        chip.classList.add('active');
      } else {
        chip.classList.remove('active');
      }
    });

    const personaSelect = document.getElementById('persona-selector');
    if (personaSelect) personaSelect.value = email;

    showToast(`Switched persona to ${STATE.currentUser.name} (${STATE.currentUser.role})`, 'info');

    // Auto-switch natural tab based on persona role
    if (autoSwitchTab) {
      if (STATE.currentUser.role === 'ROLE_EMPLOYEE') {
        switchTab('console-employee');
      } else if (STATE.currentUser.role === 'ROLE_TECHNICIAN' || STATE.currentUser.role === 'ROLE_TEAM_LEAD') {
        switchTab('console-technician');
      } else if (STATE.currentUser.role === 'ROLE_ADMIN' && email.includes('manager')) {
        switchTab('console-reports');
      } else if (STATE.currentUser.role === 'ROLE_ADMIN') {
        switchTab('console-admin');
      }
    } else {
      refreshActiveView();
    }

    loadNotifications();
  } catch (err) {
    console.error('Login failed for ' + email, err);
    showToast(`Login failed: ${err.message}`, 'error');
  }
}

function switchTab(targetId) {
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
  refreshActiveView();
}

function refreshActiveView() {
  if (STATE.activeTab === 'console-employee') {
    loadEmployeeTickets();
  } else if (STATE.activeTab === 'console-technician') {
    loadTechnicianQueue();
  } else if (STATE.activeTab === 'console-admin') {
    loadAdminIncidents();
    loadAdminTriageQueue();
    loadAssignmentStrategies();
  } else if (STATE.activeTab === 'console-reports') {
    loadExecutiveReports();
  } else if (STATE.activeTab === 'console-simulation') {
    updateClockDisplay();
  }
}

// ============================================================================
// 4. Navigation & Persona Controls Setup
// ============================================================================
function setupNavigation() {
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const targetId = tab.getAttribute('data-target');
      switchTab(targetId);
    });
  });

  // Persona Chips in Header Bar
  document.querySelectorAll('.persona-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const email = chip.getAttribute('data-email');
      authenticateUser(email, true);
    });
  });

  // Native select sync
  const personaSelect = document.getElementById('persona-selector');
  if (personaSelect) {
    personaSelect.addEventListener('change', (e) => {
      authenticateUser(e.target.value, true);
    });
  }

  // Admin Subtabs
  document.querySelectorAll('.admin-subtab').forEach(subtab => {
    subtab.addEventListener('click', () => {
      document.querySelectorAll('.admin-subtab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));

      subtab.classList.add('active');
      const target = subtab.getAttribute('data-subtarget');
      const panel = document.getElementById(target);
      if (panel) panel.classList.add('active');

      if (target === 'admin-triage-queue') loadAdminTriageQueue();
      if (target === 'admin-strategy-manager') loadAssignmentStrategies();
      if (target === 'admin-all-incidents') loadAdminIncidents();
    });
  });
}

// ============================================================================
// 5. Simulation Clock & Speed Controls
// ============================================================================
async function pollSimulationStatus() {
  try {
    const status = await apiCall('/api/simulation/status');
    if (status) {
      STATE.simulation = status;
      updateClockDisplay();
    }
  } catch (e) {
    // silently catch during startup
  }
}

function updateClockDisplay() {
  const timeStr = STATE.simulation.virtualTime ? STATE.simulation.virtualTime.replace('T', ' ').substring(0, 19) : '--:--:--';
  const speedStr = `${STATE.simulation.speedMultiplier || 1}x`;

  const topClock = document.getElementById('virtual-time-display');
  const topBadge = document.getElementById('virtual-speed-badge');
  const simClock = document.getElementById('sim-clock-display');

  if (topClock) topClock.textContent = timeStr;
  if (topBadge) topBadge.textContent = speedStr;
  if (simClock) simClock.textContent = timeStr;

  document.querySelectorAll('.sim-speed-btn').forEach(btn => {
    const spd = parseInt(btn.getAttribute('data-speed'));
    if (spd === STATE.simulation.speedMultiplier) {
      btn.classList.add('bg-cyan-600', 'text-white');
      btn.classList.remove('bg-slate-800', 'text-slate-300');
    } else {
      btn.classList.remove('bg-cyan-600', 'text-white');
      btn.classList.add('bg-slate-800', 'text-slate-300');
    }
  });
}

function setupSimulationControls() {
  // Speed Multipliers
  document.querySelectorAll('.sim-speed-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const spd = btn.getAttribute('data-speed');
      try {
        await apiCall(`/api/simulation/speed?multiplier=${spd}`, { method: 'POST' });
        showToast(`Simulation speed adjusted to ${spd}x`, 'info');
        pollSimulationStatus();
      } catch (err) {
        showToast(`Failed to set speed: ${err.message}`, 'error');
      }
    });
  });

  // Discrete Fast-Forward
  document.querySelectorAll('.sim-ff-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const mins = btn.getAttribute('data-minutes');
      try {
        await apiCall(`/api/simulation/advance?minutes=${mins}`, { method: 'POST' });
        showToast(`Fast-forwarded simulation clock by +${mins} minutes`, 'success');
        pollSimulationStatus();
        refreshActiveView();
      } catch (err) {
        showToast(`Failed to advance clock: ${err.message}`, 'error');
      }
    });
  });

  // Reset to Real-Time
  const resetBtn = document.getElementById('btn-clock-reset');
  if (resetBtn) {
    resetBtn.addEventListener('click', async () => {
      try {
        await apiCall('/api/simulation/reset', { method: 'POST' });
        showToast('Virtual clock reset to system real-time', 'info');
        pollSimulationStatus();
        refreshActiveView();
      } catch (err) {
        showToast(`Reset failed: ${err.message}`, 'error');
      }
    });
  }

  // Canonical Demonstration Scenarios (S1–S5)
  document.querySelectorAll('.btn-run-scenario').forEach(btn => {
    btn.addEventListener('click', async () => {
      const scenarioId = btn.getAttribute('data-scenario');
      const terminal = document.getElementById('sim-terminal-logs');
      terminal.innerHTML = `<div class="text-cyan-400 font-bold">> [${scenarioId}] Initializing scenario execution...</div>`;

      btn.disabled = true;
      const originalText = btn.textContent;
      btn.textContent = 'Running...';

      try {
        const result = await apiCall(`/api/simulation/run/${scenarioId}`, { method: 'POST' });
        terminal.innerHTML = '';
        (result.stepLogs || []).forEach(log => {
          const div = document.createElement('div');
          if (log.includes('Ticket created') || log.includes('acknowledged') || log.includes('Closed') || log.includes('5-star')) {
            div.className = 'text-emerald-400';
          } else if (log.includes('Escalated') || log.includes('Breach') || log.includes('FAILED')) {
            div.className = 'text-red-400 font-semibold';
          } else if (log.includes('warning') || log.includes('Paused') || log.includes('Wrong domain') || log.includes('Pending')) {
            div.className = 'text-amber-400';
          } else {
            div.className = 'text-slate-300';
          }
          div.textContent = log;
          terminal.appendChild(div);
        });

        const summary = document.createElement('div');
        summary.className = `pt-3 font-bold border-t border-slate-800 ${result.success ? 'text-emerald-400' : 'text-red-400'}`;
        summary.textContent = `[${result.scenarioId}] Status: ${result.success ? 'SUCCESS' : 'FAILED'} - ${result.summary}`;
        terminal.appendChild(summary);
        terminal.scrollTop = terminal.scrollHeight;

        showToast(`Scenario ${scenarioId} executed: ${result.success ? 'SUCCESS' : 'FAILED'}`, result.success ? 'success' : 'warning');
        pollSimulationStatus();
        refreshActiveView();
      } catch (err) {
        terminal.innerHTML += `<div class="text-red-400 font-bold">> Execution Error: ${err.message}</div>`;
        showToast(`Scenario execution failed: ${err.message}`, 'error');
      } finally {
        btn.disabled = false;
        btn.textContent = originalText;
      }
    });
  });

  // Terminal Controls
  const clearBtn = document.getElementById('btn-clear-sim-logs');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      document.getElementById('sim-terminal-logs').innerHTML = '<div class="text-slate-600">// Ready. Select a scenario above to start simulation.</div>';
    });
  }

  const copyBtn = document.getElementById('btn-copy-sim-logs');
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const text = document.getElementById('sim-terminal-logs').innerText;
      navigator.clipboard.writeText(text);
      showToast('Execution logs copied to clipboard', 'info');
    });
  }
}

// ============================================================================
// 6. CONSOLE 1: Employee Portal Logic
// ============================================================================
const TEMPLATES = {
  vpn: {
    title: 'VPN tunnel disconnects during wire transfer',
    desc: 'When attempting to complete international wire transfers via the Core Banking terminal, the Cisco AnyConnect VPN client drops connection with error CODE_704 (IPsec tunnel timeout).'
  },
  payment: {
    title: 'Payment Gateway returned HTTP 502 Bad Gateway',
    desc: 'Customer checkout service failed with 502 Bad Gateway during high-volume debit processing. Intermittent upstream timeout connecting to core ledger microservice.'
  },
  hardware: {
    title: 'Workstation ThinkPad Display Flickering on Thunderbolt Dock',
    desc: 'Dual external Dell 4K monitors intermittently go black for 3 seconds every few minutes when connected through the CalDigit Thunderbolt 4 dock at workstation desk 4B.'
  },
  sso: {
    title: 'SSO Okta Authentication Locked for Active Directory Account',
    desc: 'Cannot log into corporate Jira and Confluence wiki portals. Okta displays message: "Your account has been locked due to repeated invalid credential attempts from secondary device."'
  }
};

function setupEmployeePortal() {
  const form = document.getElementById('create-incident-form');
  const titleInput = document.getElementById('inc-title');
  const descInput = document.getElementById('inc-desc');
  const charCounter = document.getElementById('desc-char-count');

  // Character counter & live AI Intent Preview
  descInput.addEventListener('input', () => {
    const len = descInput.value.length;
    charCounter.textContent = `${len} / 4000`;
    if (len >= 20) {
      charCounter.classList.remove('text-red-400');
      charCounter.classList.add('text-emerald-400');
    } else {
      charCounter.classList.add('text-red-400');
      charCounter.classList.remove('text-emerald-400');
    }
    updateAiPreview(titleInput.value, descInput.value);
  });

  titleInput.addEventListener('input', () => {
    updateAiPreview(titleInput.value, descInput.value);
  });

  // Quick Template Chips
  document.querySelectorAll('.template-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const tKey = chip.getAttribute('data-template');
      const tData = TEMPLATES[tKey];
      if (tData) {
        titleInput.value = tData.title;
        descInput.value = tData.desc;
        charCounter.textContent = `${descInput.value.length} / 4000`;
        charCounter.classList.add('text-emerald-400');
        updateAiPreview(titleInput.value, descInput.value);
        showToast(`Template "${chip.textContent.trim()}" loaded`, 'info');
      }
    });
  });

  // Form Submission
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-incident');
    btn.disabled = true;
    btn.innerHTML = `
      <svg class="animate-spin -ml-1 mr-2 w-4 h-4 text-white" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path></svg>
      Submitting...
    `;

    try {
      const payload = {
        title: titleInput.value.trim(),
        description: descInput.value.trim()
      };
      const created = await apiCall('/api/employee/incidents', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast(`Incident created successfully! Ticket ID: ${created.id}`, 'success');
      form.reset();
      charCounter.textContent = '0 / 4000';
      charCounter.classList.remove('text-emerald-400');
      loadEmployeeTickets();
    } catch (err) {
      showToast(`Submission failed: ${err.message}`, 'error');
    } finally {
      btn.disabled = false;
      btn.innerHTML = `<span>Submit Service Ticket</span><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>`;
    }
  });

  const refreshBtn = document.getElementById('btn-refresh-employee-tickets');
  if (refreshBtn) refreshBtn.addEventListener('click', loadEmployeeTickets);
}

function updateAiPreview(title, desc) {
  const text = (title + ' ' + desc).toLowerCase();
  let category = 'NETWORK';
  let service = 'SVC-VPN (VPN Gateway)';

  if (text.includes('payment') || text.includes('gateway') || text.includes('checkout') || text.includes('stripe')) {
    category = 'CLOUD';
    service = 'SVC-PAYMENT (Payment Gateway)';
  } else if (text.includes('laptop') || text.includes('monitor') || text.includes('dock') || text.includes('screen') || text.includes('hardware')) {
    category = 'HARDWARE';
    service = 'SVC-HARDWARE (Workstation & Peripherals)';
  } else if (text.includes('database') || text.includes('sql') || text.includes('query') || text.includes('postgres') || text.includes('oracle')) {
    category = 'DATABASE';
    service = 'SVC-DWH (Data Warehouse)';
  } else if (text.includes('sso') || text.includes('okta') || text.includes('login') || text.includes('password') || text.includes('auth')) {
    category = 'SECURITY';
    service = 'SVC-SSO (Single Sign-On Identity)';
  } else if (text.includes('wiki') || text.includes('confluence') || text.includes('portal')) {
    category = 'APPLICATION';
    service = 'SVC-WIKI (Intranet Portal)';
  }

  const catEl = document.getElementById('preview-category');
  const svcEl = document.getElementById('preview-service');
  if (catEl) catEl.textContent = category;
  if (svcEl) svcEl.textContent = service;
}

async function loadEmployeeTickets() {
  const container = document.getElementById('employee-tickets-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center py-12 text-slate-500 text-xs">Loading your tickets...</div>';

  try {
    const tickets = await apiCall('/api/employee/incidents');
    if (!tickets || tickets.length === 0) {
      container.innerHTML = `
        <div class="text-center py-16 text-slate-500 space-y-2">
          <div class="w-12 h-12 rounded-full bg-slate-800 text-slate-400 mx-auto flex items-center justify-center">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          </div>
          <p class="text-sm font-semibold text-slate-300">No active incidents found</p>
          <p class="text-xs text-slate-500">Submit your first service ticket using the form on the left.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = '';
    tickets.forEach(ticket => {
      const card = document.createElement('div');
      card.className = 'p-5 rounded-xl bg-slate-950 border border-slate-800/90 space-y-4 hover:border-slate-700 transition shadow-sm';

      const currentStep = getStepperIndex(ticket.status);

      card.innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
          <div>
            <div class="flex items-center gap-2">
              <span class="font-mono font-bold text-white text-sm bg-slate-800/80 px-2 py-0.5 rounded border border-slate-700">${ticket.id}</span>
              <span class="px-2 py-0.5 rounded bg-slate-800 text-indigo-300 text-[10px] font-semibold border border-indigo-900/50">${ticket.category}</span>
            </div>
            <h4 class="text-sm font-semibold text-white mt-1.5">${escapeHtml(ticket.title)}</h4>
          </div>
          <div class="text-right">
            <span class="px-2.5 py-1 rounded-full text-xs font-semibold ${getStatusBadgeClass(ticket.status)}">${ticket.status}</span>
            <div class="text-[11px] text-slate-500 font-mono mt-1">Reported: ${formatTime(ticket.createdAt)}</div>
          </div>
        </div>

        <p class="text-xs text-slate-300 leading-relaxed">${escapeHtml(ticket.description)}</p>

        <!-- 5-Stage Stepper -->
        <div class="stepper-container">
          <div class="stepper-bar"></div>
          ${renderStep('Reported', 1, currentStep)}
          ${renderStep('Triaged', 2, currentStep)}
          ${renderStep('Investigating', 3, currentStep)}
          ${renderStep('Resolved', 4, currentStep)}
          ${renderStep('Closed', 5, currentStep)}
        </div>

        <!-- Employee Contextual Actions -->
        ${renderEmployeeActions(ticket)}
      `;

      container.appendChild(card);
    });
  } catch (err) {
    container.innerHTML = `<div class="text-center py-8 text-red-400 text-xs">Error loading incidents: ${err.message}</div>`;
  }
}

function getStepperIndex(status) {
  switch (status) {
    case 'NEW': return 1;
    case 'CLASSIFIED':
    case 'ASSIGNED': return 2;
    case 'ACKNOWLEDGED':
    case 'IN_PROGRESS':
    case 'PENDING_INFO': return 3;
    case 'RESOLVED': return 4;
    case 'VERIFIED':
    case 'CLOSED': return 5;
    default: return 1;
  }
}

function renderStep(label, stepNum, current) {
  let stepClass = '';
  if (stepNum < current) stepClass = 'completed';
  else if (stepNum === current) stepClass = 'current';

  return `
    <div class="stepper-step ${stepClass}">
      <div class="step-node">${stepNum < current ? '✓' : stepNum}</div>
      <span class="step-label">${label}</span>
    </div>
  `;
}

function renderEmployeeActions(ticket) {
  if (ticket.status === 'PENDING_INFO') {
    return `
      <div class="pt-3 border-t border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-amber-950/20 p-3 rounded-lg border border-amber-900/40">
        <div class="text-xs text-amber-300">
          <span class="font-bold">Action Required:</span> Technician requested additional diagnostics.
        </div>
        <button class="px-3.5 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-bold shadow-md transition whitespace-nowrap" onclick="openProvideInfoModal('${ticket.id}')">
          Provide Clarification
        </button>
      </div>
    `;
  }
  if (ticket.status === 'RESOLVED') {
    return `
      <div class="pt-3 border-t border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-2 bg-emerald-950/20 p-3 rounded-lg border border-emerald-900/40">
        <div class="text-xs text-emerald-300">
          <span class="font-bold">Fix Applied:</span> Please test and verify if the issue is completely resolved.
        </div>
        <button class="px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition whitespace-nowrap" onclick="openVerifyModal('${ticket.id}')">
          Verify &amp; Rate Fix (1&ndash;5★)
        </button>
      </div>
    `;
  }
  return '';
}

function openProvideInfoModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-amber-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
      Provide Additional Information
    </h3>
    <p class="text-xs text-slate-400">Your technician has temporarily paused the SLA resolution clock while awaiting your response.</p>
    <div>
      <label class="block text-xs font-semibold text-slate-300 mb-1">Your Diagnostic Details</label>
      <textarea id="modal-info-input" rows="4" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="Enter logs, software versions, screenshot description, or error codes..."></textarea>
    </div>
    <div class="flex justify-end gap-2 pt-2 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white shadow-md" onclick="submitProvideInfo('${ticketId}')">Submit &amp; Resume SLA Clock</button>
    </div>
  `);
}

async function submitProvideInfo(ticketId) {
  const info = document.getElementById('modal-info-input').value;
  if (!info || info.trim().length === 0) return showToast('Please enter clarification text', 'warning');
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/provide-info`, {
      method: 'POST',
      body: JSON.stringify({ responseText: info.trim() })
    });
    closeModal();
    showToast('Information submitted! Technician notified and SLA resumed.', 'success');
    loadEmployeeTickets();
  } catch (err) {
    showToast(`Failed to provide info: ${err.message}`, 'error');
  }
}

function openVerifyModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-emerald-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      Verify Incident Resolution
    </h3>
    <p class="text-xs text-slate-400">Did the technician's solution completely resolve your problem?</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Customer Satisfaction Rating (1 to 5 Stars)</label>
        <select id="modal-verify-rating" class="bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white w-full focus:outline-none focus:ring-1 focus:ring-indigo-500">
          <option value="5">⭐⭐⭐⭐⭐ 5 Stars (Excellent Service)</option>
          <option value="4">⭐⭐⭐⭐ 4 Stars (Good Service)</option>
          <option value="3">⭐⭐⭐ 3 Stars (Satisfactory)</option>
          <option value="2">⭐⭐ 2 Stars (Needs Improvement)</option>
          <option value="1">⭐ 1 Star (Unsatisfactory)</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Feedback Comments</label>
        <input type="text" id="modal-verify-feedback" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="Optional comments on service speed, communication, and solution quality...">
      </div>
    </div>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-2 pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-red-950/60 text-red-300 border border-red-800 hover:bg-red-900 text-xs font-semibold w-full sm:w-auto" onclick="rejectFix('${ticketId}')">
        Issue Persists (Reopen Ticket)
      </button>
      <div class="flex gap-2 w-full sm:w-auto justify-end">
        <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
        <button class="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-bold text-white shadow-md" onclick="acceptFix('${ticketId}')">
          Verify &amp; Close Ticket
        </button>
      </div>
    </div>
  `);
}

async function acceptFix(ticketId) {
  const rating = parseInt(document.getElementById('modal-verify-rating').value);
  const comment = document.getElementById('modal-verify-feedback').value || 'Resolution verified.';
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/verify`, { method: 'POST' });
    await apiCall(`/api/employee/incidents/${ticketId}/feedback`, {
      method: 'POST',
      body: JSON.stringify({ rating, comment })
    });
    closeModal();
    showToast('Thank you! Ticket verified and officially closed with your CSAT rating.', 'success');
    loadEmployeeTickets();
  } catch (err) {
    showToast(`Verification failed: ${err.message}`, 'error');
  }
}

async function rejectFix(ticketId) {
  const reason = prompt('Please explain why the issue persists (required to reopen ticket):');
  if (!reason || reason.trim().length === 0) return;
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason.trim() })
    });
    closeModal();
    showToast('Resolution rejected. Ticket reopened and returned to technician queue.', 'warning');
    loadEmployeeTickets();
  } catch (err) {
    showToast(`Rejection failed: ${err.message}`, 'error');
  }
}

// ============================================================================
// 7. CONSOLE 2: Technician Console Logic
// ============================================================================
async function loadTechnicianQueue() {
  const container = document.getElementById('tech-queue-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center py-16 text-slate-500 text-xs">Loading queue...</div>';

  try {
    const queue = await apiCall('/api/technician/queue');
    if (!queue || queue.length === 0) {
      container.innerHTML = `
        <div class="text-center py-20 text-slate-500 space-y-2">
          <div class="w-12 h-12 rounded-full bg-slate-800 text-slate-400 mx-auto flex items-center justify-center">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M5 13l4 4L19 7"/></svg>
          </div>
          <p class="text-sm font-semibold text-slate-300">Your queue is currently clear!</p>
          <p class="text-xs text-slate-500">All assigned tickets have been addressed or resolved.</p>
        </div>
      `;
      updateTechMetrics([]);
      return;
    }

    updateTechMetrics(queue);
    container.innerHTML = '';

    queue.forEach(ticket => {
      const card = document.createElement('div');
      const priorityClass = getPriorityBorderClass(ticket.priority);
      card.className = `p-5 rounded-xl bg-slate-950 border border-slate-800 space-y-4 hover:border-slate-700 transition shadow-sm ${priorityClass}`;

      card.innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
          <div class="flex items-center gap-2.5 flex-wrap">
            <span class="px-2.5 py-1 rounded text-xs font-bold font-mono ${getPriorityBadgeClass(ticket.priority)}">${ticket.priority}</span>
            <span class="font-mono font-bold text-white text-sm">${ticket.id}</span>
            <span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-semibold">${ticket.category}</span>
            ${ticket.serviceId ? `<span class="px-2 py-0.5 rounded bg-slate-850 text-cyan-400 text-[10px] font-mono">${ticket.serviceId}</span>` : ''}
          </div>
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-0.5 rounded text-[11px] font-bold ${getSlaBadgeClass(ticket.slaStatus)}">${ticket.slaStatus || 'ON_TRACK'}</span>
            <span class="px-2.5 py-1 rounded-full text-xs font-semibold ${getStatusBadgeClass(ticket.status)}">${ticket.status}</span>
          </div>
        </div>

        <div>
          <h4 class="text-sm font-semibold text-white">${escapeHtml(ticket.title)}</h4>
          <p class="text-xs text-slate-400 mt-1 leading-relaxed">${escapeHtml(ticket.description)}</p>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] text-slate-400 bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/80 font-mono">
          <div><span class="text-slate-500 font-sans">Reporter:</span> ${ticket.reporterId || 'Unknown'}</div>
          <div><span class="text-slate-500 font-sans">Reopen Count:</span> ${ticket.reopenCount || 0}</div>
          <div><span class="text-slate-500 font-sans">SLA Target:</span> ${ticket.slaDeadline ? formatTime(ticket.slaDeadline) : 'N/A'}</div>
          <div><span class="text-slate-500 font-sans">Created:</span> ${formatTime(ticket.createdAt)}</div>
        </div>

        <!-- Action Buttons -->
        <div class="pt-2 flex flex-wrap gap-2 justify-end">
          ${renderTechnicianActionButtons(ticket)}
        </div>
      `;

      container.appendChild(card);
    });
  } catch (err) {
    container.innerHTML = `<div class="text-center py-8 text-red-400 text-xs">Error loading queue: ${err.message}</div>`;
  }
}

function updateTechMetrics(queue) {
  const assigned = queue.length;
  const investigating = queue.filter(t => t.status === 'IN_PROGRESS').length;
  const pending = queue.filter(t => t.status === 'PENDING_INFO').length;
  const atRisk = queue.filter(t => t.slaStatus === 'AT_RISK' || t.slaStatus === 'WARNING' || t.slaStatus === 'BREACHED').length;

  document.getElementById('tech-metric-assigned').textContent = assigned;
  document.getElementById('tech-metric-investigating').textContent = investigating;
  document.getElementById('tech-metric-pending').textContent = pending;
  document.getElementById('tech-metric-atrisk').textContent = atRisk;
}

function renderTechnicianActionButtons(ticket) {
  let btns = '';

  if (ticket.status === 'ASSIGNED') {
    btns += `<button class="px-3.5 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition" onclick="techAcknowledge('${ticket.id}')">Acknowledge</button>`;
  }
  if (ticket.status === 'ACKNOWLEDGED') {
    btns += `<button class="px-3.5 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold shadow-sm transition" onclick="techStartInvestigation('${ticket.id}')">Start Investigation</button>`;
  }
  if (ticket.status === 'IN_PROGRESS' || ticket.status === 'PENDING_INFO') {
    btns += `
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition" onclick="openAddNoteModal('${ticket.id}')">Add Note</button>
      <button class="px-3 py-1.5 rounded-lg bg-cyan-950/70 hover:bg-cyan-900 text-cyan-300 border border-cyan-800/50 text-xs font-medium transition" onclick="openRequestInfoModal('${ticket.id}')">Request Info</button>
      <button class="px-3 py-1.5 rounded-lg bg-rose-950/70 hover:bg-rose-900 text-rose-300 border border-rose-800/50 text-xs font-medium transition" onclick="openWrongDomainModal('${ticket.id}')">Wrong Domain</button>
      <button class="px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-sm transition" onclick="openResolveModal('${ticket.id}', '${ticket.category}')">Resolve Ticket</button>
    `;
  }
  return btns;
}

async function techAcknowledge(ticketId) {
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/acknowledge`, { method: 'POST' });
    showToast(`Ticket ${ticketId} acknowledged`, 'success');
    loadTechnicianQueue();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function techStartInvestigation(ticketId) {
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/start-investigation`, { method: 'POST' });
    showToast(`Investigation started on ${ticketId}`, 'success');
    loadTechnicianQueue();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openAddNoteModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-indigo-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
      Add Internal Investigation Note
    </h3>
    <p class="text-xs text-slate-400">Internal notes are visible exclusively to IT Support staff and administrators.</p>
    <textarea id="modal-note-text" rows="4" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="Document logs, root cause analysis, or configuration steps..."></textarea>
    <div class="flex justify-end gap-2 pt-2 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white shadow-md" onclick="submitNote('${ticketId}')">Save Note</button>
    </div>
  `);
}

async function submitNote(ticketId) {
  const text = document.getElementById('modal-note-text').value;
  if (!text || text.trim().length === 0) return showToast('Note cannot be blank', 'warning');
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/notes`, {
      method: 'POST',
      body: JSON.stringify({ noteText: text.trim(), internalOnly: true })
    });
    closeModal();
    showToast('Investigation note recorded', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openRequestInfoModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-cyan-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      Request Clarification from Reporter
    </h3>
    <p class="text-xs text-slate-400">Moves ticket to PENDING_INFO and pauses the SLA resolution deadline until the employee responds.</p>
    <textarea id="modal-req-info-text" rows="3" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="Specific technical logs, screenshot details, or reproduction steps required..."></textarea>
    <div class="flex justify-end gap-2 pt-2 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-xs font-bold text-white shadow-md" onclick="submitRequestInfo('${ticketId}')">Pause SLA &amp; Request Info</button>
    </div>
  `);
}

async function submitRequestInfo(ticketId) {
  const q = document.getElementById('modal-req-info-text').value;
  if (!q || q.trim().length === 0) return showToast('Question cannot be blank', 'warning');
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/request-info`, {
      method: 'POST',
      body: JSON.stringify({ responseText: q.trim() })
    });
    closeModal();
    showToast('Clarification requested. SLA clock paused.', 'success');
    loadTechnicianQueue();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openWrongDomainModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-rose-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
      Flag Out-of-Scope (Wrong Domain)
    </h3>
    <p class="text-xs text-slate-400">Reverts ticket to CLASSIFIED state in the Admin Triage Queue for domain reassignment.</p>
    <textarea id="modal-wrong-domain-reason" rows="3" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="Explain why this ticket does not belong to your engineering domain..."></textarea>
    <div class="flex justify-end gap-2 pt-2 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-xs font-bold text-white shadow-md" onclick="submitWrongDomain('${ticketId}')">Return to Triage</button>
    </div>
  `);
}

async function submitWrongDomain(ticketId) {
  const reason = document.getElementById('modal-wrong-domain-reason').value;
  if (!reason || reason.trim().length === 0) return showToast('Reason is required', 'warning');
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/wrong-domain`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason.trim() })
    });
    closeModal();
    showToast('Ticket flagged as wrong domain and returned to Triage Queue.', 'info');
    loadTechnicianQueue();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openResolveModal(ticketId, category) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-emerald-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      Structured Incident Resolution
    </h3>
    <p class="text-xs text-slate-400">Record structured ITIL resolution metadata for post-incident reporting.</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Root Cause Explanation</label>
        <input type="text" id="res-root-cause" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="e.g. Expired SSL certificate on secondary auth gateway">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Resolution Steps Applied</label>
        <input type="text" id="res-steps" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="e.g. Renewed certificate from internal CA and reloaded Nginx proxy">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Preventive Actions</label>
        <input type="text" id="res-preventive" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="e.g. Added automated cert renewal alerts to Datadog monitoring">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Confirmed Category Domain</label>
        <select id="res-category" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500">
          <option value="NETWORK" ${category === 'NETWORK' ? 'selected' : ''}>NETWORK</option>
          <option value="DATABASE" ${category === 'DATABASE' ? 'selected' : ''}>DATABASE</option>
          <option value="APPLICATION" ${category === 'APPLICATION' ? 'selected' : ''}>APPLICATION</option>
          <option value="SECURITY" ${category === 'SECURITY' ? 'selected' : ''}>SECURITY</option>
          <option value="CLOUD" ${category === 'CLOUD' ? 'selected' : ''}>CLOUD</option>
          <option value="HARDWARE" ${category === 'HARDWARE' ? 'selected' : ''}>HARDWARE</option>
          <option value="SOFTWARE" ${category === 'SOFTWARE' ? 'selected' : ''}>SOFTWARE</option>
        </select>
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-xs font-bold text-white shadow-md" onclick="submitResolve('${ticketId}')">Resolve Incident</button>
    </div>
  `);
}

async function submitResolve(ticketId) {
  const rootCause = document.getElementById('res-root-cause').value;
  const stepsTaken = document.getElementById('res-steps').value;
  const preventiveAction = document.getElementById('res-preventive').value || 'Standard operating procedure updated.';
  const confirmedCategory = document.getElementById('res-category').value;

  if (!rootCause || !stepsTaken) return showToast('Root cause and steps taken are required', 'warning');

  try {
    await apiCall(`/api/technician/incidents/${ticketId}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ rootCause, stepsTaken, preventiveAction, confirmedCategory })
    });
    closeModal();
    showToast('Incident resolved! Sent to employee for verification.', 'success');
    loadTechnicianQueue();
  } catch (err) {
    showToast(`Resolution failed: ${err.message}`, 'error');
  }
}

// ============================================================================
// 8. CONSOLE 3: Admin Command Centre Logic
// ============================================================================
async function loadAdminIncidents() {
  const tbody = document.getElementById('admin-incidents-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-500">Loading enterprise catalog...</td></tr>';

  try {
    const list = await apiCall('/api/admin/incidents');
    STATE.adminIncidentsCache = list || [];
    renderAdminIncidentsTable(STATE.adminIncidentsCache);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-8 text-red-400">Error: ${err.message}</td></tr>`;
  }
}

function renderAdminIncidentsTable(list) {
  const tbody = document.getElementById('admin-incidents-tbody');
  if (!tbody) return;

  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-500">No matching incidents in system.</td></tr>';
    return;
  }

  tbody.innerHTML = '';
  list.slice(0, 50).forEach(inc => {
    const tr = document.createElement('tr');
    tr.className = 'border-b border-slate-800/80 hover:bg-slate-900/60 transition';
    tr.innerHTML = `
      <td class="py-3 px-3 font-mono font-bold text-white text-xs">${inc.id}</td>
      <td class="py-3 px-3 max-w-xs truncate text-xs text-slate-200" title="${escapeHtml(inc.title)}">${escapeHtml(inc.title)}</td>
      <td class="py-3 px-3"><span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-mono">${inc.category}</span></td>
      <td class="py-3 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getPriorityBadgeClass(inc.priority)}">${inc.priority || 'N/A'}</span></td>
      <td class="py-3 px-3"><span class="px-2 py-0.5 rounded-full text-[10px] font-semibold ${getStatusBadgeClass(inc.status)}">${inc.status}</span></td>
      <td class="py-3 px-3 font-mono text-[11px] text-slate-400">${inc.assigneeId || '<span class="text-amber-400">UNASSIGNED</span>'}</td>
      <td class="py-3 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getSlaBadgeClass(inc.slaStatus)}">${inc.slaStatus || 'ON_TRACK'}</span></td>
      <td class="py-3 px-3 text-right">
        <button class="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-medium transition" onclick="openTriageModal('${inc.id}', '${inc.category}', '${inc.serviceId || ''}')">Reclassify</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function setupAdminFilters() {
  const searchInput = document.getElementById('admin-search-input');
  const catFilter = document.getElementById('admin-category-filter');

  const filterHandler = () => {
    const query = (searchInput ? searchInput.value : '').toLowerCase().trim();
    const cat = catFilter ? catFilter.value : '';

    let filtered = STATE.adminIncidentsCache;
    if (query) {
      filtered = filtered.filter(i => (i.id && i.id.toLowerCase().includes(query)) || (i.title && i.title.toLowerCase().includes(query)));
    }
    if (cat) {
      filtered = filtered.filter(i => i.category === cat);
    }
    renderAdminIncidentsTable(filtered);
  };

  if (searchInput) searchInput.addEventListener('input', filterHandler);
  if (catFilter) catFilter.addEventListener('change', filterHandler);
}

async function loadAdminTriageQueue() {
  const container = document.getElementById('admin-triage-container');
  if (!container) return;
  container.innerHTML = '<div class="text-center py-8 text-slate-500 text-xs">Scanning for tickets requiring triage...</div>';

  try {
    const all = await apiCall('/api/admin/incidents');
    const triageTickets = (all || []).filter(t => t.status === 'CLASSIFIED' || !t.assigneeId);

    if (triageTickets.length === 0) {
      container.innerHTML = '<div class="text-center py-12 text-slate-500 text-sm">Triage queue empty. All tickets are properly assigned and categorized.</div>';
      return;
    }

    container.innerHTML = '';
    triageTickets.forEach(t => {
      const card = document.createElement('div');
      card.className = 'p-4 rounded-xl bg-slate-950 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-sm';
      card.innerHTML = `
        <div>
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold text-white text-xs">${t.id}</span>
            <span class="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[10px] font-bold">TRIAGE NEEDED</span>
            <span class="text-xs text-slate-400">Domain: <b class="text-white">${t.category}</b></span>
          </div>
          <h4 class="text-xs font-semibold text-white mt-1.5">${escapeHtml(t.title)}</h4>
          <p class="text-[11px] text-slate-400 mt-0.5">${escapeHtml(t.description)}</p>
        </div>
        <div class="flex gap-2 flex-shrink-0">
          <button class="px-3.5 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md whitespace-nowrap transition" onclick="openTriageModal('${t.id}', '${t.category}', '${t.serviceId || ''}')">
            Reclassify &amp; Route
          </button>
        </div>
      `;
      container.appendChild(card);
    });
  } catch (err) {
    container.innerHTML = `<div class="text-center py-8 text-red-400 text-xs">Error: ${err.message}</div>`;
  }
}

function openTriageModal(ticketId, currCategory, currService) {
  openModal(`
    <h3 class="text-base font-bold text-white flex items-center gap-2">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="text-indigo-400"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"/></svg>
      Administrative Reclassification &amp; Routing
    </h3>
    <p class="text-xs text-slate-400">Reclassifies technical domain, recalculates multi-factor priority score, and assigns technician.</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">New Category Domain</label>
        <select id="modal-new-category" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500">
          <option value="DATABASE" ${currCategory === 'DATABASE' ? 'selected' : ''}>DATABASE</option>
          <option value="NETWORK" ${currCategory === 'NETWORK' ? 'selected' : ''}>NETWORK</option>
          <option value="APPLICATION" ${currCategory === 'APPLICATION' ? 'selected' : ''}>APPLICATION</option>
          <option value="SECURITY" ${currCategory === 'SECURITY' ? 'selected' : ''}>SECURITY</option>
          <option value="CLOUD" ${currCategory === 'CLOUD' ? 'selected' : ''}>CLOUD</option>
          <option value="HARDWARE" ${currCategory === 'HARDWARE' ? 'selected' : ''}>HARDWARE</option>
          <option value="SOFTWARE" ${currCategory === 'SOFTWARE' ? 'selected' : ''}>SOFTWARE</option>
          <option value="ACCESS" ${currCategory === 'ACCESS' ? 'selected' : ''}>ACCESS</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Impacted IT Service ID</label>
        <input type="text" id="modal-new-service" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" value="${currService || 'SVC-DWH'}">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Assign Specific Technician ID (Optional)</label>
        <input type="text" id="modal-assign-tech" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="e.g. TECH-DB-1 (Leave blank to use active routing strategy)">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Triage Rationale</label>
        <input type="text" id="modal-triage-reason" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-indigo-500" placeholder="e.g. Reclassified from triage: confirmed database connection exhaustion">
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-slate-300 hover:bg-slate-700" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white shadow-md" onclick="submitTriage('${ticketId}')">Apply Reclassification</button>
    </div>
  `);
}

async function submitTriage(ticketId) {
  const category = document.getElementById('modal-new-category').value;
  const serviceId = document.getElementById('modal-new-service').value;
  const techId = document.getElementById('modal-assign-tech').value;
  const reason = document.getElementById('modal-triage-reason').value || 'Administrative triage reclassification';

  try {
    await apiCall(`/api/admin/incidents/${ticketId}/reclassify`, {
      method: 'POST',
      body: JSON.stringify({ category, serviceId, reason })
    });

    if (techId && techId.trim().length > 0) {
      await apiCall(`/api/admin/incidents/${ticketId}/assign`, {
        method: 'POST',
        body: JSON.stringify({ technicianId: techId.trim() })
      });
    }

    closeModal();
    showToast(`Ticket ${ticketId} reclassified successfully!`, 'success');
    loadAdminTriageQueue();
    loadAdminIncidents();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function loadAssignmentStrategies() {
  const container = document.getElementById('strategy-cards-container');
  if (!container) return;

  try {
    const strategies = await apiCall('/api/admin/assignment/strategies');
    if (!strategies) return;

    const activeStrategy = strategies.find(s => s.active) || strategies[0];
    const nameEl = document.getElementById('current-active-strategy-name');
    const descEl = document.getElementById('current-active-strategy-desc');
    if (nameEl && activeStrategy) nameEl.textContent = activeStrategy.name;
    if (descEl && activeStrategy) descEl.textContent = activeStrategy.description;

    container.innerHTML = '';
    strategies.forEach(st => {
      const card = document.createElement('div');
      card.className = `p-4 rounded-xl border transition flex flex-col justify-between ${st.active ? 'bg-indigo-950/40 border-indigo-500/50 shadow-lg' : 'bg-slate-950 border-slate-800 hover:border-slate-700'}`;
      card.innerHTML = `
        <div>
          <div class="flex items-center justify-between">
            <span class="font-mono text-xs font-bold ${st.active ? 'text-cyan-400' : 'text-white'}">${st.name}</span>
            ${st.active ? '<span class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 text-[10px] font-bold">ACTIVE</span>' : ''}
          </div>
          <p class="text-xs text-slate-400 mt-2 leading-relaxed">${st.description}</p>
        </div>
        ${!st.active ? `
          <button class="mt-4 w-full py-1.5 px-3 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition" onclick="activateStrategy('${st.name}')">
            Activate Strategy
          </button>
        ` : '<div class="mt-4 py-1 text-center text-[11px] text-emerald-400 font-semibold font-mono">Routing Active Tickets</div>'}
      `;
      container.appendChild(card);
    });
  } catch (err) {
    console.error('Error loading strategies', err);
  }
}

async function activateStrategy(name) {
  try {
    await apiCall('/api/admin/assignment/strategies/active', {
      method: 'PUT',
      body: JSON.stringify({ strategyName: name })
    });
    showToast(`Swapped active technician routing strategy to: ${name}`, 'success');
    loadAssignmentStrategies();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// ============================================================================
// 9. CONSOLE 5: Executive Reports Logic
// ============================================================================
async function loadExecutiveReports() {
  try {
    const [mttr, sla, workload, services, escalations] = await Promise.all([
      apiCall('/api/admin/reports/mttr'),
      apiCall('/api/admin/reports/sla'),
      apiCall('/api/admin/reports/workload'),
      apiCall('/api/admin/reports/services'),
      apiCall('/api/admin/reports/escalations')
    ]);

    // KPI Cards
    const mttrEl = document.getElementById('kpi-mttr-val');
    const mttRespEl = document.getElementById('kpi-mttresp-val');
    const slaCompEl = document.getElementById('kpi-sla-compliance-val');

    if (mttrEl) mttrEl.textContent = mttr ? Math.round(mttr.mttrMinutes || 0) : '0';
    if (mttRespEl) mttRespEl.textContent = mttr ? Math.round(mttr.mttRespMinutes || 0) : '0';
    if (slaCompEl) slaCompEl.textContent = sla ? `${Math.round(sla.compliancePercentage || 100)}%` : '100%';

    // Workload Table
    const workloadContainer = document.getElementById('workload-table-container');
    if (workloadContainer) {
      workloadContainer.innerHTML = '';
      (workload.teams || []).forEach(tm => {
        const row = document.createElement('div');
        row.className = 'p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs space-y-1.5';
        const pct = Math.min(100, Math.round((tm.utilizationRate || 0) * 100));
        row.innerHTML = `
          <div class="flex justify-between font-semibold text-white">
            <span>${tm.teamName} (${tm.category})</span>
            <span class="font-mono text-cyan-400">${tm.activeIncidentCount} tickets / ${tm.totalCapacity} capacity</span>
          </div>
          <div class="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
            <div class="h-full bg-gradient-to-r from-indigo-500 to-cyan-400 rounded-full" style="width: ${pct}%"></div>
          </div>
          <div class="flex justify-between text-[11px] text-slate-500">
            <span>Technicians: ${tm.technicianCount}</span>
            <span>Utilization: ${pct}%</span>
          </div>
        `;
        workloadContainer.appendChild(row);
      });
    }

    // Service Analysis
    const servicesContainer = document.getElementById('service-analysis-container');
    if (servicesContainer) {
      servicesContainer.innerHTML = '';
      (services.serviceBreakdowns || []).slice(0, 5).forEach(svc => {
        const row = document.createElement('div');
        row.className = 'p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs flex justify-between items-center';
        row.innerHTML = `
          <div>
            <span class="font-bold text-white">${svc.serviceName}</span>
            <div class="text-[11px] text-slate-500 font-mono">${svc.criticality}</div>
          </div>
          <div class="text-right">
            <span class="font-bold font-mono text-cyan-400">${svc.incidentCount} incidents</span>
            <div class="text-[11px] text-slate-500">${Math.round(svc.percentageOfTotal || 0)}% of total volume</div>
          </div>
        `;
        servicesContainer.appendChild(row);
      });
    }

    // SLA Breakdown
    const slaContainer = document.getElementById('sla-breakdown-container');
    if (slaContainer) {
      slaContainer.innerHTML = `
        <div class="grid grid-cols-2 gap-3 text-xs">
          <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <span class="text-slate-400 font-medium">Total Analyzed</span>
            <p class="text-xl font-bold text-white font-mono mt-1">${sla.totalIncidents || 0}</p>
          </div>
          <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <span class="text-emerald-400 font-medium">SLA Target Met</span>
            <p class="text-xl font-bold text-emerald-400 font-mono mt-1">${sla.compliantIncidents || 0}</p>
          </div>
          <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <span class="text-red-400 font-medium">SLA Breached</span>
            <p class="text-xl font-bold text-red-400 font-mono mt-1">${sla.breachedIncidents || 0}</p>
          </div>
          <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
            <span class="text-cyan-400 font-medium">Compliance Rate</span>
            <p class="text-xl font-bold text-cyan-400 font-mono mt-1">${Math.round(sla.compliancePercentage || 100)}%</p>
          </div>
        </div>
      `;
    }

    // Escalations Log
    const escContainer = document.getElementById('escalation-log-container');
    if (escContainer) {
      escContainer.innerHTML = '';
      const escList = escalations.escalations || [];
      if (escList.length === 0) {
        escContainer.innerHTML = '<div class="text-center py-6 text-slate-500 text-xs">No escalations recorded.</div>';
      } else {
        escList.slice(0, 10).forEach(esc => {
          const item = document.createElement('div');
          item.className = 'p-2.5 bg-slate-950 rounded-lg border border-slate-850 text-[11px] flex justify-between items-center';
          item.innerHTML = `
            <div>
              <span class="font-bold text-red-400 font-mono">Tier ${esc.tier} (${esc.toRole})</span>
              <span class="text-slate-300 ml-1 font-semibold">Ticket ${esc.incidentId}</span>
              <div class="text-slate-400 text-[10px] truncate max-w-sm">${escapeHtml(esc.reason)}</div>
            </div>
            <span class="text-slate-500 font-mono text-[10px] whitespace-nowrap">${formatTime(esc.timestamp)}</span>
          `;
          escContainer.appendChild(item);
        });
      }
    }

  } catch (err) {
    console.error('Error loading reports', err);
  }
}

// ============================================================================
// 10. In-App Notifications Modal
// ============================================================================
async function loadNotifications() {
  try {
    const list = await apiCall('/api/notifications');
    const badge = document.getElementById('unread-count-badge');
    const unread = (list || []).filter(n => !n.read && !n.delivered).length;
    if (unread > 0) {
      badge.textContent = unread;
      badge.classList.remove('hidden');
    } else {
      badge.classList.add('hidden');
    }

    const container = document.getElementById('notifications-list-container');
    if (!container) return;
    container.innerHTML = '';
    if (!list || list.length === 0) {
      container.innerHTML = '<div class="text-center py-6 text-slate-500">No notifications.</div>';
      return;
    }

    list.slice(0, 15).forEach(n => {
      const item = document.createElement('div');
      item.className = `p-3 rounded-xl border ${n.delivered ? 'bg-slate-950 border-slate-800 text-slate-400' : 'bg-indigo-950/40 border-indigo-700/60 text-white font-medium'}`;
      item.innerHTML = `
        <div class="flex justify-between items-start gap-2">
          <span class="font-semibold text-xs text-indigo-300">${escapeHtml(n.subject || 'System Notification')}</span>
          <span class="text-[10px] text-slate-500 font-mono">${formatTime(n.createdAt)}</span>
        </div>
        <p class="text-xs text-slate-300 mt-1">${escapeHtml(n.content || '')}</p>
      `;
      container.appendChild(item);
    });
  } catch (e) {}
}

function setupNotificationsModal() {
  const bell = document.getElementById('notification-bell-btn');
  const modal = document.getElementById('notification-modal');
  const closeBtn = document.getElementById('btn-close-notifications');

  if (bell && modal) {
    bell.addEventListener('click', () => {
      loadNotifications();
      modal.classList.remove('hidden');
    });
  }

  if (closeBtn && modal) {
    closeBtn.addEventListener('click', () => {
      modal.classList.add('hidden');
    });
  }
}

// ============================================================================
// 11. Modal Dialog & Utility Helpers
// ============================================================================
function openModal(html) {
  const container = document.getElementById('modal-container');
  const content = document.getElementById('modal-content');
  if (content && container) {
    content.innerHTML = html;
    container.classList.remove('hidden');
  }
}

function closeModal() {
  const container = document.getElementById('modal-container');
  if (container) container.classList.add('hidden');
}

// Close modals on Escape key or clicking outside backdrop
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeModal();
    const notifModal = document.getElementById('notification-modal');
    if (notifModal) notifModal.classList.add('hidden');
  }
});

document.addEventListener('click', (e) => {
  const modalContainer = document.getElementById('modal-container');
  if (e.target === modalContainer) closeModal();
  const notifModal = document.getElementById('notification-modal');
  if (e.target === notifModal) notifModal.classList.add('hidden');
});

function getPriorityBadgeClass(p) {
  switch (p) {
    case 'P1': return 'badge-p1';
    case 'P2': return 'badge-p2';
    case 'P3': return 'badge-p3';
    case 'P4': return 'badge-p4';
    default: return 'bg-slate-800 text-slate-300';
  }
}

function getPriorityBorderClass(p) {
  switch (p) {
    case 'P1': return 'border-l-p1';
    case 'P2': return 'border-l-p2';
    case 'P3': return 'border-l-p3';
    case 'P4': return 'border-l-p4';
    default: return '';
  }
}

function getSlaBadgeClass(s) {
  switch (s) {
    case 'ON_TRACK': return 'sla-on-track';
    case 'AT_RISK': return 'sla-at-risk';
    case 'WARNING': return 'sla-warning';
    case 'BREACHED': return 'sla-breached';
    default: return 'sla-on-track';
  }
}

function getStatusBadgeClass(st) {
  switch (st) {
    case 'NEW': return 'bg-indigo-950 text-indigo-300 border border-indigo-800/80';
    case 'ASSIGNED': return 'bg-cyan-950 text-cyan-300 border border-cyan-800/80';
    case 'ACKNOWLEDGED': return 'bg-blue-950 text-blue-300 border border-blue-800/80';
    case 'IN_PROGRESS': return 'bg-amber-950 text-amber-300 border border-amber-800/80';
    case 'PENDING_INFO': return 'bg-purple-950 text-purple-300 border border-purple-800/80';
    case 'RESOLVED': return 'bg-emerald-950 text-emerald-300 border border-emerald-800/80';
    case 'VERIFIED':
    case 'CLOSED': return 'bg-slate-800 text-slate-400 border border-slate-700';
    default: return 'bg-slate-800 text-slate-300';
  }
}

function formatTime(isoStr) {
  if (!isoStr) return '--:--';
  return isoStr.replace('T', ' ').substring(0, 19);
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
}

// ============================================================================
// 12. Application Bootstrapper
// ============================================================================
document.addEventListener('DOMContentLoaded', async () => {
  setupNavigation();
  setupEmployeePortal();
  setupAdminFilters();
  setupSimulationControls();
  setupNotificationsModal();

  const techRefreshBtn = document.getElementById('btn-refresh-tech-queue');
  if (techRefreshBtn) techRefreshBtn.addEventListener('click', loadTechnicianQueue);

  // Authenticate initially as System Administrator
  await authenticateUser('admin@nimbusfinserve.com', false);
  pollSimulationStatus();
  setInterval(pollSimulationStatus, 3000);
});
