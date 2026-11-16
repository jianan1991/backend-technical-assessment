package com.aquariux.technical.assessment.trade.aspect;

import com.aquariux.technical.assessment.trade.controller.TradeController;
import com.aquariux.technical.assessment.trade.exception.InsufficientBalanceException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RequestTimingAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/trades/execute")));
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(TradeController.class);
        when(signature.getName()).thenReturn("executeTrade");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logsEndpointStatusAndDuration_WhenRequestSucceeds(CapturedOutput output) throws Throwable {
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.CREATED).body("ok");
        when(joinPoint.proceed()).thenReturn(response);

        Object result = new RequestTimingAspect(Duration.ofSeconds(5)).logExecutionTime(joinPoint);

        assertThat(result).isSameAs(response);
        assertThat(output).containsPattern(
                "INFO .*POST /api/trades/execute -> 201 in \\d+\\.\\d ms \\[TradeController\\.executeTrade]");
    }

    @Test
    void logsErrorCodeAndRethrows_WhenTradeRejected(CapturedOutput output) throws Throwable {
        InsufficientBalanceException rejection = new InsufficientBalanceException("USDT", BigDecimal.TEN);
        when(joinPoint.proceed()).thenThrow(rejection);

        assertThatThrownBy(() -> new RequestTimingAspect(Duration.ofSeconds(5)).logExecutionTime(joinPoint))
                .isSameAs(rejection);
        assertThat(output).contains("POST /api/trades/execute -> 422 INSUFFICIENT_BALANCE in");
    }

    @Test
    void logsUnexpectedFailureAndRethrows(CapturedOutput output) throws Throwable {
        IllegalStateException failure = new IllegalStateException("boom");
        when(joinPoint.proceed()).thenThrow(failure);

        assertThatThrownBy(() -> new RequestTimingAspect(Duration.ofSeconds(5)).logExecutionTime(joinPoint))
                .isSameAs(failure);
        assertThat(output).contains("-> failed IllegalStateException in");
    }

    @Test
    void logsAtWarn_WhenSlowerThanThreshold(CapturedOutput output) throws Throwable {
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(20);
            return ResponseEntity.ok("slow");
        });

        new RequestTimingAspect(Duration.ofMillis(1)).logExecutionTime(joinPoint);

        assertThat(output).containsPattern("WARN .*SLOW POST /api/trades/execute -> 200 in");
    }
}
