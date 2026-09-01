# Sistema de Migración Batch - Banco XYZ

## 1. Descripción del proyecto

Proyecto desarrollado para Banco XYZ utilizando **Java 17, Spring Boot 3 y Spring Batch 5**, cuyo objetivo es optimizar la migración y procesamiento de información bancaria proveniente de archivos CSV hacia una base de datos relacional MySQL.

La solución implementa tres procesos Batch independientes para:

- Procesamiento de transacciones diarias.
- Cálculo de intereses.
- Generación y consolidación de estados de cuenta anuales.


El proyecto incorpora validación y transformación de datos, detección de anomalías, tolerancia a fallos, políticas de Skip y Retry, procesamiento multihilo y parametrización de la configuración Batch.

---

## 2. Tecnologías utilizadas

- Java 17
- Spring Boot 3
- Spring Batch 5
- Spring Data JPA
- Hibernate
- MySQL 8
- Maven
- Git / GitHub

---

## 3. Estructura del proyecto

El proyecto utiliza el paquete base:

`cl.duoc.bancoxyz`

La aplicación se encuentra organizada principalmente en:

- `config/`: configuración de los Jobs, Steps y procesamiento multihilo.
- `dto/`: objetos utilizados para la lectura de los archivos CSV.
- `model/`: entidades persistidas en MySQL.
- `processor/`: validación, transformación y detección de anomalías.
- `repository/`: repositorios Spring Data JPA.
- `policy/`: políticas personalizadas `CustomSkipPolicy` y `CustomRetryPolicy`.
- `util/`: utilidades para normalización y procesamiento de fechas.
- `resources/data/`: archivos CSV utilizados como fuente de información.

---

## 4. Procesos Batch implementados

### 4.1. Job de transacciones diarias

**Job:** `transaccionesJob`

Procesa el archivo:

`src/main/resources/data/transacciones.csv`

El proceso realiza:

1. Lectura de las transacciones desde CSV.
2. Validación y normalización de los datos.
3. Normalización de diferentes formatos de fecha.
4. Detección de montos negativos.
5. Detección de montos iguales a cero.
6. Identificación de tipos de transacción inválidos.
7. Persistencia del resultado en MySQL.

Los registros son clasificados mediante estados como:

- `VALIDA`
- `ANOMALIA_MONTO_NEGATIVO`
- `ANOMALIA_MONTO_CERO`
- `ANOMALIA_TIPO_INVALIDO`

Los registros con datos que no pueden ser procesados de forma segura son controlados mediante la política de tolerancia a fallos.

---

### 4.2. Job de cálculo de intereses

**Job:** `interesesJob`

Procesa el archivo:

`src/main/resources/data/intereses.csv`

El proceso valida los datos de cada cuenta y calcula el interés correspondiente de acuerdo con su tipo.

Tasas utilizadas:

| Tipo de cuenta | Tasa |
|---|---:|
| Ahorro | 5% |
| Préstamo | 8% |
| Hipoteca | 3,5% |

Para cada registro se almacenan datos como:

- ID de cuenta.
- Nombre.
- Edad.
- Tipo.
- Saldo inicial.
- Tasa aplicada.
- Interés calculado.
- Saldo final.

Los resultados son almacenados en la tabla:

`intereses_calculados`

---

### 4.3. Job de estados de cuenta anuales

**Job:** `cuentasAnualesJob`

Procesa el archivo:

`src/main/resources/data/cuentas_anuales.csv`

Este Job utiliza múltiples Steps para procesar y consolidar la información anual.

#### Step 1 - Limpieza

Antes de una nueva ejecución se eliminan los datos anuales generados anteriormente, evitando que las reejecuciones acumulen resultados duplicados.

#### Step 2 - Procesamiento

Se realiza:

- Lectura del archivo CSV.
- Normalización de fechas.
- Validación de movimientos.
- Clasificación de auditoría.
- Persistencia de los movimientos procesados.

Las clasificaciones incluyen:

- `INGRESO`
- `EGRESO`
- `MONTO_NULO`

#### Step 3 - Consolidación anual

Los movimientos son agrupados por:

- Cuenta.
- Año.

Para cada cuenta se calcula:

- Cantidad de movimientos.
- Total de ingresos.
- Total de egresos.
- Saldo anual.

Los resultados consolidados son almacenados en:

`estado_cuenta_resumen`

Esto permite generar un estado anual detallado y auditable para cada cuenta.

---

## 5. Normalización y validación de datos

Debido a que los archivos de origen pueden contener información inconsistente, se implementaron validaciones para controlar registros incorrectos.

Entre los casos considerados se encuentran:

- Fechas en distintos formatos.
- Fechas inválidas.
- Montos negativos.
- Montos iguales a cero.
- Valores nulos.
- Tipos de transacción inválidos.
- Datos numéricos incorrectos.

La utilidad `DateParserUtil` permite normalizar los formatos de fecha aceptados antes de persistir la información.

Cuando un registro contiene una fecha imposible, por ejemplo:

`2024-13-01`

el registro es omitido mediante la política de tolerancia a fallos sin detener la ejecución completa del Job.

---

## 6. Tolerancia a fallos

La solución utiliza las capacidades de Fault Tolerance de Spring Batch mediante políticas personalizadas.

### CustomSkipPolicy

`CustomSkipPolicy` permite omitir registros defectuosos sin detener completamente el procesamiento Batch.

El límite de registros omitidos se encuentra parametrizado mediante:

```properties
batch.skip-limit=5
```

