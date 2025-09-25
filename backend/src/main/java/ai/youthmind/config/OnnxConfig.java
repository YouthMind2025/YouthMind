package ai.youthmind.config;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

@Configuration
public class OnnxConfig {

    @Bean
    public OrtEnvironment ortEnvironment() {
        return OrtEnvironment.getEnvironment();
    }

    public static OrtSession ortSession(String model, OrtEnvironment ortEnvironment, ResourceLoader resourceLoader) throws Exception {
        // Load your ONNX model
        Resource modelResource = resourceLoader.getResource(model);
        InputStream modelStream = modelResource.getInputStream();
        byte[] modelBytes = modelStream.readAllBytes();

        // Create session options
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();

        // Configure session options for better performance
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        sessionOptions.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
        sessionOptions.setInterOpNumThreads(2);
        sessionOptions.setIntraOpNumThreads(4);

        return ortEnvironment.createSession(modelBytes, sessionOptions);
    }
}
