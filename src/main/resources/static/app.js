/**
 * Nimbus FinServe :: Intelligent IT Service Management (I-ITSM)
 * Pure JavaScript Single-Page Application Client
 */

const STATE = {
  currentUser: {
    email: 'admin@nimbusfinserve.com',
    role: 'ROLE_ADMIN',
    name: 'System Administrator',
    token: null
  },
  simulation: {
    virtualTime: null,
    multiplier: 1,
    paused: false
  },
  activeTab: 'console-employee'
};

// ==========================================
// API Client with Automatic JWT Handling
// ==========================================
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
      await authenticateUser(STATE.currentUser.email);
      return apiCall(endpoint, options);
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: res.statusText }));
      throw new Error(err.message || 'API request failed');
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

// ==========================================
// Authentication & Persona Switching
// ==========================================
async function authenticateUser(email) {
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email, password: 'Password123!' })
    });
    if (!res.ok) throw new Error('Authentication failed');
    const data = await res.json();
    STATE.currentUser.token = data.token;
    STATE.currentUser.email = data.email;
    STATE.currentUser.role = data.role;
    STATE.currentUser.name = data.name || email.split('@')[0];

    document.getElementById('active-user-name').textContent = STATE.currentUser.name;
    document.getElementById('active-user-role').textContent = STATE.currentUser.role;

    console.log(`Authenticated as ${STATE.currentUser.name} (${STATE.currentUser.role})`);
    refreshActiveView();
    loadNotifications();
  } catch (err) {
    console.error('Login failed for ' + email, err);
  }
}

// ==========================================
// Navigation & Tab Switching
// ==========================================
function setupNavigation() {
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

      tab.classList.add('active');
      const targetId = tab.getAttribute('data-target');
      document.getElementById(targetId).classList.add('active');
      STATE.activeTab = targetId;

      refreshActiveView();
    });
  });

  // Admin subtabs
  document.querySelectorAll('.admin-subtab').forEach(subtab => {
    subtab.addEventListener('click', () => {
      document.querySelectorAll('.admin-subtab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));

      subtab.classList.add('active');
      const target = subtab.getAttribute('data-subtarget');
      document.getElementById(target).classList.add('active');

      if (target === 'admin-triage-queue') loadAdminTriageQueue();
      if (target === 'admin-strategy-manager') loadAssignmentStrategies();
      if (target === 'admin-all-incidents') loadAdminIncidents();
    });
  });

  // Persona selector
  const personaSelect = document.getElementById('persona-selector');
  personaSelect.addEventListener('change', (e) => {
    authenticateUser(e.target.value);
  });
}

function refreshActiveView() {
  if (STATE.activeTab === 'console-employee') {
    loadEmployeeTickets();
  } else if (STATE.activeTab === 'console-technician') {
    loadTechnicianQueue();
  } else if (STATE.activeTab === 'console-admin') {
    loadAdminIncidents();
  } else if (STATE.activeTab === 'console-reports') {
    loadExecutiveReports();
  } else if (STATE.activeTab === 'console-simulation') {
    updateClockDisplay();
  }
}

// ==========================================
// Simulation Clock & Speed Controls
// ==========================================
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
  const speedStr = `${STATE.simulation.speedMultiplier}x`;

  document.getElementById('virtual-time-display').textContent = timeStr;
  document.getElementById('virtual-speed-badge').textContent = speedStr;
  document.getElementById('sim-clock-display').textContent = timeStr;

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
  // Speed multiplier buttons
  document.querySelectorAll('.sim-speed-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const spd = btn.getAttribute('data-speed');
      await apiCall(`/api/simulation/speed?multiplier=${spd}`, { method: 'POST' });
      pollSimulationStatus();
    });
  });

  // Fast forward buttons
  document.querySelectorAll('.sim-ff-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const mins = btn.getAttribute('data-minutes');
      await apiCall(`/api/simulation/advance?minutes=${mins}`, { method: 'POST' });
      pollSimulationStatus();
      refreshActiveView();
    });
  });

  // Reset clock button
  document.getElementById('btn-clock-reset').addEventListener('click', async () => {
    await apiCall('/api/simulation/reset', { method: 'POST' });
    pollSimulationStatus();
    refreshActiveView();
  });

  // Scenario Launchers
  document.querySelectorAll('.btn-run-scenario').forEach(btn => {
    btn.addEventListener('click', async () => {
      const scenarioId = btn.getAttribute('data-scenario');
      const terminal = document.getElementById('sim-terminal-logs');
      terminal.innerHTML = `<div class="text-cyan-400 font-bold">> Launching Scenario ${scenarioId}...</div>`;

      btn.disabled = true;
      btn.textContent = 'Running...';

      try {
        const result = await apiCall(`/api/simulation/run/${scenarioId}`, { method: 'POST' });
        terminal.innerHTML = '';
        (result.stepLogs || []).forEach(log => {
          const div = document.createElement('div');
          if (log.includes('Ticket created') || log.includes('acknowledged')) {
            div.className = 'text-emerald-400';
          } else if (log.includes('Escalated') || log.includes('Breach')) {
            div.className = 'text-red-400 font-semibold';
          } else if (log.includes('warning') || log.includes('Paused')) {
            div.className = 'text-amber-400';
          } else {
            div.className = 'text-slate-300';
          }
          div.textContent = log;
          terminal.appendChild(div);
        });

        const summary = document.createElement('div');
        summary.className = 'pt-2 text-cyan-300 font-bold border-t border-slate-800';
        summary.textContent = `[${result.scenarioId}] Status: ${result.success ? 'SUCCESS' : 'FAILED'} - ${result.summary}`;
        terminal.appendChild(summary);
        terminal.scrollTop = terminal.scrollHeight;

        pollSimulationStatus();
        refreshActiveView();
      } catch (err) {
        terminal.innerHTML += `<div class="text-red-400 font-bold">Execution Error: ${err.message}</div>`;
      } finally {
        btn.disabled = false;
        btn.textContent = `Run ${scenarioId}`;
      }
    });
  });

  document.getElementById('btn-clear-sim-logs').addEventListener('click', () => {
    document.getElementById('sim-terminal-logs').innerHTML = '<div class="text-slate-600">// Ready. Select a scenario above to start simulation.</div>';
  });
}

