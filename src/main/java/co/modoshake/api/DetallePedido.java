package co.modoshake.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetallePedido(
        int idDetalle,
        int idPedido,
        String producto,
        String categoria,
        LocalDate fecha,
        String periodo,
        String estado,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {
}