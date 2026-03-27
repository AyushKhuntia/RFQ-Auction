// ====== AUTH.JS - Authentication & Session Management ======

const API_BASE = '';

// Check auth on protected pages
(function checkAuth() {
    const path = window.location.pathname;
    const user = getUser();

    // Skip auth check on login page
    if (path === '/' || path === '/index.html') {
        if (user) {
            // Already logged in, redirect
            if (user.role === 'BUYER') window.location.href = 'buyer-dashboard.html';
            else window.location.href = 'supplier-dashboard.html';
        }
        return;
    }

    // Not logged in
    if (!user) {
        window.location.href = 'index.html';
        return;
    }

    // Set username in navbar
    const nameEl = document.getElementById('user-name');
    if (nameEl) nameEl.textContent = user.name;
})();

function getUser() {
    const data = sessionStorage.getItem('rfq_user');
    return data ? JSON.parse(data) : null;
}

function saveUser(user) {
    sessionStorage.setItem('rfq_user', JSON.stringify(user));
}

function logout() {
    sessionStorage.removeItem('rfq_user');
    window.location.href = 'index.html';
}

// ====== LOGIN PAGE ======

let currentRole = 'BUYER';
let currentTab = 'login';

function switchTab(tab) {
    currentTab = tab;
    document.getElementById('tab-login').classList.toggle('active', tab === 'login');
    document.getElementById('tab-register').classList.toggle('active', tab === 'register');
    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    hideAlerts();
}

function selectRole(role) {
    currentRole = role;
    document.getElementById('role-buyer').classList.toggle('active', role === 'BUYER');
    document.getElementById('role-supplier').classList.toggle('active', role === 'SUPPLIER');
    hideAlerts();
}

function hideAlerts() {
    const err = document.getElementById('alert-error');
    const suc = document.getElementById('alert-success');
    if (err) err.style.display = 'none';
    if (suc) suc.style.display = 'none';
}

function showError(msg) {
    const el = document.getElementById('alert-error');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
}

function showSuccess(msg) {
    const el = document.getElementById('alert-success');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
}

async function handleLogin(e) {
    e.preventDefault();
    hideAlerts();

    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const res = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, role: currentRole })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Login failed');

        saveUser(data);
        if (data.role === 'BUYER') window.location.href = 'buyer-dashboard.html';
        else window.location.href = 'supplier-dashboard.html';
    } catch (err) {
        showError(err.message);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    hideAlerts();

    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;

    try {
        const res = await fetch(`${API_BASE}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, role: currentRole })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Registration failed');

        showSuccess('Registration successful! You can now login.');
        switchTab('login');
    } catch (err) {
        showError(err.message);
    }
}

// ====== TOAST NOTIFICATIONS ======

function showToast(message, duration = 4000) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('hide');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}
