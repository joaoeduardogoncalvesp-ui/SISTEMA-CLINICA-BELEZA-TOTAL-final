const API_URL = '/api';

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(String(str)));
    return div.innerHTML;
}

const api = {
    getToken: () => localStorage.getItem('token'),
    
    setToken: (token) => localStorage.setItem('token', token),
    
    clearToken: () => localStorage.removeItem('token'),
    
    getHeaders: () => ({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${api.getToken()}`
    }),
    
    request: async (endpoint, options = {}) => {
        try {
            const response = await fetch(`${API_URL}${endpoint}`, {
                ...options,
                headers: api.getHeaders()
            });
            
            if (response.status === 401) {
                api.clearToken();
                window.location.href = 'login.html';
                return null;
            }
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.erro || 'Erro na requisição');
            }
            
            if (response.status === 204) return null;
            return await response.json();
        } catch (error) {
            throw error;
        }
    },
    
    get: (endpoint) => api.request(endpoint),
    
    post: (endpoint, data) => api.request(endpoint, {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    
    put: (endpoint, data) => api.request(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data)
    }),
    
    patch: (endpoint) => api.request(endpoint, { method: 'PATCH' }),
    
    delete: (endpoint) => api.request(endpoint, { method: 'DELETE' }),
    
    // Public endpoints (no auth required)
    publicGet: async (endpoint) => {
        try {
            const response = await fetch(`${API_URL}${endpoint}`);
            return await response.json();
        } catch (error) {
            throw error;
        }
    }
};

// Auth functions
const auth = {
    login: async (email, senha) => {
        const data = await api.post('/usuarios/login', { email, senha });
        if (data && data.token) {
            api.setToken(data.token);
            localStorage.setItem('user', JSON.stringify(data));
            return data;
        }
        return null;
    },
    
    logout: () => {
        api.clearToken();
        localStorage.removeItem('user');
        window.location.href = 'login.html';
    },
    
    getUser: () => {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },
    
    isAuthenticated: () => !!api.getToken(),
    
    hasRole: (roles) => {
        const user = auth.getUser();
        if (!user) return false;
        return roles.includes(user.perfil);
    }
};

// Format functions
const format = {
    date: (dateStr) => {
        if (!dateStr) return '-';
        const [year, month, day] = dateStr.split('-');
        return `${day}/${month}/${year}`;
    },
    
    currency: (value) => {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value);
    },
    
    phone: (phone) => phone || '-',
    
    status: (status) => {
        const statuses = {
            'AGENDADO': { label: 'Agendado', class: 'badge-info' },
            'CONFIRMADO': { label: 'Confirmado', class: 'badge-success' },
            'EM_ATENDIMENTO': { label: 'Em Atendimento', class: 'badge-warning' },
            'REALIZADO': { label: 'Realizado', class: 'badge-success' },
            'CANCELADO': { label: 'Cancelado', class: 'badge-danger' },
            'NAO_COMPARECEU': { label: 'Não Compareceu', class: 'badge-danger' }
        };
        const s = statuses[status] || { label: status, class: 'badge-info' };
        return `<span class="badge ${s.class}">${s.label}</span>`;
    }
};

// UI functions
const ui = {
    showAlert: (message, type = 'success') => {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type}`;
        alertDiv.textContent = message;
        
        const container = document.querySelector('.main-content') || document.body;
        container.insertBefore(alertDiv, container.firstChild);
        
        setTimeout(() => alertDiv.remove(), 3000);
    },
    
    showModal: (modalId) => {
        document.getElementById(modalId).style.display = 'block';
    },
    
    hideModal: (modalId) => {
        document.getElementById(modalId).style.display = 'none';
    },
    
    populateSelect: (selectId, data, valueField, labelField) => {
        const select = document.getElementById(selectId);
        select.innerHTML = '<option value="">Selecione...</option>';
        data.forEach(item => {
            const option = document.createElement('option');
            option.value = item[valueField];
            option.textContent = item[labelField];
            select.appendChild(option);
        });
    }
};
