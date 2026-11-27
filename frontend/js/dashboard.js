// Global data
let clientes = [];
let funcionarios = [];
let procedimentos = [];
let agendamentos = [];
let usuarios = [];
let horarios = [];

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

function getHoje() {
    return new Date().toISOString().split('T')[0];
}

function getProximoMes() {
    const d = new Date();
    d.setMonth(d.getMonth() + 3);
    return d.toISOString().split('T')[0];
}

function getMesPassado() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    return d.toISOString().split('T')[0];
}

function showLoading(tbodyId, cols) {
    const tbody = document.getElementById(tbodyId);
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="${cols}" style="text-align: center; padding: 2rem;"><div class="spinner" style="margin: 0 auto;"></div><p style="margin-top: 0.5rem; color: var(--text-muted);">Carregando...</p></td></tr>`;
    }
}

function showEmpty(tbodyId, cols, msg) {
    const tbody = document.getElementById(tbodyId);
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="${cols}" style="text-align: center; padding: 2rem; color: var(--text-muted);">${msg || 'Nenhum registro encontrado'}</td></tr>`;
    }
}

// Check authentication
if (!auth.isAuthenticated()) {
    window.location.href = 'login.html';
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadUserInfo();
    loadDashboard();
    setupFormHandlers();
});

function loadUserInfo() {
    const user = auth.getUser();
    if (user) {
        document.getElementById('userProfile').textContent = `${user.nome} - ${user.perfil}`;
        
        if (!auth.hasRole(['ADMIN', 'ESTETICISTA_ADMINISTRADORA'])) {
            document.getElementById('menuUsuarios').style.display = 'none';
        }
        if (!auth.hasRole(['ADMIN', 'ESTETICISTA_ADMINISTRADORA', 'RECEPCIONISTA'])) {
            document.getElementById('menuClientes').style.display = 'none';
            document.getElementById('menuFuncionarios').style.display = 'none';
        }
        if (!auth.hasRole(['ADMIN', 'ESTETICISTA_ADMINISTRADORA'])) {
            document.getElementById('menuIndicadores').style.display = 'none';
        }
        if (auth.hasRole(['FUNCIONARIO'])) {
            document.getElementById('menuHorarios').style.display = 'none';
            document.getElementById('menuHistorico').style.display = 'none';
            document.getElementById('btnNovoAgendamento').style.display = 'none';
            document.getElementById('btnNovoProcedimento').style.display = 'none';
            document.getElementById('cardStatClientes').style.display = 'none';
            document.getElementById('cardStatFuncionarios').style.display = 'none';
        }
    }
}

function podeVerCadastros() {
    return auth.hasRole(['ADMIN', 'RECEPCIONISTA', 'ESTETICISTA_ADMINISTRADORA']);
}

async function carregarCadastrosAuxiliares() {
    if (clientes.length === 0 && podeVerCadastros()) {
        clientes = await api.get('/clientes') || [];
    }
    if (funcionarios.length === 0 && podeVerCadastros()) {
        funcionarios = await api.get('/funcionarios') || [];
    }
    if (procedimentos.length === 0) {
        procedimentos = await api.get('/procedimentos') || [];
    }
}

function showSection(section) {
    document.querySelectorAll('.section').forEach(s => s.style.display = 'none');
    document.getElementById(`section${section.charAt(0).toUpperCase() + section.slice(1)}`).style.display = 'block';
    
    document.querySelectorAll('.sidebar-menu a').forEach(a => a.classList.remove('active'));
    const activeLink = document.querySelector(`.sidebar-menu a[onclick*="${section}"]`);
    if (activeLink) activeLink.classList.add('active');
    
    switch(section) {
        case 'dashboard': loadDashboard(); break;
        case 'clientes': loadClientes(); break;
        case 'funcionarios': loadFuncionarios(); break;
        case 'procedimentos': loadProcedimentos(); break;
        case 'agendamentos': loadAgendamentos(); break;
        case 'usuarios': loadUsuarios(); break;
        case 'horarios': loadHorarios(); break;
        case 'historico': loadHistoricoSelect(); break;
        case 'indicadores': loadIndicadoresInit(); break;
        case 'minhaAgenda': carregarMinhaAgenda(); break;
    }
}

