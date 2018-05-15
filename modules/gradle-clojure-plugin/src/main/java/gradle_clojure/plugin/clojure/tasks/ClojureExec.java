package gradle_clojure.plugin.clojure.tasks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public class ClojureExec extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureExec.class);

  private final ClojureExecutor clojureExecutor;

  private FileCollection classpath;
  private String main;
  private List<Object> args = new ArrayList<>();

  public ClojureExec() {
    this.clojureExecutor = new ClojureExecutor(getProject());
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public String getMain() {
    return main;
  }

  public void setMain(String main) {
    this.main = main;
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
    clojureExecutor.exec(spec -> {
      spec.setClasspath(getClasspath());
      spec.setMain(getMain());
      spec.setArgs(getArgs().toArray(new Object[getArgs().size()]));
      spec.forkOptions(fork -> {
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }
}
