package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.domain.service.ServiceRuntime;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface RuntimeSnapshotReader {
    Map<String, ServiceRuntime> readRuntime(
            ProjectContext project,
            List<ComposeService> services
    ) throws IOException, InterruptedException, TimeoutException;
}
