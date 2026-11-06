package com.vetcare.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PagoRequest(@NotEmpty List<@Valid ItemPagoRequest> pagos) {
}
