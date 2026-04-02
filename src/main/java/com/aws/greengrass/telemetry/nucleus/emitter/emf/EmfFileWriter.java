/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.nucleus.emitter.emf;

import com.aws.greengrass.telemetry.impl.Metric;
import com.aws.greengrass.telemetry.models.TelemetryUnit;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes metrics in EMF (Embedded Metric Format) JSON to files.
 * Files are named by UTC hour (e.g. {@code 2026-03-31T21.emf.json}) and rotated hourly.
 */
public class EmfFileWriter implements Closeable {

    private static final String EMF_NAMESPACE = "GreenGrass/SystemHealth";
    private static final DateTimeFormatter HOUR_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").withZone(ZoneOffset.UTC);
    private final Path outputDirectory;
    private final String thingName;
    private BufferedWriter writer;
    private String currentFileName;

    /**
     * Creates an EmfFileWriter.
     *
     * @param outputDirectory directory to write EMF files
     * @param thingName       thing name for dimensions
     */
    public EmfFileWriter(String outputDirectory, String thingName) {
        this.outputDirectory = Paths.get(outputDirectory);
        this.thingName = thingName;
    }

    /**
     * Writes metrics to the current hour's EMF JSON file.
     *
     * @param metrics list of metrics to write
     * @throws IOException if file writing fails
     */
    public synchronized void write(List<Metric> metrics) throws IOException {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        rotateIfNeeded();
        Map<String, List<Metric>> byNamespace = groupByNamespace(metrics);
        for (Map.Entry<String, List<Metric>> entry : byNamespace.entrySet()) {
            MetricsContext context = new MetricsContext();
            context.setNamespace(EMF_NAMESPACE);
            context.putDimension(DimensionSet.of("ThingName", thingName));
            boolean hasMetrics = false;
            for (Metric metric : entry.getValue()) {
                if (metric.getName() == null || metric.getValue() == null
                        || !(metric.getValue() instanceof Number)) {
                    continue;
                }
                context.putMetric(metric.getName(), ((Number) metric.getValue()).doubleValue(),
                        toEmfUnit(metric.getUnit()));
                hasMetrics = true;
            }
            if (hasMetrics) {
                for (String line : context.serialize()) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
        writer.flush();
    }

    private void rotateIfNeeded() throws IOException {
        String fileName = HOUR_FORMAT.format(Instant.now()) + ".emf.json";
        if (writer != null && fileName.equals(currentFileName)) {
            return;
        }
        close();
        Files.createDirectories(outputDirectory);
        Path file = outputDirectory.resolve(fileName);
        writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        currentFileName = fileName;
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment") // intentional: null signals writer is closed
    public synchronized void close() throws IOException {
        if (writer != null) {
            try {
                writer.close();
            } finally {
                writer = null;
            }
        }
    }

    private Map<String, List<Metric>> groupByNamespace(List<Metric> metrics) {
        Map<String, List<Metric>> result = new LinkedHashMap<>();
        for (Metric m : metrics) {
            if (m.getNamespace() == null) {
                continue;
            }
            result.computeIfAbsent(m.getNamespace(), k -> new ArrayList<>()).add(m);
        }
        return result;
    }

    private Unit toEmfUnit(TelemetryUnit unit) {
        if (unit == null) {
            return Unit.NONE;
        }
        switch (unit) {
            case Percent:
                return Unit.PERCENT;
            case Bytes:
                return Unit.BYTES;
            case Megabytes:
                return Unit.MEGABYTES;
            case Count:
                return Unit.COUNT;
            default:
                return Unit.NONE;
        }
    }
}
