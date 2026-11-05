package com.vetcare.historia.controller;

import com.vetcare.historia.dto.AdjuntoResponse;
import com.vetcare.historia.dto.HistoriaResponse;
import com.vetcare.historia.security.Permisos;
import com.vetcare.historia.service.AdjuntoService;
import com.vetcare.historia.service.HistoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Historia clínica", description = "HU-034 y HU-040")
public class HistoriaController {

    private final HistoriaService historiaService;
    private final AdjuntoService adjuntoService;

    public HistoriaController(HistoriaService historiaService, AdjuntoService adjuntoService) {
        this.historiaService = historiaService;
        this.adjuntoService = adjuntoService;
    }

    @GetMapping("/api/historias/{mascotaId}")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Consultar la historia clínica completa de una mascota")
    @ApiResponse(responseCode = "200", description = "Historia; las listas pueden estar vacías")
    @ApiResponse(responseCode = "403", description = "El usuario no es veterinario")
    @ApiResponse(responseCode = "404", description = "La mascota no existe")
    public HistoriaResponse obtener(@PathVariable String mascotaId) {
        return historiaService.obtener(mascotaId);
    }

    @PostMapping(path = "/api/historias/{mascotaId}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Adjuntar un PDF o una imagen de hasta 5 MB")
    @ApiResponse(responseCode = "201", description = "Archivo adjuntado")
    @ApiResponse(responseCode = "413", description = "El archivo supera los 5 MB")
    @ApiResponse(responseCode = "415", description = "El archivo no es PDF ni imagen")
    public AdjuntoResponse adjuntar(@PathVariable String mascotaId, @RequestParam("archivo") MultipartFile archivo) throws IOException {
        return adjuntoService.adjuntar(mascotaId, archivo);
    }

    @GetMapping("/api/adjuntos/{id}")
    @PreAuthorize(Permisos.VETERINARIO)
    @Operation(summary = "Descargar un adjunto")
    @ApiResponse(responseCode = "200", description = "Contenido del archivo")
    @ApiResponse(responseCode = "404", description = "El adjunto no existe")
    public ResponseEntity<InputStreamResource> descargar(@PathVariable String id) throws IOException {
        GridFsResource recurso = adjuntoService.descargar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recurso.getContentType()))
                .contentLength(recurso.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(recurso.getFilename()).build().toString())
                .body(new InputStreamResource(recurso.getInputStream()));
    }
}
