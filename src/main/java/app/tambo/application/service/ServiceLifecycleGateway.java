package app.tambo.application.service;

import app.tambo.project.ProjectContext;

public interface ServiceLifecycleGateway {
    LifecycleResult execute(
            ProjectContext project,
            String serviceName,
            ServiceOperation operation
    ) throws InterruptedException;
}
