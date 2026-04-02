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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for EmfFileWriter verifying end-to-end EMF file creation.
 */
class EmfFileWriterIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String THING_NAME = "integration-test-thing";

    @TempDir
    Path outputDir;

    @Test
    void GIVEN_metrics_WHEN_write_THEN_creates_emf_json_file_with_valid_structure() throws Exception {
        // Given
        List<Metric> metrics = Arrays.asList(
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("CpuUsage")
                        .unit(TelemetryUnit.Percent)
                        .aggregation(TelemetryAggregation.Average)
                        .value(65.5)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                Metric.builder()
                        .namespace("SystemMetrics")
                        .name("MemoryUsage")
                        .unit(TelemetryUnit.Megabytes)
                        .aggregation(TelemetryAggregation.Average)
                        .value(1024.0)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        // When
        try (EmfFileWriter writer = new EmfFileWriter(outputDir.toString(), THING_NAME)) {
            writer.write(metrics);
        }

        // Then - verify file created
        List<Path> emfFiles = Files.list(outputDir)
                .filter(p -> p.toString().endsWith(".emf.json"))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, emfFiles.size(), "Should create exactly one EMF file");

        // Verify valid JSON with CloudWatch EMF structure
        String content = new String(Files.readAllBytes(emfFiles.get(0)));
        JsonNode json = MAPPER.readTree(content);

        // Verify _aws.CloudWatchMetrics structure
        JsonNode awsNode = json.path("_aws");
        assertNotNull(awsNode, "_aws node should exist");
        JsonNode cwMetrics = awsNode.path("CloudWatchMetrics");
        assertTrue(cwMetrics.isArray(), "CloudWatchMetrics should be an array");
        assertEquals("GreenGrass/SystemHealth", cwMetrics.get(0).path("Namespace").asText());

        // Verify ThingName dimension
        assertEquals(THING_NAME, json.path("ThingName").asText(), "ThingName dimension should be present");

        // Verify metric values
        assertEquals(65.5, json.path("CpuUsage").asDouble(), 0.001);
        assertEquals(1024.0, json.path("MemoryUsage").asDouble(), 0.001);
    }
}
