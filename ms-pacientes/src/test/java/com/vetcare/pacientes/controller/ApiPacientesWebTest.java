package com.vetcare.pacientes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetcare.pacientes.dto.LoginResponse;
import com.vetcare.pacientes.exception.AutenticacionFallidaException;
import com.vetcare.pacientes.security.Rol;
import com.vetcare.pacientes.security.TokensPrueba;
import com.vetcare.pacientes.service.AuditoriaService;
import com.vetcare.pacientes.service.AuthService;
import com.vetcare.pacientes.service.MascotaService;
import com.vetcare.pacientes.service.PropietarioService;
import com.vetcare.pacientes.service.UsuarioService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Valida permisos, formato de entrada y códigos HTTP sin necesidad de MongoDB.
 */
@SpringBootTest(properties = "spring.mongodb.uri=mongodb://localhost:27017/?serverSelectionTimeoutMS=200")
@AutoConfigureMockMvc
class ApiPacientesWebTest {

    private static final String USUARIO = """
            {"nombre":"Ana","correo":"ana@vetcare.test","contrasena":"clave-segura","rol":"CAJERO"}""";
    private static final String PROPIETARIO = """
            {"documento":"109","nombre":"María","telefono":"300","correo":"maria@correo.test","direccion":"Calle 10"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private PropietarioService propietarioService;
    @MockitoBean
    private MascotaService mascotaService;
    @MockitoBean
    private AuditoriaService auditoriaService;

    @DynamicPropertySource
    static void claves(DynamicPropertyRegistry registro) {
        registro.add("vetcare.jwt.public-key", TokensPrueba::clavePublica);
        registro.add("vetcare.jwt.private-key", TokensPrueba::clavePrivada);
        registro.add("vetcare.admin.correo", () -> "");
    }

    private MockHttpServletRequestBuilder conRol(MockHttpServletRequestBuilder peticion, String rol) {
        return peticion.header("Authorization", "Bearer " + TokensPrueba.token(rol));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder peticion, String cuerpo) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    void hu002_elInicioDeSesionEsPublico() throws Exception {
        when(authService.iniciarSesion(any())).thenReturn(new LoginResponse("token", "Bearer", Instant.now(), Rol.CAJERO));

        mockMvc.perform(json(post("/api/auth/login"), """
                        {"correo":"cajero@vetcare.test","contrasena":"clave-segura"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.rol").value("CAJERO"));
    }

    @Test
    void hu002_credencialesIncorrectasResponden401SinDetalle() throws Exception {
        when(authService.iniciarSesion(any())).thenThrow(new AutenticacionFallidaException());

        mockMvc.perform(json(post("/api/auth/login"), """
                        {"correo":"cajero@vetcare.test","contrasena":"incorrecta"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void hu001_rolInexistenteResponde400() throws Exception {
        mockMvc.perform(json(conRol(post("/api/usuarios"), "ADMINISTRADOR"), """
                        {"nombre":"Ana","correo":"ana@vetcare.test","contrasena":"clave-segura","rol":"PIRATA"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void hu001_otroRolNoCreaUsuarios() throws Exception {
        mockMvc.perform(json(conRol(post("/api/usuarios"), "CAJERO"), USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu005_contrasenaNuevaCortaResponde400() throws Exception {
        mockMvc.perform(json(conRol(put("/api/usuarios/me/contrasena"), "CAJERO"), """
                        {"contrasenaActual":"clave-segura","contrasenaNueva":"corta"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.contrasenaNueva").isNotEmpty());
    }

    @Test
    void hu006_soloAdministradorConsultaUsuarios() throws Exception {
        mockMvc.perform(conRol(get("/api/usuarios"), "RECEPCIONISTA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu004_soloAdministradorDesactiva() throws Exception {
        mockMvc.perform(conRol(patch("/api/usuarios/u1/desactivar"), "RECEPCIONISTA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu011_correoInvalidoResponde400ConElCampo() throws Exception {
        mockMvc.perform(json(conRol(post("/api/propietarios"), "RECEPCIONISTA"), """
                        {"documento":"109","nombre":"María","telefono":"300","correo":"no-es-correo","direccion":"Calle 10"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").isNotEmpty());
    }

    @Test
    void hu011_otroRolNoRegistraPropietarios() throws Exception {
        mockMvc.perform(json(conRol(post("/api/propietarios"), "CAJERO"), PROPIETARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu012_fechaFuturaResponde400() throws Exception {
        mockMvc.perform(json(conRol(post("/api/mascotas"), "RECEPCIONISTA"), """
                        {"nombre":"Max","especie":"Canino","raza":"Criollo","sexo":"MACHO","fechaNacimiento":"2999-01-01","pesoKg":12.5,"propietarioId":"p1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.fechaNacimiento").isNotEmpty());
    }

    @Test
    void hu012_pesoNoPositivoResponde400() throws Exception {
        mockMvc.perform(json(conRol(post("/api/mascotas"), "RECEPCIONISTA"), """
                        {"nombre":"Max","especie":"Canino","raza":"Criollo","sexo":"MACHO","fechaNacimiento":"2022-01-01","pesoKg":0,"propietarioId":"p1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.pesoKg").isNotEmpty());
    }

    @Test
    void hu016_recepcionistaNoRegistraAntecedentes() throws Exception {
        mockMvc.perform(json(conRol(put("/api/mascotas/m1/antecedentes"), "RECEPCIONISTA"), """
                        {"alergias":["Penicilina"],"condicionesCronicas":[]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu003_proveedorNoConsultaMascotas() throws Exception {
        mockMvc.perform(conRol(get("/api/mascotas/m1"), "PROVEEDOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu019_soloAdministradorConsultaElReporte() throws Exception {
        mockMvc.perform(conRol(get("/api/reportes/mascotas"), "CAJERO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hu010_hu020_laDocumentacionIncluyeLosEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/propietarios'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/mascotas'].post.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.MascotaRequest.properties.especie.example").value("Canino"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void hu014_correoInvalidoResponde400ConElCampo() throws Exception {
        mockMvc.perform(json(conRol(patch("/api/propietarios/p1/contacto"), "RECEPCIONISTA"), """
                        {"telefono":"300","correo":"no-es-correo","direccion":"Calle 10"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.correo").isNotEmpty());
    }
}
