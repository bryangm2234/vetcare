package com.vetcare.facturacion.dto;

import java.util.Map;

public record IngresosResponse(long total, Map<String, Long> porServicio, Map<String, Long> porMedioPago) {
}
