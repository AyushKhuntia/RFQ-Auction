// ====== SUPPLIER-RFQ.JS - Supplier Bidding Page with WebSocket ======

let stompClient = null;
let rfqId = null;
let bidCloseTime = null;
let timerInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    const user = getUser();
    if (!user || user.role !== 'SUPPLIER') {
        window.location.href = 'index.html';
        return;
    }

    const params = new URLSearchParams(window.location.search);
    rfqId = params.get('id');
    if (!rfqId) {
        window.location.href = 'supplier-dashboard.html';
        return;
    }

    // Auto-calculate total
    ['freight-charges', 'origin-charges', 'destination-charges'].forEach(id => {
        document.getElementById(id).addEventListener('input', updateTotal);
    });

    loadRfqDetails();
    loadActivityLog();
    connectWebSocket();
});

function updateTotal() {
    const f = parseFloat(document.getElementById('freight-charges').value) || 0;
    const o = parseFloat(document.getElementById('origin-charges').value) || 0;
    const d = parseFloat(document.getElementById('destination-charges').value) || 0;
    const total = f + o + d;
    document.getElementById('total-amount').value = '₹ ' + total.toFixed(2);
}

async function loadRfqDetails() {
    try {
        const res = await fetch(`/api/rfq/${rfqId}`);
        const data = await res.json();

        document.getElementById('rfq-title').textContent = `🏭 ${data.rfqName}`;

        const statusClass = data.status === 'ACTIVE' ? 'badge-active' :
                           data.status === 'FORCE_CLOSED' ? 'badge-force-closed' : 'badge-closed';
        document.getElementById('rfq-status-badge').innerHTML =
            `<span class="badge ${statusClass}">${data.status.replace('_', ' ')}</span>`;
        document.getElementById('rfq-status-text').textContent =
            `RFQ #${data.rfqId} · By ${data.buyerName}`;

        // Bid Start Time
        if (data.bidStartTime) {
            document.getElementById('stat-start').textContent = new Date(data.bidStartTime).toLocaleString();
        }

        // Config
        if (data.auctionConfig) {
            document.getElementById('config-x').textContent = data.auctionConfig.triggerWindowMinutes;
            document.getElementById('config-y').textContent = data.auctionConfig.extensionDurationMinutes;
            document.getElementById('config-trigger').textContent = data.auctionConfig.triggerType.replace(/_/g, ' ');
        }

        // Timer
        bidCloseTime = new Date(data.bidCloseTime);
        startCountdown(data.status);

        // Bids
        renderBidsTable(data.bids || []);

        // Disable form if not active
        if (data.status !== 'ACTIVE') {
            document.getElementById('submit-bid-btn').disabled = true;
            document.getElementById('submit-bid-btn').textContent = 'Auction Closed';
        }

    } catch (err) {
        console.error('Failed to load RFQ:', err);
    }
}

function renderBidsTable(bids) {
    const tbody = document.getElementById('bids-table-body');
    const user = getUser();

    if (bids.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:40px">No bids yet. Be the first to bid!</td></tr>';
        return;
    }

    tbody.innerHTML = bids.map(bid => {
        const rankClass = bid.rank === 'L1' ? 'rank-l1' :
                          bid.rank === 'L2' ? 'rank-l2' :
                          bid.rank === 'L3' ? 'rank-l3' : 'rank-other';
        const isMe = bid.supplierId === user.id;
        const highlight = isMe ? 'style="background:rgba(139,92,246,0.08)"' : '';
        return `
            <tr class="bid-flash" ${highlight}>
                <td><span class="rank-badge ${rankClass}">${bid.rank}</span></td>
                <td style="font-weight:600">${bid.supplierName} ${isMe ? '(You)' : ''}</td>
                <td style="font-weight:700;color:var(--accent-cyan)">₹${bid.amount.toFixed(2)}</td>
                <td>₹${(bid.freightCharges || 0).toFixed(2)}</td>
                <td>₹${(bid.originCharges || 0).toFixed(2)}</td>
                <td>₹${(bid.destinationCharges || 0).toFixed(2)}</td>
                <td>${bid.transitTime || '--'}</td>
                <td>${bid.quoteValidity || '--'}</td>
            </tr>
        `;
    }).join('');
}

