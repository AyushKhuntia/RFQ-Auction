// ====== RFQ-DETAILS.JS - Buyer's RFQ Details with WebSocket ======

let stompClient = null;
let rfqId = null;
let bidCloseTime = null;
let timerInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    const user = getUser();
    if (!user) {
        window.location.href = 'index.html';
        return;
    }

    const params = new URLSearchParams(window.location.search);
    rfqId = params.get('id');
    if (!rfqId) {
        window.location.href = 'buyer-dashboard.html';
        return;
    }

    loadRfqDetails();
    loadActivityLog();
    connectWebSocket();
});

async function loadRfqDetails() {
    try {
        const res = await fetch(`/api/rfq/${rfqId}`);
        const data = await res.json();

        document.getElementById('rfq-title').textContent = `📋 ${data.rfqName}`;

        const statusClass = data.status === 'ACTIVE' ? 'badge-active' :
                           data.status === 'FORCE_CLOSED' ? 'badge-force-closed' : 'badge-closed';
        document.getElementById('rfq-status-badge').innerHTML =
            `<span class="badge ${statusClass}">${data.status.replace('_', ' ')}</span>`;
        document.getElementById('rfq-status-text').textContent =
            `RFQ #${data.rfqId} · Created by ${data.buyerName}`;

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

        // Stats
        if (data.bids) {
            document.getElementById('stat-bids').textContent = data.bids.length;
            document.getElementById('stat-suppliers').textContent = data.bids.length;
        }
        if (data.lowestBid) {
            document.getElementById('stat-lowest').textContent = '₹' + data.lowestBid.toFixed(2);
        }

        // Timer
        bidCloseTime = new Date(data.bidCloseTime);
        startCountdown(data.status);

        // Bids table
        renderBidsTable(data.bids || []);

    } catch (err) {
        console.error('Failed to load RFQ:', err);
    }
}

function renderBidsTable(bids) {
    const tbody = document.getElementById('bids-table-body');
    if (bids.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;color:var(--text-muted);padding:40px">No bids yet</td></tr>';
        return;
    }

    tbody.innerHTML = bids.map((bid, i) => {
        const rankClass = bid.rank === 'L1' ? 'rank-l1' :
                          bid.rank === 'L2' ? 'rank-l2' :
                          bid.rank === 'L3' ? 'rank-l3' : 'rank-other';
        const time = new Date(bid.bidTime).toLocaleTimeString();
        return `
            <tr class="bid-flash">
                <td><span class="rank-badge ${rankClass}">${bid.rank}</span></td>
                <td style="font-weight:600">${bid.supplierName}</td>
                <td style="font-weight:700;color:var(--accent-cyan)">₹${bid.amount.toFixed(2)}</td>
                <td>₹${(bid.freightCharges || 0).toFixed(2)}</td>
                <td>₹${(bid.originCharges || 0).toFixed(2)}</td>
                <td>₹${(bid.destinationCharges || 0).toFixed(2)}</td>
                <td>${bid.transitTime || '--'}</td>
                <td>${bid.quoteValidity || '--'}</td>
                <td style="color:var(--text-muted);font-size:12px">${time}</td>
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

        const formatted = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
        document.getElementById('countdown-timer').textContent = formatted;

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
            document.getElementById('stat-bids').textContent = update.rankings.length;
            document.getElementById('stat-suppliers').textContent = update.rankings.length;
            if (update.rankings.length > 0) {
                document.getElementById('stat-lowest').textContent = '₹' + update.rankings[0].amount.toFixed(2);
            }
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
    }

    loadActivityLog();
}
