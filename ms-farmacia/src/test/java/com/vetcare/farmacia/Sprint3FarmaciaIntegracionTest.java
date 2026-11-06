package com.vetcare.farmacia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vetcare.farmacia.client.HistoriaClient;
import com.vetcare.farmacia.config.ArranqueDatos;
import com.vetcare.farmacia.dto.FormulaVigencia;
import com.vetcare.farmacia.exception.ServicioNoDisponibleException;
import com.vetcare.farmacia.model.Lote;
import com.vetcare.farmacia.security.TokensPrueba;
import java.time.LocalDate;
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
 * Escenarios de HU-051 a HU-060 contra un MongoDB real, en una base de datos exclusiva de pruebas.
 * Solo corre si se define MONGODB_TEST_URI, como ocurre en GitHub Actions.
 */
@SpringBootTest(properties = "spring.mongodb.database=" + Sprint3FarmaciaIntegracionTest.BASE_DE_PRUEBA)
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MONGODB_TEST_URI", matches = ".+")
class Sprint3FarmaciaIntegracionTest {

    static final String BASE_DE_PRUEBA = "vetcare_farmacia_test";
    private static final LocalDate VENCE = LocalDate.now().plusYears(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ArranqueDatos arranqueDatos;
    @MockitoBean
    private HistoriaClient historiaClient;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
    }

    @BeforeEach
    void preparar() {
        limpiar();
        arranqueDatos.preparar();
    }

    @AfterEach
    void limpiar() {
        assertThat(mongoTemplate.getDb().getName()).isEqualTo(BASE_DE_PRUEBA);
        mongoTemplate.getDb().drop();
    }

    private ResultActions enviarConToken(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        peticion.header("Authorization", "Bearer " + token);
        if (cuerpo != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(peticion);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, String rol, String cuerpo) throws Exception {
        return enviarConToken(peticion, TokensPrueba.token(rol), cuerpo);
    }

    private String id(ResultActions acciones) throws Exception {
        return JsonPath.read(acciones.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String medicamento(String codigo, int minimo) throws Exception {
        return id(enviar(post("/api/medicamentos"), "FARMACIA", """
                {"codigo":"%s","nombre":"Metronidazol 250 mg","tipo":"MEDICAMENTO","presentacion":"Caja","precio":1500,"stockMinimo":%d}""".formatted(codigo, minimo))
                .andExpect(status().isCreated()));
    }

    private String lote(String medicamento, String numero, LocalDate vencimiento, int cantidad) throws Exception {
        return id(enviar(post("/api/medicamentos/" + medicamento + "/lotes"), "FARMACIA", """
                {"numero":"%s","vencimiento":"%s","cantidad":%d}""".formatted(numero, vencimiento, cantidad))
                .andExpect(status().isCreated()));
    }

    private int cantidadLote(String loteId) {
        return mongoTemplate.findById(loteId, Lote.class).getCantidad();
    }

    private void vencerLote(String loteId) {
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(loteId)), new Update().set("vencimiento", LocalDate.now().minusDays(1)), Lote.class);
    }

    private void formula(String medicamento, String estado, boolean despachable, int pendiente) {
        when(historiaClient.vigencia("F-010")).thenReturn(new FormulaVigencia("F-010", "m1", estado, despachable,
                List.of(new FormulaVigencia.ItemPendiente(medicamento, "Metronidazol", pendiente))));
    }

    private ResultActions despachar(String medicamento, int cantidad) throws Exception {
        return enviar(post("/api/despachos"), "FARMACIA", """
                {"formulaId":"F-010","items":[{"medicamentoId":"%s","cantidad":%d}]}""".formatted(medicamento, cantidad));
    }

    @Test
    void hu051_catalogo() throws Exception {
        enviar(post("/api/medicamentos"), "FARMACIA", """
                {"codigo":"MED-1","nombre":"Metronidazol","tipo":"MEDICAMENTO","presentacion":"Caja","precio":1500,"stockMinimo":20}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockDisponible").value(0));
        enviar(post("/api/medicamentos"), "FARMACIA", """
                {"codigo":"MED-1","nombre":"Otro","tipo":"INSUMO","presentacion":"Caja","precio":1,"stockMinimo":1}""")
                .andExpect(status().isConflict());
        enviar(post("/api/medicamentos"), "FARMACIA", """
                {"codigo":"MED-2","nombre":"Otro","tipo":"INSUMO","presentacion":"Caja","precio":-1,"stockMinimo":1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.precio").isNotEmpty());
        enviar(get("/api/medicamentos?texto=metro"), "VETERINARIO", null)
                .andExpect(jsonPath("$.contenido", hasSize(1)));
    }

    @Test
    void hu052_lotes() throws Exception {
        String med = medicamento("MED-1", 0);
        lote(med, "L-100", VENCE, 50);

        enviar(get("/api/medicamentos/" + med), "FARMACIA", null).andExpect(jsonPath("$.stockDisponible").value(50));
        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.contenido[0].cantidad").value(50));
        String cuerpo = """
                {"numero":"%s","vencimiento":"%s","cantidad":%d}""";
        enviar(post("/api/medicamentos/" + med + "/lotes"), "FARMACIA", cuerpo.formatted("L-100", VENCE, 1)).andExpect(status().isConflict());
        enviar(post("/api/medicamentos/" + med + "/lotes"), "FARMACIA", cuerpo.formatted("L-101", LocalDate.now().minusDays(1), 1)).andExpect(status().isUnprocessableContent());
        enviar(post("/api/medicamentos/" + med + "/lotes"), "FARMACIA", cuerpo.formatted("L-102", VENCE, 0)).andExpect(status().isBadRequest());
        enviar(post("/api/medicamentos/no-existe/lotes"), "FARMACIA", cuerpo.formatted("L-103", VENCE, 1)).andExpect(status().isNotFound());
    }

    @Test
    void hu053_rn04_despachoValidoDescuentaPrimeroElLoteQueVenceAntes() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteB = lote(med, "B", VENCE, 20);
        String loteA = lote(med, "A", LocalDate.now().plusMonths(1), 4);
        formula(med, "VIGENTE", true, 14);

        despachar(med, 10).andExpect(status().isCreated()).andExpect(jsonPath("$.items[0].precioUnitario").value(1500));

        assertThat(cantidadLote(loteA)).isZero();
        assertThat(cantidadLote(loteB)).isEqualTo(14);
        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'DESPACHO')]", hasSize(2)));
        enviar(get("/api/despachos/items-pendientes"), "CAJERO", null)
                .andExpect(jsonPath("$[0].tipo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$[0].valor").value(15000));
    }

