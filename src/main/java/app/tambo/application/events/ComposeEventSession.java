package app.tambo.application.events;

public interface ComposeEventSession extends AutoCloseable {
    @Override
    void close();
}
