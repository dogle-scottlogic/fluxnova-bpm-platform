/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.bpm.engine.plugin.otel;

import static org.assertj.core.api.Assertions.assertThat;

import org.finos.fluxnova.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.finos.fluxnova.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;

public class OpenTelemetryProcessEnginePluginTest {

  @AfterEach
  void resetSharedInstance() {
    FluxnovaOpenTelemetry.set(null);
  }

  @Test
  void shouldPublishConfiguredSdkToFluxnovaOpenTelemetry() {
    OpenTelemetryProcessEnginePlugin plugin = new OpenTelemetryProcessEnginePlugin();
    plugin.setServiceName("agentic-subprocess-test");

    ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
    plugin.preInit(configuration);

    assertThat(FluxnovaOpenTelemetry.get()).isNotSameAs(OpenTelemetry.noop());

    // downstream plugins (e.g. the agentic subprocess plugin) only need this accessor
    Tracer tracer = FluxnovaOpenTelemetry.getTracer("agentic-subprocess-plugin");
    assertThat(tracer).isNotNull();
  }

  @Test
  void shouldFallBackToNoopWhenDisabled() {
    OpenTelemetryProcessEnginePlugin plugin = new OpenTelemetryProcessEnginePlugin();
    plugin.setEnabled(false);

    ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
    plugin.preInit(configuration);

    assertThat(FluxnovaOpenTelemetry.get()).isSameAs(OpenTelemetry.noop());
  }

  @Test
  void shouldDefaultToLoggingExporterWhenNoEndpointConfigured() {
    OpenTelemetryProcessEnginePlugin plugin = new OpenTelemetryProcessEnginePlugin();

    assertThat(plugin.buildSpanExporter()).isNotNull();
    assertThat(plugin.buildMetricExporter()).isNotNull();
  }

  @Test
  void shouldPublishConfiguredMeterToFluxnovaOpenTelemetry() {
    OpenTelemetryProcessEnginePlugin plugin = new OpenTelemetryProcessEnginePlugin();
    plugin.setServiceName("agentic-subprocess-test");

    ProcessEngineConfigurationImpl configuration = new StandaloneInMemProcessEngineConfiguration();
    plugin.preInit(configuration);

    Meter meter = FluxnovaOpenTelemetry.getMeter("agentic-subprocess-plugin");
    assertThat(meter).isNotNull();
  }

}
