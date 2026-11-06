package com.vetcare.farmacia.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarcaFacturadoRequest(@NotEmpty List<String> ids, boolean facturado) {
}