    @Test
    void hu053_rn02_rechazos() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteId = lote(med, "A", VENCE, 10);

        formula(med, "VENCIDA", false, 14);
        despachar(med, 5).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("No se puede despachar sin fórmula vigente"));
        formula(med, "ANULADA", false, 14);
        despachar(med, 5).andExpect(status().isUnprocessableContent());
        formula(med, "VIGENTE", true, 14);
        despachar(med, 20).andExpect(status().isUnprocessableContent());
        despachar(med, 14).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("Stock insuficiente"));
        assertThat(cantidadLote(loteId)).isEqualTo(10);
    }

    @Test
    void hu053_loteVencidoNoSeUsa() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteId = lote(med, "A", VENCE, 10);
        vencerLote(loteId);
        formula(med, "VIGENTE", true, 5);

        despachar(med, 5).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value("Stock insuficiente"));
        assertThat(cantidadLote(loteId)).isEqualTo(10);
    }

    @Test
    void hu053_siFallaLaFormulaSeRevierteYQuedaEnElKardex() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteId = lote(med, "A", VENCE, 10);
        formula(med, "VIGENTE", true, 5);
        doThrow(new ServicioNoDisponibleException("El servicio ms-historia-clinica no está disponible"))
                .when(historiaClient).registrarDespacho(anyString(), any());

        despachar(med, 5).andExpect(status().isServiceUnavailable());

        assertThat(cantidadLote(loteId)).isEqualTo(10);
        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'REVERSION')]", hasSize(1)));
        enviar(get("/api/despachos/items-pendientes"), "CAJERO", null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rn04_despachosSimultaneosNuncaDejanStockNegativo() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteA = lote(med, "A", LocalDate.now().plusMonths(1), 7);
        String loteB = lote(med, "B", VENCE, 8);
        formula(med, "VIGENTE", true, 100);

        ExecutorService hilos = Executors.newFixedThreadPool(10);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Callable<Integer> despacho = () -> despachar(med, 3).andReturn().getResponse().getStatus();
            resultados.add(hilos.submit(despacho));
        }
        int exitosos = 0;
        for (Future<Integer> resultado : resultados) {
            int estado = resultado.get();
            assertThat(estado).isIn(201, 409);
            exitosos += estado == 201 ? 1 : 0;
        }
        hilos.shutdown();

        int stock = cantidadLote(loteA) + cantidadLote(loteB);
        assertThat(cantidadLote(loteA)).isNotNegative();
        assertThat(cantidadLote(loteB)).isNotNegative();
        assertThat(stock).isEqualTo(15 - exitosos * 3);
        assertThat(exitosos).isPositive();
    }

    @Test
    void hu054_alertasDeStockMinimo() throws Exception {
        String conMinimo = medicamento("MED-1", 20);
        lote(conMinimo, "A", VENCE, 20);
        String suficiente = medicamento("MED-2", 5);
        lote(suficiente, "B", VENCE, 50);
        String conVencido = medicamento("MED-3", 20);
        lote(conVencido, "C", VENCE, 15);
        vencerLote(lote(conVencido, "D", VENCE, 15));

        enviar(get("/api/alertas/stock-minimo"), "FARMACIA", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.codigo == 'MED-1')].stockDisponible").value(org.hamcrest.Matchers.contains(20)))
                .andExpect(jsonPath("$[?(@.codigo == 'MED-3')].stockDisponible").value(org.hamcrest.Matchers.contains(15)));
    }

    @Test
    void hu055_ajustes() throws Exception {
        String med = medicamento("MED-1", 0);
        String loteId = lote(med, "A", VENCE, 10);
        String cuerpo = """
                {"loteId":"%s","cantidad":%d,"motivo":"Rotura"}""";

        enviar(post("/api/medicamentos/" + med + "/ajustes"), "FARMACIA", cuerpo.formatted(loteId, -2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(8));
        enviar(post("/api/medicamentos/" + med + "/ajustes"), "FARMACIA", cuerpo.formatted(loteId, -11))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente"));
        assertThat(cantidadLote(loteId)).isEqualTo(8);
        enviar(post("/api/medicamentos/" + med + "/ajustes"), "FARMACIA", """
                {"loteId":"%s","cantidad":-1,"motivo":""}""".formatted(loteId)).andExpect(status().isBadRequest());
        enviar(post("/api/medicamentos/" + med + "/ajustes"), "FARMACIA", cuerpo.formatted(loteId, 0)).andExpect(status().isBadRequest());

        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[1].tipo").value("AJUSTE"))
                .andExpect(jsonPath("$.contenido[1].cantidad").value(-2))
                .andExpect(jsonPath("$.contenido[1].saldoLote").value(8))
                .andExpect(jsonPath("$.contenido[1].referencia").value("Rotura"))
                .andExpect(jsonPath("$.contenido[1].responsable").value("prueba@vetcare.test"));
    }

    @Test
    void hu056_lotesProximosAVencer() throws Exception {
        String med = medicamento("MED-1", 0);
        lote(med, "PROXIMO", LocalDate.now().plusDays(30), 5);
        lote(med, "LEJANO", LocalDate.now().plusDays(120), 5);
        String sinStock = lote(med, "SIN-STOCK", LocalDate.now().plusDays(10), 1);
        enviar(post("/api/medicamentos/" + med + "/ajustes"), "FARMACIA", """
                {"loteId":"%s","cantidad":-1,"motivo":"Rotura"}""".formatted(sinStock)).andExpect(status().isOk());

        enviar(get("/api/lotes/proximos-vencer"), "FARMACIA", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value("PROXIMO"))
                .andExpect(jsonPath("$[0].medicamento").value("Metronidazol 250 mg"));
    }

    private String proveedor(String nit) throws Exception {
        return id(enviar(post("/api/proveedores"), "ADMINISTRADOR", """
                {"nit":"%s","nombre":"Distribuciones","telefono":"607","correo":"v@proveedor.test"}""".formatted(nit))
                .andExpect(status().isCreated()));
    }

    private String orden(String proveedor, String medicamento, int cantidad) throws Exception {
        return id(enviar(post("/api/ordenes-compra"), "FARMACIA", """
                {"proveedorId":"%s","items":[{"medicamentoId":"%s","cantidad":%d}]}""".formatted(proveedor, medicamento, cantidad))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ENVIADA")));
    }

    @Test
    void hu057_proveedores() throws Exception {
        proveedor("900");
        enviar(post("/api/proveedores"), "ADMINISTRADOR", """
                {"nit":"900","nombre":"Otro","telefono":"607","correo":"o@proveedor.test"}""").andExpect(status().isConflict());
        enviar(post("/api/proveedores"), "FARMACIA", """
                {"nit":"901","nombre":"Otro","telefono":"607","correo":"o@proveedor.test"}""").andExpect(status().isForbidden());
    }

    @Test
    void hu058_ordenesDeCompra() throws Exception {
        String med = medicamento("MED-1", 0);
        String prov = proveedor("900");
        orden(prov, med, 100);

        enviar(get("/api/medicamentos/" + med), "FARMACIA", null).andExpect(jsonPath("$.stockDisponible").value(0));
        enviar(post("/api/ordenes-compra"), "FARMACIA", """
                {"proveedorId":"no-existe","items":[{"medicamentoId":"%s","cantidad":1}]}""".formatted(med)).andExpect(status().isNotFound());
        enviar(post("/api/ordenes-compra"), "FARMACIA", """
                {"proveedorId":"%s","items":[{"medicamentoId":"%s","cantidad":0}]}""".formatted(prov, med)).andExpect(status().isBadRequest());
    }

    @Test
    void hu059_portalDelProveedor() throws Exception {
        String med = medicamento("MED-1", 0);
        String prov = proveedor("900");
        String otro = proveedor("901");
        String mia = orden(prov, med, 10);
        orden(prov, med, 20);
        String ajena = orden(otro, med, 30);
        String token = TokensPrueba.tokenDeProveedor(prov);

        enviarConToken(get("/api/proveedor/ordenes-compra"), token, null).andExpect(jsonPath("$", hasSize(2)));
        enviarConToken(patch("/api/proveedor/ordenes-compra/" + mia + "/confirmacion"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        enviarConToken(patch("/api/proveedor/ordenes-compra/" + mia + "/confirmacion"), token, null).andExpect(status().isUnprocessableContent());
        enviarConToken(patch("/api/proveedor/ordenes-compra/" + ajena + "/confirmacion"), token, null).andExpect(status().isForbidden());
    }

    private ResultActions recibir(String orden, String medicamento, String lote, LocalDate vence, int cantidad) throws Exception {
        return enviar(post("/api/ordenes-compra/" + orden + "/recepciones"), "FARMACIA", """
                {"items":[{"medicamentoId":"%s","lote":"%s","vencimiento":"%s","cantidad":%d}]}""".formatted(medicamento, lote, vence, cantidad));
    }

    @Test
    void hu060_recepcionDeMercancia() throws Exception {
        String med = medicamento("MED-1", 0);
        String prov = proveedor("900");
        String orden = orden(prov, med, 100);

        recibir(orden, med, "L-1", VENCE, 60).andExpect(status().isUnprocessableContent());
        enviarConToken(patch("/api/proveedor/ordenes-compra/" + orden + "/confirmacion"), TokensPrueba.tokenDeProveedor(prov), null).andExpect(status().isOk());

        recibir(orden, med, "L-1", LocalDate.now().minusDays(1), 60).andExpect(status().isUnprocessableContent());
        recibir(orden, med, "L-1", VENCE, 60)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA_PARCIAL"))
                .andExpect(jsonPath("$.items[0].recibida").value(60));
        recibir(orden, med, "L-2", VENCE, 50).andExpect(status().isUnprocessableContent());
        enviar(get("/api/medicamentos/" + med), "FARMACIA", null).andExpect(jsonPath("$.stockDisponible").value(60));

        recibir(orden, med, "L-2", VENCE, 40).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECIBIDA"));
        enviar(get("/api/medicamentos/" + med), "FARMACIA", null).andExpect(jsonPath("$.stockDisponible").value(100));
        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'ENTRADA')]", hasSize(2)));
        recibir(orden, med, "L-3", VENCE, 1).andExpect(status().isUnprocessableContent());
    }

    @Test
    void hu037_consumoDeVacunaPorLote() throws Exception {
        String med = medicamento("VAC-1", 0);
        String loteId = lote(med, "V-1", VENCE, 1);
        String cuerpo = """
                {"cantidad":1}""";

        enviar(post("/api/lotes/V-1/consumos"), "VETERINARIO", cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.cantidad").value(0));
        enviar(post("/api/lotes/V-1/consumos"), "VETERINARIO", cuerpo).andExpect(status().isConflict());
        enviar(post("/api/lotes/NO-EXISTE/consumos"), "VETERINARIO", cuerpo).andExpect(status().isNotFound());
        enviar(get("/api/medicamentos/" + med + "/kardex"), "FARMACIA", null)
                .andExpect(jsonPath("$.contenido[?(@.tipo == 'CONSUMO_VACUNA')]", hasSize(1)));

        vencerLote(loteId);
        enviar(post("/api/lotes/V-1/consumos"), "VETERINARIO", cuerpo).andExpect(status().isUnprocessableContent());
    }

    @Test
    void soporteDeInterfaz_lotesDeUnMedicamento() throws Exception {
        String med = medicamento("MED-1", 0);
        lote(med, "LEJANO", VENCE, 5);
        lote(med, "PROXIMO", LocalDate.now().plusDays(10), 5);

        enviar(get("/api/medicamentos/" + med + "/lotes"), "FARMACIA", null)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numero").value("PROXIMO"));
        enviar(get("/api/medicamentos/no-existe/lotes"), "FARMACIA", null).andExpect(status().isNotFound());
        enviar(get("/api/medicamentos/" + med + "/lotes"), "CAJERO", null).andExpect(status().isForbidden());
    }
}
