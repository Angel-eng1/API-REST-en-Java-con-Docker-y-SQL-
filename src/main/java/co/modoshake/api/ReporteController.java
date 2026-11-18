package co.modoshake.api;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/trazabilidad")
    public List<DetallePedido> consultarDetallePedidos() {
        return servicio.consultarDetallePedidos();
    }

    @GetMapping("/metricas-categorias")
    public List<MetricaCategoria> consultarMetricasCategorias() {
        return servicio.consultarMetricasCategorias();
    }

    @GetMapping("/segmentacion-productos")
    public List<SegmentacionProducto> consultarSegmentacionProductos() {
        return servicio.consultarSegmentacionProductos();
    }

    @GetMapping("/auditoria-pagos")
    public List<AuditoriaPago> consultarAuditoriaPagos() {
        return servicio.consultarAuditoriaPagos();
    }

    @GetMapping("/reporte-filtrado")
    public List<ReporteFiltrado> consultarReporteFiltrado(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "idCategoria", required = false) Integer idCategoria,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return servicio.consultarReporteFiltrado(estado, idCategoria, desde, hasta);
    }
}
