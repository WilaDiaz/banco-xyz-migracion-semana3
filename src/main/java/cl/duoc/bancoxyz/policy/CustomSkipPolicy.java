package cl.duoc.bancoxyz.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;

public class CustomSkipPolicy implements SkipPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(CustomSkipPolicy.class);

    private final int maxSkipCount;

    public CustomSkipPolicy(int maxSkipCount) {
        this.maxSkipCount = maxSkipCount;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount)
            throws SkipLimitExceededException {

        if (skipCount >= maxSkipCount) {
            log.error(
                    "Se ha excedido el limite maximo de tolerancia a fallos " +
                    "({} registros omitidos). Abortando Step.",
                    skipCount
            );
            return false;
        }

        if (t instanceof FlatFileParseException
                || t instanceof NumberFormatException
                || t instanceof IllegalArgumentException) {

            log.warn(
                    "Tolerancia a fallos activada [Skip #{}]: " +
                    "Registro omitido por error: {}",
                    skipCount + 1,
                    t.getMessage()
            );

            return true;
        }

        return false;
    }
}
