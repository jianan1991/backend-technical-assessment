package com.aquariux.technical.assessment.trade.aspect;

import com.aquariux.technical.assessment.trade.exception.TradingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * Logs how long every REST endpoint takes, with the HTTP method, path, handler and outcome.
 * <p>
 * Requests slower than {@code trading.logging.slow-request-threshold} (default 200ms, the response-time
 * target in the business requirements) are logged at WARN so they stand out.
 * Request and response bodies are deliberately not logged, to keep user and balance data out of logs.
 */
@Slf4j
@Aspect
@Component
public class RequestTimingAspect {

    private final long slowThresholdNanos;

    public RequestTimingAspect(@Value("${trading.logging.slow-request-threshold:200ms}") Duration slowThreshold) {
        this.slowThresholdNanos = slowThreshold.toNanos();
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    void restControllerMethods() {
    }

    @Around("restControllerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String outcome = "error";
        try {
            Object result = joinPoint.proceed();
            outcome = result instanceof ResponseEntity<?> response
                    ? String.valueOf(response.getStatusCode().value())
                    : "ok";
            return result;
        } catch (TradingException ex) {
            outcome = ex.getStatus().value() + " " + ex.getCode();
            throw ex;
        } catch (Throwable ex) {
            outcome = "failed " + ex.getClass().getSimpleName();
            throw ex;
        } finally {
            log(joinPoint, outcome, System.nanoTime() - start);
        }
    }

    private void log(ProceedingJoinPoint joinPoint, String outcome, long elapsedNanos) {
        double elapsedMs = elapsedNanos / 1_000_000.0;
        String handler = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String endpoint = currentEndpoint();

        if (elapsedNanos > slowThresholdNanos) {
            log.warn("SLOW {} -> {} in {} ms [{}]", endpoint, outcome, String.format("%.1f", elapsedMs), handler);
        } else {
            log.info("{} -> {} in {} ms [{}]", endpoint, outcome, String.format("%.1f", elapsedMs), handler);
        }
    }

    private static String currentEndpoint() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "(no HTTP request)";
    }
}
