package dev.clojurephant.plugin.clojure.tasks;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.ExecOperations;

public abstract class ClojureNRepl extends DefaultTask implements ClojureTask {
  private final ForkOptions forkOptions = new ForkOptions();

  @Inject
  public ClojureNRepl() {
    // task is never up-to-date, if you ask for REPL, you get REPL
    this.getOutputs().upToDateWhen(t -> false);
  }

  @Inject
  protected abstract ExecOperations getExecOperations();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @Inject
  protected abstract ProjectLayout getProjectLayout();

  @TaskAction
  public void run() {
    getFileSystemOperations().delete(spec -> spec.delete(getTemporaryDir()));

    FileCollection cp = getProjectLayout().files(getTemporaryDir(), getClasspath());
    List<String> middleware = Stream.of(getDefaultMiddleware().getOrElse(Collections.emptyList()), getMiddleware().getOrElse(Collections.emptyList()))
        .flatMap(List::stream)
        .collect(Collectors.toList());

    getExecOperations().javaexec(spec -> {
      if (getJavaLauncher().isPresent()) {
        spec.setExecutable(getJavaLauncher().get().getExecutablePath());
      }
      spec.setClasspath(cp);
      spec.getMainClass().set("clojure.main");

      spec.args("-m", "nrepl.cmdline");

      if (getBind().isPresent()) {
        spec.args("--bind", getBind().get());
      }
      if (getPort().isPresent()) {
        spec.args("--port", getPort().get());
      }
      if (getAckPort().isPresent()) {
        spec.args("--ack", getAckPort().get());
      }
      if (getHandler().isPresent()) {
        spec.args("--handler", getHandler().get());
      }
      if (!middleware.isEmpty()) {
        spec.args("--middleware", "[" + String.join(" ", middleware) + "]");
      }

      spec.setJvmArgs(getForkOptions().getJvmArgs());
      spec.setMinHeapSize(getForkOptions().getMemoryInitialSize());
      spec.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
      spec.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
    }).assertNormalExitValue();
    System.out.println("nREPL server stopped");
  }

  @Classpath
  public abstract ConfigurableFileCollection getClasspath();

  @Input
  @Optional
  @Option(option = "bind", description = "Bind address")
  public abstract Property<String> getBind();

  @Input
  @Optional
  public abstract Property<Integer> getPort();

  // workaround for lack of support for @Option on Property<Integer>
  @Option(option = "port", description = "Port the nREPL server should listen on.")
  protected void setPortFromCli(String port) {
    getPort().set(Integer.parseInt(port));
  }

  @Input
  @Optional
  public abstract Property<Integer> getAckPort();

  // workaround for lack of support for @Option on Property<Integer>
  @Option(option = "ackPort", description = "Acknowledge the port of this server to another nREPL server.")
  protected void setAckPortFromCli(String ackPort) {
    getAckPort().set(Integer.parseInt(ackPort));
  }

  @Input
  @Optional
  @Option(option = "handler", description = "Qualified name of nREPL handler function.")
  public abstract Property<String> getHandler();

  @Input
  @Optional
  public abstract ListProperty<String> getMiddleware();

  // This is just a workaround for lack of https://github.com/gradle/gradle/issues/10517
  @Option(option = "middleware", description = "Qualified names of nREPL middleware functions.")
  protected void setMiddlewareFromCli(List<String> middleware) {
    this.getMiddleware().set(middleware);
  }

  @Input
  @Optional
  public abstract ListProperty<String> getDefaultMiddleware();
}
