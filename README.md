
# Comparing OpenTelemetry and Datadog Java Agents used together with the Otel API.

This project compares the use of the **OpenTelemetry API** with two automatic instrumentation agents: the OpenTelemetry Java Agent and the Datadog Java Agent. 

It includes a Spring Boot application instrumented with OpenTelemetry (via the **Otel agent**) and Datadog (via the **Datadog agent**). 

The project uses **context propagation** to demonstrate parent-child relationships in traces and uses `docker-compose` for a containerized setup with both the Otel Collector and Datadog Agent.

---

## **1. Project Structure**

The project directory structure is as follows:

```
spring-otel-context-prop
   ├── Dockerfile
   ├── README.md
   ├── build.gradle.kts
   ├── config.yaml
   ├── dd-java-agent.jar
   ├── docker-compose.yml
   ├── gradle
   │      └── wrapper
   │          ├── gradle-wrapper.jar
   │          └── gradle-wrapper.properties
   ├── gradlew
   ├── img
   │    ├── springdatadog.png
   │    └── springotel.png
   ├── opentelemetry-javaagent.jar
   ├── otentelemetry-javaagent.jar
   ├── settings.gradle.kts
   └── src
       └── main
           └── java
                 └── com
                     └── datadoghq
                         └── pej
                             ├── OtelCtxPropApplication.java
                             └── OtelCtxPropController.java
           └──resources
               └── application.properties

```

**Key Components**


`src/main/java/com/datadoghq/pej/OtelCtxPropController.java`: Contains the Spring Boot controller with endpoints showcasing context propagation using the Otel API.

`docker-compose.yml`: Defines services for the Datadog Agent, Otel Collector, and two Spring Boot applications (`springotel` and `springdatadog`).

`Dockerfile`: Specifies the Docker image build process for the Spring Boot applications.

`build.gradle.kts`: Kotlin-based Gradle build script for the project.

`config.yaml`: Configuration file for the Otel Collector.

`dd-java-agent.jar` and `opentelemetry-javaagent.jar`: Java agent JARs for Datadog and Otel, respectively.

---

## **2. Source Code**

#### **OtelCtxPropController.java**

This Spring Boot controller showcases OpenTelemetry **context propagation** using the `inject` and `extract` methods:

```java
package com.datadoghq.pej;


import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OtelCtxPropController {

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("example-tracer");

    @GetMapping("/inject-extract")
    public String injectExtractTrace() {
        // Create a parent span
        Span parentSpan = tracer.spanBuilder("parent-span").startSpan();
        Map<String, String> contextMap = new HashMap<>();
        try {
            // Inject the span context into a map
            GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                    .inject(Context.current().with(parentSpan), contextMap, MapSetter.INSTANCE);

            // Simulate context extraction and creating a child span
            createChildSpanFromContext(contextMap);

            return "Parent and child spans created and logged.";
        } finally {
            parentSpan.end();
        }
    }

    private void createChildSpanFromContext(Map<String, String> contextMap) {
        // Extract the context from the map
        Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), contextMap, MapGetter.INSTANCE);

        // Create a new child span using the extracted context
        Span childSpan = tracer.spanBuilder("child-span")
                .setParent(extractedContext)
                .startSpan();
        try {
            childSpan.addEvent("Child span event: Processing with extracted context");
        } finally {
            childSpan.end();
        }
    }

    // Helper for injecting context into a map
    private static class MapSetter implements TextMapSetter<Map<String, String>> {
        static final MapSetter INSTANCE = new MapSetter();

        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }
    }

    // Helper for extracting context from a map
    private static class MapGetter implements TextMapGetter<Map<String, String>> {
        static final MapGetter INSTANCE = new MapGetter();

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    }
}
```

**Explanation of the Spring Boot Controller**

The `OtelCtxPropController.java` relies on the OpenTelemetry API to propagate context. 

Here's what happens:

*Parent Span Creation:*

A parent-span is created using the Otel API. The OpenTelemetry or the Datadog java agents inject the tracer object that can be retrieved by invoking the `GlobalOpenTelemetry.getTracer()` method.

This span serves as the root of the trace for the request.

The span's context is injected into a Map using `trace.inject()`.
This simulates passing trace context downstream (e.g., to another service).

*Child Span Creation*

The context is extracted from the Map using `trace.extract()`.
A child-span is created and linked to the parent-span using the extracted context.
This shows how we can propagate across service boundaries or threads while maintaining the parent-child relationship.


---

## **3. Build Instructions**

### **Local Setup**

1. **Prerequisites**

     * **JDK 17+**: Ensure that JDK 17 or above is installed on your machine.
     * **Gradle 8.11.1**: Verify that Gradle 8.11.1 is installed.
     * **Docker**: Install Docker and Docker compose to build and run containerized services.


2. **Clone the Repository**:
   ```bash
   git clone https://github.com/ptabasso2/spring-otel-context-prop.git
   cd spring-otel-context-prop
   ```

3. **Build the Application**:
   Use the Gradle wrapper to build the project:
   ```bash
   ./gradlew clean build
   ```

