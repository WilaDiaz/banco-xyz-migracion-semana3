package cl.duoc.bancoxyz.processor;

import cl.duoc.bancoxyz.dto.CuentaAnualDTO;
import cl.duoc.bancoxyz.model.CuentaAnual;
import cl.duoc.bancoxyz.util.DateParserUtil;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.util.Set;

/**
 * Procesador que transforma CuentaAnualDTO a CuentaAnual.
 * Normaliza fechas, valida datos y clasifica los movimientos
 * con fines de auditoria financiera.
 */
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnualDTO, CuentaAnual> {

    private static final Set<String> TIPOS_VALIDOS =
        Set.of(
                "compra",
                "deposito",
                "depósito",
                "retiro",
                "pago"
        );

    @Override
    public CuentaAnual process(CuentaAnualDTO item) throws Exception {

        // Regla 1: Validacion de cuenta
        if (item.getCuentaId() == null) {
            throw new IllegalArgumentException(
                    "El movimiento anual no posee cuenta asociada"
            );
        }

        // Regla 2: Normalizacion y validacion de fecha
        LocalDate fecha = DateParserUtil.parse(item.getFecha());

        // Regla 3: Normalizacion y validacion del tipo de transaccion
        String tipoTransaccion = item.getTransaccion() != null
                ? item.getTransaccion().trim().toLowerCase()
                : "desconocido";

        if (!TIPOS_VALIDOS.contains(tipoTransaccion)) {
            throw new IllegalArgumentException(
                    "Tipo de transaccion anual invalido: " + tipoTransaccion
            );
        }

        // Regla 4: Normalizacion del monto
        double monto = item.getMonto() != null
                ? item.getMonto()
                : 0.0;

        // Regla 5: Clasificacion del movimiento
        String clasificacion;

        if (monto == 0.0) {
            clasificacion = "MONTO_NULO";
        } else if (monto < 0.0) {
            clasificacion = "EGRESO";
        } else {
            clasificacion = "INGRESO";
        }

        // Regla 6: Normalizacion de descripcion
        String descripcion = item.getDescripcion() != null
                ? item.getDescripcion().trim()
                : "";

        return new CuentaAnual(
                item.getCuentaId(),
                fecha,
                tipoTransaccion,
                monto,
                descripcion,
                clasificacion
        );
    }
}

