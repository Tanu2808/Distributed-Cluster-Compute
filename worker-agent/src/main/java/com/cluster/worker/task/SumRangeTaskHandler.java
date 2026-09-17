package com.cluster.worker.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * TaskHandler for deterministic SUM_RANGE workloads.
 * Computes the sum of integer numbers in the range [start, end] inclusive.
 */
@Component
public class SumRangeTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SumRangeTaskHandler.class);
    private static final String TYPE = "SUM_RANGE";

    public static final long MIN_ALLOWED_VALUE = -1_000_000_000_000L; // -1 trillion
    public static final long MAX_ALLOWED_VALUE = 1_000_000_000_000L;  // +1 trillion
    public static final long MAX_RANGE_SPAN = 100_000_000L;           // 100 million elements max per subtask
    private static final int CANCELLATION_CHECK_INTERVAL_MASK = 0x7FFF; // Check every 32,768 iterations

    @Override
    public boolean supports(String taskType) {
        return TYPE.equalsIgnoreCase(taskType);
    }

    @Override
    public Object execute(WorkerTask task) throws Exception {
        if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
            throw new InterruptedException("Task execution cancelled or interrupted before start");
        }

        Map<String, Object> input = task != null ? task.getInput() : null;
        if (input == null) {
            throw new IllegalArgumentException("SUM_RANGE task requires input parameters");
        }

        long start = parseLongField(input, "start");
        long end = parseLongField(input, "end");

        if (start > end) {
            throw new IllegalArgumentException("Invalid range: start (" + start + ") must be less than or equal to end (" + end + ")");
        }

        long span = (end - start) + 1;
        if (span > MAX_RANGE_SPAN) {
            throw new IllegalArgumentException("Range span (" + span + ") exceeds maximum supported limit of " + MAX_RANGE_SPAN);
        }

        if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
            throw new InterruptedException("Task execution cancelled or interrupted before summation");
        }

        long sum = 0L;
        try {
            for (long i = start; i <= end; i++) {
                if ((i & CANCELLATION_CHECK_INTERVAL_MASK) == 0) {
                    if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
                        throw new InterruptedException("Task execution cancelled or interrupted during sum_range calculation");
                    }
                }
                sum = Math.addExact(sum, i);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Arithmetic overflow: sum exceeds 64-bit signed integer limits", e);
        }

        if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
            throw new InterruptedException("Task execution cancelled or interrupted before completion");
        }

        log.debug("SUM_RANGE computed for [{}-{}] = {}", start, end, sum);
        return sum;
    }

    private long parseLongField(Map<String, Object> input, String key) {
        if (!input.containsKey(key) || input.get(key) == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }

        Object val = input.get(key);

        if (val instanceof BigInteger) {
            BigInteger bi = (BigInteger) val;
            if (bi.compareTo(BigInteger.valueOf(MIN_ALLOWED_VALUE)) < 0 || bi.compareTo(BigInteger.valueOf(MAX_ALLOWED_VALUE)) > 0) {
                throw new IllegalArgumentException("Field '" + key + "' is outside safe supported limits [" 
                        + MIN_ALLOWED_VALUE + ", " + MAX_ALLOWED_VALUE + "]");
            }
            return bi.longValue();
        }

        if (val instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) val;
            try {
                BigInteger bi = bd.toBigIntegerExact();
                if (bi.compareTo(BigInteger.valueOf(MIN_ALLOWED_VALUE)) < 0 || bi.compareTo(BigInteger.valueOf(MAX_ALLOWED_VALUE)) > 0) {
                    throw new IllegalArgumentException("Field '" + key + "' is outside safe supported limits [" 
                            + MIN_ALLOWED_VALUE + ", " + MAX_ALLOWED_VALUE + "]");
                }
                return bi.longValue();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Field '" + key + "' must be an integer, got: " + val);
            }
        }

        if (val instanceof Number) {
            double d = ((Number) val).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.floor(d)) {
                throw new IllegalArgumentException("Field '" + key + "' must be an integer, got: " + val);
            }
            long longVal = ((Number) val).longValue();
            if (longVal < MIN_ALLOWED_VALUE || longVal > MAX_ALLOWED_VALUE) {
                throw new IllegalArgumentException("Field '" + key + "' is outside safe supported limits [" 
                        + MIN_ALLOWED_VALUE + ", " + MAX_ALLOWED_VALUE + "]");
            }
            return longVal;
        }

        if (val instanceof String) {
            String str = ((String) val).trim();
            if (str.isEmpty()) {
                throw new IllegalArgumentException("Field '" + key + "' cannot be empty");
            }
            try {
                BigInteger bi = new BigInteger(str);
                if (bi.compareTo(BigInteger.valueOf(MIN_ALLOWED_VALUE)) < 0 || bi.compareTo(BigInteger.valueOf(MAX_ALLOWED_VALUE)) > 0) {
                    throw new IllegalArgumentException("Field '" + key + "' is outside safe supported limits [" 
                            + MIN_ALLOWED_VALUE + ", " + MAX_ALLOWED_VALUE + "]");
                }
                return bi.longValue();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Field '" + key + "' must be a numeric integer, got: " + str);
            }
        }

        throw new IllegalArgumentException("Field '" + key + "' must be numeric, got type: " + val.getClass().getSimpleName());
    }
}
