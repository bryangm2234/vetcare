package com.vetcare.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
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
import com.vetcare.facturacion.client.ItemsClient;
import com.vetcare.facturacion.client.PacientesClient;
import com.vetcare.facturacion.config.ArranqueDatos;
import com.vetcare.facturacion.dto.ItemPendiente;
import com.vetcare.facturacion.dto.MascotaInfo;
import com.vetcare.facturacion.dto.PropietarioInfo;
import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.ServicioNoDisponibleException;
import com.vetcare.facturacion.security.TokensPrueba;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Escenarios de HU-061 a HU-070 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = {"spring.mongodb.database=" + Sprint4FacturacionIntegracionTest.BASE_DE_PRUEBA, "vetcare.correo.remitente=vetcare@correo.test"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint4FacturacionIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_facturacion_test";
    private static final String CUERPO = """
            {"mascotaId":"m1"}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @MockitoBean
    private ItemsClient itemsClient;
    @MockitoBean
    private PacientesClient pacientesClient;
    @MockitoBean
    private JavaMailSender mailSender;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
    }

    @BeforeEach
    void preparar() throws Exception {
        limpiar();
        arranqueDatos.preparar();
        when(pacientesClient.mascota("m1")).thenReturn(new MascotaInfo("m1", "Max", "p1"));
        when(pacientesClient.propietario("p1")).thenReturn(new PropietarioInfo("p1", "p@correo.test"));
        when(itemsClient.pendientesDe("m1")).thenReturn(List.of(
                new ItemPendiente("c1", "CONSULTA", "Consulta: Gastroenteritis", "m1", 1, null),
                new ItemPendiente("l1", "LABORATORIO", "Examen: Hemograma", "m1", 1, null),
                new ItemPendiente("d1", "MEDICAMENTOS", "Metronidazol x 14", "m1", null, 21000L),
                new ItemPendiente("h1", "HOSPITALIZACION", "Estancia de 3 días", "m1", 3, null),
                new ItemPendiente("b1", "PELUQUERIA", "Baño", "m1", 1, null)));
        tarifa("CONSULTA", 60000);
        tarifa("LABORATORIO", 30000);
        tarifa("HOSPITALIZACION", 50000);
        tarifa("PELUQUERIA", 39000);
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
        return JsonPath.read(acciones.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String tarifa(String servicio, long precio) throws Exception {
        return id(enviar(post("/api/tarifas"), "ADMINISTRADOR", """
                {"servicio":"%s","nombre":"%s","precio":%d}""".formatted(servicio, servicio, precio)).andExpect(status().isCreated()));
    }

    private String factura(String cuerpo) throws Exception {
        return id(enviar(post("/api/facturas"), "CAJERO", cuerpo).andExpect(status().isCreated()));
    }

    private String caja() throws Exception {
        return id(enviar(post("/api/cajas/apertura"), "CAJERO", """
                {"base":100000}""").andExpect(status().isCreated()));
    }

    private ResultActions pagar(String factura, String medio, long valor) throws Exception {
        return enviar(post("/api/facturas/" + factura + "/pagos"), "CAJERO", """
                {"pagos":[{"medio":"%s","valor":%d}]}""".formatted(medio, valor));
    }

    @Test
    void hu061_facturaConsolidada() throws Exception {
        enviar(post("/api/facturas"), "CAJERO", CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("FAC-000001"))
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[?(@.tipo == 'HOSPITALIZACION')].subtotal").value(org.hamcrest.Matchers.contains(150000)))
                .andExpect(jsonPath("$.items[?(@.tipo == 'MEDICAMENTOS')].subtotal").value(org.hamcrest.Matchers.contains(21000)))
                .andExpect(jsonPath("$.subtotal").value(300000))
                .andExpect(jsonPath("$.impuesto").value(57000))
                .andExpect(jsonPath("$.total").value(357000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.version").doesNotExist());
        verify(itemsClient).marcar(any(), eq(true));
        enviar(post("/api/facturas"), "RECEPCIONISTA", CUERPO).andExpect(status().isForbidden());
    }

    @Test
    void hu061_sinItems() throws Exception {
        when(itemsClient.pendientesDe("m1")).thenReturn(List.of());

        enviar(post("/api/facturas"), "CAJERO", CUERPO).andExpect(status().isUnprocessableContent());
        verify(itemsClient, never()).marcar(any(), anyBoolean());
    }

    @Test
    void hu061_servicioCaidoNoCreaFacturaNiMarca() throws Exception {
        when(itemsClient.pendientesDe("m1")).thenThrow(new ServicioNoDisponibleException("El servicio ms-farmacia no está disponible"));

        enviar(post("/api/facturas"), "CAJERO", CUERPO).andExpect(status().isServiceUnavailable());
        verify(itemsClient, never()).marcar(any(), anyBoolean());
        assertThat(mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), "facturas")).isZero();
    }

    @Test
    void hu061_itemYaFacturadoPorOtroCajero() throws Exception {
        doThrow(new ConflictoException("La operación fue rechazada por conflicto en ms-farmacia")).when(itemsClient).marcar(any(), eq(true));

        enviar(post("/api/facturas"), "CAJERO", CUERPO).andExpect(status().isConflict());
        assertThat(mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), "facturas")).isZero();
    }

    @Test
    void hu061_sinTarifaIndicaElServicio() throws Exception {
        when(itemsClient.pendientesDe("m1")).thenReturn(List.of(new ItemPendiente("x", "CONSULTA", "c", "m1", 1, null)));
        mongoTemplate.dropCollection("tarifas");

        enviar(post("/api/facturas"), "CAJERO", CUERPO)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No hay tarifa vigente para el servicio CONSULTA"));
        verify(itemsClient, never()).marcar(any(), anyBoolean());
    }

    @Test
    void hu062_cambiosSoloAfectanFacturasNuevas() throws Exception {
        List<String> ids = JsonPath.read(enviar(get("/api/tarifas"), "ADMINISTRADOR", null).andReturn().getResponse().getContentAsString(), "$[?(@.servicio == 'CONSULTA')].id");
        String tarifa = ids.get(0);
        String antes = factura(CUERPO);

        enviar(put("/api/tarifas/" + tarifa), "ADMINISTRADOR", """
                {"servicio":"CONSULTA","nombre":"Consulta general","precio":80000}""")
                .andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(80000));
        enviar(get("/api/facturas/" + antes), "CAJERO", null)
                .andExpect(jsonPath("$.items[?(@.tipo == 'CONSULTA')].precioUnitario").value(org.hamcrest.Matchers.contains(60000)));
        enviar(post("/api/facturas"), "CAJERO", CUERPO)
                .andExpect(jsonPath("$.items[?(@.tipo == 'CONSULTA')].precioUnitario").value(org.hamcrest.Matchers.contains(80000)));

        enviar(post("/api/tarifas"), "ADMINISTRADOR", """
                {"servicio":"CONSULTA","nombre":"x","precio":1}""").andExpect(status().isConflict());
        enviar(put("/api/tarifas/" + tarifa), "ADMINISTRADOR", """
                {"servicio":"CONSULTA","nombre":"x","precio":-1}""").andExpect(status().isBadRequest());
        enviar(put("/api/tarifas/" + tarifa), "CAJERO", """
                {"servicio":"CONSULTA","nombre":"x","precio":1}""").andExpect(status().isForbidden());
    }

    @Test
    void hu063_descuentos() throws Exception {
        String factura = factura(CUERPO);

        enviar(patch("/api/facturas/" + factura + "/ajustes"), "CAJERO", """
                {"descuentoPorcentaje":10}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descuento").value(30000))
                .andExpect(jsonPath("$.impuesto").value(51300))
                .andExpect(jsonPath("$.total").value(321300))
                .andExpect(jsonPath("$.saldo").value(321300));
        enviar(patch("/api/facturas/" + factura + "/ajustes"), "CAJERO", """
                {"descuentoValor":300001}""").andExpect(status().isUnprocessableContent());

        caja();
        pagar(factura, "EFECTIVO", 1000).andExpect(status().isOk());
        enviar(patch("/api/facturas/" + factura + "/ajustes"), "CAJERO", """
                {"descuentoValor":1}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu064_pagos() throws Exception {
        String factura = factura(CUERPO);

        pagar(factura, "EFECTIVO", 1000).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("Debe abrir la caja"));
        caja();
        pagar(factura, "CHEQUE", 1000).andExpect(status().isBadRequest());
        pagar(factura, "EFECTIVO", 357001).andExpect(status().isUnprocessableContent());

        enviar(post("/api/facturas/" + factura + "/pagos"), "CAJERO", """
                {"pagos":[{"medio":"EFECTIVO","valor":200000},{"medio":"TARJETA","valor":100000}]}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(57000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.pagos", hasSize(2)));
        pagar(factura, "TRANSFERENCIA", 57000).andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.estado").value("PAGADA"));
        pagar(factura, "EFECTIVO", 1).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu065_anulacion() throws Exception {
        String sinPagos = factura(CUERPO);
        enviar(patch("/api/facturas/" + sinPagos + "/anulacion"), "CAJERO", """
                {"motivo":""}""").andExpect(status().isBadRequest());
        enviar(patch("/api/facturas/" + sinPagos + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADA"));
        verify(itemsClient).marcar(any(), eq(false));
        enviar(patch("/api/facturas/" + sinPagos + "/anulacion"), "CAJERO", """
                {"motivo":"Otra vez"}""").andExpect(status().isUnprocessableContent());
        enviar(get("/api/auditoria?accion=ANULAR_FACTURA"), "ADMINISTRADOR", null)
                .andExpect(jsonPath("$.contenido[0].recurso").value("facturas/" + sinPagos));

        String caja = caja();
        String conPagoAbierta = factura(CUERPO);
        pagar(conPagoAbierta, "EFECTIVO", 50000).andExpect(status().isOk());
        enviar(patch("/api/facturas/" + conPagoAbierta + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isOk()).andExpect(jsonPath("$.pagos[0].revertido").value(true));

        String conPagoCerrada = factura(CUERPO);
        pagar(conPagoCerrada, "EFECTIVO", 50000).andExpect(status().isOk());
        enviar(patch("/api/cajas/" + caja + "/cierre"), "CAJERO", """
                {"efectivoContado":150000}""").andExpect(status().isOk());
        enviar(patch("/api/facturas/" + conPagoCerrada + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu065_siUnServicioFallaLaFacturaSigueVigente() throws Exception {
        String factura = factura(CUERPO);
        doThrow(new ServicioNoDisponibleException("El servicio ms-agenda no está disponible")).when(itemsClient).marcar(any(), eq(false));

        enviar(patch("/api/facturas/" + factura + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isServiceUnavailable());
        enviar(get("/api/facturas/" + factura), "CAJERO", null).andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void hu066_caja() throws Exception {
        String caja = caja();
        enviar(post("/api/cajas/apertura"), "CAJERO", """
                {"base":1}""").andExpect(status().isConflict());

        String factura = factura(CUERPO);
        enviar(post("/api/facturas/" + factura + "/pagos"), "CAJERO", """
                {"pagos":[{"medio":"EFECTIVO","valor":200000},{"medio":"TARJETA","valor":100000}]}""").andExpect(status().isOk());
        String anulada = factura(CUERPO);
        pagar(anulada, "EFECTIVO", 40000).andExpect(status().isOk());
        enviar(patch("/api/facturas/" + anulada + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isOk());

        // Otro cajero no puede cerrarla: su token tiene otro identificador.
        enviar(patch("/api/cajas/" + caja + "/cierre"), "CAJERO", """
                {"efectivoContado":280000}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.efectivoEsperado").value(300000))
                .andExpect(jsonPath("$.diferencia").value(-20000));
        enviar(patch("/api/cajas/" + caja + "/cierre"), "CAJERO", """
                {"efectivoContado":1}""").andExpect(status().isUnprocessableContent());
        caja();
    }

    @Test
    void hu066_cajaAjena() throws Exception {
        String caja = caja();
        mongoTemplate.updateFirst(new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(caja)),
                new org.springframework.data.mongodb.core.query.Update().set("cajeroId", "otro"), "cajas");

        enviar(patch("/api/cajas/" + caja + "/cierre"), "CAJERO", """
                {"efectivoContado":1}""").andExpect(status().isForbidden());
    }

    @Test
    void hu067_portal() throws Exception {
        String factura = factura(CUERPO);
        String dueno = TokensPrueba.tokenDePropietario("p1");
        String otro = TokensPrueba.tokenDePropietario("p2");

        mockMvc.perform(get("/api/portal/facturas").header("Authorization", "Bearer " + dueno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].total").value(357000));
        mockMvc.perform(get("/api/portal/facturas").header("Authorization", "Bearer " + otro))
                .andExpect(jsonPath("$", hasSize(0)));
        byte[] pdf = mockMvc.perform(get("/api/portal/facturas/" + factura + "/pdf").header("Authorization", "Bearer " + dueno))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        mockMvc.perform(get("/api/portal/facturas/" + factura + "/pdf").header("Authorization", "Bearer " + otro))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu068_abonos() throws Exception {
        String credito = factura("""
                {"mascotaId":"m1","credito":true}""");
        String contado = factura(CUERPO);
        caja();
        String abono = """
                {"medio":"EFECTIVO","valor":%d}""";

        enviar(post("/api/facturas/" + contado + "/abonos"), "CAJERO", abono.formatted(1)).andExpect(status().isUnprocessableContent());
        pagar(credito, "EFECTIVO", 1).andExpect(status().isUnprocessableContent());
        enviar(post("/api/facturas/" + credito + "/abonos"), "CAJERO", abono.formatted(357001)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/facturas/" + credito + "/abonos"), "CAJERO", abono.formatted(57000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(300000))
                .andExpect(jsonPath("$.estado").value("CREDITO"))
                .andExpect(jsonPath("$.pagos[0].abono").value(true));
        enviar(post("/api/facturas/" + credito + "/abonos"), "CAJERO", abono.formatted(300000))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.estado").value("PAGADA"));
    }

    @Test
    void hu069_ingresos() throws Exception {
        caja();
        String factura = factura(CUERPO);
        enviar(post("/api/facturas/" + factura + "/pagos"), "CAJERO", """
                {"pagos":[{"medio":"EFECTIVO","valor":300000},{"medio":"TARJETA","valor":57000}]}""").andExpect(status().isOk());
        String anulada = factura(CUERPO);
        pagar(anulada, "EFECTIVO", 1000).andExpect(status().isOk());
        enviar(patch("/api/facturas/" + anulada + "/anulacion"), "CAJERO", """
                {"motivo":"Error"}""").andExpect(status().isOk());
        LocalDate hoy = LocalDate.now();
        String periodo = "?desde=" + hoy + "&hasta=" + hoy;

        enviar(get("/api/reportes/ingresos" + periodo), "ADMINISTRADOR", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(357000))
                .andExpect(jsonPath("$.porMedioPago.EFECTIVO").value(300000))
                .andExpect(jsonPath("$.porMedioPago.TARJETA").value(57000))
                .andExpect(jsonPath("$.porServicio.CONSULTA").value(71400));
        enviar(get("/api/reportes/ingresos" + periodo + "&medioPago=TARJETA"), "ADMINISTRADOR", null)
                .andExpect(jsonPath("$.total").value(57000));
        enviar(get("/api/reportes/ingresos" + periodo + "&servicio=CONSULTA"), "ADMINISTRADOR", null)
                .andExpect(jsonPath("$.total").value(71400));
        enviar(get("/api/reportes/ingresos?desde=" + hoy.plusDays(1) + "&hasta=" + hoy), "ADMINISTRADOR", null).andExpect(status().isBadRequest());
        enviar(get("/api/reportes/ingresos" + periodo), "CAJERO", null).andExpect(status().isForbidden());
    }

    @Test
    void hu070_envioPorCorreo() throws Exception {
        String factura = factura(CUERPO);
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));

        enviar(post("/api/facturas/" + factura + "/envio-correo"), "CAJERO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaEnvioCorreo").isNotEmpty());
        verify(mailSender).send(any(MimeMessage.class));

        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(any(MimeMessage.class));
        String otra = factura(CUERPO);
        enviar(post("/api/facturas/" + otra + "/envio-correo"), "CAJERO", null).andExpect(status().isBadGateway());
        enviar(get("/api/facturas/" + otra), "CAJERO", null).andExpect(jsonPath("$.fechaEnvioCorreo").doesNotExist());

        when(pacientesClient.propietario("p1")).thenReturn(new PropietarioInfo("p1", " "));
        String sinCorreo = factura(CUERPO);
        enviar(post("/api/facturas/" + sinCorreo + "/envio-correo"), "CAJERO", null).andExpect(status().isUnprocessableContent());
    }

    @Test
    void soporteDeInterfaz_facturasYCaja() throws Exception {
        enviar(get("/api/cajas/actual"), "CAJERO", null).andExpect(status().isUnprocessableContent());
        String caja = caja();
        enviar(get("/api/cajas/actual"), "CAJERO", null).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(caja));

        String primera = factura(CUERPO);
        String segunda = factura(CUERPO);
        pagar(segunda, "EFECTIVO", 357000).andExpect(status().isOk());

        enviar(get("/api/facturas"), "CAJERO", null).andExpect(jsonPath("$.totalElementos").value(2));
        enviar(get("/api/facturas?estado=PENDIENTE"), "CAJERO", null)
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].id").value(primera));
        enviar(get("/api/facturas"), "RECEPCIONISTA", null).andExpect(status().isForbidden());
    }
}
