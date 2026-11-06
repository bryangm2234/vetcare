package com.vetcare.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vetcare.historia.client.AgendaClient;
import com.vetcare.historia.client.FarmaciaClient;
import com.vetcare.historia.client.PacientesClient;
import com.vetcare.historia.config.ArranqueDatos;
import com.vetcare.historia.dto.CitaInfo;
import com.vetcare.historia.dto.MascotaInfo;
import com.vetcare.historia.dto.MedicamentoInfo;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import com.vetcare.historia.model.Hospitalizacion;
import com.vetcare.historia.security.TokensPrueba;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Escenarios de HU-041 a HU-050 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = "spring.mongodb.database=" + Sprint3HistoriaIntegracionTest.BASE_DE_PRUEBA)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint3HistoriaIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_historia_s3_test";
    private static final String CLINICO = """
            {"anamnesis":"a","examenFisico":"e","diagnostico":"d","planTratamiento":"p"}""";

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
    private FarmaciaClient farmaciaClient;

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
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "Canino", "p1", List.of(), List.of()));
        when(farmaciaClient.medicamento("med1")).thenReturn(new MedicamentoInfo("med1", "Metronidazol 250 mg"));
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

    private String id(ResultActions acciones) throws Exception {
        return JsonPath.read(acciones.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String consulta() throws Exception {
        return id(enviar(post("/api/consultas"), "VETERINARIO", """
                {"citaId":"c1"}"""));
    }

    private void cerrar(String consulta) throws Exception {
        enviar(put("/api/consultas/" + consulta + "/registro-clinico"), "VETERINARIO", CLINICO).andExpect(status().isOk());
        enviar(patch("/api/consultas/" + consulta + "/cierre"), "VETERINARIO", null).andExpect(status().isOk());
    }

    private String cuerpoFormula(String consulta, String medicamento, int cantidad, LocalDate vigencia) {
        return """
                {"consultaId":"%s","vigenteHasta":"%s","items":[{"medicamentoId":"%s","dosis":"1 tableta","frecuencia":"Cada 12 horas","duracion":"7 días","cantidad":%d}]}"""
                .formatted(consulta, vigencia, medicamento, cantidad);
    }

    private String formula(String consulta) throws Exception {
        return id(enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "med1", 14, LocalDate.now().plusDays(30))));
    }

    private ResultActions registrarDespacho(String formula, int cantidad) throws Exception {
        return enviar(patch("/api/formulas/" + formula + "/despachos"), "FARMACIA", """
                {"items":[{"medicamentoId":"med1","cantidad":%d}]}""".formatted(cantidad));
    }

    @Test
    void hu041_a_hu044_laboratorio() throws Exception {
        String consulta = consulta();
        String cuerpo = """
                {"examen":"Hemograma"}""";
        String primera = id(enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", cuerpo));
        String segunda = id(enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", """
                {"examen":"Coprológico"}"""));
        enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", """
                {"examen":""}""").andExpect(status().isBadRequest());

        enviar(get("/api/ordenes-laboratorio"), "LABORATORIO", null)
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].id").value(primera));
        enviar(get("/api/ordenes-laboratorio"), "RECEPCIONISTA", null).andExpect(status().isForbidden());

        enviar(put("/api/ordenes-laboratorio/" + primera + "/resultado"), "LABORATORIO", """
                {"resultado":""}""").andExpect(status().isBadRequest());
        enviar(put("/api/ordenes-laboratorio/" + primera + "/resultado"), "LABORATORIO", """
                {"resultado":"Leucocitos elevados"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.responsable").value("prueba@vetcare.test"))
                .andExpect(jsonPath("$.fechaResultado").isNotEmpty());
        enviar(put("/api/ordenes-laboratorio/" + primera + "/resultado"), "LABORATORIO", """
                {"resultado":"Otro"}""").andExpect(status().isConflict());

        enviar(get("/api/ordenes-laboratorio"), "LABORATORIO", null)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value(segunda));
        enviar(get("/api/ordenes-laboratorio/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("LABORATORIO"));

        enviar(get("/api/historias/m1/resultados-laboratorio"), "VETERINARIO", null)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.estado == 'COMPLETADA')].resultado").value(org.hamcrest.Matchers.contains("Leucocitos elevados")));
        enviar(get("/api/historias/m1/resultados-laboratorio"), "CAJERO", null).andExpect(status().isForbidden());

        cerrar(consulta);
        enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", cuerpo).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu045_emision() throws Exception {
        String consulta = consulta();

        enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "med1", 14, LocalDate.now().plusDays(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VIGENTE"))
                .andExpect(jsonPath("$.items[0].pendiente").value(14))
                .andExpect(jsonPath("$.items[0].medicamento").value("Metronidazol 250 mg"));

        when(farmaciaClient.medicamento("no-existe")).thenThrow(new RecursoNoEncontradoException("El recurso no existe en ms-farmacia"));
        enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "no-existe", 14, LocalDate.now().plusDays(30)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El medicamento no existe en farmacia"));
        enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "med1", 0, LocalDate.now().plusDays(30))).andExpect(status().isBadRequest());
        enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "med1", 14, LocalDate.now().minusDays(1))).andExpect(status().isBadRequest());
        enviar(post("/api/formulas"), "RECEPCIONISTA", cuerpoFormula(consulta, "med1", 14, LocalDate.now().plusDays(30))).andExpect(status().isForbidden());

        cerrar(consulta);
        enviar(post("/api/formulas"), "VETERINARIO", cuerpoFormula(consulta, "med1", 14, LocalDate.now().plusDays(30))).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu045_anulacion() throws Exception {
        String formula = formula(consulta());

        enviar(patch("/api/formulas/" + formula + "/anulacion"), "VETERINARIO", """
                {"motivo":""}""").andExpect(status().isBadRequest());
        enviar(patch("/api/formulas/" + formula + "/anulacion"), "VETERINARIO", """
                {"motivo":"Cambio de tratamiento"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        enviar(get("/api/formulas/" + formula + "/vigencia"), "FARMACIA", null)
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.despachable").value(false));
        registrarDespacho(formula, 1).andExpect(status().isUnprocessableContent());
        enviar(get("/api/auditoria?accion=ANULAR_FORMULA"), "ADMINISTRADOR", null).andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void hu046_vigenciaYDespachoDeLaFormula() throws Exception {
        String formula = formula(consulta());

        enviar(get("/api/formulas/" + formula + "/vigencia"), "FARMACIA", null)
                .andExpect(jsonPath("$.despachable").value(true))
                .andExpect(jsonPath("$.items[0].pendiente").value(14));
        enviar(get("/api/formulas/no-existe/vigencia"), "FARMACIA", null).andExpect(status().isNotFound());

        registrarDespacho(formula, 6).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PARCIAL"));
        registrarDespacho(formula, 9).andExpect(status().isUnprocessableContent());
        registrarDespacho(formula, 8).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DESPACHADA"));
        enviar(get("/api/formulas/" + formula + "/vigencia"), "FARMACIA", null)
                .andExpect(jsonPath("$.estado").value("DESPACHADA"))
                .andExpect(jsonPath("$.despachable").value(false));
        enviar(patch("/api/formulas/" + formula + "/despachos"), "VETERINARIO", """
                {"items":[{"medicamentoId":"med1","cantidad":1}]}""").andExpect(status().isForbidden());
    }

    @Test
    void hu046_formulaVencidaSeRechazaAunqueGuardeOtroEstado() throws Exception {
        String formula = formula(consulta());
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(formula)), new Update().set("vigenteHasta", LocalDate.now().minusDays(1)), com.vetcare.historia.model.Formula.class);

        enviar(get("/api/formulas/" + formula + "/vigencia"), "FARMACIA", null)
                .andExpect(jsonPath("$.estado").value("VENCIDA"))
                .andExpect(jsonPath("$.despachable").value(false));
        registrarDespacho(formula, 1).andExpect(status().isUnprocessableContent());
    }

    private String hospitalizar(String mascota, String cubiculo) throws Exception {
        return id(enviar(post("/api/hospitalizaciones"), "VETERINARIO", """
                {"mascotaId":"%s","motivo":"Deshidratación","cubiculo":"%s"}""".formatted(mascota, cubiculo)));
    }

    @Test
    void hu047_cubiculosYMascotaUnica() throws Exception {
        when(pacientesClient.mascota("m2")).thenReturn(new MascotaInfo("m2", "Mia", "Felino", "p1", List.of(), List.of()));
        String hospitalizacion = hospitalizar("m1", "C-01");

        enviar(post("/api/hospitalizaciones"), "VETERINARIO", """
                {"mascotaId":"m2","motivo":"x","cubiculo":"C-01"}""")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("El cubículo está ocupado"));
        enviar(post("/api/hospitalizaciones"), "VETERINARIO", """
                {"mascotaId":"m1","motivo":"x","cubiculo":"C-02"}""")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("La mascota ya está hospitalizada"));

        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "VETERINARIO", """
                {"indicaciones":"Control"}""").andExpect(status().isOk());
        hospitalizar("m2", "C-01");
        hospitalizar("m1", "C-02");
    }

    @Test
    void hu048_evolucionesYMedicamentosDespachados() throws Exception {
        String hospitalizacion = hospitalizar("m1", "C-01");
        String formula = formula(consulta());
        String conMedicamento = """
                {"observaciones":"Estable","temperatura":38.2,"medicamentosAdministrados":["med1"]}""";

        enviar(post("/api/hospitalizaciones/" + hospitalizacion + "/evoluciones"), "AUXILIAR_VETERINARIO", """
                {"observaciones":"Estable","temperatura":38.2}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evoluciones[0].responsable").value("prueba@vetcare.test"));
        enviar(post("/api/hospitalizaciones/" + hospitalizacion + "/evoluciones"), "AUXILIAR_VETERINARIO", conMedicamento)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("El medicamento no ha sido despachado para la mascota"));

        registrarDespacho(formula, 2).andExpect(status().isOk());
        enviar(post("/api/hospitalizaciones/" + hospitalizacion + "/evoluciones"), "AUXILIAR_VETERINARIO", conMedicamento)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evoluciones", hasSize(2)));

        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "VETERINARIO", """
                {"indicaciones":"Control"}""").andExpect(status().isOk());
        enviar(post("/api/hospitalizaciones/" + hospitalizacion + "/evoluciones"), "AUXILIAR_VETERINARIO", conMedicamento)
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu049_altaYEstancia() throws Exception {
        String hospitalizacion = hospitalizar("m1", "C-01");
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(hospitalizacion)),
                new Update().set("fechaIngreso", Instant.now().minus(Duration.ofDays(3)).minusSeconds(60)), Hospitalizacion.class);

        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "AUXILIAR_VETERINARIO", """
                {"indicaciones":"Control"}""").andExpect(status().isForbidden());
        enviar(get("/api/hospitalizaciones/items-pendientes"), "CAJERO", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "VETERINARIO", """
                {"indicaciones":"Control"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.diasEstancia").value(3));
        enviar(get("/api/hospitalizaciones/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].descripcion").value("Estancia de 3 días"));
        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "VETERINARIO", """
                {"indicaciones":"Otra"}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu050_portalDelPropietario() throws Exception {
        String consulta = consulta();
        String completada = id(enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", """
                {"examen":"Hemograma"}"""));
        id(enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", """
                {"examen":"Coprológico"}"""));
        enviar(put("/api/ordenes-laboratorio/" + completada + "/resultado"), "LABORATORIO", """
                {"resultado":"Normal"}""").andExpect(status().isOk());
        formula(consulta);

        enviar(get("/api/portal/mascotas/m1/resultados-formulas"), "PROPIETARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultados", hasSize(1)))
                .andExpect(jsonPath("$.resultados[0].examen").value("Hemograma"))
                .andExpect(jsonPath("$.formulas[0].estado").value("VIGENTE"));

        when(pacientesClient.mascota("m2")).thenThrow(new org.springframework.security.access.AccessDeniedException("ajena"));
        enviar(get("/api/portal/mascotas/m2/resultados-formulas"), "PROPIETARIO", null).andExpect(status().isForbidden());
    }

    @Test
    void hu034_historiaIncluyeLaboratorioFormulasYHospitalizacion() throws Exception {
        String consulta = consulta();
        id(enviar(post("/api/consultas/" + consulta + "/ordenes-laboratorio"), "VETERINARIO", """
                {"examen":"Hemograma"}"""));
        formula(consulta);
        hospitalizar("m1", "C-01");

        enviar(get("/api/historias/m1"), "VETERINARIO", null)
                .andExpect(jsonPath("$.laboratorio", hasSize(1)))
                .andExpect(jsonPath("$.formulas", hasSize(1)))
                .andExpect(jsonPath("$.hospitalizaciones", hasSize(1)));
    }

    @Test
    void soporteDeInterfaz_listados() throws Exception {
        String consulta = consulta();
        String formula = formula(consulta);
        String hospitalizacion = hospitalizar("m1", "C-01");

        enviar(get("/api/consultas/abiertas"), "VETERINARIO", null).andExpect(jsonPath("$[0].id").value(consulta));
        enviar(get("/api/formulas/despachables"), "FARMACIA", null).andExpect(jsonPath("$[0].id").value(formula));
        enviar(get("/api/hospitalizaciones/activas"), "AUXILIAR_VETERINARIO", null).andExpect(jsonPath("$[0].id").value(hospitalizacion));

        enviar(get("/api/hospitalizaciones/" + hospitalizacion + "/medicamentos-despachados"), "AUXILIAR_VETERINARIO", null).andExpect(jsonPath("$", hasSize(0)));
        registrarDespacho(formula, 14).andExpect(status().isOk());
        enviar(get("/api/hospitalizaciones/" + hospitalizacion + "/medicamentos-despachados"), "AUXILIAR_VETERINARIO", null)
                .andExpect(jsonPath("$[0].medicamentoId").value("med1"))
                .andExpect(jsonPath("$[0].medicamento").value("Metronidazol 250 mg"));
        enviar(get("/api/formulas/despachables"), "FARMACIA", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(patch("/api/hospitalizaciones/" + hospitalizacion + "/alta"), "VETERINARIO", """
                {"indicaciones":"Control"}""").andExpect(status().isOk());
        enviar(get("/api/hospitalizaciones/activas"), "VETERINARIO", null).andExpect(jsonPath("$", hasSize(0)));
        cerrar(consulta);
        enviar(get("/api/consultas/abiertas"), "VETERINARIO", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/consultas/abiertas"), "CAJERO", null).andExpect(status().isForbidden());
    }
}
