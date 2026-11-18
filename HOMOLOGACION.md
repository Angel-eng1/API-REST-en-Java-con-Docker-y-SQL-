# Homologación de Modo Shake

El microservicio utiliza el modelo relacional aprobado del proyecto Modo Shake, consolidado en los avances 1 y 2. Las entidades se corresponden con los roles estructurales definidos en el taller.

## Correspondencia de entidades

| Rol del taller | Tablas de Modo Shake | Función en el negocio |
| --- | --- | --- |
| Entidad Catálogo (A) | `categorias` | Clasifica los productos ofrecidos por el restaurante. |
| Entidad Recurso / Ítem (B) | `productos` | Representa los productos que se incluyen en los pedidos. |
| Entidad Transaccional (C) | `pedidos`, `detalle_pedido`, `pagos` | Registra las operaciones con sus fechas, estados, cantidades, precios e importes pagados. |
| Entidad Actor (D) | `empleados`, `clientes` | Identifica al empleado que registra el pedido y al cliente asociado. El cliente es opcional en el modelo. La auditoría utiliza la relación obligatoria con el empleado. |

Las tablas y relaciones están definidas en [database/init.sql](database/init.sql).

## Casos de uso de los cinco endpoints

Todos los endpoints utilizan el método `GET` y devuelven JSON. Las consultas se implementan en [ReporteRepository.java](src/main/java/co/modoshake/api/ReporteRepository.java).

| Endpoint | Caso de uso de negocio | Conceptos SQL aplicados |
| --- | --- | --- |
| `/api/reportes/trazabilidad` | Consultar cada línea del pedido con su producto, categoría, fecha, cantidad, precio histórico y subtotal. El nombre se normaliza a mayúsculas y la fecha se expresa también como año-mes. | `INNER JOIN` entre detalles, pedidos, productos y categorías. Funciones escalares `UPPER` y `DATE_FORMAT`, y cálculo unitario de cantidad por precio. |
| `/api/reportes/metricas-categorias` | Comparar la actividad de las categorías que tienen al menos tres pedidos distintos. Presenta el importe total y el promedio, máximo y mínimo de los subtotales de sus líneas. | `INNER JOIN` de cuatro tablas, `COUNT(DISTINCT)`, `SUM`, `AVG`, `MAX`, `MIN`, `GROUP BY` y `HAVING`. |
| `/api/reportes/segmentacion-productos` | Identificar la posición de cada producto por unidades pedidas dentro de su categoría y su porcentaje de participación. Conserva cada línea de pedido y asigna el mismo puesto a productos empatados. | `INNER JOIN`, `SUM(...) OVER (PARTITION BY ...)` y `DENSE_RANK() OVER (PARTITION BY id_categoria ORDER BY unidades_producto DESC)`. |
| `/api/reportes/auditoria-pagos` | Comparar el importe de cada pedido con sus pagos registrados y clasificarlo como pendiente, parcial, pagado o con excedente. Los pedidos sin pagos registrados se evalúan con un importe pagado de cero. | `INNER JOIN` con empleados e importes del pedido, `LEFT JOIN` con pagos agregados y llamada a la función persistida `fn_clasificar_pago` dentro del `SELECT`. La función recibe dos valores numéricos y aplica condicionales `IF/ELSEIF`. |
| `/api/reportes/reporte-filtrado` | Consultar el número de pedidos y sus importes por categoría y mes, combinando filtros opcionales de estado, identificador de categoría y fechas inicial y final. | `INNER JOIN` múltiple, funciones escalares `UPPER` y `DATE_FORMAT`, agregaciones, `GROUP BY` y filtros con valores parametrizados mediante marcadores JDBC `?`. |

Los reportes incluyen todos los estados del pedido, excepto cuando el quinto endpoint recibe un filtro de estado. Los importes de los productos pedidos y los pagos registrados se calculan por separado.

En el tercer endpoint, el ranking, las unidades acumuladas y la participación de cada producto se repiten en sus líneas para conservar la granularidad del reporte. El porcentaje de un producto se calcula una sola vez respecto al total de su categoría y se muestra en cada línea correspondiente.
