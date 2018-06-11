package gradle_clojure.plugin.clojurescript.tasks;


import java.nio.charset.StandardCharsets;

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import gradle_clojure.plugin.common.internal.Namespaces;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;

public class ClojureScriptCompile extends DefaultTask {
  private final ClojureExecutor clojureExecutor;

  private ConfigurableFileCollection sourceRoots;
  private final DirectoryProperty destinationDir;
  private final ConfigurableFileCollection classpath;
  private ClojureScriptCompileOptions options;
  private final ForkOptions forkOptions;

  public ClojureScriptCompile() {
    this.clojureExecutor = new ClojureExecutor(getProject());
    this.destinationDir = getProject().getLayout().directoryProperty();
    this.sourceRoots = getProject().files();
    this.classpath = getProject().files();
    this.options = new ClojureScriptCompileOptions(getProject(), destinationDir);
    this.forkOptions = new ForkOptions();
  }

  @InputFiles
  @SkipWhenEmpty
  public FileCollection getSource() {
    return Namespaces.getSources(sourceRoots, Namespaces.CLOJURESCRIPT_EXTENSIONS);
  }

  @Internal
  public ConfigurableFileCollection getSourceRoots() {
    return sourceRoots;
  }

  @OutputDirectory
  public DirectoryProperty getDestinationDir() {
    return destinationDir;
  }

  @Classpath
  public ConfigurableFileCollection getClasspath() {
    return classpath;
  }

  @Nested
  public ClojureScriptCompileOptions getOptions() {
    return options;
  }

  public void setOptions(ClojureScriptCompileOptions options) {
    this.options = options;
  }

  @Nested
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public void forkOptions(Action<? super ForkOptions> configureAction) {
    configureAction.execute(forkOptions);
  }

  @TaskAction
  public void compile() {
    FileCollection classpath = getClasspath().plus(getSourceRoots());

    clojureExecutor.exec(spec -> {
      spec.setClasspath(classpath);
      spec.setMain("gradle-clojure.tools.clojurescript-compiler");
      spec.args(getSourceRoots(), getOptions());
      spec.forkOptions(fork -> {
        fork.setJvmArgs(forkOptions.getJvmArgs());
        fork.setMinHeapSize(forkOptions.getMemoryInitialSize());
        fork.setMaxHeapSize(forkOptions.getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }
}
