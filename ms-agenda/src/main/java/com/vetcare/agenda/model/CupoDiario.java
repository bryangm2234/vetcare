package com.vetcare.agenda.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Contador de cupos por recurso y fecha. Se actualiza de forma atómica para respetar RN-03 con peticiones simultáneas.
 */
@Document("cupos_diarios")
public class CupoDiario {

    @Id
    private String id;
    private int usados;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getUsados() {
        return usados;
    }

    public void setUsados(int usados) {
        this.usados = usados;
    }
}