// ==========================================
// CONSOLE 1: Employee Portal Logic
// ==========================================
function setupEmployeePortal() {
  const form = document.getElementById('create-incident-form');
  const titleInput = document.getElementById('inc-title');
  const descInput = document.getElementById('inc-desc');
  const charCounter = document.getElementById('desc-char-count');

  descInput.addEventListener('input', () => {
    charCounter.textContent = `${descInput.value.length} / 4000`;
    if (descInput.value.length >= 20) {
      charCounter.classList.remove('text-red-400');
      charCounter.classList.add('text-slate-500');
    } else {
      charCounter.classList.add('text-red-400');
      charCounter.classList.remove('text-slate-500');
    }
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-incident');
    btn.disabled = true;
    btn.textContent = 'Submitting...';

    try {
      const payload = {
        title: titleInput.value.trim(),
        description: descInput.value.trim()
      };
      const created = await apiCall('/api/employee/incidents', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      alert(`Incident created successfully!\nTicket ID: ${created.id}\nCategory: ${created.category}`);
      form.reset();
      charCounter.textContent = '0 / 4000';
      loadEmployeeTickets();
    } catch (err) {
      alert(`Error submitting ticket: ${err.message}`);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Submit Ticket';
    }
  });

  document.getElementById('btn-refresh-employee-tickets').addEventListener('click', loadEmployeeTickets);
}

async function loadEmployeeTickets() {
  const container = document.getElementById('employee-tickets-container');
  container.innerHTML = '<div class="text-center py-8 text-slate-500 text-xs">Loading your tickets...</div>';

  try {
    const tickets = await apiCall('/api/employee/incidents');
    if (!tickets || tickets.length === 0) {
      container.innerHTML = '<div class="text-center py-12 text-slate-500 text-sm">No incidents reported yet. Submit your first ticket using the form on the left.</div>';
      return;
    }

    container.innerHTML = '';
    tickets.forEach(ticket => {
      const card = document.createElement('div');
      card.className = 'p-5 rounded-xl bg-slate-950 border border-slate-800 space-y-4 hover:border-slate-750 transition shadow-sm';

      // Stepper calculation: NEW -> ASSIGNED -> IN PROGRESS -> RESOLVED -> CLOSED
      const currentStep = getStepperIndex(ticket.status);

      card.innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
          <div>
            <div class="flex items-center gap-2">
              <span class="font-mono font-bold text-white text-sm">${ticket.id}</span>
              <span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-semibold border border-slate-700">${ticket.category}</span>
            </div>
            <h4 class="text-sm font-semibold text-white mt-1">${ticket.title}</h4>
          </div>
          <div class="text-right">
            <span class="px-2.5 py-1 rounded-full text-xs font-semibold ${getStatusBadgeClass(ticket.status)}">${ticket.status}</span>
            <div class="text-[11px] text-slate-500 mt-1">Reported: ${formatTime(ticket.createdAt)}</div>
          </div>
        </div>

        <p class="text-xs text-slate-400 line-clamp-2">${ticket.description}</p>

        <!-- 5-Stage Stepper -->
        <div class="stepper-container">
          <div class="stepper-bar"></div>
          ${renderStep('NEW', 1, currentStep)}
          ${renderStep('ASSIGNED', 2, currentStep)}
          ${renderStep('IN PROGRESS', 3, currentStep)}
          ${renderStep('RESOLVED', 4, currentStep)}
          ${renderStep('CLOSED', 5, currentStep)}
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
      <div class="pt-3 border-t border-slate-800 flex items-center justify-between">
        <span class="text-xs text-amber-400 font-medium">Technician requested additional details</span>
        <button class="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold" onclick="openProvideInfoModal('${ticket.id}')">
          Provide Information
        </button>
      </div>
    `;
  }
  if (ticket.status === 'RESOLVED') {
    return `
      <div class="pt-3 border-t border-slate-800 flex items-center justify-between">
        <span class="text-xs text-emerald-400 font-medium">Fix applied. Please verify the solution:</span>
        <button class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold" onclick="openVerifyModal('${ticket.id}')">
          Verify & Rate Fix
        </button>
      </div>
    `;
  }
  return '';
}

function openProvideInfoModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Provide Additional Information</h3>
    <p class="text-xs text-slate-400">Your technician has paused the SLA target while awaiting your response.</p>
    <div>
      <label class="block text-xs font-semibold text-slate-300 mb-1">Your Diagnostic Details</label>
      <textarea id="modal-info-input" rows="4" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white" placeholder="Enter logs, software versions, or reproduction steps..."></textarea>
    </div>
    <div class="flex justify-end gap-2 pt-2">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-indigo-600 text-xs font-bold text-white" onclick="submitProvideInfo('${ticketId}')">Submit & Resume SLA</button>
    </div>
  `);
}

async function submitProvideInfo(ticketId) {
  const info = document.getElementById('modal-info-input').value;
  if (!info || info.trim().length === 0) return alert('Please enter information');
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/provide-info`, {
      method: 'POST',
      body: JSON.stringify({ additionalInfo: info })
    });
    closeModal();
    alert('Information provided! Technician notified and SLA timer resumed.');
    loadEmployeeTickets();
  } catch (err) {
    alert(err.message);
  }
}

function openVerifyModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Verify Incident Resolution</h3>
    <p class="text-xs text-slate-400">Did the technician's solution completely resolve your problem?</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Star Rating (1 to 5 Stars)</label>
        <select id="modal-verify-rating" class="bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white w-full">
          <option value="5">⭐⭐⭐⭐⭐ 5 Stars (Excellent)</option>
          <option value="4">⭐⭐⭐⭐ 4 Stars (Good)</option>
          <option value="3">⭐⭐⭐ 3 Stars (Satisfactory)</option>
          <option value="2">⭐⭐ 2 Stars (Poor)</option>
          <option value="1">⭐ 1 Star (Unacceptable)</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Feedback Comments</label>
        <input type="text" id="modal-verify-feedback" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="Optional comments on service speed and quality...">
      </div>
    </div>

    <div class="flex justify-between items-center pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded bg-red-950 text-red-300 border border-red-800 text-xs font-bold" onclick="rejectFix('${ticketId}')">
        Issue Persists (Reopen)
      </button>
      <div class="flex gap-2">
        <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
        <button class="px-4 py-1.5 rounded bg-emerald-600 text-xs font-bold text-white" onclick="acceptFix('${ticketId}')">
          Verify & Close Ticket
        </button>
      </div>
    </div>
  `);
}

async function acceptFix(ticketId) {
  const rating = parseInt(document.getElementById('modal-verify-rating').value);
  const feedback = document.getElementById('modal-verify-feedback').value || 'Resolution verified.';
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/verify`, { method: 'POST' });
    await apiCall(`/api/employee/incidents/${ticketId}/close`, {
      method: 'POST',
      body: JSON.stringify({ rating, comments: feedback })
    });
    closeModal();
    alert('Thank you! Ticket has been verified and formally closed.');
    loadEmployeeTickets();
  } catch (err) {
    alert(err.message);
  }
}

async function rejectFix(ticketId) {
  const reason = prompt('Please explain why the issue persists (required for reopen):');
  if (!reason) return;
  try {
    await apiCall(`/api/employee/incidents/${ticketId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
    closeModal();
    alert('Resolution rejected. Ticket reopened and sent back to technician for rework.');
    loadEmployeeTickets();
  } catch (err) {
    alert(err.message);
  }
}

// ==========================================
// CONSOLE 2: Technician Console Logic
// ==========================================
async function loadTechnicianQueue() {
  const container = document.getElementById('tech-queue-container');
  container.innerHTML = '<div class="text-center py-12 text-slate-500 text-xs">Loading queue...</div>';

  try {
    const queue = await apiCall('/api/technician/queue');
    if (!queue || queue.length === 0) {
      container.innerHTML = '<div class="text-center py-12 text-slate-500 text-sm">Your queue is currently clear! Excellent work.</div>';
      updateTechMetrics([]);
      return;
    }

    updateTechMetrics(queue);
    container.innerHTML = '';

    queue.forEach(ticket => {
      const card = document.createElement('div');
      card.className = 'p-5 rounded-xl bg-slate-950 border border-slate-800 space-y-4 hover:border-slate-750 transition shadow-sm';

      card.innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
          <div class="flex items-center gap-3">
            <span class="px-2.5 py-1 rounded text-xs font-bold font-mono ${getPriorityBadgeClass(ticket.priority)}">${ticket.priority}</span>
            <span class="font-mono font-bold text-white text-sm">${ticket.id}</span>
            <span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-semibold">${ticket.category}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="px-2 py-0.5 rounded text-[10px] font-bold ${getSlaBadgeClass(ticket.slaStatus)}">${ticket.slaStatus}</span>
            <span class="px-2.5 py-1 rounded-full text-xs font-semibold ${getStatusBadgeClass(ticket.status)}">${ticket.status}</span>
          </div>
        </div>

        <div>
          <h4 class="text-sm font-semibold text-white">${ticket.title}</h4>
          <p class="text-xs text-slate-400 mt-1">${ticket.description}</p>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] text-slate-400 bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/80">
          <div><span class="text-slate-500">Reporter:</span> ${ticket.reporterId}</div>
          <div><span class="text-slate-500">Service:</span> ${ticket.serviceId || 'N/A'}</div>
          <div><span class="text-slate-500">Reopen Count:</span> ${ticket.reopenCount || 0}</div>
          <div><span class="text-slate-500">SLA Due:</span> ${ticket.slaDeadline ? formatTime(ticket.slaDeadline) : 'N/A'}</div>
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
  document.getElementById('tech-metric-assigned').textContent = queue.length;
  document.getElementById('tech-metric-investigating').textContent = queue.filter(t => t.status === 'IN_PROGRESS').length;
  document.getElementById('tech-metric-pending').textContent = queue.filter(t => t.status === 'PENDING_INFO').length;
  document.getElementById('tech-metric-atrisk').textContent = queue.filter(t => t.slaStatus === 'AT_RISK' || t.slaStatus === 'WARNING' || t.slaStatus === 'BREACHED').length;
}

function renderTechnicianActionButtons(ticket) {
  let btns = '';

  if (ticket.status === 'ASSIGNED') {
    btns += `<button class="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold" onclick="techAcknowledge('${ticket.id}')">Acknowledge</button>`;
  }
  if (ticket.status === 'ACKNOWLEDGED') {
    btns += `<button class="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold" onclick="techStartInvestigation('${ticket.id}')">Start Investigation</button>`;
  }
  if (ticket.status === 'IN_PROGRESS') {
    btns += `
      <button class="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium" onclick="openAddNoteModal('${ticket.id}')">Add Note</button>
      <button class="px-3 py-1.5 rounded-lg bg-cyan-900/60 hover:bg-cyan-800 text-cyan-200 text-xs font-medium" onclick="openRequestInfoModal('${ticket.id}')">Request Info</button>
      <button class="px-3 py-1.5 rounded-lg bg-rose-950/60 hover:bg-rose-900 text-rose-300 text-xs font-medium" onclick="openWrongDomainModal('${ticket.id}')">Wrong Domain</button>
      <button class="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold" onclick="openResolveModal('${ticket.id}')">Resolve Ticket</button>
    `;
  }
  return btns;
}

async function techAcknowledge(ticketId) {
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/acknowledge`, { method: 'POST' });
    loadTechnicianQueue();
  } catch (err) { alert(err.message); }
}

async function techStartInvestigation(ticketId) {
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/start-investigation`, { method: 'POST' });
    loadTechnicianQueue();
  } catch (err) { alert(err.message); }
}

function openAddNoteModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Add Internal Investigation Note</h3>
    <p class="text-xs text-slate-400">Internal notes are visible exclusively to IT Support staff and administrators.</p>
    <textarea id="modal-note-text" rows="4" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white" placeholder="Document debug logs, root cause analysis, or system configuration notes..."></textarea>
    <div class="flex justify-end gap-2 pt-2">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-indigo-600 text-xs font-bold text-white" onclick="submitNote('${ticketId}')">Add Note</button>
    </div>
  `);
}

async function submitNote(ticketId) {
  const text = document.getElementById('modal-note-text').value;
  if (!text) return alert('Note cannot be blank');
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/notes`, {
      method: 'POST',
      body: JSON.stringify({ content: text, internalOnly: true })
    });
    closeModal();
    alert('Investigation note recorded.');
  } catch (err) { alert(err.message); }
}

function openRequestInfoModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Request Information from Reporter</h3>
    <p class="text-xs text-slate-400">This action moves the ticket to PENDING_INFO and pauses the SLA resolution target clock.</p>
    <textarea id="modal-req-info-text" rows="3" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white" placeholder="Specific details needed from user..."></textarea>
    <div class="flex justify-end gap-2 pt-2">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-cyan-600 text-xs font-bold text-white" onclick="submitRequestInfo('${ticketId}')">Pause SLA & Send Request</button>
    </div>
  `);
}

async function submitRequestInfo(ticketId) {
  const q = document.getElementById('modal-req-info-text').value;
  if (!q) return;
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/request-info`, {
      method: 'POST',
      body: JSON.stringify({ question: q })
    });
    closeModal();
    loadTechnicianQueue();
  } catch (err) { alert(err.message); }
}

function openWrongDomainModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Flag Out-of-Scope (Wrong Domain)</h3>
    <p class="text-xs text-slate-400">Reverts ticket to CLASSIFIED in Admin Triage Queue for reclassification.</p>
    <textarea id="modal-wrong-domain-reason" rows="3" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-white" placeholder="Explain why this ticket does not belong to your domain..."></textarea>
    <div class="flex justify-end gap-2 pt-2">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-rose-600 text-xs font-bold text-white" onclick="submitWrongDomain('${ticketId}')">Return to Triage</button>
    </div>
  `);
}

async function submitWrongDomain(ticketId) {
  const reason = document.getElementById('modal-wrong-domain-reason').value;
  if (!reason) return;
  try {
    await apiCall(`/api/technician/incidents/${ticketId}/wrong-domain`, {
      method: 'POST',
      body: JSON.stringify({ reason: reason })
    });
    closeModal();
    loadTechnicianQueue();
  } catch (err) { alert(err.message); }
}

function openResolveModal(ticketId) {
  openModal(`
    <h3 class="text-base font-bold text-white">Structured Incident Resolution</h3>
    <p class="text-xs text-slate-400">Record structured ITIL resolution metadata for post-incident learning.</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Root Cause Explanation</label>
        <input type="text" id="res-root-cause" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="e.g. Expired SSL certificate on secondary auth gateway">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Resolution Steps Applied</label>
        <input type="text" id="res-steps" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="e.g. Renewed certificate from internal CA and reloaded Nginx">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Preventive Actions</label>
        <input type="text" id="res-preventive" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="e.g. Added automated cert renewal alerts to monitoring">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Failure Domain Category</label>
        <select id="res-category" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white">
          <option value="NETWORK">NETWORK</option>
          <option value="DATABASE">DATABASE</option>
          <option value="APPLICATION">APPLICATION</option>
          <option value="SECURITY">SECURITY</option>
          <option value="INFRASTRUCTURE">INFRASTRUCTURE</option>
        </select>
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-emerald-600 text-xs font-bold text-white" onclick="submitResolve('${ticketId}')">Resolve Incident</button>
    </div>
  `);
}

async function submitResolve(ticketId) {
  const rootCause = document.getElementById('res-root-cause').value;
  const steps = document.getElementById('res-steps').value;
  const preventive = document.getElementById('res-preventive').value;
  const failureCategory = document.getElementById('res-category').value;

  if (!rootCause || !steps) return alert('Root cause and resolution steps are required');

  try {
    await apiCall(`/api/technician/incidents/${ticketId}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ rootCause, resolutionSteps: steps, preventiveActions: preventive, failureCategory })
    });
    closeModal();
    alert('Incident resolved and sent to reporter for verification!');
    loadTechnicianQueue();
  } catch (err) { alert(err.message); }
}

