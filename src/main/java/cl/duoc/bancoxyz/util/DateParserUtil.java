package cl.duoc.bancoxyz.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class DateParserUtil {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private DateParserUtil() {
    }

    public static LocalDate parse(String fecha) {

        if (fecha == null || fecha.isBlank()) {
            throw new IllegalArgumentException("La fecha no puede estar vacia");
        }

        String fechaLimpia = fecha.trim();

        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(fechaLimpia, formato);
            } catch (DateTimeParseException ignored) {
                // Intenta con el siguiente formato
            }
        }

        throw new IllegalArgumentException(
                "Formato de fecha invalido: " + fecha
        );
    }
}