/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aws.greengrass.telemetry.nucleus.emitter;

import com.aws.greengrass.dependency.State;
import com.aws.greengrass.lifecyclemanager.Kernel;
import com.aws.greengrass.testcommons.testutilities.GGExtension;
import com.aws.greengrass.testcommons.testutilities.NoOpPathOwnershipHandler;
import com.aws.greengrass.util.SerializerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Objects;

import static com.aws.greengrass.telemetry.nucleus.emitter.NucleusEmitterTestUtils.startKernelWithConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for detailed metrics (disk and network) functionality.
 */
@ExtendWith({MockitoExtension.class, GGExtension.class})
class NucleusEmitterDetailedMetricsIntegrationTest {

    private static final String DETAILED_CONFIG = "config_detailed.yaml";

    @TempDir
    Path rootDir;

    private Kernel kernel;

    @BeforeEach
    void setup() {
        kernel = new Kernel();
        NoOpPathOwnershipHandler.register(kernel);
    }

    @AfterEach
    void teardown() {
        kernel.shutdown();
    }

    @Test
    void GIVEN_detailed_metrics_config_WHEN_kernel_starts_THEN_emitter_collects_disk_metrics()
            throws InterruptedException {
        String configUrl = Objects.requireNonNull(
                NucleusEmitterDetailedMetricsIntegrationTest.class.getResource(DETAILED_CONFIG)).toString();
        startKernelWithConfig(configUrl, kernel, rootDir);

        NucleusEmitter emitter = kernel.getContext().get(NucleusEmitter.class);
        assertEquals(State.RUNNING, emitter.getState());

        NucleusEmitterConfiguration config = emitter.getCurrentConfiguration().get();
        assertTrue(config.isDetailedMetrics());

        String json = emitter.retrieveMetricsJson(MAPPER);
        assertNotNull(json);
        assertTrue(json.contains("DiskMetrics"), "Expected DiskMetrics in published JSON");
    }
}
