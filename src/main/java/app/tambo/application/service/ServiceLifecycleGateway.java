package app.tambo.application.service;

import app.tambo.project.ProjectContext;

public interface ServiceLifecycleGateway {
    LifecycleResult up(ProjectContext project, String serviceName) throws InterruptedException;
}
