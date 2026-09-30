package com.ghostprofiler.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DemoApplication — the Spring Boot entry point for the GhostProfiler demo app.
 *
 * <p>This application exists solely to give the GhostProfiler agent something
 * realistic to instrument. It intentionally contains:
 * <ul>
 *   <li>A slow HTTP endpoint (artificial sleep to simulate I/O latency)</li>
 *   <li>An N+1 query bug in {@link com.ghostprofiler.demo.service.OrderService}
 *       that the agent's {@code NPlusOneDetector} should flag</li>
 * </ul>
 *
 * <p>Run with:
 * <pre>
 *   java -javaagent:../ghostprofiler-agent/target/ghostprofiler-agent-1.0.0-SNAPSHOT.jar \
 *        -Dghost.packages=com.ghostprofiler.demo \
 *        -Dghost.port=9090 \
 *        -jar target/ghostprofiler-demo-app-1.0.0-SNAPSHOT.jar
 * </pre>
 *
 * <p>STUB — real content added in step 7.
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
