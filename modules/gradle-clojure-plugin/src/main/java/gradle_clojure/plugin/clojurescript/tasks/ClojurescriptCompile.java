package gradle_clojure.plugin.clojurescript.tasks;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import gradle_clojure.plugin.clojure.tasks.ClojureCompile;
import gradle_clojure.plugin.common.internal.ClojureExecutor;
import org.gradle.api.Action;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;

public class ClojurescriptCompile extends AbstractCompile {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final ClojureExecutor clojureExecutor;

  private final ClojurescriptCompileOptions options = new ClojurescriptCompileOptions();

  public ClojurescriptCompile() {
    this.clojureExecutor = new ClojureExecutor(getProject());
  }

  @Nested
  public ClojurescriptCompileOptions getOptions() {
    return options;
  }

  public ClojurescriptCompile options(Action<? super ClojurescriptCompileOptions> configureAction) {
    configureAction.execute(options);
    return this;
  }

  @Override
  @TaskAction
  protected void compile() {
    FileCollection classpath = getClasspath().plus(getProject().files(getSourceRootsFiles()));

    clojureExecutor.exec(spec -> {
      spec.setClasspath(classpath);
      spec.setMain("gradle-clojure.tools.clojurescript-compiler");
      spec.args(getSourceRootsFiles(), getOptions());
      spec.forkOptions(fork -> {
        fork.setJvmArgs(options.getForkOptions().getJvmArgs());
        fork.setMinHeapSize(options.getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(options.getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }

  private List<File> getSourceRootsFiles() {
    // accessing the List<Object> field not the FileTree from getSource
    return source.stream()
        .filter(it -> it instanceof SourceDirectorySet)
        .flatMap(it -> ((SourceDirectorySet) it).getSrcDirs().stream())
        .collect(Collectors.toList());
  }
}
