package com.vetcare.farmacia.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ordenes_compra")
public class OrdenCompra {

    @Id
    private String id;
    private String proveedorId;
    private List<ItemOrdenCompra> items;
    private EstadoOrdenCompra estado = EstadoOrdenCompra.ENVIADA;
    private String creadaPor;
    private Instant fecha = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(String proveedorId) {
        this.proveedorId = proveedorId;
    }

    public List<ItemOrdenCompra> getItems() {
        return items;
    }

    public void setItems(List<ItemOrdenCompra> items) {
        this.items = items;
    }

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompra estado) {
        this.estado = estado;
    }

    public String getCreadaPor() {
        return creadaPor;
    }

    public void setCreadaPor(String creadaPor) {
        this.creadaPor = creadaPor;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }
}
