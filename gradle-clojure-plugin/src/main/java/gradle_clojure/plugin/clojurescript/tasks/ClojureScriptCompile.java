package gradle_clojure.plugin.clojurescript.tasks;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import gradle_clojure.plugin.common.internal.ClojureExecutor;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;

public class ClojureScriptCompile extends DefaultTask {
  private final ClojureExecutor clojureExecutor;

  private SourceDirectorySet source;
  private final DirectoryProperty destinationDir;
  private final ConfigurableFileCollection classpath;
  private final ClojureScriptCompileOptions options;
  private final ForkOptions forkOptions;

  public ClojureScriptCompile() {
    this.clojureExecutor = new ClojureExecutor(getProject());
    this.destinationDir = getProject().getLayout().directoryProperty();
    this.classpath = getProject().files();
    this.options = new ClojureScriptCompileOptions(getProject(), destinationDir);
    this.forkOptions = new ForkOptions();
  }

  @InputFiles
  @SkipWhenEmpty
  public FileCollection getSource() {
    return source;
  }

  public void setSource(SourceDirectorySet source) {
    this.source = source;
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

  public ClojureScriptCompile options(Action<? super ClojureScriptCompileOptions> configureAction) {
    configureAction.execute(options);
    return this;
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
    FileCollection classpath = getClasspath().plus(getProject().files(getSourceRootsFiles()));

    clojureExecutor.exec(spec -> {
      spec.setClasspath(classpath);
      spec.setMain("gradle-clojure.tools.clojurescript-compiler");
      spec.args(getSourceRootsFiles(), getOptions());
      spec.forkOptions(fork -> {
        fork.setJvmArgs(forkOptions.getJvmArgs());
        fork.setMinHeapSize(forkOptions.getMemoryInitialSize());
        fork.setMaxHeapSize(forkOptions.getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });
  }

  private Set<File> getSourceRootsFiles() {
    // accessing the List<Object> field not the FileTree from getSource
    return source.getSrcDirs();
  }
}
