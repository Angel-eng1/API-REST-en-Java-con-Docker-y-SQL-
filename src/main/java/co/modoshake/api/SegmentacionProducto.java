package co.modoshake.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SegmentacionProducto(
        int idDetalle,
        int idPedido,
        LocalDate fecha,
        int idProducto,
        String producto,
        int idCategoria,
        String categoria,
        int cantidad,
        long unidadesProducto,
        long unidadesCategoria,
        long rankingCategoria,
        BigDecimal porcentajeProducto) {
}