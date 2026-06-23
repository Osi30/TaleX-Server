package com.talex.server.exceptions;

import com.talex.server.exceptions.details.InteractionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        // Log exception overall
        log.error("Async method '{}' with parameters {} threw an exception.",
                method.getName(), Arrays.toString(params), throwable);

        // Log exception details
        if (throwable instanceof InteractionException) {
            logInteractionException(params);
        }
    }

    private void logInteractionException(Object... params) {
        log.error("Interaction Error");
    }
}