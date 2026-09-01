package cl.duoc.bancoxyz.processor;

import cl.duoc.bancoxyz.dto.TransaccionDTO;
import cl.duoc.bancoxyz.model.Transaccion;
import cl.duoc.bancoxyz.util.DateParserUtil;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.util.Set;

/**
 * Procesador que transforma TransaccionDTO a Transaccion.
 * Normaliza datos y clasifica anomalías detectadas en las transacciones.
 */
public class TransaccionProcessor implements ItemProcessor<TransaccionDTO, Transaccion> {

    private static final Set<String> TIPOS_VALIDOS =
            Set.of("credito", "debito", "transferencia");

    @Override
    public Transaccion process(TransaccionDTO item) throws Exception {

        if (item.getId() == null) {
            throw new IllegalArgumentException(
                    "La transaccion no posee identificador"
            );
        }

        // Las fechas con formatos válidos se normalizan.
        // Una fecha imposible sí genera excepción y puede ser omitida por SkipPolicy.
        LocalDate fecha = DateParserUtil.parse(item.getFecha());

        String tipoNormalizado = item.getTipo() != null
                ? item.getTipo().trim().toLowerCase()
                : "desconocido";

        String estado = "VALIDA";

        // Prioridad 1: anomalía por tipo
        if (!TIPOS_VALIDOS.contains(tipoNormalizado)) {
            estado = "ANOMALIA_TIPO_INVALIDO";
        }

        // Prioridad 2: anomalía por monto
        if (item.getMonto() == null || item.getMonto() == 0.0) {
            estado = "ANOMALIA_MONTO_CERO";
        } else if (item.getMonto() < 0.0) {
            estado = "ANOMALIA_MONTO_NEGATIVO";
        }

        return new Transaccion(
                item.getId(),
                fecha,
                item.getMonto(),
                tipoNormalizado,
                estado
        );
    }
}