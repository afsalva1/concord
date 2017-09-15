package com.walmartlabs.concord.server.project;

import com.walmartlabs.concord.common.IOUtils;
import com.walmartlabs.concord.server.api.project.RepositoryEntry;
import com.walmartlabs.concord.server.metrics.WithTimer;
import com.walmartlabs.concord.server.process.Payload;
import com.walmartlabs.concord.server.process.ProcessException;
import com.walmartlabs.concord.server.process.keys.HeaderKey;
import com.walmartlabs.concord.server.process.logs.LogManager;
import com.walmartlabs.concord.server.process.pipelines.processors.Chain;
import com.walmartlabs.concord.server.process.pipelines.processors.PayloadProcessor;
import com.walmartlabs.concord.server.security.secret.Secret;
import com.walmartlabs.concord.server.security.secret.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Adds repository files to a payload.
 */
@Named
public class RepositoryProcessor implements PayloadProcessor {

    private static final Logger log = LoggerFactory.getLogger(RepositoryProcessor.class);

    /**
     * Repository effective parameters.
     */
    public static final HeaderKey<RepositoryInfo> REPOSITORY_INFO_KEY = HeaderKey.register("_repositoryInfo", RepositoryInfo.class);

    private static final String DEFAULT_BRANCH = "master";

    private final RepositoryDao repositoryDao;
    private final SecretManager secretManager;
    private final RepositoryManager repositoryManager;
    private final LogManager logManager;

    @Inject
    public RepositoryProcessor(RepositoryDao repositoryDao,
                               SecretManager secretManager,
                               RepositoryManager repositoryManager,
                               LogManager logManager) {

        this.repositoryDao = repositoryDao;
        this.secretManager = secretManager;
        this.repositoryManager = repositoryManager;
        this.logManager = logManager;
    }

    @Override
    @WithTimer
    public Payload process(Chain chain, Payload payload) {
        UUID instanceId = payload.getInstanceId();

        String projectName = payload.getHeader(Payload.PROJECT_NAME);
        String repoName = payload.getHeader(Payload.REPOSITORY_NAME);
        if (projectName == null || repoName == null) {
            return chain.process(payload);
        }

        RepositoryEntry repo = repositoryDao.get(projectName, repoName);
        if (repo == null) {
            return chain.process(payload);
        }

        String branch = repo.getBranch();
        if (branch == null || branch.trim().isEmpty()) {
            branch = DEFAULT_BRANCH;
        }

        Secret secret = null;
        if (repo.getSecret() != null) {
            secret = secretManager.getSecret(repo.getSecret());
            if (secret == null) {
                logManager.error(instanceId, "Secret not found: " + repo.getSecret());
                throw new ProcessException(instanceId, "Secret not found: " + repo.getSecret());
            }
        }

        try {
            log.info("process ['{}'] -> retrieving the repository files...", instanceId);
            Path src;
            if (repo.getCommitId() != null) {
                src = repositoryManager.fetchByCommit(projectName, repo.getName(), repo.getUrl(), repo.getCommitId(), repo.getPath(), secret);
            } else {
                src = repositoryManager.fetch(projectName, repo.getName(), repo.getUrl(), branch, repo.getPath(), secret);
            }

            Path dst = payload.getHeader(Payload.WORKSPACE_DIR);
            IOUtils.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RepositoryException e) {
            log.error("process ['{}'] -> repository error", instanceId, e);
            logManager.error(instanceId, "Error while pulling a repository: " + repo.getUrl(), e);
            throw new ProcessException(instanceId, "Error while pulling a repository: " + repo.getUrl(), e);
        }

        payload = payload.putHeader(REPOSITORY_INFO_KEY, new RepositoryInfo(repo.getName(), repo.getUrl(), branch, repo.getCommitId()));

        return chain.process(payload);
    }

    public static final class RepositoryInfo implements Serializable {

        private final String name;
        private final String url;
        private final String branch;
        private final String commitId;

        public RepositoryInfo(String name, String url, String branch, String commitId) {
            this.name = name;
            this.url = url;
            this.branch = branch;
            this.commitId = commitId;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getBranch() {
            return branch;
        }

        public String getCommitId() {
            return commitId;
        }

        @Override
        public String toString() {
            return "RepositoryInfo{" +
                    "name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    ", branch='" + branch + '\'' +
                    ", commitId='" + commitId + '\'' +
                    '}';
        }
    }
}