4. **Start the OpenTelemetry Collector**:
   Replace `xxxxxxxx` with your Datadog API key:
   ```bash
   docker run --rm -d --name otel-collector -v $(pwd)/config.yaml:/etc/otelcol-contrib/config.yaml -e DD_API_KEY=xxxxxxxx -p 4317:4317 -p 4318:4318 -p 55681:55681 otel/opentelemetry-collector-contrib:0.116.1
   ```
   This starts the Otel Collector container, exposing the required ports:
   * **4317**: For OTLP/gRPC.
   * **4318**: For OTLP/HTTP.
   * **55681**: For custom protocols.


5. **Start the Datadog Agent**:
   Replace `xxxxxxxx` with your Datadog API key:
   ```bash
   docker run --rm -d --name dd-agent-dogfood-jmx -v /var/run/docker.sock:/var/run/docker.sock:ro -v /proc/:/host/proc/:ro -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro -p 8126:8126 -p 8125:8125/udp -e DD_API_KEY=xxxxxxxx -e DD_APM_ENABLED=true -e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC="true" -e DD_LOG_LEVEL=debug -e DD_LOGS_ENABLED=true -e DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true -e DD_CONTAINER_EXCLUDE_LOGS="name:datadog-agent" gcr.io/datadoghq/agent:latest-jmx
   ```

6. **Run the Services**:
    - **Datadog-Instrumented Service (Port 8080)**:
      ```bash
      java -javaagent:dd-java-agent.jar -Ddd.env=dev -Ddd.service=springdatadog -Ddd.version=1.2 -Ddd.trace.otel.enabled=true -jar build/libs/spring-otel-context-prop-0.0.1-SNAPSHOT.jar
      ```

    - **OpenTelemetry-Instrumented Service (Port 8081)**:
      ```bash
      java -javaagent:opentelemetry-javaagent.jar -Dotel.service.name=springotel -Dotel.exporter.otlp.endpoint=http://localhost:4318 -Dotel.traces.exporter=otlp -Dotel.logs.exporter=none -Dotel.resource.attributes=env=dev          -jar build/libs/spring-otel-context-prop-0.0.1-SNAPSHOT.jar --server.port=8081
      ```

6. **(Appendix) Downloading the agents**:
   
   In case you need to download the latest (or a specific version) of the agents, you would do as follows:
   
   * **Otel:**
      ```bash
      wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
      ```
   * **Datadog**
      ```bash
      wget -O dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'
      ```

---

### **Docker Compose Setup**

1. **Build and Start Services**:

Replace `xxxxxxxx` with your Datadog API key:

   ```bash
    docker-compose build
    DD_API_KEY=xxxxxxxx docker-compose up -d
   ```

Or in one pass

   ```bash
    DD_API_KEY=xxxxxxxx docker-compose up --build -d
   ```

2. **Verify Services**:
   Use `docker-compose ps` to confirm that all services are running.

```bash
[root@pt-instance-2:~/otelctxprop]$ docker-compose ps
    Name                   Command                       State                                                     Ports                                          
------------------------------------------------------------------------------------------------------------------------------------------------------------------
    Name                   Command                  State                                                     Ports                                               
------------------------------------------------------------------------------------------------------------------------------------------------------------------
dd-agent        /bin/entrypoint.sh               Up (healthy)   0.0.0.0:8125->8125/tcp,:::8125->8125/tcp, 8125/udp, 0.0.0.0:8126->8126/tcp,:::8126->8126/tcp      
otelcollector   /otelcol-contrib --config  ...   Up             0.0.0.0:13133->13133/tcp,:::13133->13133/tcp, 0.0.0.0:1888->1888/tcp,:::1888->1888/tcp,           
                                                                0.0.0.0:4317->4317/tcp,:::4317->4317/tcp, 0.0.0.0:4318->4318/tcp,:::4318->4318/tcp, 55678/tcp,    
                                                                0.0.0.0:55679->55679/tcp,:::55679->55679/tcp, 0.0.0.0:8888->8888/tcp,:::8888->8888/tcp,           
                                                                0.0.0.0:8889->8889/tcp,:::8889->8889/tcp, 0.0.0.0:9411->9411/tcp,:::9411->9411/tcp                
springdatadog   /__cacert_entrypoint.sh ja ...   Up             0.0.0.0:8080->8080/tcp,:::8080->8080/tcp                                                          
springotel      /__cacert_entrypoint.sh ja ...   Up             0.0.0.0:8081->8080/tcp,:::8081->8080/tcp                                                          
```

---

## **4. Testing**

Run the following `curl` commands to test both services:

#### **Datadog Service**:
```bash
curl localhost:8080/api/inject-extract
```

#### **OpenTelemetry Service**:
```bash
curl localhost:8081/api/inject-extract
```

---

## **5. Observing Results**

#### **Datadog Traces**:
- Go to the Datadog dashboard under **APM > Traces > Explorer**.

<p align="left">
  <img src="img/springdatadog.png" width="850" />
</p>

#### **OpenTelemetry Traces**:
- Go to the Datadog dashboard under **APM > Traces > Explorer**.

<p align="left">
  <img src="img/springotel.png" width="850" />
</p>

---
