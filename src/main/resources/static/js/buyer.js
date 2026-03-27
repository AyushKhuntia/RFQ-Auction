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

            const closeTime = new Date(rfq.bidCloseTime).toLocaleString();
            const forcedTime = new Date(rfq.forcedCloseTime).toLocaleString();

            return `
                <div class="rfq-card" onclick="window.location.href='rfq-details.html?id=${rfq.rfqId}'">
                    <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:12px">
                        <div class="rfq-card-title">${rfq.rfqName}</div>
                        <span class="badge ${statusClass}">${statusText}</span>
                    </div>
                    <div class="rfq-card-meta">
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
                        <span class="btn btn-secondary btn-sm">View Details →</span>
                    </div>
                </div>
            `;
        }).join('');

    } catch (err) {
        console.error('Failed to load RFQs:', err);
    }
}
