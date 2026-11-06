package com.vetcare.farmacia.dto;

public record AlertaStockResponse(String medicamentoId, String codigo, String nombre, long stockDisponible, int stockMinimo) {
}
