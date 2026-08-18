package org.finos.fluxnova.bpm.engine.plugin.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;

/**
 * <p>Central access point to the {@link OpenTelemetry} instance configured for this process
 * engine instance.</p>
 *
 * <p>{@link OpenTelemetryProcessEnginePlugin} initializes the shared SDK once - resource
 * attributes, exporter and sampling are configured in a single place - so that every
 * Fluxnova plugin obtains {@link Tracer}s and {@link Meter}s that are consistently
 * configured and report to the same backend, instead of each plugin bootstrapping
 * (and potentially conflicting with) its own SDK.</p>
 *
 * <p>Plugins that add their own instrumentation - e.g. the agentic subprocess plugin -
 * should depend on this module and call {@link #getTracer(String)} / {@link
 * #getMeter(String)} rather than creating their own {@code OpenTelemetrySdk}.</p>
 */
public final class FluxnovaOpenTelemetry {

  private static volatile OpenTelemetry openTelemetry = OpenTelemetry.noop();

  private FluxnovaOpenTelemetry() {
  }

  /**
   * Registers the {@link OpenTelemetry} instance to be shared across all Fluxnova plugins
   * running in this process engine. Intended to be called once by {@link
   * OpenTelemetryProcessEnginePlugin} during engine bootstrap.
   *
   * @param instance the instance to share, or {@code null} to fall back to a no-op
   *                 implementation
   */
  public static void set(OpenTelemetry instance) {
    openTelemetry = instance != null ? instance : OpenTelemetry.noop();
  }

  /**
   * @return the shared {@link OpenTelemetry} instance, or a no-op implementation if
   *         {@link OpenTelemetryProcessEnginePlugin} has not been configured on the engine.
   */
  public static OpenTelemetry get() {
    return openTelemetry;
  }

  /**
   * @param instrumentationScopeName the name of the instrumentation scope/library
   *                                 requesting the tracer, e.g. the plugin name
   * @return a {@link Tracer} obtained from the shared {@link OpenTelemetry} instance
   */
  public static Tracer getTracer(String instrumentationScopeName) {
    return get().getTracer(instrumentationScopeName);
  }

  /**
   * @param instrumentationScopeName the name of the instrumentation scope/library
   *                                 requesting the meter, e.g. the plugin name
   * @return a {@link Meter} obtained from the shared {@link OpenTelemetry} instance
   */
  public static Meter getMeter(String instrumentationScopeName) {
    return get().getMeterProvider().get(instrumentationScopeName);
  }

}
