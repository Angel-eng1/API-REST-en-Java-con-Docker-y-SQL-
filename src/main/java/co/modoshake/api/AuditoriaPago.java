package co.modoshake.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditoriaPago(
        int idPedido,
        LocalDate fecha,
        String estadoPedido,
        String empleado,
        BigDecimal totalPedido,
        BigDecimal totalPagado,
        BigDecimal saldo,
        String clasificacion) {
}