// Dashboard
async function loadDashboard() {
    try {
        let c, f, p, a;
        if (auth.hasRole(['FUNCIONARIO'])) {
            [p, a] = await Promise.all([
                api.get('/procedimentos'),
                api.get('/agendamentos/me')
            ]);
            c = [];
            f = [];
        } else {
            [c, f, p, a] = await Promise.all([
                api.get('/clientes'),
                api.get('/funcionarios'),
                api.get('/procedimentos'),
                api.get('/agendamentos/periodo?inicio=' + getMesPassado() + '&fim=' + getProximoMes())
            ]);
        }
        
        clientes = c || [];
        funcionarios = f || [];
        procedimentos = p || [];
        agendamentos = a || [];
        
        document.getElementById('statClientes').textContent = clientes.length;
        document.getElementById('statFuncionarios').textContent = funcionarios.length;
        document.getElementById('statProcedimentos').textContent = procedimentos.length;
        document.getElementById('statAgendamentos').textContent = agendamentos.length;
    } catch (error) {
        console.error('Erro ao carregar dashboard:', error);
    }
}

// Clientes
async function loadClientes() {
    showLoading('clientesTable', 5);
    try {
        clientes = await api.get('/clientes') || [];
        const tbody = document.getElementById('clientesTable');
        if (clientes.length === 0) {
            showEmpty('clientesTable', 5);
            return;
        }
        tbody.innerHTML = clientes.map(c => `
            <tr>
                <td>${c.id}</td>
                <td>${escapeHtml(c.nome)}</td>
                <td>${escapeHtml(c.telefone)}</td>
                <td>${escapeHtml(c.email) || '-'}</td>
                <td>
                    <button class="btn btn-warning" onclick="editCliente(${c.id})">Editar</button>
                    <button class="btn btn-danger" onclick="deleteCliente(${c.id})">Excluir</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Erro ao carregar clientes:', error);
    }
}

function openClienteModal(cliente = null) {
    document.getElementById('clienteModalTitle').textContent = cliente ? 'Editar Cliente' : 'Novo Cliente';
    document.getElementById('clienteId').value = cliente ? cliente.id : '';
    document.getElementById('clienteNome').value = cliente ? cliente.nome : '';
    document.getElementById('clienteTelefone').value = cliente ? cliente.telefone : '';
    document.getElementById('clienteCpf').value = cliente ? cliente.cpf || '' : '';
    document.getElementById('clienteEmail').value = cliente ? cliente.email || '' : '';
    document.getElementById('clienteDataNascimento').value = cliente ? cliente.dataNascimento || '' : '';
    document.getElementById('clienteConsentimento').checked = cliente ? cliente.consentimentoDados : false;
    ui.showModal('clienteModal');
}

function editCliente(id) {
    const cliente = clientes.find(c => c.id === id);
    if (cliente) openClienteModal(cliente);
}

async function deleteCliente(id) {
    if (confirm('Tem certeza que deseja excluir este cliente?')) {
        try {
            await api.delete(`/clientes/${id}`);
            ui.showAlert('Cliente excluído com sucesso!');
            loadClientes();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Funcionários
async function loadFuncionarios() {
    showLoading('funcionariosTable', 5);
    try {
        funcionarios = await api.get('/funcionarios') || [];
        const tbody = document.getElementById('funcionariosTable');
        if (funcionarios.length === 0) {
            showEmpty('funcionariosTable', 5);
            return;
        }
        tbody.innerHTML = funcionarios.map(f => `
            <tr>
                <td>${f.id}</td>
                <td>${escapeHtml(f.nome)}</td>
                <td>${escapeHtml(f.telefone)}</td>
                <td>${escapeHtml(f.especialidade) || '-'}</td>
                <td>
                    <button class="btn btn-warning" onclick="editFuncionario(${f.id})">Editar</button>
                    <button class="btn btn-danger" onclick="deleteFuncionario(${f.id})">Excluir</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Erro ao carregar funcionários:', error);
    }
}

async function openFuncionarioModal(funcionario = null) {
    document.getElementById('funcionarioModalTitle').textContent = funcionario ? 'Editar Funcionário' : 'Novo Funcionário';
    document.getElementById('funcionarioId').value = funcionario ? funcionario.id : '';
    document.getElementById('funcionarioNome').value = funcionario ? funcionario.nome : '';
    document.getElementById('funcionarioTelefone').value = funcionario ? funcionario.telefone : '';
    document.getElementById('funcionarioCpf').value = funcionario ? funcionario.cpf : '';
    document.getElementById('funcionarioEspecialidade').value = funcionario ? funcionario.especialidade || '' : '';
    
    if (procedimentos.length === 0) {
        procedimentos = await api.get('/procedimentos') || [];
    }
    const select = document.getElementById('funcionarioProcedimentos');
    select.innerHTML = procedimentos.map(p => 
        `<option value="${p.id}" ${funcionario?.procedimentoIds?.includes(p.id) ? 'selected' : ''}>${escapeHtml(p.nome)}</option>`
    ).join('');
    
    ui.showModal('funcionarioModal');
}

function editFuncionario(id) {
    const funcionario = funcionarios.find(f => f.id === id);
    if (funcionario) openFuncionarioModal(funcionario);
}

async function deleteFuncionario(id) {
    if (confirm('Tem certeza que deseja excluir este funcionário?')) {
        try {
            await api.delete(`/funcionarios/${id}`);
            ui.showAlert('Funcionário excluído com sucesso!');
            loadFuncionarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Procedimentos
async function loadProcedimentos() {
    showLoading('procedimentosTable', 6);
    try {
        procedimentos = await api.get('/procedimentos') || [];
        const isFuncionario = auth.hasRole(['FUNCIONARIO']);
        const tbody = document.getElementById('procedimentosTable');
        if (procedimentos.length === 0) {
            showEmpty('procedimentosTable', 6);
            return;
        }
        tbody.innerHTML = procedimentos.map(p => `
            <tr>
                <td>${p.id}</td>
                <td>${escapeHtml(p.nome)}</td>
                <td><span class="badge badge-info">${p.categoria}</span></td>
                <td>${p.duracaoMinutos} min</td>
                <td>${p.valor ? format.currency(p.valor) : '-'}</td>
                <td>
                    ${isFuncionario ? '-' : `
                    <button class="btn btn-warning" onclick="editProcedimento(${p.id})">Editar</button>
                    <button class="btn btn-danger" onclick="deleteProcedimento(${p.id})">Excluir</button>`}
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Erro ao carregar procedimentos:', error);
    }
}

function openProcedimentoModal(procedimento = null) {
    document.getElementById('procedimentoModalTitle').textContent = procedimento ? 'Editar Procedimento' : 'Novo Procedimento';
    document.getElementById('procedimentoId').value = procedimento ? procedimento.id : '';
    document.getElementById('procedimentoNome').value = procedimento ? procedimento.nome : '';
    document.getElementById('procedimentoDescricao').value = procedimento ? procedimento.descricao : '';
    document.getElementById('procedimentoDuracao').value = procedimento ? procedimento.duracaoMinutos : '';
    document.getElementById('procedimentoValor').value = procedimento ? procedimento.valor : '';
    document.getElementById('procedimentoCategoria').value = procedimento ? procedimento.categoria : 'FACIAL';
    ui.showModal('procedimentoModal');
}

function editProcedimento(id) {
    const procedimento = procedimentos.find(p => p.id === id);
    if (procedimento) openProcedimentoModal(procedimento);
}

async function deleteProcedimento(id) {
    if (confirm('Tem certeza que deseja excluir este procedimento?')) {
        try {
            await api.delete(`/procedimentos/${id}`);
            ui.showAlert('Procedimento excluído com sucesso!');
            loadProcedimentos();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Agendamentos
async function loadAgendamentos() {
    showLoading('agendamentosTable', 8);
    try {
        const isFuncionario = auth.hasRole(['FUNCIONARIO']);
        agendamentos = await api.get(isFuncionario
            ? '/agendamentos/me'
            : '/agendamentos/periodo?inicio=' + getMesPassado() + '&fim=' + getProximoMes()) || [];
        
        await carregarCadastrosAuxiliares();
        
        const tbody = document.getElementById('agendamentosTable');
        if (agendamentos.length === 0) {
            showEmpty('agendamentosTable', 8);
            return;
        }
        tbody.innerHTML = agendamentos.map(a => {
            const cliente = clientes.find(c => c.id === a.clienteId);
            const funcionario = funcionarios.find(f => f.id === a.funcionarioId);
            const procedimento = procedimentos.find(p => p.id === a.procedimentoId);
            
            const acoes = isFuncionario
                ? `<button class="btn btn-primary" onclick="atualizarStatusAgendamento(${a.id})">Atualizar Status</button>`
                : `<button class="btn btn-warning" onclick="editAgendamento(${a.id})">Editar</button>
                   <button class="btn btn-danger" onclick="excluirAgendamento(${a.id})">Excluir</button>
                   <button class="btn btn-primary" onclick="atualizarStatusAgendamento(${a.id})">Atualizar Status</button>`;
            
            return `
                <tr>
                    <td>${a.id}</td>
                    <td>${escapeHtml(cliente?.nome || a.clienteNome) || '-'}</td>
                    <td>${escapeHtml(funcionario?.nome || a.funcionarioNome) || '-'}</td>
                    <td>${escapeHtml(procedimento?.nome || a.procedimentoNome) || '-'}</td>
                    <td>${format.date(a.data)}</td>
                    <td>${a.horaInicio}</td>
                    <td>${format.status(a.status || 'AGENDADO')}</td>
                    <td>${acoes}</td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        console.error('Erro ao carregar agendamentos:', error);
    }
}

async function openAgendamentoModal(agendamento = null) {
    document.getElementById('agendamentoModalTitle').textContent = agendamento ? 'Editar Agendamento' : 'Novo Agendamento';
    document.getElementById('agendamentoId').value = agendamento ? agendamento.id : '';
    document.getElementById('agendamentoData').value = agendamento ? agendamento.data : '';
    document.getElementById('agendamentoHora').value = agendamento ? agendamento.horaInicio : '';
    
    if (clientes.length === 0) clientes = await api.get('/clientes') || [];
    if (procedimentos.length === 0) procedimentos = await api.get('/procedimentos') || [];
    
    ui.populateSelect('agendamentoCliente', clientes, 'id', 'nome');
    ui.populateSelect('agendamentoProcedimento', procedimentos, 'id', 'nome');
    
    if (agendamento) {
        document.getElementById('agendamentoCliente').value = agendamento.clienteId;
        document.getElementById('agendamentoProcedimento').value = agendamento.procedimentoId;
        await carregarFuncionariosPorProcedimento();
        document.getElementById('agendamentoFuncionario').value = agendamento.funcionarioId;
    }
    
    ui.showModal('agendamentoModal');
}

async function carregarFuncionariosPorProcedimento() {
    const procedimentoId = document.getElementById('agendamentoProcedimento').value;
    if (procedimentoId) {
        try {
            const funcs = await api.get(`/funcionarios/procedimento/${procedimentoId}`);
            ui.populateSelect('agendamentoFuncionario', funcs || [], 'id', 'nome');
        } catch (error) {
            console.error('Erro ao carregar funcionários:', error);
        }
    }
}

function editAgendamento(id) {
    const agendamento = agendamentos.find(a => a.id === id);
    if (agendamento) openAgendamentoModal(agendamento);
}

async function excluirAgendamento(id) {
    if (confirm('Tem certeza que deseja excluir este agendamento?')) {
        try {
            await api.delete(`/agendamentos/${id}`);
            ui.showAlert('Agendamento excluído com sucesso!');
            loadAgendamentos();
            loadDashboard();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Usuários
async function loadUsuarios() {
    showLoading('usuariosTable', 5);
    try {
        usuarios = await api.get('/usuarios') || [];
        const tbody = document.getElementById('usuariosTable');
        if (usuarios.length === 0) {
            showEmpty('usuariosTable', 5);
            return;
        }
        tbody.innerHTML = usuarios.map(u => `
            <tr>
                <td>${u.id}</td>
                <td>${escapeHtml(u.nome)}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="badge badge-info">${u.perfil}</span></td>
                <td>
                    <button class="btn btn-warning" onclick="editUsuario(${u.id})">Editar</button>
                    <button class="btn btn-danger" onclick="deleteUsuario(${u.id})">Excluir</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Erro ao carregar usuários:', error);
    }
}

async function openUsuarioModal(usuario = null) {
    document.getElementById('usuarioModalTitle').textContent = usuario ? 'Editar Usuário' : 'Novo Usuário';
    document.getElementById('usuarioId').value = usuario ? usuario.id : '';
    document.getElementById('usuarioNome').value = usuario ? usuario.nome : '';
    document.getElementById('usuarioEmail').value = usuario ? usuario.email : '';
    document.getElementById('usuarioSenha').value = '';
    document.getElementById('usuarioPerfil').value = usuario ? usuario.perfil : 'FUNCIONARIO';

    if (funcionarios.length === 0) funcionarios = await api.get('/funcionarios') || [];
    const selectFuncionario = document.getElementById('usuarioFuncionario');
    selectFuncionario.innerHTML = '<option value="">— Nenhum —</option>' +
        funcionarios.map(f => `<option value="${f.id}">${escapeHtml(f.nome)}</option>`).join('');
    selectFuncionario.value = usuario?.funcionarioId ? String(usuario.funcionarioId) : '';

    ui.showModal('usuarioModal');
}

function editUsuario(id) {
    const usuario = usuarios.find(u => u.id === id);
    if (usuario) openUsuarioModal(usuario);
}

async function deleteUsuario(id) {
    if (confirm('Tem certeza que deseja excluir este usuário?')) {
        try {
            await api.delete(`/usuarios/${id}`);
            ui.showAlert('Usuário excluído com sucesso!');
            loadUsuarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Horários
async function loadHorarios() {
    showLoading('horariosTable', 6);
    try {
        if (funcionarios.length === 0) funcionarios = await api.get('/funcionarios') || [];
        
        const allHorarios = [];
        for (const func of funcionarios) {
            const h = await api.get(`/horarios/funcionario/${func.id}`);
            if (h) h.forEach(item => allHorarios.push({...item, funcionarioNome: func.nome}));
        }
        horarios = allHorarios;
        
        const tbody = document.getElementById('horariosTable');
        if (horarios.length === 0) {
            showEmpty('horariosTable', 6);
            return;
        }
        tbody.innerHTML = horarios.map(h => `
            <tr>
                <td>${h.id}</td>
                <td>${escapeHtml(h.funcionarioNome) || '-'}</td>
                <td>${h.diaSemana}</td>
                <td>${h.horaInicio}</td>
                <td>${h.horaFim}</td>
                <td>
                    <button class="btn btn-warning" onclick="editHorario(${h.id})">Editar</button>
                    <button class="btn btn-danger" onclick="deleteHorario(${h.id})">Excluir</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Erro ao carregar horários:', error);
    }
}

async function openHorarioModal(horario = null) {
    if (funcionarios.length === 0) funcionarios = await api.get('/funcionarios') || [];
    ui.populateSelect('horarioFuncionario', funcionarios, 'id', 'nome');
    
    if (horario) {
        document.getElementById('horarioId').value = horario.id;
        document.getElementById('horarioFuncionario').value = horario.funcionarioId;
        document.getElementById('horarioDia').value = horario.diaSemana;
        document.getElementById('horarioInicio').value = horario.horaInicio;
        document.getElementById('horarioFim').value = horario.horaFim;
        document.getElementById('horarioModalTitle').textContent = 'Editar Horário';
    } else {
        document.getElementById('horarioId').value = '';
        document.getElementById('horarioDia').value = '';
        document.getElementById('horarioInicio').value = '';
        document.getElementById('horarioFim').value = '';
        document.getElementById('horarioModalTitle').textContent = 'Novo Horário';
    }
    
    ui.showModal('horarioModal');
}

function editHorario(id) {
    const horario = horarios.find(h => h.id === id);
    if (horario) openHorarioModal(horario);
}

async function deleteHorario(id) {
    if (confirm('Tem certeza que deseja excluir este horário?')) {
        try {
            await api.delete(`/horarios/${id}`);
            ui.showAlert('Horário excluído com sucesso!');
            loadHorarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    }
}

// Histórico
async function loadHistoricoSelect() {
    try {
        if (clientes.length === 0) clientes = await api.get('/clientes') || [];
        ui.populateSelect('historicoCliente', clientes, 'id', 'nome');
    } catch (error) {
        console.error('Erro ao carregar clientes para histórico:', error);
    }
}

async function buscarHistorico() {
    const clienteId = document.getElementById('historicoCliente').value;
    if (!clienteId) {
        ui.showAlert('Selecione um cliente', 'error');
        return;
    }
    
    try {
        const historico = await api.get(`/agendamentos/cliente/${clienteId}/historico`);
        const tbody = document.getElementById('historicoTable');
        
        if (!historico || historico.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">Nenhum agendamento encontrado</td></tr>';
            return;
        }
        
        tbody.innerHTML = historico.map(h => `
            <tr>
                <td>${h.id}</td>
                <td>${escapeHtml(h.nomeCliente)}</td>
                <td>${escapeHtml(h.nomeFuncionario)}</td>
                <td>${escapeHtml(h.nomeProcedimento)}</td>
                <td>${format.date(h.data)}</td>
                <td>${h.horaInicio} - ${h.horaFim}</td>
                <td>${format.status(h.status)}</td>
            </tr>
        `).join('');
    } catch (error) {
        ui.showAlert(error.message, 'error');
    }
}

// Indicadores
function loadIndicadoresInit() {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    document.getElementById('indicadorInicio').value = firstDay.toISOString().split('T')[0];
    document.getElementById('indicadorFim').value = today.toISOString().split('T')[0];
    carregarIndicadores();
}

async function carregarIndicadores() {
    const inicio = document.getElementById('indicadorInicio').value;
    const fim = document.getElementById('indicadorFim').value;

    if (!inicio || !fim) {
        ui.showAlert('Informe o período', 'error');
        return;
    }

    try {
        const data = await api.get(`/indicadores?inicio=${inicio}&fim=${fim}`);

        document.getElementById('indHoje').textContent = data.totalAgendamentosHoje || 0;
        document.getElementById('indPeriodo').textContent = data.totalAgendamentosPeriodo || 0;
        document.getElementById('indRealizados').textContent = data.realizados || 0;
        document.getElementById('indCancelados').textContent = data.cancelados || 0;
        document.getElementById('indPendentes').textContent = data.pendentes || 0;
        document.getElementById('indNaoCompareceram').textContent = data.naoCompareceram || 0;
        document.getElementById('indTotalClientes').textContent = data.totalClientes || 0;

        const tbodyProf = document.getElementById('indicadorProfissionaisTable');
        if (data.porProfissional && data.porProfissional.length > 0) {
            tbodyProf.innerHTML = data.porProfissional.map(p => `
                <tr>
                    <td>${escapeHtml(p.nomeProfissional)}</td>
                    <td>${p.total}</td>
                    <td>${p.realizados}</td>
                    <td>${p.cancelados}</td>
                </tr>
            `).join('');
        } else {
            tbodyProf.innerHTML = '<tr><td colspan="4" style="text-align: center;">Sem dados</td></tr>';
        }

        const tbodyProc = document.getElementById('indicadorProcedimentosTable');
        if (data.porProcedimento && data.porProcedimento.length > 0) {
            tbodyProc.innerHTML = data.porProcedimento.map(p => `
                <tr>
                    <td>${escapeHtml(p.nomeProcedimento)}</td>
                    <td><span class="badge badge-info">${p.categoria}</span></td>
                    <td>${p.total}</td>
                </tr>
            `).join('');
        } else {
            tbodyProc.innerHTML = '<tr><td colspan="3" style="text-align: center;">Sem dados</td></tr>';
        }
    } catch (error) {
        ui.showAlert(error.message, 'error');
    }
}

// Minha Agenda
async function carregarMinhaAgenda() {
    try {
        const agenda = await api.get('/agendamentos/me');

        await carregarCadastrosAuxiliares();

        renderMinhaAgenda(agenda || []);
    } catch (error) {
        ui.showAlert(error.message, 'error');
    }
}

function filtrarMinhaAgenda() {
    const dataFiltro = document.getElementById('minhaAgendaData').value;
    if (!dataFiltro) {
        carregarMinhaAgenda();
        return;
    }
    const filtrados = (window._minhaAgendaData || []).filter(a => a.data === dataFiltro);
    renderMinhaAgenda(filtrados);
}

function renderMinhaAgenda(agendamentos) {
    window._minhaAgendaData = agendamentos;
    const tbody = document.getElementById('minhaAgendaTable');

    if (!agendamentos || agendamentos.length === 0) {
        showEmpty('minhaAgendaTable', 7, 'Nenhum agendamento encontrado');
        return;
    }

    tbody.innerHTML = agendamentos.map(a => {
        const cliente = clientes.find(c => c.id === a.clienteId);
        const procedimento = procedimentos.find(p => p.id === a.procedimentoId);

        return `
            <tr>
                <td>${a.id}</td>
                <td>${escapeHtml(cliente?.nome || a.clienteNome) || '-'}</td>
                <td>${escapeHtml(procedimento?.nome || a.procedimentoNome) || '-'}</td>
                <td>${format.date(a.data)}</td>
                <td>${a.horaInicio}</td>
                <td>${format.status(a.status || 'AGENDADO')}</td>
                <td>
                    <button class="btn btn-warning" onclick="atualizarStatusAgendamento(${a.id})">Atualizar Status</button>
                </td>
            </tr>
        `;
    }).join('');
}

async function atualizarStatusAgendamento(id) {
    document.getElementById('statusAgendamentoId').value = id;
    document.getElementById('statusSelect').value = '';
    ui.showModal('statusModal');
}

async function salvarStatusAgendamento() {
    const id = document.getElementById('statusAgendamentoId').value;
    const status = document.getElementById('statusSelect').value;
    
    if (!status) {
        ui.showAlert('Selecione um status', 'error');
        return;
    }

    try {
        await api.patch(`/agendamentos/${id}/status?status=${status}`);
        ui.showAlert('Status atualizado com sucesso!');
        ui.hideModal('statusModal');
        if (document.getElementById('sectionMinhaAgenda').style.display !== 'none') {
            await carregarMinhaAgenda();
        } else {
            await loadAgendamentos();
        }
        await loadDashboard();
    } catch (error) {
        ui.showAlert(error.message, 'error');
    }
}

// Filters
function filtrarClientes() {
    const busca = document.getElementById('clienteSearch').value.toLowerCase();
    const filtrados = clientes.filter(c => c.nome.toLowerCase().includes(busca));
    const tbody = document.getElementById('clientesTable');
    if (filtrados.length === 0) {
        showEmpty('clientesTable', 5, 'Nenhum cliente encontrado');
        return;
    }
    tbody.innerHTML = filtrados.map(c => `
        <tr>
            <td>${c.id}</td>
            <td>${escapeHtml(c.nome)}</td>
            <td>${escapeHtml(c.telefone)}</td>
            <td>${escapeHtml(c.email) || '-'}</td>
            <td>
                <button class="btn btn-warning" onclick="editCliente(${c.id})">Editar</button>
                <button class="btn btn-danger" onclick="deleteCliente(${c.id})">Excluir</button>
            </td>
        </tr>
    `).join('');
}

function filtrarAgendamentos() {
    const busca = document.getElementById('agendamentoSearch').value.toLowerCase();
    const statusFiltro = document.getElementById('agendamentoStatusFilter').value;
    let filtrados = agendamentos.filter(a => {
        const cliente = clientes.find(c => c.id === a.clienteId);
        const matchBusca = !busca || (cliente && cliente.nome.toLowerCase().includes(busca));
        const matchStatus = !statusFiltro || a.status === statusFiltro;
        return matchBusca && matchStatus;
    });
    const tbody = document.getElementById('agendamentosTable');
    if (filtrados.length === 0) {
        showEmpty('agendamentosTable', 8, 'Nenhum agendamento encontrado');
        return;
    }
    tbody.innerHTML = filtrados.map(a => {
        const cliente = clientes.find(c => c.id === a.clienteId);
        const funcionario = funcionarios.find(f => f.id === a.funcionarioId);
        const procedimento = procedimentos.find(p => p.id === a.procedimentoId);
        return `
            <tr>
                <td>${a.id}</td>
                <td>${escapeHtml(cliente?.nome) || '-'}</td>
                <td>${escapeHtml(funcionario?.nome) || '-'}</td>
                <td>${escapeHtml(procedimento?.nome) || '-'}</td>
                <td>${format.date(a.data)}</td>
                <td>${a.horaInicio}</td>
                <td>${format.status(a.status || 'AGENDADO')}</td>
                <td>
                    <button class="btn btn-warning" onclick="editAgendamento(${a.id})">Editar</button>
                    <button class="btn btn-danger" onclick="excluirAgendamento(${a.id})">Excluir</button>
                    <button class="btn btn-primary" onclick="atualizarStatusAgendamento(${a.id})">Atualizar Status</button>
                </td>
            </tr>
        `;
    }).join('');
}

// Form handlers
function setupFormHandlers() {
    // Cliente
    document.getElementById('clienteForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('clienteId').value;
        const data = {
            nome: document.getElementById('clienteNome').value,
            telefone: document.getElementById('clienteTelefone').value,
            cpf: document.getElementById('clienteCpf').value || null,
            email: document.getElementById('clienteEmail').value || null,
            dataNascimento: document.getElementById('clienteDataNascimento').value || null,
            consentimentoDados: document.getElementById('clienteConsentimento').checked,
            consentimentoWhatsApp: document.getElementById('clienteConsentimento').checked
        };
        
        try {
            if (id) {
                await api.put(`/clientes/${id}`, data);
                ui.showAlert('Cliente atualizado com sucesso!');
            } else {
                await api.post('/clientes', data);
                ui.showAlert('Cliente cadastrado com sucesso!');
            }
            ui.hideModal('clienteModal');
            loadClientes();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Funcionário
    document.getElementById('funcionarioForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('funcionarioId').value;
        const procedimentoIds = Array.from(document.getElementById('funcionarioProcedimentos').selectedOptions).map(o => parseInt(o.value));
        
        const data = {
            nome: document.getElementById('funcionarioNome').value,
            telefone: document.getElementById('funcionarioTelefone').value,
            cpf: document.getElementById('funcionarioCpf').value,
            especialidade: document.getElementById('funcionarioEspecialidade').value || null,
            procedimentoIds: procedimentoIds
        };
        
        try {
            if (id) {
                await api.put(`/funcionarios/${id}`, data);
                ui.showAlert('Funcionário atualizado com sucesso!');
            } else {
                await api.post('/funcionarios', data);
                ui.showAlert('Funcionário cadastrado com sucesso!');
            }
            ui.hideModal('funcionarioModal');
            loadFuncionarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Procedimento
    document.getElementById('procedimentoForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('procedimentoId').value;
        const data = {
            nome: document.getElementById('procedimentoNome').value,
            descricao: document.getElementById('procedimentoDescricao').value,
                    duracaoMinutos: parseInt(document.getElementById('procedimentoDuracao').value),
                    valor: parseFloat(document.getElementById('procedimentoValor').value) || null,
            categoria: document.getElementById('procedimentoCategoria').value
        };
        
        try {
            if (id) {
                await api.put(`/procedimentos/${id}`, data);
                ui.showAlert('Procedimento atualizado com sucesso!');
            } else {
                await api.post('/procedimentos', data);
                ui.showAlert('Procedimento cadastrado com sucesso!');
            }
            ui.hideModal('procedimentoModal');
            loadProcedimentos();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Agendamento
    document.getElementById('agendamentoForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('agendamentoId').value;
        const data = {
            clienteId: parseInt(document.getElementById('agendamentoCliente').value),
            procedimentoId: parseInt(document.getElementById('agendamentoProcedimento').value),
            funcionarioId: parseInt(document.getElementById('agendamentoFuncionario').value),
            data: document.getElementById('agendamentoData').value,
            horaInicio: document.getElementById('agendamentoHora').value
        };
        
        try {
            if (id) {
                await api.put(`/agendamentos/${id}`, data);
                ui.showAlert('Agendamento atualizado com sucesso!');
            } else {
                await api.post('/agendamentos', data);
                ui.showAlert('Agendamento realizado com sucesso!');
            }
            ui.hideModal('agendamentoModal');
            loadAgendamentos();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Usuário
    document.getElementById('usuarioForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('usuarioId').value;
        const senha = document.getElementById('usuarioSenha').value;
        const perfil = document.getElementById('usuarioPerfil').value;
        const funcionarioId = document.getElementById('usuarioFuncionario').value;
        
        if (perfil === 'FUNCIONARIO' && !funcionarioId) {
            ui.showAlert('Selecione o funcionário vinculado para o perfil Funcionário', 'error');
            return;
        }
        
        const data = {
            nome: document.getElementById('usuarioNome').value,
            email: document.getElementById('usuarioEmail').value,
            senha: senha || undefined,
            perfil: perfil,
            funcionarioId: funcionarioId ? Number(funcionarioId) : null
        };
        
        try {
            if (id) {
                await api.put(`/usuarios/${id}`, data);
                ui.showAlert('Usuário atualizado com sucesso!');
            } else {
                if (!senha) {
                    ui.showAlert('Senha é obrigatória para novo usuário', 'error');
                    return;
                }
                await api.post('/usuarios', data);
                ui.showAlert('Usuário cadastrado com sucesso!');
            }
            ui.hideModal('usuarioModal');
            loadUsuarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Horário
    document.getElementById('horarioForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('horarioId')?.value;
        const data = {
            funcionarioId: parseInt(document.getElementById('horarioFuncionario').value),
            diaSemana: document.getElementById('horarioDia').value,
            horaInicio: document.getElementById('horarioInicio').value,
            horaFim: document.getElementById('horarioFim').value
        };
        
        try {
            if (id) {
                await api.put(`/horarios/${id}`, data);
                ui.showAlert('Horário atualizado com sucesso!');
            } else {
                await api.post('/horarios', data);
                ui.showAlert('Horário cadastrado com sucesso!');
            }
            ui.hideModal('horarioModal');
            loadHorarios();
        } catch (error) {
            ui.showAlert(error.message, 'error');
        }
    });

    // Status
    document.getElementById('statusForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        salvarStatusAgendamento();
    });
}
