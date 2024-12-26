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
