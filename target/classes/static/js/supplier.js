// ====== SUPPLIER.JS - Supplier Dashboard ======

document.addEventListener('DOMContentLoaded', () => {
    const user = getUser();
    if (!user || user.role !== 'SUPPLIER') {
        window.location.href = 'index.html';
        return;
    }
    loadAvailableRfqs();
});

async function loadAvailableRfqs() {
    const user = getUser();
    try {
        const res = await fetch(`/api/rfq/supplier/${user.id}/available`);
        const rfqs = await res.json();

        const grid = document.getElementById('rfq-grid');
        const emptyState = document.getElementById('empty-state');

        if (rfqs.length === 0) {
            grid.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        // Stats
        const joined = rfqs.filter(r => r.joined).length;
        document.getElementById('stat-available').textContent = rfqs.length;
        document.getElementById('stat-joined').textContent = joined;

        grid.innerHTML = rfqs.map(rfq => {
            const startTime = new Date(rfq.bidStartTime).toLocaleString();
            const closeTime = new Date(rfq.bidCloseTime).toLocaleString();
            const isJoined = rfq.joined;

            return `
                <div class="rfq-card">
                    <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:12px">
                        <div class="rfq-card-title">${rfq.rfqName}</div>
                        <span class="badge badge-active">ACTIVE</span>
                    </div>
                    <div class="rfq-card-meta">
                        <div class="rfq-meta-item">
                            <span class="label">Buyer</span>
                            <span class="value">${rfq.buyerName}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">🟢 Bid Start</span>
                            <span class="value" style="color:var(--accent-green);font-weight:600">${startTime}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Bid Close</span>
                            <span class="value">${closeTime}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Lowest Bid</span>
                            <span class="value" style="color:var(--accent-cyan);font-weight:700">${rfq.lowestBid ? '₹' + rfq.lowestBid.toFixed(2) : 'No bids'}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Total Bids</span>
                            <span class="value">${rfq.totalBids}</span>
                        </div>
                    </div>
                    <div class="rfq-card-footer">
                        <span style="color:var(--text-muted);font-size:13px">RFQ #${rfq.rfqId}</span>
                        ${isJoined
                            ? `<a href="supplier-rfq.html?id=${rfq.rfqId}" class="btn btn-primary btn-sm">Place Bid →</a>`
                            : `<button class="btn btn-success btn-sm" onclick="joinRfq(${rfq.rfqId}, event)">Join Auction</button>`
                        }
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load RFQs:', err);
    }
}

async function joinRfq(rfqId, event) {
    event.stopPropagation();
    const user = getUser();

    try {
        const res = await fetch(`/api/rfq/${rfqId}/join?supplierId=${user.id}`, {
            method: 'POST'
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to join');

        showToast('✅ Successfully joined the auction!');
        loadAvailableRfqs();
    } catch (err) {
        showToast('❌ ' + err.message);
    }
}
