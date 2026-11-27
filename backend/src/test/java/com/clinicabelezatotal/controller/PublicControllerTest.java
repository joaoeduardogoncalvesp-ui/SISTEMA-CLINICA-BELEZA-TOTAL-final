package com.clinicabelezatotal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homepage_DeveRetornarOk() throws Exception {
        mockMvc.perform(get("/api/public/homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Clínica Beleza Total"));
    }

    @Test
    void portfolio_DeveRetornarOk() throws Exception {
        mockMvc.perform(get("/api/public/portfolio"))
                .andExpect(status().isOk());
    }

    @Test
    void portfolio_Categoria_DeveRetornarOk() throws Exception {
        mockMvc.perform(get("/api/public/portfolio/categoria/FACIAL"))
                .andExpect(status().isOk());
    }

    @Test
    void funcionarios_DeveRetornarOk() throws Exception {
        mockMvc.perform(get("/api/public/funcionarios"))
                .andExpect(status().isOk());
    }

    @Test
    void paginaNaoExistente_DeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }
}
