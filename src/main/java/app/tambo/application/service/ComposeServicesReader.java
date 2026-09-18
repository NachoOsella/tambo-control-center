package app.tambo.application.service;

import app.tambo.domain.service.ComposeService;
import app.tambo.project.ProjectContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface ComposeServicesReader {
    List<ComposeService> readServices(ProjectContext project)
            throws IOException, InterruptedException, TimeoutException;
}
