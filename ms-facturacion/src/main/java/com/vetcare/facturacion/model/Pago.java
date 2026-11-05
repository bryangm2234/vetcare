package com.vetcare.facturacion.model;

import java.time.Instant;

public class Pago {

    private MedioPago medio;
    private long valor;
    private String cajaId;
    private String cajero;
    private boolean abono;
    private boolean revertido;
    private Instant fecha = Instant.now();

    public MedioPago getMedio() {
        return medio;
    }

    public void setMedio(MedioPago medio) {
        this.medio = medio;
    }

    public long getValor() {
        return valor;
    }

    public void setValor(long valor) {
        this.valor = valor;
    }

    public String getCajaId() {
        return cajaId;
    }

    public void setCajaId(String cajaId) {
        this.cajaId = cajaId;
    }

    public String getCajero() {
        return cajero;
    }

    public void setCajero(String cajero) {
        this.cajero = cajero;
    }

    public boolean isAbono() {
        return abono;
    }

    public void setAbono(boolean abono) {
        this.abono = abono;
    }

    public boolean isRevertido() {
        return revertido;
    }

    public void setRevertido(boolean revertido) {
        this.revertido = revertido;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }
}
