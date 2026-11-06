package com.vetcare.facturacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetcare.facturacion.security.TokensPrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Un servidor de correo caído o sin configurar no debe marcar el servicio como caído. */
@SpringBootTest(properties = {
        "spring.mongodb.uri=mongodb://127.0.0.1:1/?serverSelectionTimeoutMS=200",
        "management.health.mongodb.enabled=false",
        "spring.mail.host=smtp.invalid"})
@AutoConfigureMockMvc
class SaludSinCorreoTest {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void claves(DynamicPropertyRegistry registro) {
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
        registro.add("vetcare.jwt.private-key", TokensPrueba::clavePrivada);
    }

    @Test
    void hu074_elCorreoNoAfectaLaSaludDelServicio() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
