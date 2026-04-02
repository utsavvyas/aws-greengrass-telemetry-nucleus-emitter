/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.nucleus.emitter.metrics;

import com.aws.greengrass.telemetry.impl.Metric;
import com.aws.greengrass.testcommons.testutilities.GGExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class, GGExtension.class})
class SystemMetricsEmitterDetailedTest {

    @Test
    void GIVEN_basic_emitter_WHEN_getMetrics_THEN_returns_only_3_metrics() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter();
        List<Metric> metrics = emitter.getMetrics();
        assertEquals(3, metrics.size());
        assertFalse(metrics.stream().anyMatch(m -> "DiskMetrics".equals(m.getNamespace())));
    }

    @Test
    void GIVEN_extended_emitter_WHEN_getMetrics_THEN_includes_disk_metrics() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter(
                true, Collections.emptyList(), Collections.emptyList());
        List<Metric> metrics = emitter.getMetrics();
        assertTrue(metrics.size() > 3);
        assertTrue(metrics.stream().anyMatch(m -> "DiskMetrics".equals(m.getNamespace())));
    }

    @Test
    void GIVEN_extended_emitter_WHEN_getMetrics_THEN_disk_metrics_include_mount_in_name() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter(
                true, Collections.emptyList(), Collections.emptyList());
        List<Metric> metrics = emitter.getMetrics();
        assertTrue(metrics.stream()
                .filter(m -> "DiskMetrics".equals(m.getNamespace()))
                .anyMatch(m -> m.getName().contains("_/")));
    }

    @Test
    void GIVEN_extended_emitter_WHEN_first_call_THEN_no_network_metrics() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter(
                true, Collections.emptyList(), Collections.emptyList());
        List<Metric> metrics = emitter.getMetrics();
        assertFalse(metrics.stream().anyMatch(m -> "NetworkMetrics".equals(m.getNamespace())));
    }

    @Test
    void GIVEN_extended_emitter_WHEN_second_call_THEN_has_network_metrics() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter(
                true, Collections.emptyList(), Collections.emptyList());
        emitter.getMetrics();
        List<Metric> metrics = emitter.getMetrics();
        assertTrue(metrics.stream().anyMatch(m -> "NetworkMetrics".equals(m.getNamespace())));
    }

    @Test
    void GIVEN_excluded_mount_WHEN_getMetrics_THEN_mount_not_in_results() {
        SystemMetricsEmitter emitter = new SystemMetricsEmitter(
                true, Collections.singletonList("/"), Collections.emptyList());
        List<Metric> metrics = emitter.getMetrics();
        boolean hasRootMount = metrics.stream()
                .filter(m -> "DiskMetrics".equals(m.getNamespace()))
                .anyMatch(m -> m.getName().endsWith("_/"));
        assertFalse(hasRootMount, "Root mount should be excluded");
    }
}
