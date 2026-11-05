package com.vetcare.agenda.service;

import com.vetcare.agenda.dto.RecordatoriosResponse;
import com.vetcare.agenda.model.Cita;
import com.vetcare.agenda.model.EstadoCita;
import com.vetcare.agenda.repository.CitaRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecordatorioService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioService.class);

    private final CitaRepository citaRepository;
    private final CorreoService correoService;

    public RecordatorioService(CitaRepository citaRepository, CorreoService correoService) {
        this.citaRepository = citaRepository;
        this.correoService = correoService;
    }

    public RecordatoriosResponse enviarDeManana() {
        LocalDate manana = LocalDate.now().plusDays(1);
        int enviados = 0;
        int fallidos = 0;
        for (Cita cita : citaRepository.findByFechaAndEstadoAndRecordatorioEnviadoFalse(manana, EstadoCita.PROGRAMADA)) {
            if (cita.getPropietarioCorreo() == null || cita.getPropietarioCorreo().isBlank()) {
                continue;
            }
            try {
                correoService.enviar(cita.getPropietarioCorreo(), "Recordatorio de cita en VetCare",
                        "Le recordamos la cita de " + cita.getMascotaNombre() + " el " + cita.getFecha() + " a las " + cita.getHora() + ".");
                cita.setRecordatorioEnviado(true);
                citaRepository.save(cita);
                enviados++;
            } catch (RuntimeException ex) {
                log.error("No se pudo enviar el recordatorio de la cita {}: {}", cita.getId(), ex.getMessage());
                fallidos++;
            }
        }
        return new RecordatoriosResponse(enviados, fallidos);
    }
}
