package co.modoshake.api;

import java.math.BigDecimal;

public record MetricaCategoria(
        int idCategoria,
        String categoria,
        long totalPedidos,
        BigDecimal totalImporte,
        BigDecimal promedioSubtotal,
        BigDecimal mayorSubtotal,
        BigDecimal menorSubtotal) {
}