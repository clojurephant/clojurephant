package dev.clojurephant.plugin.clojurescript.tasks;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import dev.clojurephant.plugin.common.internal.ClojureException;
import dev.clojurephant.plugin.common.internal.Edn;
import dev.clojurephant.plugin.common.internal.Namespaces;
import dev.clojurephant.plugin.common.internal.Prepl;
import dev.clojurephant.plugin.common.internal.PreplClient;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
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
import us.bpsm.edn.Symbol;

public class ClojureScriptCompile extends DefaultTask {
  private final Prepl prepl;

  private ConfigurableFileCollection sourceRoots;
  private final DirectoryProperty destinationDir;
  private final ConfigurableFileCollection classpath;
  private ClojureScriptCompileOptions options;
  private final ForkOptions forkOptions;

  public ClojureScriptCompile() {
    this.prepl = new Prepl(getProject());
    this.destinationDir = getProject().getObjects().directoryProperty();
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
    File outputDir = getDestinationDir().get().getAsFile();
    if (!getProject().delete(outputDir)) {
      throw new GradleException("Cannot clean destination directory: " + outputDir.getAbsolutePath());
    }
    if (!outputDir.mkdirs()) {
      throw new GradleException("Cannot create destination directory: " + outputDir.getAbsolutePath());
    }

    FileCollection classpath = getClasspath().plus(getSourceRoots());

    PreplClient preplClient = prepl.start(spec -> {
      spec.setClasspath(classpath);
      spec.setPort(0);
      spec.forkOptions(fork -> {
        fork.setJvmArgs(forkOptions.getJvmArgs());
        fork.setMinHeapSize(forkOptions.getMemoryInitialSize());
        fork.setMaxHeapSize(forkOptions.getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });

    boolean failures = false;
    try {
      preplClient.evalEdn("(require '[cljs.build.api :as api])");
      List<?> form = Edn.list(
          Symbol.newSymbol("api", "build"),
          Edn.list(Symbol.newSymbol("apply"), Symbol.newSymbol("api", "inputs"), getSourceRoots()),
          getOptions());
      preplClient.evalData(form);
      preplClient.evalEdn("(.flush *err*)");
    } catch (ClojureException e) {
      System.err.println(e.getMessage());
      failures = true;
    } catch (InterruptedException e) {
      Thread.interrupted();
    }

    preplClient.close();

    preplClient.pollOutput().forEach(System.out::println);

    if (failures) {
      throw new GradleException("Compilation failed. See output above.");
    }
  }
}
