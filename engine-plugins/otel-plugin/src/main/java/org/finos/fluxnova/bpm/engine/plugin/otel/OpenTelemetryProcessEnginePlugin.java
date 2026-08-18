package org.finos.fluxnova.bpm.engine.plugin.otel;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.finos.fluxnova.bpm.engine.ProcessEngine;
import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * <p>Bootstraps a single, shared {@link OpenTelemetry} SDK instance for the process engine
 * and publishes it via {@link FluxnovaOpenTelemetry} so that all Fluxnova plugins can obtain
 * consistently configured tracers/meters.</p>
 *
 * <p>This plugin owns the OTEL SDK lifecycle - resource attributes, exporter and span
 * processor - in one place. Downstream plugins (e.g. the agentic subprocess plugin) do not
 * need to initialize their own {@code OpenTelemetrySdk}: they request a {@link
 * io.opentelemetry.api.trace.Tracer} from {@link FluxnovaOpenTelemetry} and add their own
 * domain-specific spans/attributes on top of this shared foundation.</p>
 *
 * <p>Register in {@code camunda.cfg.xml}/{@code bpm-platform.xml} (or Spring Boot's
 * {@code camunda.bpm.process-engine-plugins}) like any other {@code ProcessEnginePlugin}.</p>
 */
public class OpenTelemetryProcessEnginePlugin extends OpenTelemetryConfiguration {

  protected static final AttributeKey<String> SERVICE_NAME_KEY = AttributeKey.stringKey("service.name");

  protected OpenTelemetrySdk openTelemetrySdk;

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (!enabled) {
      FluxnovaOpenTelemetry.set(OpenTelemetry.noop());
      return;
    }

    Resource resource = buildResource();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .setResource(resource)
        .addSpanProcessor(BatchSpanProcessor.builder(buildSpanExporter()).build())
        .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
        .setResource(resource)
        .registerMetricReader(PeriodicMetricReader.builder(buildMetricExporter())
            .setInterval(Duration.ofMillis(metricExportIntervalMillis))
            .build())
        .build();

    openTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setMeterProvider(meterProvider)
        .setPropagators(ContextPropagators.create(io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator.getInstance()))
        .build();

    FluxnovaOpenTelemetry.set(openTelemetrySdk);
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    // Extension point reserved for engine-level instrumentation, e.g. registering
    // ExecutionListeners/ParseListeners that create spans for process instance lifecycle
    // events. Left empty so this plugin stays a thin, generic OTEL foundation - engine and
    // domain-specific spans are added by the plugins that need them.
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
    if (openTelemetrySdk != null) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        openTelemetrySdk.getSdkTracerProvider().shutdown().join(10, TimeUnit.SECONDS);
        openTelemetrySdk.getSdkMeterProvider().shutdown().join(10, TimeUnit.SECONDS);
      }, "fluxnova-otel-shutdown"));
    }
  }

  protected Resource buildResource() {
    AttributesBuilder builder = Attributes.builder()
        .put(SERVICE_NAME_KEY, serviceName);

    for (Map.Entry<String, String> entry : resourceAttributes.entrySet()) {
      builder.put(AttributeKey.stringKey(entry.getKey()), entry.getValue());
    }

    return Resource.getDefault().merge(Resource.create(builder.build()));
  }

  protected SpanExporter buildSpanExporter() {
    if (exporterEndpoint == null || exporterEndpoint.isBlank()) {
      return LoggingSpanExporter.create();
    }

    if ("http".equalsIgnoreCase(exporterProtocol)) {
      return OtlpHttpSpanExporter.builder().setEndpoint(exporterEndpoint).build();
    }

    return OtlpGrpcSpanExporter.builder().setEndpoint(exporterEndpoint).build();
  }

  protected MetricExporter buildMetricExporter() {
    if (exporterEndpoint == null || exporterEndpoint.isBlank()) {
      return LoggingMetricExporter.create();
    }

    if ("http".equalsIgnoreCase(exporterProtocol)) {
      return OtlpHttpMetricExporter.builder().setEndpoint(exporterEndpoint).build();
    }

    return OtlpGrpcMetricExporter.builder().setEndpoint(exporterEndpoint).build();
  }

}
