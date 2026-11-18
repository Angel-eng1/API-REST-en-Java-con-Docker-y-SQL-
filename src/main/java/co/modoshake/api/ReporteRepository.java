package co.modoshake.api;

import java.util.List;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepository {
    private final JdbcTemplate jdbc;

    public ReporteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DetallePedido> consultarDetallePedidos() {
        String sql = """
                SELECT d.id_detalle, p.id_pedido,
                       UPPER(pr.nombre) AS producto,
                       c.nombre AS categoria,
                       p.fecha,
                       DATE_FORMAT(p.fecha, '%Y-%m') AS periodo,
                       p.estado, d.cantidad, d.precio_unitario,
                       d.cantidad * d.precio_unitario AS subtotal
                FROM detalle_pedido d
                INNER JOIN pedidos p ON p.id_pedido = d.id_pedido
                INNER JOIN productos pr ON pr.id_producto = d.id_producto
                INNER JOIN categorias c ON c.id_categoria = pr.id_categoria
                ORDER BY p.fecha, p.id_pedido, d.id_detalle
                """;
        return jdbc.query(sql, (fila, numero) -> new DetallePedido(
                fila.getInt("id_detalle"),
                fila.getInt("id_pedido"),
                fila.getString("producto"),
                fila.getString("categoria"),
                fila.getDate("fecha").toLocalDate(),
                fila.getString("periodo"),
                fila.getString("estado"),
                fila.getInt("cantidad"),
                fila.getBigDecimal("precio_unitario"),
                fila.getBigDecimal("subtotal")));
    }

    public List<MetricaCategoria> consultarMetricasCategorias() {
        String sql = """
                SELECT c.id_categoria, c.nombre AS categoria,
                       COUNT(DISTINCT p.id_pedido) AS total_pedidos,
                       SUM(d.cantidad * d.precio_unitario) AS total_importe,
                       AVG(d.cantidad * d.precio_unitario) AS promedio_subtotal,
                       MAX(d.cantidad * d.precio_unitario) AS mayor_subtotal,
                       MIN(d.cantidad * d.precio_unitario) AS menor_subtotal
                FROM categorias c
                INNER JOIN productos pr ON pr.id_categoria = c.id_categoria
                INNER JOIN detalle_pedido d ON d.id_producto = pr.id_producto
                INNER JOIN pedidos p ON p.id_pedido = d.id_pedido
                GROUP BY c.id_categoria, c.nombre
                HAVING COUNT(DISTINCT p.id_pedido) >= ?
                ORDER BY total_importe DESC, c.id_categoria
                """;
        // Un minimo de tres pedidos permite comparar categorias con actividad recurrente.
        return jdbc.query(sql, (fila, numero) -> new MetricaCategoria(
                fila.getInt("id_categoria"),
                fila.getString("categoria"),
                fila.getLong("total_pedidos"),
                fila.getBigDecimal("total_importe"),
                fila.getBigDecimal("promedio_subtotal"),
                fila.getBigDecimal("mayor_subtotal"),
                fila.getBigDecimal("menor_subtotal")), 3);
    }

    public List<SegmentacionProducto> consultarSegmentacionProductos() {
        String sql = """
                WITH detalle_con_totales AS (
                    SELECT d.id_detalle, p.id_pedido, p.fecha,
                           pr.id_producto, pr.nombre AS producto,
                           c.id_categoria, c.nombre AS categoria, d.cantidad,
                           SUM(d.cantidad) OVER (
                               PARTITION BY pr.id_producto
                           ) AS unidades_producto,
                           SUM(d.cantidad) OVER (
                               PARTITION BY c.id_categoria
                           ) AS unidades_categoria
                    FROM detalle_pedido d
                    INNER JOIN pedidos p ON p.id_pedido = d.id_pedido
                    INNER JOIN productos pr ON pr.id_producto = d.id_producto
                    INNER JOIN categorias c ON c.id_categoria = pr.id_categoria
                )
                SELECT id_detalle, id_pedido, fecha, id_producto, producto,
                       id_categoria, categoria, cantidad,
                       unidades_producto, unidades_categoria,
                       DENSE_RANK() OVER (
                           PARTITION BY id_categoria
                           ORDER BY unidades_producto DESC
                       ) AS ranking_categoria,
                       ROUND(100.0 * unidades_producto /
                             NULLIF(unidades_categoria, 0), 2) AS porcentaje_producto
                FROM detalle_con_totales
                ORDER BY id_categoria, ranking_categoria, id_producto, fecha, id_detalle
                """;
        return jdbc.query(sql, (fila, numero) -> new SegmentacionProducto(
                fila.getInt("id_detalle"),
                fila.getInt("id_pedido"),
                fila.getDate("fecha").toLocalDate(),
                fila.getInt("id_producto"),
                fila.getString("producto"),
                fila.getInt("id_categoria"),
                fila.getString("categoria"),
                fila.getInt("cantidad"),
                fila.getLong("unidades_producto"),
                fila.getLong("unidades_categoria"),
                fila.getLong("ranking_categoria"),
                fila.getBigDecimal("porcentaje_producto")));
    }

    public List<AuditoriaPago> consultarAuditoriaPagos() {
        String sql = """
                WITH importes AS (
                    SELECT id_pedido,
                           SUM(cantidad * precio_unitario) AS total_pedido
                    FROM detalle_pedido
                    GROUP BY id_pedido
                ), pagos_registrados AS (
                    SELECT id_pedido, SUM(monto) AS total_pagado
                    FROM pagos
                    WHERE estado = 'Registrado'
                    GROUP BY id_pedido
                )
                SELECT p.id_pedido, p.fecha, p.estado AS estado_pedido,
                       CONCAT(e.nombre, ' ', e.apellido) AS empleado,
                       i.total_pedido,
                       COALESCE(pg.total_pagado, 0) AS total_pagado,
                       i.total_pedido - COALESCE(pg.total_pagado, 0) AS saldo,
                       fn_clasificar_pago(
                           i.total_pedido, COALESCE(pg.total_pagado, 0)
                       ) AS clasificacion
                FROM pedidos p
                INNER JOIN empleados e ON e.id_empleado = p.id_empleado
                INNER JOIN importes i ON i.id_pedido = p.id_pedido
                LEFT JOIN pagos_registrados pg ON pg.id_pedido = p.id_pedido
                ORDER BY p.fecha, p.id_pedido
                """;
        return jdbc.query(sql, (fila, numero) -> new AuditoriaPago(
                fila.getInt("id_pedido"),
                fila.getDate("fecha").toLocalDate(),
                fila.getString("estado_pedido"),
                fila.getString("empleado"),
                fila.getBigDecimal("total_pedido"),
                fila.getBigDecimal("total_pagado"),
                fila.getBigDecimal("saldo"),
                fila.getString("clasificacion")));
    }

    public boolean existeCategoria(int idCategoria) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM categorias WHERE id_categoria = ?",
                Integer.class, idCategoria);
        return cantidad != null && cantidad > 0;
    }

    public List<ReporteFiltrado> consultarReporteFiltrado(
            String estado, Integer idCategoria, LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT DATE_FORMAT(p.fecha, '%Y-%m') AS periodo,
                       c.id_categoria, UPPER(c.nombre) AS categoria,
                       COUNT(DISTINCT p.id_pedido) AS total_pedidos,
                       SUM(d.cantidad * d.precio_unitario) AS total_importe,
                       AVG(d.cantidad * d.precio_unitario) AS promedio_subtotal,
                       MAX(d.cantidad * d.precio_unitario) AS mayor_subtotal,
                       MIN(d.cantidad * d.precio_unitario) AS menor_subtotal
                FROM pedidos p
                INNER JOIN detalle_pedido d ON d.id_pedido = p.id_pedido
                INNER JOIN productos pr ON pr.id_producto = d.id_producto
                INNER JOIN categorias c ON c.id_categoria = pr.id_categoria
                WHERE (? IS NULL OR p.estado = ?)
                  AND (? IS NULL OR c.id_categoria = ?)
                  AND (? IS NULL OR p.fecha >= ?)
                  AND (? IS NULL OR p.fecha <= ?)
                GROUP BY DATE_FORMAT(p.fecha, '%Y-%m'), c.id_categoria, c.nombre
                ORDER BY periodo, total_importe DESC, c.id_categoria
                """;
        java.sql.Date fechaDesde = desde == null ? null : java.sql.Date.valueOf(desde);
        java.sql.Date fechaHasta = hasta == null ? null : java.sql.Date.valueOf(hasta);
        return jdbc.query(sql, (fila, numero) -> new ReporteFiltrado(
                fila.getString("periodo"),
                fila.getInt("id_categoria"),
                fila.getString("categoria"),
                fila.getLong("total_pedidos"),
                fila.getBigDecimal("total_importe"),
                fila.getBigDecimal("promedio_subtotal"),
                fila.getBigDecimal("mayor_subtotal"),
                fila.getBigDecimal("menor_subtotal")),
                estado, estado, idCategoria, idCategoria,
                fechaDesde, fechaDesde, fechaHasta, fechaHasta);
    }
}
