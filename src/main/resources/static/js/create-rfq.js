// ====== CREATE-RFQ.JS - RFQ Creation ======

document.addEventListener('DOMContentLoaded', () => {
    const user = getUser();
    if (!user || user.role !== 'BUYER') {
        window.location.href = 'index.html';
        return;
    }

    // Set default dates
    const now = new Date();
    const start = new Date(now.getTime() + 5 * 60000);
    const close = new Date(now.getTime() + 60 * 60000);
    const forced = new Date(now.getTime() + 90 * 60000);
    const service = new Date(now.getTime() + 7 * 24 * 60 * 60000);

    document.getElementById('bid-start').value = formatDateLocal(start);
    document.getElementById('bid-close').value = formatDateLocal(close);
    document.getElementById('forced-close').value = formatDateLocal(forced);
    document.getElementById('service-date').value = formatDateLocal(service);
});

function formatDateLocal(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    return `${y}-${m}-${d}T${h}:${min}`;
}

async function handleCreateRfq(e) {
    e.preventDefault();
    hideAlerts();

    const user = getUser();
    const bidClose = new Date(document.getElementById('bid-close').value);
    const forcedClose = new Date(document.getElementById('forced-close').value);

    if (forcedClose <= bidClose) {
        showError('Forced Bid Close Time must be after Bid Close Time');
        return;
    }

    const payload = {
        buyerId: user.id,
        rfqName: document.getElementById('rfq-name').value,
        bidStartTime: document.getElementById('bid-start').value,
        bidCloseTime: document.getElementById('bid-close').value,
        forcedCloseTime: document.getElementById('forced-close').value,
        serviceDate: document.getElementById('service-date').value || null,
        triggerWindowMinutes: parseInt(document.getElementById('trigger-window').value),
        extensionDurationMinutes: parseInt(document.getElementById('extension-duration').value),
        triggerType: document.getElementById('trigger-type').value
    };

    try {
        const res = await fetch('/api/rfq', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to create RFQ');

        showSuccess('RFQ created successfully!');
        setTimeout(() => {
            window.location.href = 'buyer-dashboard.html';
        }, 1500);

    } catch (err) {
        showError(err.message);
    }
}
