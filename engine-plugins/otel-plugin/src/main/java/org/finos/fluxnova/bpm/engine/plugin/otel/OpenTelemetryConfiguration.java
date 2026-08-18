package org.finos.fluxnova.bpm.engine.plugin.otel;

import java.util.HashMap;
import java.util.Map;

import org.finos.fluxnova.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;

/**
 * Java Bean holding the OpenTelemetry plugin configuration properties. Properties can be
 * set via {@code camunda.cfg.xml}/{@code bpm-platform.xml} bean properties, or
 * programmatically (e.g. Spring Boot/Quarkus configuration).
 */
public class OpenTelemetryConfiguration extends AbstractProcessEnginePlugin {

  public static final String DEFAULT_SERVICE_NAME = "fluxnova-bpm-engine";
  public static final String DEFAULT_EXPORTER_PROTOCOL = "grpc";
  public static final long DEFAULT_METRIC_EXPORT_INTERVAL_MILLIS = 60_000L;

  /** Whether the shared OpenTelemetry SDK should be initialized at all. */
  protected boolean enabled = true;

  /** Value of the {@code service.name} resource attribute reported to the backend. */
  protected String serviceName = DEFAULT_SERVICE_NAME;

  /**
   * OTLP exporter endpoint, e.g. {@code http://localhost:4317}. If not set, spans are
   * written to the log instead of being exported (useful for local development).
   */
  protected String exporterEndpoint;

  /** OTLP exporter protocol: {@code grpc} (default) or {@code http}. */
  protected String exporterProtocol = DEFAULT_EXPORTER_PROTOCOL;

  /** Additional resource attributes attached to every span/metric, e.g. deployment.environment. */
  protected Map<String, String> resourceAttributes = new HashMap<>();

  /**
   * How often metrics are exported, in milliseconds. Only applies to the periodic metric
   * reader used alongside the OTLP/logging metric exporter.
   */
  protected long metricExportIntervalMillis = DEFAULT_METRIC_EXPORT_INTERVAL_MILLIS;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getExporterEndpoint() {
    return exporterEndpoint;
  }

  public void setExporterEndpoint(String exporterEndpoint) {
    this.exporterEndpoint = exporterEndpoint;
  }

  public String getExporterProtocol() {
    return exporterProtocol;
  }

  public void setExporterProtocol(String exporterProtocol) {
    this.exporterProtocol = exporterProtocol;
  }

  public Map<String, String> getResourceAttributes() {
    return resourceAttributes;
  }

  public void setResourceAttributes(Map<String, String> resourceAttributes) {
    this.resourceAttributes = resourceAttributes;
  }

  public long getMetricExportIntervalMillis() {
    return metricExportIntervalMillis;
  }

  public void setMetricExportIntervalMillis(long metricExportIntervalMillis) {
    this.metricExportIntervalMillis = metricExportIntervalMillis;
  }

}