function startCountdown(status) {
    if (timerInterval) clearInterval(timerInterval);

    if (status !== 'ACTIVE') {
        document.getElementById('countdown-timer').textContent = status === 'FORCE_CLOSED' ? 'FORCE CLOSED' : 'CLOSED';
        document.getElementById('countdown-timer').classList.remove('urgent');
        return;
    }

    timerInterval = setInterval(() => {
        const now = new Date();
        const diff = bidCloseTime - now;

        if (diff <= 0) {
            document.getElementById('countdown-timer').textContent = '00:00:00';
            clearInterval(timerInterval);
            return;
        }

        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);

        document.getElementById('countdown-timer').textContent =
            `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;

        if (diff < 300000) {
            document.getElementById('countdown-timer').classList.add('urgent');
        } else {
            document.getElementById('countdown-timer').classList.remove('urgent');
        }
    }, 1000);
}

async function loadActivityLog() {
    try {
        const res = await fetch(`/api/rfq/${rfqId}/activity`);
        const logs = await res.json();
        renderActivityLog(logs);
    } catch (err) {
        console.error('Failed to load activity log:', err);
    }
}

function renderActivityLog(logs) {
    const container = document.getElementById('activity-log');
    if (logs.length === 0) {
        container.innerHTML = '<div class="empty-state" style="padding:30px"><p style="color:var(--text-muted)">No activity yet</p></div>';
        return;
    }

    container.innerHTML = logs.map(log => {
        const iconClass = log.eventType === 'BID_PLACED' ? 'bid' :
                          log.eventType === 'EXTENDED' ? 'extend' :
                          log.eventType === 'RANK_CHANGED' ? 'rank' : 'close';
        const icon = log.eventType === 'BID_PLACED' ? '💰' :
                     log.eventType === 'EXTENDED' ? '⏰' :
                     log.eventType === 'RANK_CHANGED' ? '📊' : '🔒';
        const time = new Date(log.createdAt).toLocaleTimeString();
        return `
            <div class="log-entry">
                <div class="log-icon ${iconClass}">${icon}</div>
                <div class="log-content">
                    <p>${log.description}</p>
                    <div class="log-time">${time}</div>
                </div>
            </div>
        `;
    }).join('');
}

// ====== BID SUBMISSION ======
async function handlePlaceBid(e) {
    e.preventDefault();
    hideAlerts();

    const user = getUser();

    const payload = {
        rfqId: parseInt(rfqId),
        supplierId: user.id,
        freightCharges: parseFloat(document.getElementById('freight-charges').value),
        originCharges: parseFloat(document.getElementById('origin-charges').value),
        destinationCharges: parseFloat(document.getElementById('destination-charges').value),
        transitTime: document.getElementById('transit-time').value,
        quoteValidity: document.getElementById('quote-validity').value
    };

    try {
        const res = await fetch('/api/bid', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to place bid');

        showSuccess(`Bid placed! Total: ₹${data.amount.toFixed(2)}`);
        document.getElementById('bid-form').reset();
        document.getElementById('total-amount').value = '₹ 0.00';

    } catch (err) {
        showError(err.message);
    }
}

// ====== WebSocket ======
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        console.log('WebSocket connected');

        stompClient.subscribe(`/topic/rfq/${rfqId}`, function (message) {
            const update = JSON.parse(message.body);
            handleUpdate(update);
        });
    }, function (error) {
        console.error('WebSocket error:', error);
        setTimeout(connectWebSocket, 3000);
    });
}

function handleUpdate(update) {
    showToast(update.message);

    if (update.type === 'NEW_BID' || update.type === 'RANK_CHANGE') {
        if (update.rankings) {
            renderBidsTable(update.rankings);
        }
    }

    if (update.type === 'TIME_EXTENSION') {
        bidCloseTime = new Date(update.newCloseTime);
        startCountdown('ACTIVE');

        const banner = document.getElementById('extension-banner');
        document.getElementById('extension-text').textContent = update.message;
        banner.classList.add('show');
        setTimeout(() => banner.classList.remove('show'), 8000);
    }

    if (update.type === 'AUCTION_CLOSED') {
        if (timerInterval) clearInterval(timerInterval);
        document.getElementById('countdown-timer').textContent = 'CLOSED';
        document.getElementById('countdown-timer').classList.remove('urgent');
        document.getElementById('submit-bid-btn').disabled = true;
        document.getElementById('submit-bid-btn').textContent = 'Auction Closed';
    }

    loadActivityLog();
}

function showError(msg) {
    const el = document.getElementById('alert-error');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
    const suc = document.getElementById('alert-success');
    if (suc) suc.style.display = 'none';
}

function showSuccess(msg) {
    const el = document.getElementById('alert-success');
    if (el) { el.textContent = msg; el.style.display = 'block'; }
    const err = document.getElementById('alert-error');
    if (err) err.style.display = 'none';
}
