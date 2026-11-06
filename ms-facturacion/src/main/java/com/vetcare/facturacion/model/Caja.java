package com.vetcare.facturacion.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cajas")
public class Caja {

    @Id
    private String id;
    private String cajeroId;
    private String cajero;
    private String cajeroConCajaAbierta;
    private long base;
    private EstadoCaja estado = EstadoCaja.ABIERTA;
    private long efectivoEsperado;
    private long efectivoContado;
    private long diferencia;
    private Instant apertura = Instant.now();
    private Instant cierre;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCajeroId() {
        return cajeroId;
    }

    public void setCajeroId(String cajeroId) {
        this.cajeroId = cajeroId;
    }

    public String getCajero() {
        return cajero;
    }

    public void setCajero(String cajero) {
        this.cajero = cajero;
    }

    public String getCajeroConCajaAbierta() {
        return cajeroConCajaAbierta;
    }

    public void setCajeroConCajaAbierta(String cajeroConCajaAbierta) {
        this.cajeroConCajaAbierta = cajeroConCajaAbierta;
    }

    public long getBase() {
        return base;
    }

    public void setBase(long base) {
        this.base = base;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaja estado) {
        this.estado = estado;
    }

    public long getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public void setEfectivoEsperado(long efectivoEsperado) {
        this.efectivoEsperado = efectivoEsperado;
    }

    public long getEfectivoContado() {
        return efectivoContado;
    }

    public void setEfectivoContado(long efectivoContado) {
        this.efectivoContado = efectivoContado;
    }

    public long getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(long diferencia) {
        this.diferencia = diferencia;
    }

    public Instant getApertura() {
        return apertura;
    }

    public void setApertura(Instant apertura) {
        this.apertura = apertura;
    }

    public Instant getCierre() {
        return cierre;
    }

    public void setCierre(Instant cierre) {
        this.cierre = cierre;
    }
}
