package com.vetcare.historia.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.vetcare.historia.dto.AdjuntoResponse;
import com.vetcare.historia.exception.ArchivoNoAdmitidoException;
import com.vetcare.historia.exception.RecursoNoEncontradoException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adjuntos de la historia clínica en GridFS (D13).
 */
@Service
public class AdjuntoService {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final GridFsTemplate gridFsTemplate;
    private final AuditoriaService auditoriaService;

    public AdjuntoService(GridFsTemplate gridFsTemplate, AuditoriaService auditoriaService) {
        this.gridFsTemplate = gridFsTemplate;
        this.auditoriaService = auditoriaService;
    }

    public AdjuntoResponse adjuntar(String mascotaId, MultipartFile archivo) throws IOException {
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ArchivoNoAdmitidoException(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera los 5 MB");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !(tipo.equals("application/pdf") || tipo.startsWith("image/"))) {
            throw new ArchivoNoAdmitidoException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo se admiten archivos PDF e imágenes");
        }
        ObjectId id;
        try (InputStream contenido = archivo.getInputStream()) {
            id = gridFsTemplate.store(contenido, archivo.getOriginalFilename(), tipo, new Document("mascotaId", mascotaId));
        }
        auditoriaService.registrar("ADJUNTAR_ARCHIVO", "adjuntos/" + id.toHexString(), "EXITOSO");
        return respuesta(gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id))));
    }

    public List<AdjuntoResponse> deMascota(String mascotaId) {
        List<AdjuntoResponse> adjuntos = new ArrayList<>();
        gridFsTemplate.find(new Query(Criteria.where("metadata.mascotaId").is(mascotaId)).with(Sort.by(Sort.Direction.DESC, "uploadDate")))
                .forEach(archivo -> adjuntos.add(respuesta(archivo)));
        return adjuntos;
    }

    public GridFsResource descargar(String id) {
        if (!ObjectId.isValid(id)) {
            throw new RecursoNoEncontradoException("El adjunto no existe");
        }
        GridFSFile archivo = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(id))));
        if (archivo == null) {
            throw new RecursoNoEncontradoException("El adjunto no existe");
        }
        return gridFsTemplate.getResource(archivo);
    }

    private AdjuntoResponse respuesta(GridFSFile archivo) {
        Document metadata = archivo.getMetadata();
        String tipo = metadata != null ? metadata.getString("_contentType") : null;
        return new AdjuntoResponse(archivo.getObjectId().toHexString(), archivo.getFilename(), tipo, archivo.getLength(),
                archivo.getUploadDate().toInstant());
    }
}
