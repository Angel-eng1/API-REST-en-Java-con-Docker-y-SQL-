package co.modoshake.api;

import java.util.List;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

@Service
public class ReporteService {
    private final ReporteRepository repositorio;

    public ReporteService(ReporteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<DetallePedido> consultarDetallePedidos() {
        return repositorio.consultarDetallePedidos();
    }

    public List<MetricaCategoria> consultarMetricasCategorias() {
        return repositorio.consultarMetricasCategorias();
    }

    public List<SegmentacionProducto> consultarSegmentacionProductos() {
        return repositorio.consultarSegmentacionProductos();
    }

    public List<AuditoriaPago> consultarAuditoriaPagos() {
        return repositorio.consultarAuditoriaPagos();
    }

    public List<ReporteFiltrado> consultarReporteFiltrado(
            String estado, Integer idCategoria, LocalDate desde, LocalDate hasta) {
        if (estado != null && !List.of(
                "Recibido", "En preparación", "Entregado", "Cancelado").contains(estado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Estado de pedido no valido.");
        }
        if (idCategoria != null && idCategoria <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "idCategoria debe ser un entero positivo.");
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "desde debe ser anterior o igual a hasta.");
        }
        if (idCategoria != null && !repositorio.existeCategoria(idCategoria)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "La categoria indicada no existe.");
        }
        return repositorio.consultarReporteFiltrado(estado, idCategoria, desde, hasta);
    }
}
