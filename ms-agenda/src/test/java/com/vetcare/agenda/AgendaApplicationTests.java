package com.vetcare.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/?serverSelectionTimeoutMS=200")
@AutoConfigureMockMvc
class AgendaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

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
    void loDemasRequiereAutenticacion() throws Exception {
        int estado = mockMvc.perform(get("/api/recurso-protegido")).andReturn().getResponse().getStatus();

        assertThat(estado).isIn(401, 403);
    }
}
