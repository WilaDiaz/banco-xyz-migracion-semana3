package cl.duoc.bancoxyz.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

import java.sql.SQLException;

public class CustomRetryPolicy implements RetryPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(CustomRetryPolicy.class);

    private final int maxAttempts;

    public CustomRetryPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean canRetry(RetryContext context) {

        Throwable lastThrowable = context.getLastThrowable();

        // Permite el primer intento
        if (lastThrowable == null) {
            return true;
        }

        // Si se alcanzo el maximo de intentos, no reintenta
        if (context.getRetryCount() >= maxAttempts) {
            return false;
        }

        // Reintenta solo errores transitorios de acceso a datos
        // o errores SQL.
        if (isRetryable(lastThrowable)) {

            log.warn(
                    "Reintentando operacion batch tras fallo transitorio. " +
                    "Intento #{}/{}",
                    context.getRetryCount() + 1,
                    maxAttempts
            );

            return true;
        }

        return false;
    }

    private boolean isRetryable(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof TransientDataAccessException
                    || current instanceof SQLException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new RetryContextSupport(parent);
    }

    @Override
    public void close(RetryContext context) {
        // No requiere acciones adicionales al cerrar el contexto.
    }

    @Override
    public void registerThrowable(
            RetryContext context,
            Throwable throwable) {

        ((RetryContextSupport) context)
                .registerThrowable(throwable);
    }
}

