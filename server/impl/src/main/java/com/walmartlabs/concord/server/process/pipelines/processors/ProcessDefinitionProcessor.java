package com.walmartlabs.concord.server.process.pipelines.processors;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmartlabs.concord.imports.Import;
import com.walmartlabs.concord.imports.ImportProcessingException;
import com.walmartlabs.concord.process.loader.ProjectLoader;
import com.walmartlabs.concord.process.loader.model.ProcessDefinition;
import com.walmartlabs.concord.repository.Snapshot;
import com.walmartlabs.concord.sdk.Constants;
import com.walmartlabs.concord.sdk.MapUtils;
import com.walmartlabs.concord.server.process.ImportsNormalizerFactory;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.PayloadUtils;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.sdk.ConcordApplicationException;
import com.walmartlabs.concord.server.sdk.ProcessKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads the process definition using the working directory and configured {@code imports}.
 */
@Named
@Singleton
public class ProcessDefinitionProcessor implements PayloadProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProcessDefinitionProcessor.class);

    private static final int MAX_DEPENDENCIES_COUNT = 100;

    private final ProjectLoader projectLoader;
    private final ImportsNormalizerFactory importsNormalizer;
    private final ObjectMapper objectMapper;

    @Inject
    public ProcessDefinitionProcessor(ProjectLoader projectLoader,
                                      ImportsNormalizerFactory importsNormalizer,
                                      ObjectMapper objectMapper) {

        this.projectLoader = projectLoader;
        this.importsNormalizer = importsNormalizer;
        this.objectMapper = objectMapper;
    }

    @Override
    public Payload process(Chain chain, Payload payload) {
        ProcessKey processKey = payload.getProcessKey();

        Path workDir = payload.getHeader(Payload.WORKSPACE_DIR);
        if (workDir == null) {
            return chain.process(payload);
        }

        UUID projectId = payload.getHeader(Payload.PROJECT_ID);

        try {
            String runtime = getRuntimeType(payload);
            ProjectLoader.Result result = projectLoader.loadProject(workDir, runtime, importsNormalizer.forProject(projectId));

            List<Snapshot> snapshots = result.snapshots();
            payload = PayloadUtils.addSnapshots(payload, snapshots);

            ProcessDefinition pd = result.projectDefinition();
            int depsCount = pd.configuration().dependencies().size();
            if (depsCount > MAX_DEPENDENCIES_COUNT) {
                String msg = String.format("Too many dependencies. Current: %d, maximum allowed: %d", depsCount, MAX_DEPENDENCIES_COUNT);
                throw new ConcordApplicationException(msg, Response.Status.BAD_REQUEST);
            }

            payload = payload.putHeader(Payload.PROJECT_DEFINITION, pd)
                    .putHeader(Payload.RUNTIME, pd.runtime())
                    .putHeader(Payload.IMPORTS, pd.imports())
                    .putHeader(Payload.DEPENDENCIES, pd.configuration().dependencies());

            // save the runtime type in the process configuration
            Map<String, Object> cfg = payload.getHeader(Payload.CONFIGURATION, Collections.emptyMap());
            cfg = new HashMap<>(cfg); // make mutable
            cfg.put(Constants.Request.RUNTIME_KEY, runtime);
            payload = payload.putHeader(Payload.CONFIGURATION, cfg);
        } catch (ImportProcessingException e) {
            throw new ProcessException(processKey, "Error while processing import " + toString(e.getImport()) + ". Error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.warn("process -> ({}) project loading error: {}", workDir, e.getMessage());
            throw new ProcessException(processKey, "Error while loading the project, check the syntax. " + e.getMessage(), e);
        }
        return chain.process(payload);
    }

    /**
     * Returns the runtime type for the specified payload.
     * <p/>
     * The method looks for the process configuration's {@code runtime} key first
     * and then into the root concord.yml's {@code configuration.runtime} value.
     */
    private static String getRuntimeType(Payload payload) throws IOException {
        Map<String, Object> cfg = payload.getHeader(Payload.CONFIGURATION);
        if (cfg != null) {
            String s = MapUtils.getString(cfg, Constants.Request.RUNTIME_KEY); // TODO constants
            if (s != null) {
                return s;
            }
        }

        Path workDir = payload.getHeader(Payload.WORKSPACE_DIR);
        return ProjectLoader.getRuntimeType(workDir, "concord-v1"); // TODO constants or configuration
    }

    private String toString(Import i) {
        try {
            return objectMapper.writeValueAsString(i);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
