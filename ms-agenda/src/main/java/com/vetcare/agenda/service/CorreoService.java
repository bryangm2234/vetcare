package com.vetcare.agenda.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Envía correos por SMTP. Si no hay SMTP configurado falla, para que el envío quede pendiente (D12).
 */
@Service
public class CorreoService {

    private final ObjectProvider<MailSender> mailSender;
    private final String remitente;

    public CorreoService(ObjectProvider<MailSender> mailSender, @Value("${vetcare.correo.remitente:}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(String destinatario, String asunto, String texto) {
        MailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("No hay un servidor SMTP configurado");
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        sender.send(mensaje);
    }
}
