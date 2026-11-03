/**
 * GestureTalk AI — Recognition Audit History Client Module (history.js)
 * Manages fetching, filtering, rendering, and clearing of persistent gesture history logs.
 */

class HistoryManager {

    constructor() {
        this.filterSelect = document.getElementById('historyGestureFilter');
        this.refreshBtn = document.getElementById('btnRefreshHistory');
        this.clearBtn = document.getElementById('btnClearHistory');
        this.tableBody = document.getElementById('historyTableBody');
        this.countBadge = document.getElementById('historyCountBadge');
        this.emptyState = document.getElementById('historyEmptyState');
        this.tableContainer = document.getElementById('historyTableContainer');
        this.loadingSpinner = document.getElementById('historyLoadingSpinner');
        this.statusAlert = document.getElementById('historyStatusAlert');

        this.gestureColors = {
            'THUMBS_UP': 'success',
            'THUMBS_DOWN': 'danger',
            'OPEN_PALM': 'warning',
            'V_SIGN': 'primary',
            'INDEX_POINTING': 'info',
            'UNKNOWN': 'secondary'
        };
    }

    /**
     * Initializes DOM bindings and loads the latest 20 history records.
     */
    init() {
        if (!this.tableBody) {
            console.warn('History table body element not found in DOM.');
            return;
        }

        if (this.filterSelect) {
            this.filterSelect.addEventListener('change', () => this.loadHistory());
        }

        if (this.refreshBtn) {
            this.refreshBtn.addEventListener('click', () => this.loadHistory());
        }

        if (this.clearBtn) {
            this.clearBtn.addEventListener('click', () => this.confirmAndClearHistory());
        }

        // Initial fetch
        this.loadHistory();
    }

    /**
     * Fetches up to 20 recent recognition records, optionally filtered by gesture type.
     */
    async loadHistory() {
        this.showLoading(true);
        this.hideStatusAlert();

        const selectedGesture = this.filterSelect ? this.filterSelect.value : 'ALL';
        let url = 'api/history?limit=20';
        if (selectedGesture && selectedGesture !== 'ALL') {
            url += '&gesture=' + encodeURIComponent(selectedGesture);
        }

        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.message || ('Server error: ' + response.status));
            }

            const data = await response.json();
            if (data.success && Array.isArray(data.records)) {
                this.renderTable(data.records);
            } else {
                throw new Error(data.message || 'Unexpected response format from history API.');
            }
        } catch (err) {
            console.warn('History retrieval warning:', err.message);
            this.showStatusAlert('Database service notice: ' + err.message, 'warning');
            this.renderTable([]);
        } finally {
            this.showLoading(false);
        }
    }

    /**
     * Renders records into the table body, or shows the empty state placeholder if none exist.
     */
    renderTable(records) {
        if (this.countBadge) {
            this.countBadge.textContent = records.length + (records.length === 1 ? ' record' : ' records');
        }

        if (!this.tableBody) return;
        this.tableBody.innerHTML = '';

        if (!records || records.length === 0) {
            if (this.tableContainer) this.tableContainer.classList.add('d-none');
            if (this.emptyState) this.emptyState.classList.remove('d-none');
            return;
        }

        if (this.emptyState) this.emptyState.classList.add('d-none');
        if (this.tableContainer) this.tableContainer.classList.remove('d-none');

        records.forEach(record => {
            const row = document.createElement('tr');
            row.className = 'align-middle';

            const badgeColor = this.gestureColors[record.gestureCode] || 'secondary';
            const confidencePct = (record.confidence * 100).toFixed(1) + '%';
            const formattedDate = this.formatDate(record.createdAt);

            const sourceBadge = record.sourceType === 'WEBCAM'
                ? '<span class="badge bg-dark border border-primary text-primary"><i class="bi bi-camera-video me-1"></i>Webcam</span>'
                : '<span class="badge bg-dark border border-info text-info"><i class="bi bi-upload me-1"></i>Upload</span>';

            row.innerHTML = `
                <td class="text-secondary small fw-mono">#${this.escapeHtml(String(record.id))}</td>
                <td>
                    <span class="badge bg-${badgeColor}-subtle text-${badgeColor} border border-${badgeColor} px-2 py-1">
                        ${this.escapeHtml(record.gestureCode)}
                    </span>
                </td>
                <td class="text-white fw-semibold small">${this.escapeHtml(record.mappedMessage)}</td>
                <td class="small">
                    <div class="d-flex align-items-center gap-2">
                        <span class="text-light fw-bold">${confidencePct}</span>
                        <div class="progress flex-grow-1" style="height: 6px; min-width: 60px; background-color: rgba(255,255,255,0.1);">
                            <div class="progress-bar bg-${badgeColor}" role="progressbar" style="width: ${confidencePct};" aria-valuenow="${confidencePct}" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                    </div>
                </td>
                <td class="small">${sourceBadge}</td>
                <td class="text-muted small">${this.escapeHtml(record.explanation || '—')}</td>
                <td class="text-secondary small text-nowrap">${this.escapeHtml(formattedDate)}</td>
            `;

            this.tableBody.appendChild(row);
        });
    }

    /**
     * Prompts the user for confirmation and issues DELETE /api/history to clear all records.
     */
    async confirmAndClearHistory() {
        const confirmed = window.confirm(
            'Are you sure you want to permanently clear all gesture recognition audit records?\n\nThis action cannot be undone.'
        );

        if (!confirmed) {
            return;
        }

        this.showLoading(true);
        try {
            const response = await fetch('api/history', {
                method: 'DELETE',
                headers: { 'Accept': 'application/json' }
            });

            const data = await response.json();
            if (response.ok && data.success) {
                this.showStatusAlert(`History cleared: ${data.deletedCount} record(s) removed.`, 'success');
                this.loadHistory();
            } else {
                throw new Error(data.message || 'Failed to clear history records.');
            }
        } catch (err) {
            console.error('Clear history error:', err);
            this.showStatusAlert('Failed to clear history: ' + err.message, 'danger');
        } finally {
            this.showLoading(false);
        }
    }

    showLoading(isLoading) {
        if (this.loadingSpinner) {
            this.loadingSpinner.classList.toggle('d-none', !isLoading);
        }
        if (this.refreshBtn) {
            this.refreshBtn.disabled = isLoading;
        }
    }

    showStatusAlert(message, type = 'info') {
        if (!this.statusAlert) return;
        this.statusAlert.className = `alert alert-${type} py-2 px-3 small mb-3 border-${type}`;
        this.statusAlert.textContent = message;
        this.statusAlert.classList.remove('d-none');
    }

    hideStatusAlert() {
        if (this.statusAlert) {
            this.statusAlert.classList.add('d-none');
        }
    }

    formatDate(timestamp) {
        if (!timestamp) return '—';
        try {
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) return String(timestamp);
            return date.toLocaleString();
        } catch (e) {
            return String(timestamp);
        }
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Instantiate and expose globally
window.historyManager = new HistoryManager();
document.addEventListener('DOMContentLoaded', () => {
    window.historyManager.init();
});
