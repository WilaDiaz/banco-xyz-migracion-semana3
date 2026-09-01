package cl.duoc.bancoxyz.processor;

import cl.duoc.bancoxyz.dto.InteresDTO;
import cl.duoc.bancoxyz.model.InteresCuenta;
import org.springframework.batch.item.ItemProcessor;

import java.util.Set;

/**
 * Procesador que calcula el interes y saldo final de cada cuenta.
 * Valida saldo, edad, nombre y tipo de producto antes de realizar
 * el calculo financiero.
 */
public class InteresProcessor implements ItemProcessor<InteresDTO, InteresCuenta> {

    private static final Set<String> TIPOS_VALIDOS =
            Set.of("ahorro", "prestamo", "hipoteca");

    @Override
    public InteresCuenta process(InteresDTO item) throws Exception {

        // Regla 1: Validacion del identificador de cuenta
        if (item.getCuentaId() == null) {
            return null;
        }

        // Regla 2: Validacion del nombre
        if (item.getNombre() == null || item.getNombre().isBlank()) {
            return null;
        }

        // Regla 3: Validacion del saldo
        if (item.getSaldo() == null || item.getSaldo() <= 0.0) {
            return null;
        }

        // Regla 4: Validacion de edad
        if (item.getEdad() == null
                || item.getEdad() <= 0
                || item.getEdad() > 120) {
            return null;
        }

        // Regla 5: Normalizacion y validacion del tipo de producto
        if (item.getTipo() == null || item.getTipo().isBlank()) {
            return null;
        }

        String tipo = item.getTipo().trim().toLowerCase();

        if (!TIPOS_VALIDOS.contains(tipo)) {
            return null;
        }

        // Regla 6: Determinacion de tasa
        double tasa = switch (tipo) {
            case "ahorro" -> 0.05;
            case "prestamo" -> 0.08;
            case "hipoteca" -> 0.035;
            default -> throw new IllegalArgumentException(
                    "Tipo de producto no soportado: " + tipo
            );
        };

        // Regla 7: Calculo financiero
        double interes = item.getSaldo() * tasa;
        double saldoFinal = item.getSaldo() + interes;

        return new InteresCuenta(
                item.getCuentaId(),
                item.getNombre().trim(),
                item.getSaldo(),
                item.getEdad(),
                tipo,
                tasa,
                interes,
                saldoFinal
        );
    }
}
