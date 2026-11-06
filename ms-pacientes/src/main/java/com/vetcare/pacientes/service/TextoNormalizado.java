package com.vetcare.pacientes.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Permite buscar sin distinguir mayúsculas ni tildes: "gomez" encuentra "María Gómez".
 */
final class TextoNormalizado {

    private TextoNormalizado() {
    }

    static String de(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
