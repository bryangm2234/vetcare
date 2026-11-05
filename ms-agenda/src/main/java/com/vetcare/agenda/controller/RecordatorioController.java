package com.vetcare.agenda.controller;

import com.vetcare.agenda.dto.RecordatoriosResponse;
import com.vetcare.agenda.security.ClaveProceso;
import com.vetcare.agenda.service.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recordatorios")
@Tag(name = "Recordatorios", description = "HU-030")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;
    private final ClaveProceso claveProceso;

    public RecordatorioController(RecordatorioService recordatorioService, ClaveProceso claveProceso) {
        this.recordatorioService = recordatorioService;
        this.claveProceso = claveProceso;
    }

    @PostMapping("/ejecucion")
    @SecurityRequirements
    @Operation(summary = "Enviar los recordatorios de las citas de mañana", description = "Lo invoca la ejecución diaria externa con la cabecera X-Clave-Proceso.")
    @ApiResponse(responseCode = "200", description = "Cantidad de correos enviados y fallidos")
    @ApiResponse(responseCode = "403", description = "Clave de proceso ausente o incorrecta")
    public RecordatoriosResponse ejecutar(@RequestHeader(name = "X-Clave-Proceso", required = false) String clave) {
        claveProceso.validar(clave);
        return recordatorioService.enviarDeManana();
    }
}
