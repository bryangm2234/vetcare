package com.vetcare.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetcare.agenda.dto.PaginaResponse;
import com.vetcare.agenda.security.TokensPrueba;
import com.vetcare.agenda.service.AuditoriaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/?serverSelectionTimeoutMS=200")
@AutoConfigureMockMvc
class AgendaApplicationTests {

    private static final String PROTEGIDO = "/api/auditoria";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @DynamicPropertySource
    static void claves(DynamicPropertyRegistry registro) {
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
        registro.add("vetcare.jwt.private-key", TokensPrueba::clavePrivada);
        registro.add("vetcare.admin.correo", () -> "");
    }

    @Test
    void usaSuPropiaBaseDeDatos() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo("vetcare_agenda");
    }

    @Test
    void laSaludEsPublica() throws Exception {
        int estado = mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();

        // Responde 503 cuando MongoDB no está disponible, como ocurre en esta prueba.
        assertThat(estado).isIn(200, 503);
    }

    @Test
    void laDocumentacionEsPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VetCare - ms-agenda"));
    }

    @Test
    void sinTokenResponde401EnFormatoEstandar() throws Exception {
        mockMvc.perform(get(PROTEGIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.ruta").value(PROTEGIDO));
    }

    @Test
    void tokenConFirmaInvalidaResponde401() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.tokenConOtraClave("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoResponde401() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.tokenExpirado("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolNoAutorizadoResponde403EnFormatoEstandar() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("CAJERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void administradorConsultaLaAuditoria() throws Exception {
        when(auditoriaService.consultar(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void rutaInexistenteResponde404EnFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/no-existe").header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
