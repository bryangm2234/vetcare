package com.vetcare.agenda.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("horarios")
public class Horario {

    @Id
    private String id;
    private TipoCita tipo;
    private String nombre;
    private List<DayOfWeek> dias;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionMinutos;
    private int cuposDiarios;

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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<DayOfWeek> getDias() {
        return dias;
    }

    public void setDias(List<DayOfWeek> dias) {
        this.dias = dias;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public int getCuposDiarios() {
        return cuposDiarios;
    }

    public void setCuposDiarios(int cuposDiarios) {
        this.cuposDiarios = cuposDiarios;
    }
}
