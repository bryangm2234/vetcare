package com.vetcare.farmacia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RecepcionRequest(@NotEmpty List<@Valid ItemRecepcionRequest> items) {
}
