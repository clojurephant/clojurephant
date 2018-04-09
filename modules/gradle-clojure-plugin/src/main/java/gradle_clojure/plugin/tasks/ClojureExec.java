package gradle_clojure.plugin.tasks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import gradle_clojure.plugin.internal.ClojureWorkerExecutor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

public class ClojureExec extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureExec.class);

  private final ClojureWorkerExecutor workerExecutor;

  private FileCollection classpath;
  private String namespace;
  private String function;
  private List<Object> args = new ArrayList<>();

  @Inject
  public ClojureExec(WorkerExecutor workerExecutor) {
    this.workerExecutor = new ClojureWorkerExecutor(getProject(), workerExecutor);
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Input
  public String getFunction() {
    return function;
  }

  public void setFunction(String function) {
    this.function = function;
  }

  @Input
  public List<Object> getArgs() {
    return args;
  }

  public void setArgs(List<Object> args) {
    this.args = args;
  }

  public void args(Object... args) {
    Collections.addAll(this.args, args);
  }

  @TaskAction
  public void exec() {
    workerExecutor.submit(config -> {
      config.setClasspath(getClasspath());
      config.setNamespace(getNamespace());
      config.setFunction(getFunction());
      config.setArgs(getArgs().toArray(new Object[getArgs().size()]));
      config.forkOptions(fork -> {
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }
}
