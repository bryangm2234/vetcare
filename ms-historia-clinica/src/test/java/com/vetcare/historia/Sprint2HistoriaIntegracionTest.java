package com.vetcare.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vetcare.historia.client.AgendaClient;
import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.config.ArranqueDatos;
import com.vetcare.historia.dto.CitaInfo;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.exception.ServicioNoDisponibleException;
import com.vetcare.historia.security.TokensPrueba;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Escenarios de HU-031 a HU-040 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = "spring.mongodb.database=" + Sprint2HistoriaIntegracionTest.BASE_DE_PRUEBA)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint2HistoriaIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_historia_test";
    private static final String CLINICO = """
            {"anamnesis":"Vómito","examenFisico":"Abdomen sensible","diagnostico":"Gastroenteritis","planTratamiento":"Dieta blanda"}""";
    private static final String SIGNOS = """
            {"pesoKg":12.5,"temperatura":38.6,"frecuenciaCardiaca":90,"frecuenciaRespiratoria":24}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @MockitoBean
    private AgendaClient agendaClient;
    @MockitoBean
    private PacientesClient pacientesClient;
    @MockitoBean
    private com.vetcare.historia.client.FarmaciaClient farmaciaClient;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
    }

    @BeforeEach
    void preparar() {
        limpiar();
        arranqueDatos.preparar();
        when(agendaClient.cita("c1")).thenReturn(new CitaInfo("c1", "m1", "EN_ESPERA"));
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "Canino", "p1", List.of("Penicilina"), List.of("Diabetes")));
    }

    @AfterEach
    void limpiar() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(BASE_DE_PRUEBA);
        mongoTemplate.getDb().drop();
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String rol, String cuerpo) throws Exception {
        peticion.header("Authorization", "Bearer " + TokensPrueba.token(rol));
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(peticion);
    }

    private String abrirConsulta() throws Exception {
        String respuesta = enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c1"}""").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private void esquemaRabia(int dosis) throws Exception {
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Canino","vacuna":"Rabia","dosis":%d,"intervaloDias":365}""".formatted(dosis))
                .andExpect(status().isCreated());
    }

    private ResultActions vacunar(LocalDate fecha) throws Exception {
        return enviar(post("/api/vacunas"), "VETERINARIO", """
                {"mascotaId":"m1","vacuna":"Rabia","lote":"L-100","fecha":"%s"}""".formatted(fecha));
    }

    @Test
    void hu031_aperturaDesdeCitaEnEspera() throws Exception {
        enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c1"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.citaId").value("c1"))
                .andExpect(jsonPath("$.mascotaId").value("m1"));
        enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c1"}""").andExpect(status().isConflict());

        when(agendaClient.cita("c2")).thenReturn(new CitaInfo("c2", "m1", "PROGRAMADA"));
        enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c2"}""").andExpect(status().isUnprocessableContent());

        when(agendaClient.cita("c3")).thenThrow(new ServicioNoDisponibleException("El servicio ms-agenda no está disponible"));
        enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c3"}""").andExpect(status().isServiceUnavailable());
    }

    @Test
    void hu032_signosVisiblesEnLaConsulta() throws Exception {
        enviar(put("/api/citas/c1/signos-vitales"), "AUXILIAR_VETERINARIO", SIGNOS).andExpect(status().isOk());
        enviar(put("/api/citas/c1/signos-vitales"), "AUXILIAR_VETERINARIO", """
                {"pesoKg":0,"temperatura":38.6,"frecuenciaCardiaca":90,"frecuenciaRespiratoria":24}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.pesoKg").isNotEmpty());

        String consulta = abrirConsulta();
        enviar(get("/api/consultas/" + consulta), "VETERINARIO", null)
                .andExpect(jsonPath("$.signosVitales.temperatura").value(38.6));

        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "VETERINARIO", CLINICO).andExpect(status().isOk());
        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null).andExpect(status().isOk());
        enviar(put("/api/citas/c1/signos-vitales"), "AUXILIAR_VETERINARIO", SIGNOS).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu033_hu035_registroYCierre() throws Exception {
        String consulta = abrirConsulta();

        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "AUXILIAR_VETERINARIO", CLINICO).andExpect(status().isForbidden());
        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La consulta no tiene diagnóstico"));
        enviar(get("/api/consultas/items-pendientes"), "CAJERO", null).andExpect(jsonPath("$", hasSize(0)));

        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "VETERINARIO", CLINICO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostico").value("Gastroenteritis"));
        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));
        verify(agendaClient).marcarAtendida("c1");

        enviar(get("/api/consultas/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(consulta))
                .andExpect(jsonPath("$[0].tipo").value("CONSULTA"));
        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "VETERINARIO", CLINICO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La consulta está cerrada"));
        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu035_siAgendaFallaLaConsultaSigueAbierta() throws Exception {
        String consulta = abrirConsulta();
        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "VETERINARIO", CLINICO).andExpect(status().isOk());
        doThrow(new ServicioNoDisponibleException("El servicio ms-agenda no está disponible")).when(agendaClient).marcarAtendida("c1");

        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null).andExpect(status().isServiceUnavailable());

        enviar(get("/api/consultas/" + consulta), "VETERINARIO", null).andExpect(jsonPath("$.estado").value("ABIERTA"));
    }

    @Test
    void hu034_historiaCompleta() throws Exception {
        enviar(get("/api/historias/m1"), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultas", hasSize(0)))
                .andExpect(jsonPath("$.alergias[0]").value("Penicilina"));

        abrirConsulta();
        esquemaRabia(2);
        vacunar(LocalDate.now()).andExpect(status().isCreated());
        enviar(get("/api/historias/m1"), "VETERINARIO", null)
                .andExpect(jsonPath("$.consultas", hasSize(1)))
                .andExpect(jsonPath("$.vacunas", hasSize(1)))
                .andExpect(jsonPath("$.avisos", hasSize(0)));

        enviar(get("/api/historias/m1"), "CAJERO", null).andExpect(status().isForbidden());

        when(pacientesClient.mascota("m1")).thenThrow(new ServicioNoDisponibleException("El servicio ms-pacientes no está disponible"));
        enviar(get("/api/historias/m1"), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisos[0]").value("Alergias no disponibles"))
                .andExpect(jsonPath("$.consultas", hasSize(1)));
    }

    @Test
    void hu036_esquemas() throws Exception {
        esquemaRabia(2);
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Canino","vacuna":"Rabia","dosis":1,"intervaloDias":30}""").andExpect(status().isConflict());
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Felino","vacuna":"Rabia","dosis":0,"intervaloDias":0}""")
                .andExpect(status().isBadRequest());
        enviar(get("/api/esquemas-vacunacion?especie=Canino"), "VETERINARIO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].intervaloDias").value(365));
    }

    @Test
    void hu037_rn01_dosisYProximaFecha() throws Exception {
        esquemaRabia(2);
        LocalDate fecha = LocalDate.now().minusDays(10);

        vacunar(fecha)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dosis").value(1))
                .andExpect(jsonPath("$.proximaDosis").value(fecha.plusDays(365).toString()));
        verify(farmaciaClient).descontarVacuna("L-100");
        vacunar(LocalDate.now())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dosis").value(2))
                .andExpect(jsonPath("$.proximaDosis").doesNotExist());
        vacunar(LocalDate.now()).andExpect(status().isUnprocessableContent());
        vacunar(LocalDate.now().plusDays(1)).andExpect(status().isBadRequest());

        when(pacientesClient.mascota("m2")).thenReturn(new MascotaInfo("m2", "Mia", "Felino", "p1", List.of(), List.of()));
        enviar(post("/api/vacunas"), "VETERINARIO", """
                {"mascotaId":"m2","vacuna":"Rabia","lote":"L-100","fecha":"%s"}""".formatted(LocalDate.now()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu038_rn01_alertas() throws Exception {
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Canino","vacuna":"Moquillo","dosis":3,"intervaloDias":15}""").andExpect(status().isCreated());
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Canino","vacuna":"Parvovirus","dosis":2,"intervaloDias":20}""").andExpect(status().isCreated());
        enviar(post("/api/esquemas-vacunacion"), "VETERINARIO", """
                {"especie":"Canino","vacuna":"Rabia","dosis":2,"intervaloDias":365}""").andExpect(status().isCreated());
        String cuerpo = """
                {"mascotaId":"m1","vacuna":"%s","lote":"L-1","fecha":"%s"}""";

        // Próxima dentro de 10 días
        enviar(post("/api/vacunas"), "VETERINARIO", cuerpo.formatted("Moquillo", LocalDate.now().minusDays(5))).andExpect(status().isCreated());
        // Vencida hace 5 días
        enviar(post("/api/vacunas"), "VETERINARIO", cuerpo.formatted("Parvovirus", LocalDate.now().minusDays(25))).andExpect(status().isCreated());
        // Fuera de la ventana
        enviar(post("/api/vacunas"), "VETERINARIO", cuerpo.formatted("Rabia", LocalDate.now())).andExpect(status().isCreated());

        enviar(get("/api/vacunas/alertas"), "RECEPCIONISTA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vacuna").value("Parvovirus"))
                .andExpect(jsonPath("$[0].estado").value("VENCIDA"))
                .andExpect(jsonPath("$[0].mascotaNombre").value("Max"))
                .andExpect(jsonPath("$[1].vacuna").value("Moquillo"))
                .andExpect(jsonPath("$[1].estado").value("PROXIMA"));

        // Aplicar la dosis siguiente atiende la alerta.
        enviar(post("/api/vacunas"), "VETERINARIO", cuerpo.formatted("Parvovirus", LocalDate.now())).andExpect(status().isCreated());
        enviar(get("/api/vacunas/alertas"), "RECEPCIONISTA", null).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void hu039_carneDelPropietario() throws Exception {
        enviar(get("/api/portal/mascotas/m1/carne"), "PROPIETARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        esquemaRabia(2);
        vacunar(LocalDate.now()).andExpect(status().isCreated());
        enviar(get("/api/portal/mascotas/m1/carne"), "PROPIETARIO", null)
                .andExpect(jsonPath("$[0].vacuna").value("Rabia"))
                .andExpect(jsonPath("$[0].proximaDosis").isNotEmpty());

        when(pacientesClient.mascota("m2")).thenThrow(new org.springframework.security.access.AccessDeniedException("ajena"));
        enviar(get("/api/portal/mascotas/m2/carne"), "PROPIETARIO", null).andExpect(status().isForbidden());
        enviar(get("/api/portal/mascotas/m1/carne"), "VETERINARIO", null).andExpect(status().isForbidden());
    }

    private ResultActions adjuntar(MockMultipartFile archivo) throws Exception {
        return mockMvc.perform(multipart("/api/historias/m1/adjuntos").file(archivo)
                .header("Authorization", "Bearer " + TokensPrueba.token("VETERINARIO")));
    }

    @Test
    void hu040_adjuntos() throws Exception {
        byte[] contenido = "contenido del pdf".getBytes();
        String respuesta = adjuntar(new MockMultipartFile("archivo", "radiografia.pdf", "application/pdf", contenido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("radiografia.pdf"))
                .andExpect(jsonPath("$.tipo").value("application/pdf"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(respuesta, "$.id");

        enviar(get("/api/adjuntos/" + id), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(content().bytes(contenido));
        enviar(get("/api/historias/m1"), "VETERINARIO", null).andExpect(jsonPath("$.adjuntos", hasSize(1)));
        enviar(get("/api/adjuntos/no-existe"), "VETERINARIO", null).andExpect(status().isNotFound());

        adjuntar(new MockMultipartFile("archivo", "grande.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]))
                .andExpect(status().isPayloadTooLarge());
        adjuntar(new MockMultipartFile("archivo", "virus.exe", "application/octet-stream", contenido))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void hu037_loteInexistenteOSinStockNoRegistraLaDosis() throws Exception {
        esquemaRabia(2);
        doThrow(new com.vetcare.historia.exception.RecursoNoEncontradoException("El lote no existe")).when(farmaciaClient).descontarVacuna("L-100");
        vacunar(LocalDate.now()).andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").value("El lote no existe"));

        org.mockito.Mockito.reset(farmaciaClient);
        doThrow(new com.vetcare.historia.exception.ConflictoException("Stock insuficiente")).when(farmaciaClient).descontarVacuna("L-100");
        vacunar(LocalDate.now()).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("Stock insuficiente"));

        enviar(get("/api/historias/m1"), "VETERINARIO", null).andExpect(jsonPath("$.vacunas", hasSize(0)));
    }

    @Test
    void hu071_vacunasPendientesDelPropietario() throws Exception {
        esquemaRabia(2);
        vacunar(LocalDate.now()).andExpect(status().isCreated());
        when(pacientesClient.mascotasPropias()).thenReturn(new MascotaInfo[] {new MascotaInfo("m1", "Max", "Canino", "p1", List.of(), List.of())});

        enviar(get("/api/portal/vacunas/pendientes"), "PROPIETARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vacuna").value("Rabia"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));

        when(pacientesClient.mascotasPropias()).thenReturn(new MascotaInfo[0]);
        enviar(get("/api/portal/vacunas/pendientes"), "PROPIETARIO", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/portal/vacunas/pendientes"), "RECEPCIONISTA", null).andExpect(status().isForbidden());
    }
}
