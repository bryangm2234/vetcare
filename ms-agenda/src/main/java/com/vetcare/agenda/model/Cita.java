package com.vetcare.agenda.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("citas")
public class Cita {

    @Id
    private String id;
    private TipoCita tipo = TipoCita.VETERINARIA;
    private String recursoId;
    private String mascotaId;
    private String mascotaNombre;
    private String propietarioId;
    private String propietarioCorreo;
    private LocalDate fecha;
    private LocalTime hora;
    private String motivo;
    private EstadoCita estado = EstadoCita.PROGRAMADA;
    private String claveFranja;
    private Instant horaLlegada;
    private String motivoCancelacion;
    private String observaciones;
    private boolean recordatorioEnviado;
    private boolean facturado;
    private Instant creadaEn = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TipoCita getTipo() {
        return tipo;
    }

    public void setTipo(TipoCita tipo) {
        this.tipo = tipo;
    }

    public String getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(String recursoId) {
        this.recursoId = recursoId;
    }

    public String getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getMascotaNombre() {
        return mascotaNombre;
    }

    public void setMascotaNombre(String mascotaNombre) {
        this.mascotaNombre = mascotaNombre;
    }

    public String getPropietarioId() {
        return propietarioId;
    }

    public void setPropietarioId(String propietarioId) {
        this.propietarioId = propietarioId;
    }

    public String getPropietarioCorreo() {
        return propietarioCorreo;
    }

    public void setPropietarioCorreo(String propietarioCorreo) {
        this.propietarioCorreo = propietarioCorreo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public String getClaveFranja() {
        return claveFranja;
    }

    public void setClaveFranja(String claveFranja) {
        this.claveFranja = claveFranja;
    }

    public Instant getHoraLlegada() {
        return horaLlegada;
    }

    public void setHoraLlegada(Instant horaLlegada) {
        this.horaLlegada = horaLlegada;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isRecordatorioEnviado() {
        return recordatorioEnviado;
    }

    public void setRecordatorioEnviado(boolean recordatorioEnviado) {
        this.recordatorioEnviado = recordatorioEnviado;
    }

    public boolean isFacturado() {
        return facturado;
    }

    public void setFacturado(boolean facturado) {
        this.facturado = facturado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(Instant creadaEn) {
        this.creadaEn = creadaEn;
    }
}
