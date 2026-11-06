package com.vetcare.agenda;

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
import com.vetcare.agenda.client.PacientesClient;
import com.vetcare.agenda.config.ArranqueDatos;
import com.vetcare.agenda.dto.MascotaInfo;
import com.vetcare.agenda.dto.PropietarioInfo;
import com.vetcare.agenda.security.TokensPrueba;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Escenarios de HU-071 a HU-073 y la marca de ítems facturados de peluquería.
 */
@SpringBootTest(properties = "spring.mongodb.database=" + Sprint4AgendaIntegracionTest.BASE_DE_PRUEBA)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint4AgendaIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_agenda_s4_test";
    private static final LocalDate LUNES = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @MockitoBean
    private PacientesClient pacientesClient;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
    }

    @BeforeEach
    void preparar() {
        limpiar();
        arranqueDatos.preparar();
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "p1", "ACTIVA"));
        when(pacientesClient.propietario("p1")).thenReturn(new PropietarioInfo("p1", "p@correo.test"));
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

    private String cita(String hora) throws Exception {
        String respuesta = enviar(post("/api/citas"), "RECEPCIONISTA", """
                {"mascotaId":"m1","veterinarioId":"v1","fecha":"%s","hora":"%s","motivo":"Control"}""".formatted(LUNES, hora))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private void horario() throws Exception {
        enviar(put("/api/horarios/v1"), "ADMINISTRADOR", """
                {"nombre":"Dra. Pérez","dias":["MONDAY"],"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":8}""").andExpect(status().isOk());
    }

    @Test
    void hu073_ocupacion() throws Exception {
        horario();
        String[] citas = {cita("08:00"), cita("08:30"), cita("09:00"), cita("09:30"), cita("10:00"), cita("10:30")};
        String periodo = "&desde=" + LUNES + "&hasta=" + LUNES;

        enviar(get("/api/reportes/ocupacion?veterinarioId=v1" + periodo), "ADMINISTRADOR", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cupos").value(8))
                .andExpect(jsonPath("$[0].citas").value(6))
                .andExpect(jsonPath("$[0].porcentaje").value(75));

        for (int i = 0; i < 2; i++) {
            enviar(patch("/api/citas/" + citas[i] + "/cancelacion"), "RECEPCIONISTA", """
                    {"motivo":"No asiste"}""").andExpect(status().isOk());
        }
        enviar(get("/api/reportes/ocupacion?veterinarioId=v1" + periodo), "ADMINISTRADOR", null)
                .andExpect(jsonPath("$[0].porcentaje").value(50));
        enviar(get("/api/reportes/ocupacion?veterinarioId=sin-horario" + periodo), "ADMINISTRADOR", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].porcentaje").value(0));
        enviar(get("/api/reportes/ocupacion?desde=" + LUNES.plusDays(1) + "&hasta=" + LUNES), "ADMINISTRADOR", null).andExpect(status().isBadRequest());
        enviar(get("/api/reportes/ocupacion?desde=" + LUNES + "&hasta=" + LUNES), "RECEPCIONISTA", null).andExpect(status().isForbidden());
    }

    @Test
    void hu072_citasDelPeriodo() throws Exception {
        horario();
        String cancelada = cita("08:00");
        enviar(patch("/api/citas/" + cancelada + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"No asiste"}""").andExpect(status().isOk());

        enviar(get("/api/reportes/citas?desde=" + LUNES + "&hasta=" + LUNES), "ADMINISTRADOR", null)
                .andExpect(jsonPath("$.canceladas").value(1))
                .andExpect(jsonPath("$.atendidas").value(0));
    }

    @Test
    void hu071_proximasCitasDelPropietario() throws Exception {
        horario();
        cita("08:00");
        String cancelada = cita("09:00");
        enviar(patch("/api/citas/" + cancelada + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"No asiste"}""").andExpect(status().isOk());
        when(pacientesClient.mascotasPropias()).thenReturn(new MascotaInfo[] {new MascotaInfo("m1", "Max", "p1", "ACTIVA")});

        enviar(get("/api/portal/citas"), "PROPIETARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].mascotaNombre").value("Max"));

        when(pacientesClient.mascotasPropias()).thenReturn(new MascotaInfo[0]);
        enviar(get("/api/portal/citas"), "PROPIETARIO", null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void hu061_marcaDeItemsDePeluqueria() throws Exception {
        enviar(put("/api/peluqueria/horario"), "ADMINISTRADOR", """
                {"nombre":"Peluquería","dias":["MONDAY"],"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":8}""").andExpect(status().isOk());
        String respuesta = enviar(post("/api/peluqueria/citas"), "PELUQUERIA", """
                {"mascotaId":"m1","fecha":"%s","hora":"08:00","servicio":"Baño"}""".formatted(LUNES)).andReturn().getResponse().getContentAsString();
        String cita = JsonPath.read(respuesta, "$.id");
        enviar(patch("/api/peluqueria/citas/" + cita + "/servicio"), "PELUQUERIA", """
                {"observaciones":"Listo"}""").andExpect(status().isOk());
        String marca = """
                {"ids":["%s"],"facturado":%s}""";

        enviar(patch("/api/peluqueria/items-facturados"), "PELUQUERIA", marca.formatted(cita, true)).andExpect(status().isForbidden());
        enviar(patch("/api/peluqueria/items-facturados"), "CAJERO", marca.formatted(cita, true)).andExpect(status().isOk());
        enviar(get("/api/peluqueria/items-pendientes"), "CAJERO", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(patch("/api/peluqueria/items-facturados"), "CAJERO", marca.formatted(cita, true)).andExpect(status().isConflict());
        enviar(patch("/api/peluqueria/items-facturados"), "CAJERO", marca.formatted(cita, false)).andExpect(status().isOk());
        enviar(get("/api/peluqueria/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cantidad").value(1));
    }

    @Test
    void soporteDeInterfaz_listados() throws Exception {
        horario();
        cita("09:00");
        cita("08:00");

        enviar(get("/api/citas?fecha=" + LUNES), "RECEPCIONISTA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].hora").value(org.hamcrest.Matchers.startsWith("08:00")));
        enviar(get("/api/citas?fecha=" + LUNES), "AUXILIAR_VETERINARIO", null).andExpect(status().isOk());
        enviar(get("/api/citas?fecha=" + LUNES), "CAJERO", null).andExpect(status().isForbidden());
        enviar(get("/api/horarios"), "RECEPCIONISTA", null).andExpect(jsonPath("$[0].nombre").value("Dra. Pérez"));
        enviar(get("/api/peluqueria/citas?fecha=" + LUNES), "PELUQUERIA", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(get("/api/peluqueria/citas?fecha=" + LUNES), "RECEPCIONISTA", null).andExpect(status().isForbidden());
    }
}
