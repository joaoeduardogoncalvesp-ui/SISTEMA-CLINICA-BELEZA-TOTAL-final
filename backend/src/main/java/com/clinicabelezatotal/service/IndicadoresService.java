package com.clinicabelezatotal.service;

import com.clinicabelezatotal.dto.IndicadorPorProfissionalDTO;
import com.clinicabelezatotal.dto.IndicadorPorProcedimentoDTO;
import com.clinicabelezatotal.dto.IndicadoresDTO;
import com.clinicabelezatotal.model.Agendamento.StatusAgendamento;
import com.clinicabelezatotal.repository.AgendamentoRepository;
import com.clinicabelezatotal.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndicadoresService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;

    public IndicadoresDTO buscarIndicadores(LocalDate inicio, LocalDate fim) {
        IndicadoresDTO dto = new IndicadoresDTO();

        dto.setTotalAgendamentosHoje(agendamentoRepository.countByData(LocalDate.now()));
        dto.setTotalAgendamentosPeriodo(agendamentoRepository.countByPeriodo(inicio, fim));
        dto.setRealizados(agendamentoRepository.countRealizadosByPeriodo(inicio, fim));
        dto.setCancelados(agendamentoRepository.countCanceladosByPeriodo(inicio, fim));
        dto.setNaoCompareceram(agendamentoRepository.countNaoCompareceramByPeriodo(inicio, fim));
        dto.setPendentes(agendamentoRepository.countPendentesByPeriodo(inicio, fim));
        dto.setTotalClientes(clienteRepository.countByAtivoTrue());

        dto.setPorProfissional(montarIndicadoresPorProfissional(inicio, fim));
        dto.setPorProcedimento(montarIndicadoresPorProcedimento(inicio, fim));

        return dto;
    }

    private List<IndicadorPorProfissionalDTO> montarIndicadoresPorProfissional(LocalDate inicio, LocalDate fim) {
        List<Object[]> resultados = agendamentoRepository.countByProfissionalAndStatusByPeriodo(inicio, fim);

        Map<Long, IndicadorPorProfissionalDTO> map = new HashMap<>();

        for (Object[] row : resultados) {
            Long funcionarioId = (Long) row[0];
            String nome = (String) row[1];
            StatusAgendamento status = (StatusAgendamento) row[2];
            Long count = (Long) row[3];

            IndicadorPorProfissionalDTO ind = map.computeIfAbsent(funcionarioId, id -> {
                IndicadorPorProfissionalDTO dto = new IndicadorPorProfissionalDTO();
                dto.setFuncionarioId(id);
                dto.setNomeProfissional(nome);
                return dto;
            });

            ind.setTotal(ind.getTotal() + count);
            switch (status) {
                case REALIZADO -> ind.setRealizados(ind.getRealizados() + count);
                case CANCELADO -> ind.setCancelados(ind.getCancelados() + count);
                case NAO_COMPARECEU -> ind.setNaoCompareceram(ind.getNaoCompareceram() + count);
                default -> {}
            }
        }

        return new ArrayList<>(map.values());
    }

    private List<IndicadorPorProcedimentoDTO> montarIndicadoresPorProcedimento(LocalDate inicio, LocalDate fim) {
        List<Object[]> resultados = agendamentoRepository.countByProcedimentoByPeriodo(inicio, fim);
        List<IndicadorPorProcedimentoDTO> lista = new ArrayList<>();

        for (Object[] row : resultados) {
            IndicadorPorProcedimentoDTO dto = new IndicadorPorProcedimentoDTO();
            dto.setProcedimentoId((Long) row[0]);
            dto.setNomeProcedimento((String) row[1]);
            dto.setCategoria(row[2].toString());
            dto.setTotal((Long) row[3]);
            lista.add(dto);
        }

        return lista;
    }
}
