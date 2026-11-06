package com.vetcare.facturacion.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vetcare.facturacion.dto.ItemPendiente;
import com.vetcare.facturacion.exception.ConflictoException;
import com.vetcare.facturacion.exception.ServicioNoDisponibleException;
import com.vetcare.facturacion.model.ItemFactura;
import com.vetcare.facturacion.model.TipoServicio;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * HU-061: recoge los ítems de los demás servicios y evita dejar marcas a medias.
 */
class ItemsClientTest {

    private static final String AGENDA = "http://agenda.test";
    private static final String HISTORIA = "http://historia.test";
    private static final String FARMACIA = "http://farmacia.test";

    private MockRestServiceServer servidor;
    private ItemsClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ItemsClient(builder, AGENDA, HISTORIA, FARMACIA);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), Map.of("sub", "u1"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static final String VACIO = "[]";

    @Test
    void hu061_soloIncluyeLosItemsDeLaMascota() {
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-pendientes")).andRespond(withSuccess("""
                [{"id":"c1","tipo":"CONSULTA","descripcion":"Consulta","mascotaId":"m1","cantidad":1},
                 {"id":"c2","tipo":"CONSULTA","descripcion":"Otra","mascotaId":"m2","cantidad":1}]""", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(HISTORIA + "/api/ordenes-laboratorio/items-pendientes")).andRespond(withSuccess(VACIO, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(FARMACIA + "/api/despachos/items-pendientes")).andRespond(withSuccess("""
                [{"id":"d1","tipo":"MEDICAMENTOS","descripcion":"Metronidazol","mascotaId":"m1","valor":21000}]""", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(HISTORIA + "/api/hospitalizaciones/items-pendientes")).andRespond(withSuccess(VACIO, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(AGENDA + "/api/peluqueria/items-pendientes")).andRespond(withSuccess(VACIO, MediaType.APPLICATION_JSON));

        List<ItemPendiente> items = cliente.pendientesDe("m1");

        assertThat(items).extracting(ItemPendiente::id).containsExactlyInAnyOrder("c1", "d1");
        assertThat(items).filteredOn(i -> i.id().equals("d1")).first().extracting(ItemPendiente::valor).isEqualTo(21000L);
        servidor.verify();
    }

    @Test
    void hu061_servicioCaidoImpideGenerarLaFactura() {
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-pendientes")).andRespond(withServerError());

        assertThatThrownBy(() -> cliente.pendientesDe("m1")).isInstanceOf(ServicioNoDisponibleException.class);
    }

    private final List<ItemFactura> items = List.of(
            new ItemFactura("c1", TipoServicio.CONSULTA, "Consulta", 1, 60000, 60000),
            new ItemFactura("b1", TipoServicio.PELUQUERIA, "Baño", 1, 39000, 39000));

    @Test
    void hu061_siUnServicioFallaAlMarcarDeshaceLoAplicado() {
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-facturados")).andExpect(method(HttpMethod.PATCH)).andExpect(jsonPath("$.facturado").value(true)).andRespond(withSuccess());
        servidor.expect(requestTo(AGENDA + "/api/peluqueria/items-facturados")).andExpect(jsonPath("$.ids[0]").value("b1")).andRespond(withServerError());
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-facturados")).andExpect(jsonPath("$.facturado").value(false)).andRespond(withSuccess());

        assertThatThrownBy(() -> cliente.marcar(items, true)).isInstanceOf(ServicioNoDisponibleException.class);
        servidor.verify();
    }

    @Test
    void hu061_cobroSimultaneoRespondeConflictoYDeshace() {
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-facturados")).andRespond(withSuccess());
        servidor.expect(requestTo(AGENDA + "/api/peluqueria/items-facturados")).andRespond(withStatus(HttpStatus.CONFLICT));
        servidor.expect(requestTo(HISTORIA + "/api/consultas/items-facturados")).andExpect(jsonPath("$.facturado").value(false)).andRespond(withSuccess());

        assertThatThrownBy(() -> cliente.marcar(items, true)).isInstanceOf(ConflictoException.class);
        servidor.verify();
    }
}
