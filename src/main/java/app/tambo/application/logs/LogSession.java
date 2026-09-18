package app.tambo.application.logs;

public interface LogSession extends AutoCloseable {
    @Override
    void close();
}
