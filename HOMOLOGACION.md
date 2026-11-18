# Homologacion de Modo Shake

El microservicio usa el modelo relacional aprobado de Modo Shake, consolidado en los avances 1 y 2. El esquema, los datos de prueba, los procedimientos, los triggers y la funcion del taller se aprovisionan desde database/init.sql sobre MySQL 8.4.

## Correspondencia de entidades

| Rol | Tablas del proyecto | Justificacion |
| --- | --- | --- |
| Catalogo (A) | categorias | Agrupa los productos ofrecidos. |
| Recurso (B) | productos | Representa los productos incluidos en los pedidos. |
| Transaccional (C) | pedidos, detalle_pedido, pagos | Registran fecha, estado, cantidades, precios e importes pagados. |
| Actor (D) | empleados, clientes | El empleado registra el pedido. El cliente es opcional en el modelo. La auditoria usa empleados, cuya relacion con el pedido es obligatoria. |

Tambien se conserva el esquema de roles, ingredientes, recetas (producto_ingrediente), inventario e Historial_Auditoria del proyecto aprobado.

## Casos de uso de los endpoints

Todos los reportes usan GET y responden JSON. La ruta base es http://localhost:8080/api/reportes.

| Endpoint | Caso de uso | SQL demostrado |
| --- | --- | --- |
| /trazabilidad | Consultar cada linea de pedido con el producto en mayusculas, categoria, fecha, periodo y subtotal. | INNER JOIN de cuatro tablas, UPPER, DATE_FORMAT y multiplicacion de cantidad por precio historico. |
| /metricas-categorias | Comparar categorias con actividad recurrente, definida para el taller como al menos tres pedidos distintos. | INNER JOIN de cuatro tablas, COUNT(DISTINCT), SUM, AVG, MAX, MIN, GROUP BY y HAVING con parametro ?. |
| /segmentacion-productos | Identificar la posicion y participacion de cada producto dentro de su categoria conservando cada linea original del pedido. | INNER JOIN, SUM OVER PARTITION BY, DENSE_RANK OVER (PARTITION BY id_categoria ORDER BY unidades_producto DESC). |
| /auditoria-pagos | Comparar el importe de cada pedido con los pagos registrados y clasificar su situacion de pago. | INNER JOIN con empleados e importes, LEFT JOIN para conservar pedidos sin pago y llamada a fn_clasificar_pago en el SELECT. |
| /reporte-filtrado | Consultar importes y numero de pedidos por categoria y mes con filtros opcionales combinables. | INNER JOIN multiple, funciones escalares UPPER y DATE_FORMAT, agregaciones, GROUP BY y filtros con marcadores ?. |

### Significado de las metricas

Los reportes de pedidos incluyen todos los estados salvo que el endpoint 5 reciba un filtro de estado. Los importes corresponden a productos pedidos. El cobro registrado se consulta en la auditoria.

Cada fila del endpoint 1 corresponde a un id_detalle. El endpoint 2 cuenta pedidos distintos por categoria. Su promedio, maximo y minimo corresponden a subtotales de lineas de pedido. Un pedido que contiene varias categorias participa en cada categoria correspondiente.

El endpoint 3 conserva cada id_detalle. unidadesProducto es la suma de cantidades del producto y unidadesCategoria es la suma de cantidades de la categoria. El porcentaje es 100 * unidadesProducto / unidadesCategoria, redondeado a dos decimales. La posicion y el porcentaje de un producto se repiten en sus lineas. Para sumar porcentajes se toma cada producto una sola vez. Los empates comparten posicion y DENSE_RANK no deja saltos entre posiciones.

La auditoria incluye pedidos con detalles. Se agregan por separado los detalles y los pagos para evitar multiplicar importes. Solo cuentan los pagos con estado Registrado. Se considera cero cuando el pedido carece de pagos registrados. La funcion fn_clasificar_pago recibe dos DECIMAL(20,2) y usa IF/ELSEIF para devolver PENDIENTE, PARCIAL, PAGADO, EXCEDENTE o DATOS_INVALIDOS. Un pedido con total cero y pago cero se clasifica PAGADO. El saldo es totalPedido - totalPagado, y es negativo cuando existe excedente.

### Parametros del endpoint 5

| Parametro opcional | Formato | Aplicacion |
| --- | --- | --- |
| estado | Recibido, En preparacion (con tilde en la o), Entregado o Cancelado | Coincidencia con el estado del pedido. Ejemplo codificado para el segundo estado: En%20preparaci%C3%B3n. |
| idCategoria | Entero positivo | Identificador de categorias. |
| desde | AAAA-MM-DD | Fecha inicial inclusiva. |
| hasta | AAAA-MM-DD | Fecha final inclusiva. |