// ==========================================
// CONSOLE 3: Admin Command Centre Logic
// ==========================================
async function loadAdminIncidents() {
  const tbody = document.getElementById('admin-incidents-tbody');
  tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-500">Loading enterprise catalog...</td></tr>';

  try {
    const list = await apiCall('/api/admin/incidents');
    if (!list || list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-500">No incidents in system.</td></tr>';
      return;
    }

    tbody.innerHTML = '';
    list.slice(0, 50).forEach(inc => {
      const tr = document.createElement('tr');
      tr.className = 'border-b border-slate-850 hover:bg-slate-850/50 transition';
      tr.innerHTML = `
        <td class="py-3 px-3 font-mono font-bold text-white">${inc.id}</td>
        <td class="py-3 px-3 max-w-xs truncate">${inc.title}</td>
        <td class="py-3 px-3"><span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] font-mono">${inc.category}</span></td>
        <td class="py-3 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getPriorityBadgeClass(inc.priority)}">${inc.priority || 'N/A'}</span></td>
        <td class="py-3 px-3"><span class="px-2 py-0.5 rounded-full text-[10px] font-semibold ${getStatusBadgeClass(inc.status)}">${inc.status}</span></td>
        <td class="py-3 px-3 font-mono text-[11px] text-slate-400">${inc.assigneeId || 'UNASSIGNED'}</td>
        <td class="py-3 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getSlaBadgeClass(inc.slaStatus)}">${inc.slaStatus || 'ON_TRACK'}</span></td>
        <td class="py-3 px-3 text-right">
          <button class="px-2 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px]" onclick="openTriageModal('${inc.id}', '${inc.category}', '${inc.serviceId}')">Reclassify</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-8 text-red-400">Error: ${err.message}</td></tr>`;
  }
}

async function loadAdminTriageQueue() {
  const container = document.getElementById('admin-triage-container');
  container.innerHTML = '<div class="text-center py-8 text-slate-500 text-xs">Scanning for tickets requiring triage...</div>';

  try {
    const all = await apiCall('/api/admin/incidents');
    const triageTickets = all.filter(t => t.status === 'CLASSIFIED' || !t.assigneeId);

    if (triageTickets.length === 0) {
      container.innerHTML = '<div class="text-center py-12 text-slate-500 text-sm">Triage queue empty. All tickets are assigned and categorized properly.</div>';
      return;
    }

    container.innerHTML = '';
    triageTickets.forEach(t => {
      const card = document.createElement('div');
      card.className = 'p-4 rounded-xl bg-slate-950 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3';
      card.innerHTML = `
        <div>
          <div class="flex items-center gap-2">
            <span class="font-mono font-bold text-white">${t.id}</span>
            <span class="px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800 text-[10px] font-bold">TRIAGE NEEDED</span>
            <span class="text-xs text-slate-400">Current Category: <b>${t.category}</b></span>
          </div>
          <h4 class="text-xs font-semibold text-white mt-1">${t.title}</h4>
          <p class="text-[11px] text-slate-400 mt-0.5">${t.description}</p>
        </div>
        <div class="flex gap-2">
          <button class="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold whitespace-nowrap" onclick="openTriageModal('${t.id}', '${t.category}', '${t.serviceId}')">
            Reclassify & Assign
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
    <h3 class="text-base font-bold text-white">Administrative Reclassification & Routing</h3>
    <p class="text-xs text-slate-400">Reclassifies domain, recalculates multi-factor priority score, and assigns technician.</p>
    
    <div class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">New Category</label>
        <select id="modal-new-category" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white">
          <option value="DATABASE">DATABASE</option>
          <option value="NETWORK">NETWORK</option>
          <option value="APPLICATION">APPLICATION</option>
          <option value="SECURITY">SECURITY</option>
          <option value="CLOUD">CLOUD</option>
          <option value="HARDWARE">HARDWARE</option>
          <option value="SOFTWARE">SOFTWARE</option>
          <option value="EMAIL">EMAIL</option>
          <option value="ACCESS">ACCESS</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Impacted IT Service ID</label>
        <input type="text" id="modal-new-service" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" value="${currService || 'SVC-DWH'}">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Assign Technician ID (optional)</label>
        <input type="text" id="modal-assign-tech" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="e.g. TECH-DB-1">
      </div>
      <div>
        <label class="block text-xs font-semibold text-slate-300 mb-1">Triage Rationale</label>
        <input type="text" id="modal-triage-reason" class="w-full bg-slate-950 border border-slate-800 rounded-lg p-2 text-xs text-white" placeholder="e.g. Reclassified from triage: confirmed database query exhaustion">
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-slate-800">
      <button class="px-3 py-1.5 rounded bg-slate-800 text-xs text-slate-300" onclick="closeModal()">Cancel</button>
      <button class="px-4 py-1.5 rounded bg-indigo-600 text-xs font-bold text-white" onclick="submitTriage('${ticketId}')">Apply Reclassification</button>
    </div>
  `);
}

async function submitTriage(ticketId) {
  const newCat = document.getElementById('modal-new-category').value;
  const newSvc = document.getElementById('modal-new-service').value;
  const techId = document.getElementById('modal-assign-tech').value;
  const reason = document.getElementById('modal-triage-reason').value || 'Administrative triage reclassification';

  try {
    await apiCall(`/api/admin/incidents/${ticketId}/reclassify`, {
      method: 'POST',
      body: JSON.stringify({ newCategory: newCat, serviceId: newSvc, reason })
    });

    if (techId && techId.trim().length > 0) {
      await apiCall(`/api/admin/incidents/${ticketId}/assign`, {
        method: 'POST',
        body: JSON.stringify({ technicianId: techId.trim() })
      });
    }

    closeModal();
    alert('Ticket reclassified and reassigned!');
    loadAdminTriageQueue();
    loadAdminIncidents();
  } catch (err) { alert(err.message); }
}

async function loadAssignmentStrategies() {
  const container = document.getElementById('strategy-cards-container');
  try {
    const active = await apiCall('/api/admin/intelligence/assignment/active');
    const strategies = await apiCall('/api/admin/intelligence/assignment/strategies');

    document.getElementById('current-active-strategy-name').textContent = active.strategyName;
    document.getElementById('current-active-strategy-desc').textContent = active.description;

    container.innerHTML = '';
    strategies.forEach(st => {
      const isActive = st.strategyName === active.strategyName;
      const card = document.createElement('div');
      card.className = `p-4 rounded-xl border transition ${isActive ? 'bg-indigo-950/40 border-indigo-500/50' : 'bg-slate-950 border-slate-800 hover:border-slate-700'}`;
      card.innerHTML = `
        <div class="flex items-center justify-between">
          <span class="font-mono text-xs font-bold ${isActive ? 'text-cyan-400' : 'text-white'}">${st.strategyName}</span>
          ${isActive ? '<span class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400 text-[10px] font-bold">ACTIVE</span>' : ''}
        </div>
        <p class="text-xs text-slate-400 mt-2">${st.description}</p>
        ${!isActive ? `
          <button class="mt-4 w-full py-1.5 px-3 rounded bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 transition" onclick="activateStrategy('${st.strategyName}')">
            Activate Strategy
          </button>
        ` : ''}
      `;
      container.appendChild(card);
    });
  } catch (err) { console.error('Error loading strategies', err); }
}

async function activateStrategy(name) {
  try {
    await apiCall(`/api/admin/intelligence/assignment/strategy/${name}`, { method: 'POST' });
    alert(`Swapped active technician routing strategy to: ${name}`);
    loadAssignmentStrategies();
  } catch (err) { alert(err.message); }
}

// ==========================================
// CONSOLE 5: Executive Reports Logic
// ==========================================
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
    document.getElementById('kpi-mttr-val').textContent = mttr ? Math.round(mttr.mttrMinutes) : '0';
    document.getElementById('kpi-mttresp-val').textContent = mttr ? Math.round(mttr.mttRespMinutes) : '0';
    document.getElementById('kpi-sla-compliance-val').textContent = sla ? `${Math.round(sla.compliancePercentage)}%` : '100%';

    // Workload Table
    const workloadContainer = document.getElementById('workload-table-container');
    workloadContainer.innerHTML = '';
    (workload.teams || []).forEach(tm => {
      const row = document.createElement('div');
      row.className = 'p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs space-y-1.5';
      const pct = Math.min(100, Math.round(tm.utilizationRate * 100));
      row.innerHTML = `
        <div class="flex justify-between font-semibold text-white">
          <span>${tm.teamName} (${tm.category})</span>
          <span>${tm.activeIncidentCount} tickets / ${tm.totalCapacity} capacity</span>
        </div>
        <div class="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
          <div class="h-full bg-indigo-500 rounded-full" style="width: ${pct}%"></div>
        </div>
        <div class="flex justify-between text-[11px] text-slate-500">
          <span>Technicians: ${tm.technicianCount}</span>
          <span>Load: ${pct}%</span>
        </div>
      `;
      workloadContainer.appendChild(row);
    });

    // Service Analysis
    const servicesContainer = document.getElementById('service-analysis-container');
    servicesContainer.innerHTML = '';
    (services.serviceBreakdowns || []).slice(0, 5).forEach(svc => {
      const row = document.createElement('div');
      row.className = 'p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs flex justify-between items-center';
      row.innerHTML = `
        <div>
          <span class="font-bold text-white">${svc.serviceName}</span>
          <div class="text-[11px] text-slate-500 font-mono">${svc.criticality}</div>
        </div>
        <div class="text-right">
          <span class="font-bold font-mono text-cyan-400">${svc.incidentCount} incidents</span>
          <div class="text-[11px] text-slate-500">${Math.round(svc.percentageOfTotal)}% of total</div>
        </div>
      `;
      servicesContainer.appendChild(row);
    });

    // SLA Breakdown
    const slaContainer = document.getElementById('sla-breakdown-container');
    slaContainer.innerHTML = `
      <div class="grid grid-cols-2 gap-3 text-xs">
        <div class="p-3 bg-slate-950 rounded-lg border border-slate-800">
          <span class="text-slate-500">Total Analyzed</span>
          <p class="text-lg font-bold text-white mt-1">${sla.totalIncidents}</p>
        </div>
        <div class="p-3 bg-slate-950 rounded-lg border border-slate-800">
          <span class="text-emerald-500">SLA Met</span>
          <p class="text-lg font-bold text-emerald-400 mt-1">${sla.compliantIncidents}</p>
        </div>
        <div class="p-3 bg-slate-950 rounded-lg border border-slate-800">
          <span class="text-red-500">SLA Breached</span>
          <p class="text-lg font-bold text-red-400 mt-1">${sla.breachedIncidents}</p>
        </div>
        <div class="p-3 bg-slate-950 rounded-lg border border-slate-800">
          <span class="text-cyan-500">Compliance Rate</span>
          <p class="text-lg font-bold text-cyan-400 mt-1">${Math.round(sla.compliancePercentage)}%</p>
        </div>
      </div>
    `;

    // Escalations Log
    const escContainer = document.getElementById('escalation-log-container');
    escContainer.innerHTML = '';
    const escList = escalations.escalations || [];
    if (escList.length === 0) {
      escContainer.innerHTML = '<div class="text-center py-6 text-slate-500 text-xs">No escalations recorded.</div>';
    } else {
      escList.slice(0, 10).forEach(esc => {
        const item = document.createElement('div');
        item.className = 'p-2 bg-slate-950 rounded border border-slate-850 text-[11px] flex justify-between items-center';
        item.innerHTML = `
          <div>
            <span class="font-bold text-red-400 font-mono">Tier ${esc.tier} (${esc.toRole})</span>
            <span class="text-slate-400 ml-1">Ticket ${esc.incidentId}</span>
            <div class="text-slate-500 text-[10px] truncate max-w-sm">${esc.reason}</div>
          </div>
          <span class="text-slate-500 font-mono text-[10px]">${formatTime(esc.timestamp)}</span>
        `;
        escContainer.appendChild(item);
      });
    }

  } catch (err) { console.error('Error loading reports', err); }
}

// ==========================================
// Notifications Modal
// ==========================================
async function loadNotifications() {
  try {
    const list = await apiCall('/api/notifications');
    const badge = document.getElementById('unread-count-badge');
    const unread = (list || []).filter(n => !n.read).length;
    if (unread > 0) {
      badge.textContent = unread;
      badge.classList.remove('hidden');
    } else {
      badge.classList.add('hidden');
    }

    const container = document.getElementById('notifications-list-container');
    container.innerHTML = '';
    if (!list || list.length === 0) {
      container.innerHTML = '<div class="text-center py-6 text-slate-500">No notifications.</div>';
      return;
    }

    list.slice(0, 15).forEach(n => {
      const item = document.createElement('div');
      item.className = `p-2.5 rounded-lg border ${n.read ? 'bg-slate-950 border-slate-850 text-slate-400' : 'bg-indigo-950/40 border-indigo-800 text-white font-medium'}`;
      item.innerHTML = `
        <div class="flex justify-between items-start gap-2">
          <span class="font-semibold text-xs text-indigo-300">${n.subject}</span>
          <span class="text-[10px] text-slate-500 font-mono">${formatTime(n.createdAt)}</span>
        </div>
        <p class="text-xs text-slate-300 mt-1">${n.content}</p>
      `;
      container.appendChild(item);
    });
  } catch (e) {}
}

function setupNotificationsModal() {
  const bell = document.getElementById('notification-bell-btn');
  const modal = document.getElementById('notification-modal');
  const closeBtn = document.getElementById('btn-close-notifications');

  bell.addEventListener('click', () => {
    loadNotifications();
    modal.classList.remove('hidden');
  });

  closeBtn.addEventListener('click', () => {
    modal.classList.add('hidden');
  });
}

// ==========================================
// Utilities & Modal Helpers
// ==========================================
function openModal(html) {
  const container = document.getElementById('modal-container');
  const content = document.getElementById('modal-content');
  content.innerHTML = html;
  container.classList.remove('hidden');
}

function closeModal() {
  document.getElementById('modal-container').classList.add('hidden');
}

function getPriorityBadgeClass(p) {
  switch (p) {
    case 'P1': return 'badge-p1';
    case 'P2': return 'badge-p2';
    case 'P3': return 'badge-p3';
    case 'P4': return 'badge-p4';
    default: return 'bg-slate-800 text-slate-300';
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
    case 'NEW': return 'bg-indigo-950 text-indigo-300 border border-indigo-800';
    case 'ASSIGNED': return 'bg-cyan-950 text-cyan-300 border border-cyan-800';
    case 'ACKNOWLEDGED': return 'bg-blue-950 text-blue-300 border border-blue-800';
    case 'IN_PROGRESS': return 'bg-amber-950 text-amber-300 border border-amber-800';
    case 'PENDING_INFO': return 'bg-purple-950 text-purple-300 border border-purple-800';
    case 'RESOLVED': return 'bg-emerald-950 text-emerald-300 border border-emerald-800';
    case 'VERIFIED':
    case 'CLOSED': return 'bg-slate-800 text-slate-400 border border-slate-700';
    default: return 'bg-slate-800 text-slate-300';
  }
}

function formatTime(isoStr) {
  if (!isoStr) return '--:--';
  return isoStr.replace('T', ' ').substring(0, 19);
}

// ==========================================
// Initialization on DOM Load
// ==========================================
document.addEventListener('DOMContentLoaded', async () => {
  setupNavigation();
  setupEmployeePortal();
  document.getElementById('btn-refresh-tech-queue').addEventListener('click', loadTechnicianQueue);
  setupSimulationControls();
  setupNotificationsModal();

  // Initial authentication as Admin
  await authenticateUser('admin@nimbusfinserve.com');
  pollSimulationStatus();
  setInterval(pollSimulationStatus, 3000);
});

