package gradle_clojure.plugin.common.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;

public final class ClojureExecutor {
  private final Project project;

  public ClojureExecutor(Project project) {
    this.project = project;
  }

  public void exec(Action<ClojureExecSpec> action) {
    ClojureExecSpec cljSpec = new ClojureExecSpec();
    action.execute(cljSpec);
    exec(cljSpec);
  }

  public void exec(ClojureExecSpec cljSpec) {
    FileCollection fullClasspath = cljSpec.getClasspath();
    project.javaexec(spec -> {
      spec.setMain("clojure.main");
      spec.args("-m", cljSpec.getMain());

      String ednArgs = Edn.print(Arrays.asList(cljSpec.getArgs()));
      ByteArrayInputStream input = new ByteArrayInputStream(ednArgs.getBytes(StandardCharsets.UTF_8));
      spec.setStandardInput(input);

      spec.setClasspath(fullClasspath);
      cljSpec.getConfigureFork().forEach(forkAction -> forkAction.execute(spec));

      spec.systemProperty("gradle-clojure.tools.logger.level", getLogLevel());
    });
  }

  private String getLogLevel() {
    Supplier<String> gradleLevel = () -> Stream.of(LogLevel.DEBUG, LogLevel.INFO, LogLevel.LIFECYCLE, LogLevel.WARN, LogLevel.QUIET, LogLevel.ERROR)
        .filter(project.getLogger()::isEnabled)
        .map(LogLevel::toString)
        .map(String::toLowerCase)
        .findFirst()
        .orElse("info");

    // allow level to come from either a project property or whatever level Gradle is set to
    return Optional.ofNullable(project.findProperty("gradle-clojure.tools.logger.level"))
        .map(Object::toString)
        .orElseGet(gradleLevel);
  }
}
