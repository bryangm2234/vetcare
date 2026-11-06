package com.vetcare.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetcare.pacientes.dto.PaginaResponse;
import com.vetcare.pacientes.security.TokensPrueba;
import com.vetcare.pacientes.service.AuditoriaService;
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

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://127.0.0.1:1/?serverSelectionTimeoutMS=200")
@AutoConfigureMockMvc
class PacientesApplicationTests {

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
    void hu008_usaSuPropiaBaseDeDatos() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo("vetcare_pacientes");
    }

    @Test
    void hu008_sinMongoDbLaSaludEsPublicaYRespondeDown() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void hu010_laDocumentacionEsPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VetCare - ms-pacientes"));
    }

    @Test
    void hu003_sinTokenResponde401EnFormatoEstandar() throws Exception {
        mockMvc.perform(get(PROTEGIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.ruta").value(PROTEGIDO));
    }

    @Test
    void hu003_tokenConFirmaInvalidaResponde401() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.tokenConOtraClave("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hu002_tokenExpiradoResponde401() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.tokenExpirado("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hu003_rolNoAutorizadoResponde403EnFormatoEstandar() throws Exception {
        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("CAJERO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void hu003_rolAutorizadoConsultaLaAuditoria() throws Exception {
        when(auditoriaService.consultar(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void hu079_rutaInexistenteResponde404EnFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/no-existe").header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void corsPermiteLaInterfazYRechazaOtrosOrigenes() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(PROTEGIDO)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(PROTEGIDO)
                        .header("Origin", "https://otro-sitio.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu079_errorInesperadoNoExponeDetallesInternos() throws Exception {
        when(auditoriaService.consultar(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new IllegalStateException("detalle-interno-secreto"));

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error interno"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("detalle-interno-secreto"))));
    }

    @Test
    void hu079_servicioDependienteCaidoResponde503() throws Exception {
        when(auditoriaService.consultar(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new com.vetcare.pacientes.exception.ServicioNoDisponibleException("El servicio ms-prueba no está disponible"));

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.mensaje").value("El servicio ms-prueba no está disponible"));
    }

    @Test
    void hu079_reglaDeNegocioResponde422() throws Exception {
        when(auditoriaService.consultar(any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new com.vetcare.pacientes.exception.ReglaNegocioException("Regla incumplida"));

        mockMvc.perform(get(PROTEGIDO).header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Regla incumplida"));
    }

    @Test
    void hu079_parametroInvalidoResponde400() throws Exception {
        mockMvc.perform(get(PROTEGIDO + "?desde=no-es-fecha").header("Authorization", "Bearer " + TokensPrueba.token("ADMINISTRADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
