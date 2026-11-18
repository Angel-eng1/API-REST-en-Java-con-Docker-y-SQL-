# Modo Shake · API REST con Java, MySQL y Docker

Microservicio de reportes sobre el modelo relacional del proyecto Modo Shake. Implementa cinco endpoints GET con SQL nativo y una interfaz web para consultar resultados, aplicar filtros y descargar las respuestas en JSON.

## Tecnologías

- Java 21 y Spring Boot 3.5.16.
- Maven y JDBC mediante `JdbcTemplate`.
- MySQL 8.4.
- Docker y Docker Compose.
- HTML, CSS y JavaScript para la interfaz.

## Ejecutar con Docker

Se requieren Git y Docker Desktop con el motor en ejecución. Los siguientes comandos se ejecutan en PowerShell.

1. Clonar el repositorio y entrar en su carpeta:

   ```powershell
   git clone https://github.com/Angel-eng1/API-REST-en-Java-con-Docker-y-SQL-.git modo-shake-api
   cd modo-shake-api
   ```

2. Crear la configuración local:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Definir `MYSQL_USER`, `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD`. El archivo [.env.example](.env.example) contiene los nombres de las variables. Las credenciales locales de `.env` están excluidas de Git.

3. Compilar e iniciar la solución:

   ```powershell
   docker compose up -d --build --wait
   ```

4. Abrir el [panel de reportes](http://localhost:8080/).

Los enlaces a `localhost:8080` funcionan en el equipo donde se está ejecutando la aplicación. Para acceder desde GitHub es necesario iniciar primero la solución local.

Docker Compose inicia la API y MySQL en una red bridge. La API espera a que MySQL esté saludable. El despliegue principal publica el puerto 8080 de la API y conserva MySQL dentro de la red de contenedores.

## Interfaz web

El panel organiza los cinco reportes mediante un menú de navegación. Cada reporte incluye indicadores de resumen, tabla de resultados, búsqueda de texto y paginación. El quinto reporte incorpora controles de estado, categoría y fechas.

El botón **Descargar JSON** guarda todos los registros de la respuesta de la API con los filtros aplicados. La búsqueda de texto y la paginación de la tabla no restringen el archivo descargado. **Abrir respuesta JSON** permite consultar la respuesta directamente en el navegador.

Los indicadores resumen el resultado completo de la consulta seleccionada.

## Endpoints

| Reporte | Solicitud GET |
| --- | --- |
| Detalle de pedidos | [`/api/reportes/trazabilidad`](http://localhost:8080/api/reportes/trazabilidad) |
| Métricas por categoría | [`/api/reportes/metricas-categorias`](http://localhost:8080/api/reportes/metricas-categorias) |
| Ranking de productos | [`/api/reportes/segmentacion-productos`](http://localhost:8080/api/reportes/segmentacion-productos) |
| Auditoría de pagos | [`/api/reportes/auditoria-pagos`](http://localhost:8080/api/reportes/auditoria-pagos) |
| Reporte con filtros | [`/api/reportes/reporte-filtrado`](http://localhost:8080/api/reportes/reporte-filtrado) |

El archivo [requests.http](requests.http) contiene las cinco solicitudes y ejemplos de filtros y errores.

### Filtros opcionales

| Parámetro | Valores o formato | Comportamiento |
| --- | --- | --- |
| `estado` | `Recibido`, `En preparación`, `Entregado`, `Cancelado` | Selecciona el estado del pedido. |
| `idCategoria` | Entero positivo | Selecciona una categoría existente. |
| `desde` | `AAAA-MM-DD` | Fecha inicial inclusiva. |
| `hasta` | `AAAA-MM-DD` | Fecha final inclusiva. |

Los filtros se pueden combinar. Al omitir un filtro se incluyen todos sus valores.

[Ejemplo con cuatro filtros](http://localhost:8080/api/reportes/reporte-filtrado?estado=Entregado&idCategoria=1&desde=2026-10-01&hasta=2026-10-08).

### Respuestas HTTP

| Código | Situación |
| --- | --- |
| `200` | Consulta correcta. Una consulta válida sin coincidencias devuelve `[]`. |
| `400` | Formato de parámetro incorrecto, estado desconocido, identificador no positivo o fechas invertidas. |
| `404` | La categoría solicitada en el reporte filtrado no existe. |
| `500` | Fallo al acceder a la base de datos. |

## Ejecutar Java desde NetBeans

Se requieren JDK 21, Apache NetBeans con soporte Maven, Docker Desktop y el archivo `.env` configurado.

Desde la carpeta del proyecto:

```powershell
docker compose stop api
docker compose -f docker-compose.yml -f docker-compose.netbeans.yml up -d --wait db
```

Abrir la carpeta que contiene [pom.xml](pom.xml) en NetBeans y seleccionar **Ejecutar proyecto (F6)**. La clase principal es [ModoShakeApplication.java](src/main/java/co/modoshake/api/ModoShakeApplication.java).

[nbactions.xml](nbactions.xml) inicia Spring Boot con el perfil `netbeans`. Este perfil lee las credenciales de `.env` y se conecta al MySQL de Docker mediante `127.0.0.1:3307`.

Cuando la salida muestre `Started ModoShakeApplication`, abrir el [panel de reportes](http://localhost:8080/). El proceso Java debe permanecer activo.

Para volver a ejecutar la API en Docker, detener la ejecución de NetBeans y usar `docker compose up -d --build --wait`. El puerto 8080 debe estar disponible para el proceso que se va a iniciar.

## Estructura del proyecto

| Archivo o directorio | Contenido |
| --- | --- |
| [src/main/java/co/modoshake/api](src/main/java/co/modoshake/api) | Las diez clases Java en un único paquete. |
| [src/main/resources/static](src/main/resources/static) | Interfaz web. |
| [database/init.sql](database/init.sql) | Esquema, datos de prueba, procedimientos, triggers y función de clasificación de pagos. |
| [Dockerfile](Dockerfile) | Compilación multietapa y ejecución con un usuario sin privilegios administrativos. |
| [docker-compose.yml](docker-compose.yml) | API, MySQL, red, volumen y comprobación de salud. |
| [docker-compose.netbeans.yml](docker-compose.netbeans.yml) | Acceso local a MySQL para el desarrollo desde NetBeans. |
| [HOMOLOGACION.md](HOMOLOGACION.md) | Correspondencia de entidades y casos de uso de los cinco endpoints. |
| [requests.http](requests.http) | Solicitudes para probar los reportes. |

Las responsabilidades se distribuyen entre `ReporteController`, `ReporteService`, `ReporteRepository`, `ManejoErrores` y los objetos de respuesta, dentro del paquete `co.modoshake.api`.

## Base de datos y persistencia

MySQL conserva sus datos en el volumen nombrado `mysql_data`. [init.sql](database/init.sql) se ejecuta automáticamente al inicializar un volumen vacío. Editar ese archivo requiere aplicar los cambios correspondientes a una base ya inicializada.

La función `fn_clasificar_pago` compara el importe del pedido y el total pagado mediante condicionales. Los pagos con estado `Registrado` se suman por separado de los detalles para evitar duplicar importes.

## Comandos de operación

Comprobar el estado:

```powershell
docker compose ps
```

Consultar los registros:

```powershell
docker compose logs --tail 50 api db
```

Detener los servicios conservando los datos:

```powershell
docker compose stop
```

Reanudar la solución en Docker:

```powershell
docker compose up -d --wait
```
