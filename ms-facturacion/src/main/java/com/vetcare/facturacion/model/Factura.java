package com.vetcare.facturacion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("facturas")
public class Factura {

    @Id
    private String id;
    private String numero;
    private String mascotaId;
    private String mascotaNombre;
    private String propietarioId;
    private String propietarioCorreo;
    private List<ItemFactura> items;
    private long subtotal;
    private long descuento;
    private int tasaImpuesto;
    private long impuesto;
    private long total;
    private long saldo;
    private EstadoFactura estado;
    private List<Pago> pagos = new ArrayList<>();
    private String motivoAnulacion;
    private String cajero;
    private Instant fecha = Instant.now();
    private Instant fechaEnvioCorreo;
    @Version
    @JsonIgnore
    private Long version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
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

    public List<ItemFactura> getItems() {
        return items;
    }

    public void setItems(List<ItemFactura> items) {
        this.items = items;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(long subtotal) {
        this.subtotal = subtotal;
    }

    public long getDescuento() {
        return descuento;
    }

    public void setDescuento(long descuento) {
        this.descuento = descuento;
    }

    public int getTasaImpuesto() {
        return tasaImpuesto;
    }

    public void setTasaImpuesto(int tasaImpuesto) {
        this.tasaImpuesto = tasaImpuesto;
    }

    public long getImpuesto() {
        return impuesto;
    }

    public void setImpuesto(long impuesto) {
        this.impuesto = impuesto;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getSaldo() {
        return saldo;
    }

    public void setSaldo(long saldo) {
        this.saldo = saldo;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }

    public List<Pago> getPagos() {
        return pagos;
    }

    public void setPagos(List<Pago> pagos) {
        this.pagos = pagos;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public String getCajero() {
        return cajero;
    }

    public void setCajero(String cajero) {
        this.cajero = cajero;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public Instant getFechaEnvioCorreo() {
        return fechaEnvioCorreo;
    }

    public void setFechaEnvioCorreo(Instant fechaEnvioCorreo) {
        this.fechaEnvioCorreo = fechaEnvioCorreo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
