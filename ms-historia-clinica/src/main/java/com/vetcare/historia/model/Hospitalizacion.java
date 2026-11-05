package com.vetcare.historia.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("hospitalizaciones")
public class Hospitalizacion {

    @Id
    private String id;
    private String mascotaId;
    private String veterinarioId;
    private String motivo;
    private String cubiculo;
    private String cubiculoOcupado;
    private String mascotaInternada;
    private EstadoHospitalizacion estado = EstadoHospitalizacion.ACTIVA;
    private List<Evolucion> evoluciones = new ArrayList<>();
    private String indicacionesAlta;
    private Instant fechaIngreso = Instant.now();
    private Instant fechaAlta;
    private long diasEstancia;
    private boolean facturado;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMascotaId() {
        return mascotaId;
    }

    public void setMascotaId(String mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getVeterinarioId() {
        return veterinarioId;
    }

    public void setVeterinarioId(String veterinarioId) {
        this.veterinarioId = veterinarioId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getCubiculo() {
        return cubiculo;
    }

    public void setCubiculo(String cubiculo) {
        this.cubiculo = cubiculo;
    }

    public String getCubiculoOcupado() {
        return cubiculoOcupado;
    }

    public void setCubiculoOcupado(String cubiculoOcupado) {
        this.cubiculoOcupado = cubiculoOcupado;
    }

    public String getMascotaInternada() {
        return mascotaInternada;
    }

    public void setMascotaInternada(String mascotaInternada) {
        this.mascotaInternada = mascotaInternada;
    }

    public EstadoHospitalizacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoHospitalizacion estado) {
        this.estado = estado;
    }

    public List<Evolucion> getEvoluciones() {
        return evoluciones;
    }

    public void setEvoluciones(List<Evolucion> evoluciones) {
        this.evoluciones = evoluciones;
    }

    public String getIndicacionesAlta() {
        return indicacionesAlta;
    }

    public void setIndicacionesAlta(String indicacionesAlta) {
        this.indicacionesAlta = indicacionesAlta;
    }

    public Instant getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Instant fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Instant getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(Instant fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public long getDiasEstancia() {
        return diasEstancia;
    }

    public void setDiasEstancia(long diasEstancia) {
        this.diasEstancia = diasEstancia;
    }

    public boolean isFacturado() {
        return facturado;
    }

    public void setFacturado(boolean facturado) {
        this.facturado = facturado;
    }
}
