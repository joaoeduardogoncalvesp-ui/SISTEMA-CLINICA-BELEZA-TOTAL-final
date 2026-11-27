package com.clinicabelezatotal.config;

import com.clinicabelezatotal.model.*;
import com.clinicabelezatotal.model.Agendamento.StatusAgendamento;
import com.clinicabelezatotal.model.Usuario.PerfilUsuario;
import com.clinicabelezatotal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ClienteRepository clienteRepository;
    private final HorarioAtendimentoRepository horarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        removerUsuariosInativos();
        criarProcedimentosSeNaoExistirem();
        criarFuncionariosSeNaoExistirem();
        criarUsuariosSeNaoExistirem();
        criarClientesSeNaoExistirem();
        criarHorariosSeNaoExistirem();
        criarAgendamentosSeNaoExistirem();
    }

    private void removerUsuariosInativos() {
        List<Usuario> inativos = usuarioRepository.findByAtivoFalse();
        if (!inativos.isEmpty()) {
            usuarioRepository.deleteAll(inativos);
            log.info("{} usuário(s) inativo(s) removido(s) do banco", inativos.size());
        }
    }

    private void criarProcedimentosSeNaoExistirem() {
        List<Procedimento> existentes = procedimentoRepository.findAll();

        List<Procedimento> procedimentos = List.of(
                Procedimento.builder()
                    .nome("Limpeza de Pele")
                    .descricao("Limpeza profunda da pele com extração de impurezas, hidratação e proteção")
                    .duracaoMinutos(60)
                    .categoria(Procedimento.CategoriaProcedimento.FACIAL)
                    .imagemUrl("https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=400&h=300&fit=crop")
                    .ativo(true)
                    .build(),
                Procedimento.builder()
                    .nome("Maquiagem Profissional")
                    .descricao("Maquiagem completa para eventos, formaturas, casamentos e ocasiões especiais")
                    .duracaoMinutos(90)
                    .categoria(Procedimento.CategoriaProcedimento.FACIAL)
                    .imagemUrl("https://images.unsplash.com/photo-1487412947147-5cebf100ffc2?w=400&h=300&fit=crop")
                    .ativo(true)
                    .build(),
                Procedimento.builder()
                    .nome("Penteados")
                    .descricao("Penteados para noivas, formaturas e eventos. Escova, progressiva, tratamentos capilares")
                    .duracaoMinutos(120)
                    .categoria(Procedimento.CategoriaProcedimento.CAPILAR)
                    .imagemUrl("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=400&h=300&fit=crop")
                    .ativo(true)
                    .build(),
                Procedimento.builder()
                    .nome("Unhas em Gel")
                    .descricao("Alongamento e manutenção de unhas em gel com acabamento perfeito e duradouro")
                    .duracaoMinutos(90)
                    .categoria(Procedimento.CategoriaProcedimento.CORPORAL)
                    .imagemUrl("https://images.unsplash.com/photo-1604654894610-df63bc536371?w=400&h=300&fit=crop")
                    .ativo(true)
                    .build(),
                Procedimento.builder()
                    .nome("Massagem Relaxante")
                    .descricao("Massagem corporal relaxante com óleos essenciais para alívio de tensões e estresse")
                    .duracaoMinutos(60)
                    .categoria(Procedimento.CategoriaProcedimento.CORPORAL)
                    .imagemUrl("https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=400&h=300&fit=crop")
                    .ativo(true)
                    .build()
            );

        List<Procedimento> novos = procedimentos.stream()
            .filter(p -> existentes.stream().noneMatch(e -> e.getNome().equals(p.getNome())))
            .toList();

        if (!novos.isEmpty()) {
            procedimentoRepository.saveAll(novos);
            log.info("{} procedimento(s) criado(s) com sucesso", novos.size());
        }
    }

    private Procedimento buscarProcedimento(List<Procedimento> procedimentos, String nome) {
        return procedimentos.stream()
            .filter(p -> p.getNome().equals(nome))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Procedimento não encontrado: " + nome));
    }

    private void criarFuncionariosSeNaoExistirem() {
        if (funcionarioRepository.count() == 0) {
            List<Procedimento> todosProcedimentos = procedimentoRepository.findAll();

            List<Funcionario> funcionarios = List.of(
                Funcionario.builder()
                    .nome("Ana Carolina Silva")
                    .telefone("(62) 99991-1111")
                    .cpf("111.111.111-11")
                    .especialidade("Esteticista Facial")
                    .procedimentosHabilitados(List.of(buscarProcedimento(todosProcedimentos, "Limpeza de Pele"), buscarProcedimento(todosProcedimentos, "Unhas em Gel")))
                    .ativo(true)
                    .build(),
                Funcionario.builder()
                    .nome("Beatriz Santos")
                    .telefone("(62) 99992-2222")
                    .cpf("222.222.222-22")
                    .especialidade("Esteticista Corporal")
                    .procedimentosHabilitados(List.of(buscarProcedimento(todosProcedimentos, "Maquiagem Profissional"), buscarProcedimento(todosProcedimentos, "Massagem Relaxante")))
                    .ativo(true)
                    .build(),
                Funcionario.builder()
                    .nome("Camila Oliveira")
                    .telefone("(62) 99993-3333")
                    .cpf("333.333.333-33")
                    .especialidade("Tricologista")
                    .procedimentosHabilitados(List.of(buscarProcedimento(todosProcedimentos, "Penteados")))
                    .ativo(true)
                    .build(),
                Funcionario.builder()
                    .nome("Daniela Costa")
                    .telefone("(62) 99994-4444")
                    .cpf("444.444.444-44")
                    .especialidade("Esteticista Geral")
                    .procedimentosHabilitados(List.of(buscarProcedimento(todosProcedimentos, "Limpeza de Pele"), buscarProcedimento(todosProcedimentos, "Maquiagem Profissional"), buscarProcedimento(todosProcedimentos, "Unhas em Gel")))
                    .ativo(true)
                    .build(),
                Funcionario.builder()
                    .nome("Elena Martins")
                    .telefone("(62) 99995-5555")
                    .cpf("555.555.555-55")
                    .especialidade("Massoterapeuta")
                    .procedimentosHabilitados(List.of(buscarProcedimento(todosProcedimentos, "Maquiagem Profissional"), buscarProcedimento(todosProcedimentos, "Massagem Relaxante")))
                    .ativo(true)
                    .build()
            );
            funcionarioRepository.saveAll(funcionarios);
            log.info("5 funcionários criados com sucesso");
        }
    }

    /**
     * Cria individualmente os logins padrão que ainda não existem.
     *
     * A verificação é feita por e-mail, e não pelo total de usuários, para que seja
     * possível recriar um login removido (por exemplo, os usuários com senha fixa
     * eliminados pela migração V5) sem tocar nos demais. A senha é sempre aleatória e
     * exibida uma única vez no log, nunca gravada em arquivo de migração.
     */
    private void criarUsuariosSeNaoExistirem() {
        List<Funcionario> funcionarios = funcionarioRepository.findAll();

        boolean algumCriado = false;
        algumCriado |= criarUsuarioSeAusente("admin@belezatotal.com.br", "Administrador",
                PerfilUsuario.ADMIN, null);
        algumCriado |= criarUsuarioSeAusente("silvana@belezatotal.com.br", "Silvana da Costa",
                PerfilUsuario.ESTETICISTA_ADMINISTRADORA, null);
        algumCriado |= criarUsuarioSeAusente("fernanda@belezatotal.com.br", "Fernanda Lima",
                PerfilUsuario.RECEPCIONISTA, null);
        algumCriado |= criarUsuarioSeAusente("ana@belezatotal.com.br", "Ana Carolina Silva",
                PerfilUsuario.FUNCIONARIO, funcionarios.size() > 0 ? funcionarios.get(0) : null);
        algumCriado |= criarUsuarioSeAusente("beatriz@belezatotal.com.br", "Beatriz Santos",
                PerfilUsuario.FUNCIONARIO, funcionarios.size() > 1 ? funcionarios.get(1) : null);

        if (algumCriado) {
            log.info("==============================================");
        }
    }

    private boolean criarUsuarioSeAusente(String email, String nome, PerfilUsuario perfil,
                                          Funcionario funcionario) {
        if (usuarioRepository.existsByEmail(email)) {
            return false;
        }

        String senha = UUID.randomUUID().toString().substring(0, 12);
        usuarioRepository.save(Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .perfil(perfil)
                .funcionario(funcionario)
                .ativo(true)
                .build());

        log.info("=== USUÁRIO CRIADO (SENHA AUTOMÁTICA, EXIBIDA UMA ÚNICA VEZ) ===");
        log.info("{}: {} / {}", perfil, email, senha);
        return true;
    }

    private void criarClientesSeNaoExistirem() {
        if (clienteRepository.count() == 0) {
            List<Cliente> clientes = List.of(
                Cliente.builder()
                    .nome("Maria José da Silva")
                    .telefone("(62) 98881-1111")
                    .cpf("111.222.333-44")
                    .email("maria.jose@email.com")
                    .dataNascimento(LocalDate.of(1985, 3, 15))
                    .consentimentoDados(true)
                    .consentimentoWhatsApp(true)
                    .dataConsentimento(LocalDate.now().atStartOfDay())
                    .ativo(true)
                    .build(),
                Cliente.builder()
                    .nome("Juliana Pereira")
                    .telefone("(62) 98882-2222")
                    .cpf("222.333.444-55")
                    .email("juliana@email.com")
                    .dataNascimento(LocalDate.of(1990, 7, 22))
                    .consentimentoDados(true)
                    .consentimentoWhatsApp(true)
                    .dataConsentimento(LocalDate.now().atStartOfDay())
                    .ativo(true)
                    .build(),
                Cliente.builder()
                    .nome("Patrícia Ferreira")
                    .telefone("(62) 98883-3333")
                    .cpf("333.444.555-66")
                    .email("patricia@email.com")
                    .dataNascimento(LocalDate.of(1978, 11, 8))
                    .consentimentoDados(true)
                    .consentimentoWhatsApp(false)
                    .dataConsentimento(LocalDate.now().atStartOfDay())
                    .ativo(true)
                    .build(),
                Cliente.builder()
                    .nome("Camila Souza")
                    .telefone("(62) 98884-4444")
                    .cpf("444.555.666-77")
                    .email("camila@email.com")
                    .dataNascimento(LocalDate.of(1995, 5, 30))
                    .consentimentoDados(true)
                    .consentimentoWhatsApp(true)
                    .dataConsentimento(LocalDate.now().atStartOfDay())
                    .ativo(true)
                    .build(),
                Cliente.builder()
                    .nome("Renata Almeida")
                    .telefone("(62) 98885-5555")
                    .cpf("555.666.777-88")
                    .email("renata@email.com")
                    .dataNascimento(LocalDate.of(1988, 9, 12))
                    .consentimentoDados(true)
                    .consentimentoWhatsApp(true)
                    .dataConsentimento(LocalDate.now().atStartOfDay())
                    .ativo(true)
                    .build()
            );
            clienteRepository.saveAll(clientes);
            log.info("5 clientes criados com sucesso");
        }
    }

    private void criarHorariosSeNaoExistirem() {
        if (horarioRepository.count() == 0) {
            List<Funcionario> funcionarios = funcionarioRepository.findAll();

            List<HorarioAtendimento> horarios = List.of(
                HorarioAtendimento.builder()
                    .funcionario(funcionarios.get(0))
                    .diaSemana(DayOfWeek.MONDAY)
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFim(LocalTime.of(18, 0))
                    .ativo(true)
                    .build(),
                HorarioAtendimento.builder()
                    .funcionario(funcionarios.get(1))
                    .diaSemana(DayOfWeek.MONDAY)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFim(LocalTime.of(17, 0))
                    .ativo(true)
                    .build(),
                HorarioAtendimento.builder()
                    .funcionario(funcionarios.get(2))
                    .diaSemana(DayOfWeek.MONDAY)
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFim(LocalTime.of(16, 0))
                    .ativo(true)
                    .build(),
                HorarioAtendimento.builder()
                    .funcionario(funcionarios.get(0))
                    .diaSemana(DayOfWeek.TUESDAY)
                    .horaInicio(LocalTime.of(8, 0))
                    .horaFim(LocalTime.of(18, 0))
                    .ativo(true)
                    .build(),
                HorarioAtendimento.builder()
                    .funcionario(funcionarios.get(1))
                    .diaSemana(DayOfWeek.TUESDAY)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFim(LocalTime.of(17, 0))
                    .ativo(true)
                    .build()
            );
            horarioRepository.saveAll(horarios);
            log.info("5 horários de atendimento criados com sucesso");
        }
    }

    private void criarAgendamentosSeNaoExistirem() {
        if (agendamentoRepository.count() == 0) {
            List<Cliente> clientes = clienteRepository.findAll();
            List<Funcionario> funcionarios = funcionarioRepository.findAll();
            List<Procedimento> procedimentos = procedimentoRepository.findAll();

            LocalDate segunda = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            LocalDate terca = LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));

            List<Agendamento> agendamentos = List.of(
                Agendamento.builder()
                    .cliente(clientes.get(0))
                    .funcionario(funcionarios.get(0))
                    .procedimento(buscarProcedimento(procedimentos, "Limpeza de Pele"))
                    .data(segunda)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFim(LocalTime.of(10, 0))
                    .status(StatusAgendamento.AGENDADO)
                    .lembreteEnviado(false)
                    .build(),
                Agendamento.builder()
                    .cliente(clientes.get(1))
                    .funcionario(funcionarios.get(1))
                    .procedimento(buscarProcedimento(procedimentos, "Maquiagem Profissional"))
                    .data(segunda)
                    .horaInicio(LocalTime.of(10, 0))
                    .horaFim(LocalTime.of(11, 0))
                    .status(StatusAgendamento.CONFIRMADO)
                    .lembreteEnviado(true)
                    .build(),
                Agendamento.builder()
                    .cliente(clientes.get(2))
                    .funcionario(funcionarios.get(2))
                    .procedimento(buscarProcedimento(procedimentos, "Penteados"))
                    .data(segunda)
                    .horaInicio(LocalTime.of(14, 0))
                    .horaFim(LocalTime.of(15, 30))
                    .status(StatusAgendamento.AGENDADO)
                    .lembreteEnviado(false)
                    .build(),
                Agendamento.builder()
                    .cliente(clientes.get(3))
                    .funcionario(funcionarios.get(0))
                    .procedimento(buscarProcedimento(procedimentos, "Unhas em Gel"))
                    .data(terca)
                    .horaInicio(LocalTime.of(9, 0))
                    .horaFim(LocalTime.of(9, 45))
                    .status(StatusAgendamento.AGENDADO)
                    .lembreteEnviado(false)
                    .build(),
                Agendamento.builder()
                    .cliente(clientes.get(4))
                    .funcionario(funcionarios.get(1))
                    .procedimento(buscarProcedimento(procedimentos, "Massagem Relaxante"))
                    .data(terca)
                    .horaInicio(LocalTime.of(14, 0))
                    .horaFim(LocalTime.of(15, 0))
                    .status(StatusAgendamento.AGENDADO)
                    .lembreteEnviado(false)
                    .build()
            );
            agendamentoRepository.saveAll(agendamentos);
            log.info("5 agendamentos criados com sucesso");
        }
    }
}