Al omitir parametros se incluyen todos los registros. Cada filtro usa (? IS NULL OR columna = ?) o la comparacion de fecha correspondiente, con valores enviados mediante JdbcTemplate. El texto SQL permanece fijo. El endpoint combina funciones escalares y agregadas.

Los filtros invalidos, estados desconocidos, identificadores no positivos y rangos de fechas invertidos producen HTTP 400. Una categoria solicitada que no existe produce HTTP 404. Una consulta valida sin coincidencias devuelve HTTP 200 con []. Los fallos de acceso a la base de datos producen HTTP 500 con un mensaje general.

## Ejecucion y archivos de entrega

1. Instalar y abrir Docker Desktop.
2. Copiar .env.example como .env y configurar MYSQL_USER, MYSQL_PASSWORD y MYSQL_ROOT_PASSWORD.
3. Ejecutar docker compose up -d --build --wait desde la carpeta del proyecto.
4. Abrir http://localhost:8080/api/reportes/trazabilidad.
5. Usar requests.http para consultar los cinco reportes y ejemplos de filtros y errores.

docker-compose.yml configura api y db en la red bridge taller. Solo se publica el puerto 8080 de la API. MySQL conserva los datos en el volumen nombrado mysql_data. La API espera el healthcheck de db. El host, puerto, base, usuario y contrasena de JDBC se inyectan mediante variables de entorno.

El Dockerfile compila con Maven y JDK 21 en una etapa y ejecuta el jar con JRE 21 Alpine y usuario app en otra. El codigo usa JDBC con JdbcTemplate y separa las responsabilidades en ReporteController, ReporteService, ReporteRepository y los objetos de respuesta, dentro del paquete unico co.modoshake.api.

init.sql se ejecuta automaticamente al inicializar un volumen vacio. Los cambios posteriores en ese archivo requieren una migracion sobre una base existente. Detener los contenedores con docker compose stop conserva el volumen. Para reanudarlos se usa docker compose up -d --wait.

Se entregan src, pom.xml, Dockerfile, docker-compose.yml, .dockerignore, .gitignore, .env.example, database/init.sql, HOMOLOGACION.md y requests.http. Las credenciales locales se guardan en .env, excluido de Git.

## Preparacion de la sustentacion

En ReporteRepository.consultarSegmentacionProductos se puede explicar y modificar PARTITION BY para cambiar la agrupacion del ranking. La modificacion debe conservar la coherencia entre el grupo y las metricas comparadas.

En database/init.sql se puede explicar y modificar fn_clasificar_pago. Para cambiar una funcion instalada se ejecuta su nueva definicion en el MySQL del contenedor. Editar init.sql por si solo no actualiza el volumen existente. Despues se consulta /auditoria-pagos para observar el resultado.
## Interfaz de reportes

La interfaz se abre en http://localhost:8080/. Spring Boot sirve index.html, styles.css y app.js desde src/main/resources/static. Permite navegar por los cinco reportes, buscar en los resultados, usar paginacion, aplicar filtros y abrir la respuesta JSON. Los indicadores resumen los datos completos de la consulta seleccionada. La busqueda de texto solo filtra la tabla visible.


## Ejecucion desde NetBeans

Todas las clases estan en co.modoshake.api. La clase principal es ModoShakeApplication. Las capas conservan sus responsabilidades mediante clases separadas.

Para ejecutar desde NetBeans, iniciar Docker Desktop y ejecutar desde la carpeta del proyecto:

~~~powershell
docker compose stop api
docker compose -f docker-compose.yml -f docker-compose.netbeans.yml up -d --wait db
~~~

Abrir el proyecto en NetBeans y usar Ejecutar proyecto (F6). nbactions.xml inicia Spring Boot con el perfil netbeans, que lee las credenciales de .env y se conecta a MySQL por 127.0.0.1:3307. Abrir http://localhost:8080/. El proceso Java debe permanecer activo.

Para volver a ejecutar toda la solucion en Docker, detener primero la ejecucion de NetBeans y usar docker compose up -d --build --wait. El archivo Compose principal conserva MySQL sin puertos publicados. El override netbeans publica MySQL solo en loopback para desarrollo local.

El boton Descargar JSON guarda la respuesta completa del reporte seleccionado, incluidos los filtros aplicados a la API. La busqueda de texto de la tabla y la pagina visible no restringen la descarga. Una consulta sin coincidencias descarga []. Durante la carga o ante un error el boton permanece deshabilitado.
