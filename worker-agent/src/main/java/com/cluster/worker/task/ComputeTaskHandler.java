package com.cluster.worker.task;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ComputeTaskHandler implements TaskHandler {

    private static final String TYPE = "COMPUTE";

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
            throw new IllegalArgumentException("COMPUTE task requires input parameters");
        }

        String operation = (String) input.getOrDefault("operation", "add");
        Map<String, Object> result = new HashMap<>();

        if ("add".equalsIgnoreCase(operation)) {
            double a = getNumber(input, "a");
            double b = getNumber(input, "b");
            result.put("result", a + b);
        } else if ("multiply".equalsIgnoreCase(operation)) {
            double a = getNumber(input, "a");
            double b = getNumber(input, "b");
            result.put("result", a * b);
        } else if ("fibonacci".equalsIgnoreCase(operation)) {
            int n = (int) getNumber(input, "n");
            if (n < 0 || n > 1000) {
                throw new IllegalArgumentException(
                        "Fibonacci n must be between 0 and 1000 for safety bounds");
            }
            result.put("result", computeFibonacci(n, task));
        } else {
            throw new IllegalArgumentException("Unsupported compute operation: " + operation);
        }

        if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
            throw new InterruptedException(
                    "Task execution cancelled or interrupted before completing");
        }

        return result;
    }

    private double getNumber(Map<String, Object> input, String key) {
        Object val = input.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else if (val instanceof String) {
            return Double.parseDouble((String) val);
        }
        return 0;
    }

    private long computeFibonacci(int n, WorkerTask task) throws InterruptedException {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        long prev = 0, curr = 1;
        for (int i = 2; i <= n; i++) {
            if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
                throw new InterruptedException(
                        "Computation interrupted during fibonacci calculation");
            }
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }
}
