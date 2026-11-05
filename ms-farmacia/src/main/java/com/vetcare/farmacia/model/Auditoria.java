package com.vetcare.farmacia.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("auditoria")
public class Auditoria {

    @Id
    private String id;
    @Indexed
    private Instant fecha;
    private String usuario;
    private String accion;
    private String recurso;
    private String resultado;

    public Auditoria() {
    }

    public Auditoria(Instant fecha, String usuario, String accion, String recurso, String resultado) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.accion = accion;
        this.recurso = recurso;
        this.resultado = resultado;
    }

    public String getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getRecurso() {
        return recurso;
    }

    public String getResultado() {
        return resultado;
    }
}
