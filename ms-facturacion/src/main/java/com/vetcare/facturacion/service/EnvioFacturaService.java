package com.vetcare.facturacion.service;

import com.vetcare.facturacion.exception.ReglaNegocioException;
import com.vetcare.facturacion.exception.ServicioExternoException;
import com.vetcare.facturacion.model.Factura;
import com.vetcare.facturacion.repository.FacturaRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envía el PDF de la factura por SMTP; una falla del correo no altera la factura (D12).
 */
@Service
public class EnvioFacturaService {

    private static final Logger log = LoggerFactory.getLogger(EnvioFacturaService.class);

    private final FacturaService facturaService;
    private final FacturaRepository facturaRepository;
    private final DocumentoFacturaService documentoService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remitente;
    private final AuditoriaService auditoriaService;

    public EnvioFacturaService(FacturaService facturaService, FacturaRepository facturaRepository, DocumentoFacturaService documentoService,
                               ObjectProvider<JavaMailSender> mailSender, @Value("${vetcare.correo.remitente:}") String remitente,
                               AuditoriaService auditoriaService) {
        this.facturaService = facturaService;
        this.facturaRepository = facturaRepository;
        this.documentoService = documentoService;
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.auditoriaService = auditoriaService;
    }

    public Factura enviar(String id) {
        Factura factura = facturaService.buscar(id);
        if (factura.getPropietarioCorreo() == null || factura.getPropietarioCorreo().isBlank()) {
            throw new ReglaNegocioException("El propietario no tiene correo registrado");
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new ServicioExternoException("No hay un servidor de correo configurado");
        }
        try {
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, "UTF-8");
            ayudante.setFrom(remitente);
            ayudante.setTo(factura.getPropietarioCorreo());
            ayudante.setSubject("Factura " + factura.getNumero() + " de VetCare");
            ayudante.setText("Adjuntamos la factura " + factura.getNumero() + ".");
            ayudante.addAttachment(factura.getNumero() + ".pdf", new ByteArrayResource(documentoService.pdf(factura)), "application/pdf");
            sender.send(mensaje);
        } catch (MailException | MessagingException ex) {
            log.error("No se pudo enviar la factura {}: {}", factura.getNumero(), ex.getMessage());
            throw new ServicioExternoException("El servicio de correo no respondió");
        }
        factura.setFechaEnvioCorreo(Instant.now());
        Factura guardada = facturaRepository.save(factura);
        auditoriaService.registrar("ENVIAR_FACTURA", "facturas/" + id, "EXITOSO");
        return guardada;
    }
}
