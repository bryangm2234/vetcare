package com.vetcare.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.vetcare.agenda.exception.RecursoNoEncontradoException;
import com.vetcare.agenda.security.TokensPrueba;
import com.vetcare.agenda.service.CorreoService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * Escenarios de HU-021 a HU-030 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = {"spring.mongodb.database=" + Sprint2AgendaIntegracionTest.BASE_DE_PRUEBA,
        "vetcare.clave-proceso=clave-de-prueba"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint2AgendaIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_agenda_test";
    private static final LocalDate LUNES = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final String TODOS_LOS_DIAS = "[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @MockitoBean
    private PacientesClient pacientesClient;
    @MockitoBean
    private CorreoService correoService;

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

    private void horario(String recurso, int cupos) throws Exception {
        enviar(put("/api/horarios/" + recurso), "ADMINISTRADOR", """
                {"nombre":"Dra. Pérez","dias":%s,"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":%d}""".formatted(TODOS_LOS_DIAS, cupos))
                .andExpect(status().isOk());
    }

    private String cuerpoCita(String recurso, LocalDate fecha, String hora) {
        return """
                {"mascotaId":"m1","veterinarioId":"%s","fecha":"%s","hora":"%s","motivo":"Control"}""".formatted(recurso, fecha, hora);
    }

    private ResultActions agendar(LocalDate fecha, String hora) throws Exception {
        return enviar(post("/api/citas"), "RECEPCIONISTA", cuerpoCita("v1", fecha, hora));
    }

    private String idDe(ResultActions acciones) throws Exception {
        return JsonPath.read(acciones.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void hu021_configuracionYValidaciones() throws Exception {
        horario("v1", 8);

        enviar(put("/api/horarios/v1"), "ADMINISTRADOR", """
                {"nombre":"X","dias":["MONDAY"],"horaInicio":"12:00","horaFin":"08:00","cuposDiarios":8}""")
                .andExpect(status().isBadRequest());
        enviar(put("/api/horarios/v1"), "ADMINISTRADOR", """
                {"nombre":"X","dias":["MONDAY"],"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":0}""")
                .andExpect(status().isBadRequest());
        enviar(put("/api/horarios/v1"), "RECEPCIONISTA", """
                {"nombre":"X","dias":["MONDAY"],"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":8}""")
                .andExpect(status().isForbidden());
    }

    @Test
    void hu021_noReduceCuposPorDebajoDeLasCitasAgendadas() throws Exception {
        horario("v1", 8);
        agendar(LUNES, "08:00").andExpect(status().isCreated());
        agendar(LUNES, "08:30").andExpect(status().isCreated());

        enviar(put("/api/horarios/v1"), "ADMINISTRADOR", """
                {"nombre":"X","dias":%s,"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":1}""".formatted(TODOS_LOS_DIAS))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(LUNES.toString())));
    }

    @Test
    void hu022_rn03_cuposYFranjas() throws Exception {
        horario("v1", 2);

        agendar(LUNES, "09:00").andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        agendar(LUNES, "09:00").andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("La franja ya está ocupada"));
        agendar(LUNES, "09:30").andExpect(status().isCreated());
        agendar(LUNES, "10:00").andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("No hay cupos disponibles"));
        agendar(LUNES.plusDays(1), "10:00").andExpect(status().isCreated());
    }

    @Test
    void hu022_fueraDeHorarioFechaPasadaYMascotaNoActiva() throws Exception {
        horario("v1", 8);

        agendar(LUNES, "07:00").andExpect(status().isUnprocessableContent());
        agendar(LUNES, "09:15").andExpect(status().isUnprocessableContent());
        agendar(LocalDate.now().minusDays(1), "09:00").andExpect(status().isUnprocessableContent());
        enviar(post("/api/citas"), "RECEPCIONISTA", cuerpoCita("sin-horario", LUNES, "09:00")).andExpect(status().isNotFound());

        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "p1", "INACTIVA"));
        agendar(LUNES, "09:00").andExpect(status().isUnprocessableContent());

        when(pacientesClient.mascota("m1")).thenThrow(new RecursoNoEncontradoException("El recurso no existe en ms-pacientes"));
        agendar(LUNES, "09:00").andExpect(status().isNotFound());
    }

    @Test
    void hu022_rn03_reservasSimultaneasRespetanCuposYFranjas() throws Exception {
        horario("v1", 3);
        String[] horas = {"08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "08:00", "08:30"};
        ExecutorService hilos = Executors.newFixedThreadPool(horas.length);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (String hora : horas) {
            Callable<Integer> reserva = () -> agendar(LUNES, hora).andReturn().getResponse().getStatus();
            resultados.add(hilos.submit(reserva));
        }
        int creadas = 0;
        for (Future<Integer> resultado : resultados) {
            int estado = resultado.get();
            assertThat(estado).isIn(201, 409);
            creadas += estado == 201 ? 1 : 0;
        }
        hilos.shutdown();

        assertThat(creadas).isEqualTo(3);
        enviar(get("/api/disponibilidad?fecha=" + LUNES + "&veterinarioId=v1"), "RECEPCIONISTA", null)
                .andExpect(jsonPath("$[0].cuposRestantes").value(0));
    }

    @Test
    void hu023_disponibilidad() throws Exception {
        horario("v1", 8);
        agendar(LUNES, "08:00").andExpect(status().isCreated());
        agendar(LUNES, "08:30").andExpect(status().isCreated());
        agendar(LUNES, "09:00").andExpect(status().isCreated());

        enviar(get("/api/disponibilidad?fecha=" + LUNES), "RECEPCIONISTA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuposRestantes").value(5))
                .andExpect(jsonPath("$[0].franjasLibres", hasSize(5)));
        enviar(get("/api/disponibilidad?fecha=" + LocalDate.now().minusDays(1)), "RECEPCIONISTA", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void hu023_sinCuposNoMuestraFranjas() throws Exception {
        horario("v1", 1);
        agendar(LUNES, "08:00").andExpect(status().isCreated());

        enviar(get("/api/disponibilidad?fecha=" + LUNES), "RECEPCIONISTA", null)
                .andExpect(jsonPath("$[0].cuposRestantes").value(0))
                .andExpect(jsonPath("$[0].franjasLibres", hasSize(0)));
    }

    @Test
    void hu024_portalDelPropietario() throws Exception {
        horario("v1", 1);
        String cuerpo = cuerpoCita("v1", LUNES, "08:00");

        enviar(post("/api/portal/citas"), "PROPIETARIO", cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        enviar(post("/api/portal/citas"), "PROPIETARIO", cuerpoCita("v1", LUNES, "08:30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay cupos disponibles"));
        when(pacientesClient.mascota("m1")).thenThrow(new org.springframework.security.access.AccessDeniedException("ajena"));
        enviar(post("/api/portal/citas"), "PROPIETARIO", cuerpoCita("v1", LUNES.plusDays(1), "08:30"))
                .andExpect(status().isForbidden());
        enviar(post("/api/portal/citas"), "RECEPCIONISTA", cuerpo).andExpect(status().isForbidden());
    }

    @Test
    void hu025_cancelarYReprogramar() throws Exception {
        horario("v1", 1);
        String id = idDe(agendar(LUNES, "08:00"));

        enviar(put("/api/citas/" + id + "/reprogramacion"), "RECEPCIONISTA", """
                {"fecha":"%s","hora":"09:00"}""".formatted(LUNES.plusDays(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(LUNES.plusDays(1).toString()));
        // El cupo y la franja de origen quedaron libres.
        agendar(LUNES, "08:00").andExpect(status().isCreated());

        // El destino está lleno: la cita conserva su fecha.
        enviar(put("/api/citas/" + id + "/reprogramacion"), "RECEPCIONISTA", """
                {"fecha":"%s","hora":"10:00"}""".formatted(LUNES))
                .andExpect(status().isConflict());
        enviar(get("/api/citas/" + id), "RECEPCIONISTA", null)
                .andExpect(jsonPath("$.fecha").value(LUNES.plusDays(1).toString()));

        enviar(patch("/api/citas/" + id + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"No puede asistir"}""").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        agendar(LUNES.plusDays(1), "09:00").andExpect(status().isCreated());
        enviar(patch("/api/citas/" + id + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"Otra vez"}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu026_hu035_llegadaYAtencion() throws Exception {
        horario("v1", 8);
        String hoy = idDe(agendar(LocalDate.now(), "08:00"));
        String otroDia = idDe(agendar(LUNES.plusDays(7), "08:00"));

        enviar(patch("/api/citas/" + otroDia + "/llegada"), "RECEPCIONISTA", null).andExpect(status().isUnprocessableContent());
        enviar(patch("/api/citas/" + hoy + "/atencion"), "VETERINARIO", null).andExpect(status().isUnprocessableContent());
        enviar(patch("/api/citas/" + hoy + "/llegada"), "RECEPCIONISTA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ESPERA"))
                .andExpect(jsonPath("$.horaLlegada").isNotEmpty());
        enviar(patch("/api/citas/" + hoy + "/atencion"), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
        enviar(patch("/api/citas/" + hoy + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"Tarde"}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu027_agendaPropiaOrdenadaPorHora() throws Exception {
        // TokensPrueba firma con sub "usuario-prueba": ese es el veterinario autenticado.
        horario("usuario-prueba", 8);
        horario("otro", 8);
        enviar(post("/api/citas"), "RECEPCIONISTA", cuerpoCita("usuario-prueba", LUNES, "10:00")).andExpect(status().isCreated());
        enviar(post("/api/citas"), "RECEPCIONISTA", cuerpoCita("usuario-prueba", LUNES, "08:00")).andExpect(status().isCreated());
        enviar(post("/api/citas"), "RECEPCIONISTA", cuerpoCita("otro", LUNES, "08:00")).andExpect(status().isCreated());

        enviar(get("/api/citas/mi-agenda?fecha=" + LUNES), "VETERINARIO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].hora").value(org.hamcrest.Matchers.startsWith("08:00")))
                .andExpect(jsonPath("$[1].hora").value(org.hamcrest.Matchers.startsWith("10:00")));
        enviar(get("/api/citas/mi-agenda"), "CAJERO", null).andExpect(status().isForbidden());
    }

    @Test
    void hu028_hu029_peluqueriaConCuposPropios() throws Exception {
        horario("v1", 1);
        enviar(put("/api/peluqueria/horario"), "ADMINISTRADOR", """
                {"nombre":"Peluquería","dias":%s,"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":1}""".formatted(TODOS_LOS_DIAS))
                .andExpect(status().isOk());
        agendar(LUNES, "08:00").andExpect(status().isCreated());

        String cita = idDe(enviar(post("/api/peluqueria/citas"), "PELUQUERIA", """
                {"mascotaId":"m1","fecha":"%s","hora":"08:00","servicio":"Baño"}""".formatted(LUNES)));
        enviar(post("/api/peluqueria/citas"), "PELUQUERIA", """
                {"mascotaId":"m1","fecha":"%s","hora":"09:00","servicio":"Baño"}""".formatted(LUNES))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No hay cupos disponibles"));

        enviar(get("/api/peluqueria/items-pendientes"), "CAJERO", null).andExpect(jsonPath("$", hasSize(0)));
        enviar(patch("/api/peluqueria/citas/" + cita + "/servicio"), "PELUQUERIA", """
                {"observaciones":"Sin novedades"}""").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REALIZADA"));
        enviar(patch("/api/peluqueria/citas/" + cita + "/servicio"), "PELUQUERIA", """
                {"observaciones":"Otra vez"}""").andExpect(status().isConflict());
        enviar(get("/api/peluqueria/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cita))
                .andExpect(jsonPath("$[0].tipo").value("PELUQUERIA"));
    }

    @Test
    void hu029_citaCanceladaNoAdmiteServicio() throws Exception {
        enviar(put("/api/peluqueria/horario"), "ADMINISTRADOR", """
                {"nombre":"Peluquería","dias":%s,"horaInicio":"08:00","horaFin":"12:00","cuposDiarios":1}""".formatted(TODOS_LOS_DIAS))
                .andExpect(status().isOk());
        String cita = idDe(enviar(post("/api/peluqueria/citas"), "PELUQUERIA", """
                {"mascotaId":"m1","fecha":"%s","hora":"08:00","servicio":"Baño"}""".formatted(LUNES)));
        enviar(patch("/api/citas/" + cita + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"No asiste"}""").andExpect(status().isOk());

        enviar(patch("/api/peluqueria/citas/" + cita + "/servicio"), "PELUQUERIA", """
                {"observaciones":"Nada"}""").andExpect(status().isUnprocessableContent());
    }

    private ResultActions ejecutarRecordatorios(String clave) throws Exception {
        MockHttpServletRequestBuilder peticion = post("/api/recordatorios/ejecucion");
        if (clave != null) {
            peticion.header("X-Clave-Proceso", clave);
        }
        return mockMvc.perform(peticion);
    }

    @Test
    void hu030_recordatorios() throws Exception {
        horario("v1", 8);
        LocalDate manana = LocalDate.now().plusDays(1);
        agendar(manana, "08:00").andExpect(status().isCreated());
        String cancelada = idDe(agendar(manana, "08:30"));
        enviar(patch("/api/citas/" + cancelada + "/cancelacion"), "RECEPCIONISTA", """
                {"motivo":"No asiste"}""").andExpect(status().isOk());

        ejecutarRecordatorios(null).andExpect(status().isForbidden());
        ejecutarRecordatorios("incorrecta").andExpect(status().isForbidden());
        verify(correoService, never()).enviar(anyString(), anyString(), anyString());

        doThrow(new IllegalStateException("SMTP caído")).when(correoService).enviar(anyString(), anyString(), anyString());
        ejecutarRecordatorios("clave-de-prueba").andExpect(status().isOk())
                .andExpect(jsonPath("$.enviados").value(0))
                .andExpect(jsonPath("$.fallidos").value(1));

        org.mockito.Mockito.reset(correoService);
        ejecutarRecordatorios("clave-de-prueba").andExpect(jsonPath("$.enviados").value(1));
        verify(correoService).enviar(org.mockito.ArgumentMatchers.eq("p@correo.test"), anyString(), anyString());
        ejecutarRecordatorios("clave-de-prueba").andExpect(jsonPath("$.enviados").value(0));
    }
}
