package com.vetcare.historia.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ordenes_laboratorio")
public class OrdenLaboratorio {

    @Id
    private String id;
    private String consultaId;
    private String mascotaId;
    private String examen;
    private String indicaciones;
    private EstadoOrdenLaboratorio estado = EstadoOrdenLaboratorio.PENDIENTE;
    private String resultado;
    private String responsable;
    private Instant fechaOrden = Instant.now();
    private Instant fechaResultado;
    private boolean facturado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConsultaId() {
        return consultaId;
    }

    public void setConsultaId(String consultaId) {
        this.consultaId = consultaId;
    }

    public String getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getExamen() {
        return examen;
    }

    public void setExamen(String examen) {
        this.examen = examen;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }

    public EstadoOrdenLaboratorio getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenLaboratorio estado) {
        this.estado = estado;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getResponsable() {
        return responsable;
    }

    public void setResponsable(String responsable) {
        this.responsable = responsable;
    }

    public Instant getFechaOrden() {
        return fechaOrden;
    }

    public void setFechaOrden(Instant fechaOrden) {
        this.fechaOrden = fechaOrden;
    }

    public Instant getFechaResultado() {
        return fechaResultado;
    }

    public void setFechaResultado(Instant fechaResultado) {
        this.fechaResultado = fechaResultado;
    }

    public boolean isFacturado() {
        return facturado;
    }

    public void setFacturado(boolean facturado) {
        this.facturado = facturado;
    }
}
