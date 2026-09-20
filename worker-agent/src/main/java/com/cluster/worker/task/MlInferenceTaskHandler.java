package com.cluster.worker.task;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MlInferenceTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(MlInferenceTaskHandler.class);
    private static final String TYPE = "ML_INFERENCE";
    private final ObjectMapper objectMapper;
    private final OrtEnvironment env;

    public MlInferenceTaskHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.env = OrtEnvironment.getEnvironment();
    }

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
            throw new IllegalArgumentException("ML_INFERENCE task requires input parameters");
        }

        String modelName = (String) input.get("model");
        if (modelName == null || modelName.isEmpty()) {
            throw new IllegalArgumentException("Model name is required");
        }

        Object dataObj = input.get("data");
        if (dataObj == null) {
            throw new IllegalArgumentException("Data payload is required");
        }

        // Parse data
        JsonNode dataNode = objectMapper.convertValue(dataObj, JsonNode.class);
        if (!dataNode.isArray()) {
            throw new IllegalArgumentException("Data must be an array of records");
        }

        List<Float> results = new ArrayList<>();

        ClassPathResource resource = new ClassPathResource("models/" + modelName);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Model " + modelName + " not found in classpath");
        }

        try (InputStream is = resource.getInputStream()) {
            byte[] modelBytes = is.readAllBytes();
            try (OrtSession session = env.createSession(modelBytes)) {
                
                String inputName = session.getInputNames().iterator().next();

                for (JsonNode record : dataNode) {
                    if (Thread.currentThread().isInterrupted() || (task != null && task.isCancelled())) {
                        throw new InterruptedException("Task execution cancelled during inference");
                    }

                    // Assume each record is a single feature array like [1.0]
                    float val = 0.0f;
                    if (record.isArray() && record.size() > 0) {
                        val = (float) record.get(0).asDouble();
                    } else if (record.isNumber()) {
                        val = (float) record.asDouble();
                    }

                    float[][] inputArray = new float[][]{{val}};
                    
                    try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputArray);
                         OrtSession.Result result = session.run(java.util.Collections.singletonMap(inputName, tensor))) {
                        
                        float[][] output = (float[][]) result.get(0).getValue();
                        results.add(output[0][0]);
                    }
                }
            }
        }

        log.debug("ML_INFERENCE completed for {} records", results.size());
        return results;
    }
}
