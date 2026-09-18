package app.tambo.application.service;

import app.tambo.domain.service.ContainerInstance;
import app.tambo.domain.service.ResourceUsage;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface ResourceStatsReader {
    Map<String, ResourceUsage> readStats(
            ProjectContext project,
            List<ContainerInstance> containers
    ) throws IOException, InterruptedException, TimeoutException;
}
