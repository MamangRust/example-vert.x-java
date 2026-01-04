package com.sanedge.example_crud.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature;
import io.vertx.core.json.JsonObject;

import java.time.Duration;

public class TelemetryConfig {

  private String otlpEndpoint;
  private String serviceName;
  private String serviceVersion;
  private boolean jfrEnabled;

  private OpenTelemetry openTelemetry;
  private SdkTracerProvider tracerProvider;
  private SdkMeterProvider meterProvider;
  private SdkLoggerProvider loggerProvider;
  private RuntimeMetrics runtimeMetrics;
  private Tracer tracer;

  public TelemetryConfig(JsonObject config) {
    this.otlpEndpoint = config.getString("otel.exporter.otlp.endpoint", "http://otel-collector:4317");
    this.serviceName = config.getString("service.name", "apigateway");
    this.serviceVersion = config.getString("service.version", "1.0.0");
    this.jfrEnabled = config.getBoolean("otel.jfr.enabled", true);
  }

  public OpenTelemetry initialize() {
    Resource resource = Resource.getDefault()
        .merge(Resource.create(Attributes.of(
            AttributeKey.stringKey("service.name"), serviceName,
            AttributeKey.stringKey("service.version"), serviceVersion,
            AttributeKey.stringKey("deployment.environment"), "production")));

    tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(
            OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build())
            .setScheduleDelay(Duration.ofMillis(100))
            .build())
        .setResource(resource)
        .build();

    meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.builder(
            OtlpGrpcMetricExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build())
            .setInterval(Duration.ofSeconds(30))
            .build())
        .setResource(resource)
        .build();

    loggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(BatchLogRecordProcessor.builder(
            OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build())
            .setScheduleDelay(Duration.ofMillis(100))
            .build())
        .setResource(resource)
        .build();

    openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setMeterProvider(meterProvider)
        .setLoggerProvider(loggerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();

    tracer = openTelemetry.getTracer(serviceName, serviceVersion);

    OpenTelemetryAppender.install(openTelemetry);

    if (jfrEnabled) {
      registerJvmMetrics();
    }

    return openTelemetry;
  }

  private void registerJvmMetrics() {
    try {
      runtimeMetrics = RuntimeMetrics.builder(openTelemetry)
          .enableFeature(JfrFeature.MEMORY_POOL_METRICS)
          .enableFeature(JfrFeature.GC_DURATION_METRICS)
          .enableFeature(JfrFeature.CPU_UTILIZATION_METRICS)
          .enableFeature(JfrFeature.THREAD_METRICS)
          .enableFeature(JfrFeature.CLASS_LOAD_METRICS)
          .enableFeature(JfrFeature.BUFFER_METRICS)
          .build();

      System.out.println("✅ JVM Runtime Metrics (JFR) initialized successfully");
      System.out.println("📊 Enabled features: MEMORY_POOL, GC_DURATION, CPU_UTILIZATION, THREAD, CLASS_LOAD, BUFFER");
      System.out.println("📊 Default features: CONTEXT_SWITCH, CPU_COUNT, LOCK, MEMORY_ALLOCATION, NETWORK_IO");

    } catch (Exception e) {
      System.err.println("⚠️ Failed to initialize JFR metrics: " + e.getMessage());
      System.err.println("   This is expected if running on Java < 17 or GraalVM Community Edition");
    }
  }

  public void shutdown() {
    if (runtimeMetrics != null) {
      try {
        runtimeMetrics.close();
        System.out.println("✅ JVM Runtime Metrics (JFR) closed");
      } catch (Exception e) {
        System.err.println("⚠️ Error closing JFR metrics: " + e.getMessage());
      }
    }

    if (tracerProvider != null) {
      tracerProvider.close();
    }
    if (meterProvider != null) {
      meterProvider.close();
    }
    if (loggerProvider != null) {
      loggerProvider.close();
    }

    System.out.println("✅ OpenTelemetry shutdown complete");
  }

  public OpenTelemetry getOpenTelemetry() {
    return openTelemetry;
  }

  public Tracer getTracer() {
    return tracer;
  }

  public W3CTraceContextPropagator getPropagator() {
    return W3CTraceContextPropagator.getInstance();
  }
}
