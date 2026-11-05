package com.vetcare.farmacia.model;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("lotes")
public class Lote {

    @Id
    private String id;
    private String medicamentoId;
    private String numero;
    private LocalDate vencimiento;
    private int cantidad;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(String medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getVencimiento() {
        return vencimiento;
    }

    public void setVencimiento(LocalDate vencimiento) {
        this.vencimiento = vencimiento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
