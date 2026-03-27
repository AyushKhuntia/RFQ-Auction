// ====== BUYER.JS - Buyer Dashboard ======

document.addEventListener('DOMContentLoaded', () => {
    const user = getUser();
    if (!user || user.role !== 'BUYER') {
        window.location.href = 'index.html';
        return;
    }
    loadBuyerRfqs();
});

async function loadBuyerRfqs() {
    const user = getUser();
    try {
        const res = await fetch(`/api/rfq/buyer/${user.id}`);
        const rfqs = await res.json();

        const grid = document.getElementById('rfq-grid');
        const emptyState = document.getElementById('empty-state');

        if (rfqs.length === 0) {
            grid.style.display = 'none';
            emptyState.style.display = 'block';
            return;
        }

        emptyState.style.display = 'none';
        grid.style.display = '';

        // Stats
        const total = rfqs.length;
        const active = rfqs.filter(r => r.status === 'ACTIVE').length;
        const closed = rfqs.filter(r => r.status !== 'ACTIVE').length;

        document.getElementById('stat-total').textContent = total;
        document.getElementById('stat-active').textContent = active;
        document.getElementById('stat-closed').textContent = closed;

        grid.innerHTML = rfqs.map(rfq => {
            const statusClass = rfq.status === 'ACTIVE' ? 'badge-active' :
                               rfq.status === 'FORCE_CLOSED' ? 'badge-force-closed' : 'badge-closed';
            const statusText = rfq.status.replace('_', ' ');

            const startTime = new Date(rfq.bidStartTime).toLocaleString();
            const closeTime = new Date(rfq.bidCloseTime).toLocaleString();
            const forcedTime = new Date(rfq.forcedCloseTime).toLocaleString();

            return `
                <div class="rfq-card">
                    <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:12px">
                        <div class="rfq-card-title">${rfq.rfqName}</div>
                        <span class="badge ${statusClass}">${statusText}</span>
                    </div>
                    <div class="rfq-card-meta">
                        <div class="rfq-meta-item">
                            <span class="label">🟢 Bid Start</span>
                            <span class="value" style="color:var(--accent-green);font-weight:600">${startTime}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Bid Close</span>
                            <span class="value">${closeTime}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Forced Close</span>
                            <span class="value">${forcedTime}</span>
                        </div>
                        <div class="rfq-meta-item">
                            <span class="label">Trigger Type</span>
                            <span class="value">${rfq.auctionConfig ? rfq.auctionConfig.triggerType : '--'}</span>
                        </div>
                    </div>
                    <div class="rfq-card-footer">
                        <span style="color:var(--text-muted);font-size:13px">RFQ #${rfq.rfqId}</span>
                        <div style="display:flex;gap:8px">
                            <button class="btn btn-danger btn-sm" onclick="deleteRfq(${rfq.rfqId}, event)" title="Delete RFQ">🗑️ Delete</button>
                            <a href="rfq-details.html?id=${rfq.rfqId}" class="btn btn-secondary btn-sm">View Details →</a>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load RFQs:', err);
    }
}

async function deleteRfq(rfqId, event) {
    event.stopPropagation();

    if (!confirm('Are you sure you want to delete this RFQ? This will remove all bids, participations, and activity logs. This action cannot be undone.')) {
        return;
    }

    const user = getUser();
    try {
        const res = await fetch(`/api/rfq/${rfqId}?buyerId=${user.id}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to delete RFQ');

        showToast('✅ RFQ deleted successfully!');
        loadBuyerRfqs(); // Reload
    } catch (err) {
        showToast('❌ ' + err.message);
    }
}
