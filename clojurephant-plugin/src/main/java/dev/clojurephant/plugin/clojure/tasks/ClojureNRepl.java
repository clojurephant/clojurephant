package dev.clojurephant.plugin.clojure.tasks;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.options.Option;

public class ClojureNRepl extends DefaultTask {
  private ForkOptions forkOptions = new ForkOptions();
  private FileCollection classpath;
  private final Property<String> bind;
  private int port = 0;
  private int ackPort = 0;
  private final Property<String> handler;
  private final ListProperty<String> userMiddleware;
  private final ListProperty<String> defaultMiddleware;
  private final Property<String> transport;

  public ClojureNRepl() {
    this.bind = getProject().getObjects().property(String.class);
    this.handler = getProject().getObjects().property(String.class);
    this.userMiddleware = getProject().getObjects().listProperty(String.class);
    this.defaultMiddleware = getProject().getObjects().listProperty(String.class);
    this.transport = getProject().getObjects().property(String.class);

    // task is never up-to-date, if you ask for REPL, you get REPL
    this.getOutputs().upToDateWhen(t -> false);
  }

  @TaskAction
  public void run() {
    if (!getProject().delete(getTemporaryDir())) {
      throw new GradleException("Cannot clean temporary directory: " + getTemporaryDir().getAbsolutePath());
    }

    FileCollection cp = getProject().files(getTemporaryDir(), getClasspath());
    List<String> middleware = Stream.of(defaultMiddleware.getOrElse(Collections.emptyList()), userMiddleware.getOrElse(Collections.emptyList()))
        .flatMap(List::stream)
        .collect(Collectors.toList());
    getProject().javaexec(spec -> {
      spec.setClasspath(cp);
      spec.setMain("clojure.main");

      spec.args("-m", "nrepl.cmdline");

      if (bind.isPresent()) {
        spec.args("--bind", bind.get());
      }
      if (port > 0) {
        spec.args("--port", port);
      }
      if (ackPort > 0) {
        spec.args("--ack", ackPort);
      }
      if (handler.isPresent()) {
        spec.args("--handler", handler.get());
      }
      if (!middleware.isEmpty()) {
        spec.args("--middleware", "[" + String.join(" ", middleware) + "]");
      }
      if (transport.isPresent()) {
        spec.args("--transport", transport.get());
      }

      spec.setJvmArgs(getForkOptions().getJvmArgs());
      spec.setMinHeapSize(getForkOptions().getMemoryInitialSize());
      spec.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
      spec.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
    });
  }

  @Nested
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public ClojureNRepl forkOptions(Action<? super ForkOptions> configureAction) {
    configureAction.execute(forkOptions);
    return this;
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Option(option = "port", description = "Port the nREPL server should listen on.")
  public void setPort(String port) {
    setPort(Integer.parseInt(port));
  }

  @Input
  public int getAckPort() {
    return ackPort;
  }

  public void setAckPort(int ackPort) {
    this.ackPort = ackPort;
  }

  @Option(option = "ackPort", description = "Acknowledge the port of this server to another nREPL server.")
  public void setAckPort(String ackPort) {
    setAckPort(Integer.parseInt(ackPort));
  }

  @org.gradle.api.tasks.Optional
  @Input
  public Property<String> getHandler() {
    return handler;
  }

  @Option(option = "handler", description = "Qualified name of nREPL handler function.")
  public void setHandler(String handler) {
    this.handler.set(handler);
  }

  @org.gradle.api.tasks.Optional
  @Input
  public ListProperty<String> getMiddleware() {
    return userMiddleware;
  }

  @Option(option = "middleware", description = "Qualified names of nREPL middleware functions.")
  public void setMiddleware(List<String> middleware) {
    if (middleware != null) {
      this.userMiddleware.set(middleware);
    }
  }

  @org.gradle.api.tasks.Optional
  @Input
  public ListProperty<String> getDefaultMiddleware() {
    return defaultMiddleware;
  }

  @Internal
  public Map<String, Object> getContextData() {
    return contextData;
  }
}
