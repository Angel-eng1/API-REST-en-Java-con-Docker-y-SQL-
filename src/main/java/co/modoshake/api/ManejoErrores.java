package co.modoshake.api;

import java.util.Map;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejoErrores {
    private static final Logger LOG = LoggerFactory.getLogger(ManejoErrores.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> manejarErrorBaseDatos(DataAccessException error) {
        LOG.error("Fallo al consultar el reporte", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensaje", "No se pudo consultar el reporte."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarFiltroInvalido(ResponseStatusException error) {
        String mensaje = error.getReason() == null ? "Solicitud no valida." : error.getReason();
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarFormatoInvalido(
            MethodArgumentTypeMismatchException error) {
        return ResponseEntity.badRequest().body(Map.of(
                "mensaje", "Formato no valido para el parametro " + error.getName()
                        + ". Usa fechas AAAA-MM-DD e identificadores enteros."));
    }
}
