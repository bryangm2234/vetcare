package com.vetcare.farmacia.model;

public class ItemOrdenCompra {

    private String medicamentoId;
    private int cantidad;
    private int recibida;

    public String getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(String medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getRecibida() {
        return recibida;
    }

    public void setRecibida(int recibida) {
        this.recibida = recibida;
    }
}
