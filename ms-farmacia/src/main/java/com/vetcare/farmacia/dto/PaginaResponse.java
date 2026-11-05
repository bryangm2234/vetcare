package com.vetcare.farmacia.dto;

import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    private static final int TAMANO_MAXIMO = 100;

    public static int paginaValida(int pagina) {
        return Math.max(pagina, 0);
    }

    public static int tamanoValido(int tamano) {
        return Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
    }

    public static <E, T> PaginaResponse<T> de(List<E> elementos, Function<E, T> conversor, int pagina, int tamano, long total) {
        int paginas = (int) Math.ceil((double) total / tamano);
        return new PaginaResponse<>(elementos.stream().map(conversor).toList(), pagina, tamano, total, paginas);
    }
}
