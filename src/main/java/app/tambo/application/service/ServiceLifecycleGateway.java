package app.tambo.application.service;

import app.tambo.project.ProjectContext;

public interface ServiceLifecycleGateway {
    LifecycleResult execute(
            ProjectContext project,
            OperationTarget target,
            ServiceOperation operation
    ) throws InterruptedException;
}