De esta forma, errores controlados en registros individuales no provocan inmediatamente la interrupción de todo el proceso.

### CustomRetryPolicy

`CustomRetryPolicy` permite reintentar operaciones cuando se producen errores transitorios relacionados con el acceso a datos o la base de datos.

La cantidad de reintentos se encuentra configurada mediante:

```properties
batch.retry-limit=3
```

Esto mejora la resiliencia de los procesos frente a fallos temporales.

---

## 7. Escalamiento y procesamiento multihilo

Para mejorar el rendimiento de los Jobs se implementó un `ThreadPoolTaskExecutor`.

Los parámetros de concurrencia se encuentran externalizados en `application.properties`, permitiendo modificar la configuración sin alterar el código fuente.

La configuración seleccionada utiliza:

```properties
batch.chunk-size=5
batch.threads.core=5
batch.threads.max=5
batch.queue-capacity=15
batch.skip-limit=5
batch.retry-limit=3
```

Durante las pruebas se evaluaron diferentes configuraciones de cantidad de hilos.

| Configuración | Transacciones | Intereses | Estados anuales | Tiempo total |
|---|---:|---:|---:|---:|
| 1 hilo | 180 ms | 110 ms | 527 ms | 817 ms |
| 3 hilos | 186 ms | 104 ms | 410 ms | 700 ms |
| 5 hilos | 294 ms | 88 ms | 244 ms | 626 ms |

La configuración de **5 hilos** obtuvo el menor tiempo global de ejecución, con aproximadamente **626 ms** en las pruebas comparativas.

Aunque el Job de transacciones presentó un aumento individual de tiempo debido al overhead asociado a la concurrencia, los Jobs de intereses y estados anuales mejoraron su rendimiento, obteniéndose el mejor resultado global con 5 hilos.

Por este motivo se seleccionó esta configuración como alternativa final.

---

## 8. Estrategia de reejecución

Los Jobs son ejecutados utilizando parámetros dinámicos, permitiendo generar nuevas instancias de ejecución en Spring Batch.

La aplicación utiliza un parámetro basado en timestamp para diferenciar cada ejecución.

Esto permite ejecutar nuevamente los procesos sin producir conflictos por una instancia de Job previamente completada.

Adicionalmente, el procesamiento de estados anuales incorpora un Step de limpieza previo para evitar la acumulación de resultados consolidados entre ejecuciones.

---

## 9. Base de datos

El proyecto utiliza MySQL.

Base de datos utilizada:

`banco_xyz_batch`

Entre las tablas generadas por el proyecto se encuentran:

- `reporte_transacciones`
- `intereses_calculados`
- `estados_cuenta_anual`
- `estado_cuenta_resumen`

Spring Batch también genera sus tablas internas para administrar Jobs, Steps y ejecuciones.

---

## 10. Configuración de conexión

La aplicación permite configurar la conexión mediante variables de entorno.

Ejemplo en PowerShell:

```powershell
$env:DB_PORT="3307"
$env:DB_USER="root"
$env:DB_PASSWORD="TU_CONTRASEÑA_MYSQL"
```

Posteriormente se puede ejecutar el proyecto con:

```powershell
.\mvnw.cmd spring-boot:run
```

La contraseña de MySQL no debe almacenarse directamente en el repositorio.

---

## 11. Compilación del proyecto

Para verificar la compilación:

```powershell
.\mvnw.cmd clean compile
```

Una compilación correcta debe finalizar con:

```text
BUILD SUCCESS
```

---

## 12. Ejecución

Al ejecutar la aplicación se procesan secuencialmente los tres Jobs:

```text
PROCESO 1: TRANSACCIONES DIARIAS
PROCESO 2: CALCULO DE INTERESES
PROCESO 3: ESTADOS DE CUENTA
```

Cada Job registra en consola su inicio, término, estado y duración.

Una ejecución correcta finaliza con los Jobs en estado:

`COMPLETED`

y con el mensaje final correspondiente a la migración Batch completada exitosamente.

---

## 13. Resultados obtenidos

### Transacciones

La información procesada permite distinguir transacciones válidas y anomalías como:

- Montos negativos.
- Montos cero.
- Tipos de transacción inválidos.

### Intereses

Se almacenan los intereses calculados junto con la tasa aplicada, saldo inicial y saldo final de cada cuenta.

### Estados anuales

Se genera una consolidación por cuenta y año que incluye:

- Cantidad de movimientos.
- Total de ingresos.
- Total de egresos.
- Saldo anual.

Esto permite obtener información resumida y auditable a partir de los movimientos procesados.

---

## 14. Conclusión

La solución desarrollada permite realizar la migración y procesamiento de datos bancarios mediante Spring Batch, incorporando mecanismos orientados a mejorar la integridad, rendimiento y resiliencia del proceso.

La implementación de validaciones, normalización de datos, políticas de Skip y Retry permite controlar registros defectuosos y fallos transitorios sin comprometer innecesariamente la ejecución completa.

Asimismo, las pruebas de escalamiento permitieron comparar diferentes configuraciones de concurrencia, seleccionándose una configuración de 5 hilos por presentar el mejor tiempo global entre los escenarios evaluados.

Finalmente, la consolidación de estados de cuenta permite agrupar los movimientos por cuenta y año, generando información detallada de ingresos, egresos, saldo y cantidad de movimientos para facilitar la auditoría de los datos migrados.

---

## Autor

**Wilangely Diaz**

Proyecto desarrollado para la asignatura **Desarrollo Backend III - Semana 3**.