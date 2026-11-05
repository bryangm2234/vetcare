package com.vetcare.historia.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("formulas")
public class Formula {

    @Id
    private String id;
    private String consultaId;
    private String mascotaId;
    private String veterinarioId;
    private List<ItemFormula> items;
    private LocalDate vigenteHasta;
    private EstadoFormula estado = EstadoFormula.VIGENTE;
    private String motivoAnulacion;
    private Instant fecha = Instant.now();
    @Version
    private Long version;

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

    public String getVeterinarioId() {
        return veterinarioId;
    }

    public void setVeterinarioId(String veterinarioId) {
        this.veterinarioId = veterinarioId;
    }

    public List<ItemFormula> getItems() {
        return items;
    }

    public void setItems(List<ItemFormula> items) {
        this.items = items;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(LocalDate vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public EstadoFormula getEstado() {
        return estado;
    }

    public void setEstado(EstadoFormula estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
