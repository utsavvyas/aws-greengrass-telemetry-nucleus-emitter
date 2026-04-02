/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.nucleus.emitter.emf;

import com.aws.greengrass.telemetry.impl.Metric;
import com.aws.greengrass.telemetry.models.TelemetryAggregation;
import com.aws.greengrass.telemetry.models.TelemetryUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmfFileWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void GIVEN_system_metrics_WHEN_write_THEN_emf_file_created() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        List<Path> files = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, files.size());
    }

    @Test
    void GIVEN_system_metrics_WHEN_write_THEN_emf_contains_namespace() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        Path emfFile = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json")).findFirst().get();
        String content = new String(Files.readAllBytes(emfFile));
        JsonNode json = MAPPER.readTree(content);
        assertEquals("GreenGrass/SystemHealth", json.path("_aws").path("CloudWatchMetrics").get(0).path("Namespace").asText());
    }

    @Test
    void GIVEN_system_metrics_WHEN_write_THEN_emf_contains_dimensions() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        Path emfFile = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json")).findFirst().get();
        String content = new String(Files.readAllBytes(emfFile));
        JsonNode json = MAPPER.readTree(content);
        assertEquals("test-thing", json.path("ThingName").asText());
    }

    @Test
    void GIVEN_disk_and_network_metrics_WHEN_write_THEN_multiple_emf_lines() throws IOException {
        List<Metric> metrics = Arrays.asList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                Metric.builder()
                        .namespace("DiskMetrics")
                        .name("DiskUsagePercent")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(75.0)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                Metric.builder()
                        .namespace("NetworkMetrics")
                        .name("BytesRecvPerSec")
                        .unit(TelemetryUnit.Bytes)
                        .aggregation(TelemetryAggregation.Average)
                        .value(1000.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        Path emfFile = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json")).findFirst().get();
        List<String> lines = Files.readAllLines(emfFile);
        assertEquals(3, lines.size());
    }

    @Test
    void GIVEN_empty_metrics_WHEN_write_THEN_no_file_created() throws IOException {
        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(Collections.emptyList());
        }

        List<Path> files = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertTrue(files.isEmpty());
    }

    @Test
    void GIVEN_null_metrics_WHEN_write_THEN_no_file_created() throws IOException {
        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(null);
        }

        List<Path> files = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertTrue(files.isEmpty());
    }

    @Test
    void GIVEN_metrics_with_null_value_WHEN_write_THEN_skipped() throws IOException {
        List<Metric> metrics = Arrays.asList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("NullMetric")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(null)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        Path emfFile = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json")).findFirst().get();
        String content = new String(Files.readAllBytes(emfFile));
        assertTrue(content.contains("CpuUsage"));
        assertFalse(content.contains("NullMetric"));
    }

    @Test
    void GIVEN_system_metrics_WHEN_write_THEN_values_correct() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(75.5)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
        }

        Path emfFile = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json")).findFirst().get();
        String content = new String(Files.readAllBytes(emfFile));
        JsonNode json = MAPPER.readTree(content);
        assertEquals(75.5, json.path("CpuUsage").asDouble(), 0.001);
    }

    @Test
    void GIVEN_multiple_writes_WHEN_write_THEN_same_file_used() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
            writer.write(metrics);
        }

        List<Path> files = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, files.size());
        List<String> lines = Files.readAllLines(files.get(0));
        assertEquals(2, lines.size());
    }

    @Test
    void GIVEN_closed_writer_WHEN_write_THEN_new_file_created() throws IOException {
        List<Metric> metrics = Collections.singletonList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(50.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        try (EmfFileWriter writer = new EmfFileWriter(tempDir.toString(), "test-thing")) {
            writer.write(metrics);
            writer.close();
            writer.write(metrics);
        }

        List<Path> files = Files.list(tempDir).filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, files.size(), "Same hour should use same file");
        List<String> lines = Files.readAllLines(files.get(0));
        assertEquals(2, lines.size(), "Two writes should produce two lines");
    }
}
