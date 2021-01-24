package dev.clojurephant.plugin.clojure.tasks;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import us.bpsm.edn.Symbol;

public class ClojureCompile extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final Prepl prepl;

  private final DirectoryProperty destinationDir;
  private final ConfigurableFileCollection sourceRoots;
  private final ConfigurableFileCollection classpath;
  private ClojureCompileOptions options;
  private final ForkOptions forkOptions;

  private final SetProperty<String> namespaces;

  public ClojureCompile() {
    this.prepl = new Prepl(getProject());
    this.destinationDir = getProject().getObjects().directoryProperty();
    this.sourceRoots = getProject().files();
    this.classpath = getProject().files();
    this.options = new ClojureCompileOptions();
    this.forkOptions = new ForkOptions();
    this.namespaces = getProject().getObjects().setProperty(String.class);

    // skip if no namespaces defined
    onlyIf(task -> {
      return !getNamespaces().getOrElse(Collections.emptySet()).isEmpty();
    });
  }

  @OutputDirectory
  public DirectoryProperty getDestinationDir() {
    return destinationDir;
  }

  @InputFiles
  @SkipWhenEmpty
  public FileTree getSource() {
    return Namespaces.getSources(sourceRoots, Namespaces.CLOJURE_EXTENSIONS);
  }

  @Internal
  public ConfigurableFileCollection getSourceRoots() {
    return sourceRoots;
  }

  @Classpath
  public ConfigurableFileCollection getClasspath() {
    return classpath;
  }

  @Nested
  public ClojureCompileOptions getOptions() {
    return options;
  }

  public void setOptions(ClojureCompileOptions options) {
    this.options = options;
  }

  @Nested
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public void forkOptions(Action<? super ForkOptions> configureAction) {
    configureAction.execute(forkOptions);
  }

  @Input
  public SetProperty<String> getNamespaces() {
    return namespaces;
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

    Set<String> namespaces = getNamespaces().getOrElse(Collections.emptySet());
    if (namespaces.isEmpty()) {
      logger.info("No AOT namespaces requested, skipping {}", getName());
      return;
    }

    logger.info("Compiling {}", String.join(", ", namespaces));

    FileCollection classpath = getClasspath()
        .plus(sourceRoots)
        .plus(getProject().files(outputDir));

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
    for (String namespace : namespaces) {
      List<?> form = Edn.list(
          Symbol.newSymbol("binding"),
          Edn.vector(
              Symbol.newSymbol("*compile-path*"), getDestinationDir(),
              Symbol.newSymbol("*compiler-options*"), options),
          Edn.list(Symbol.newSymbol("compile"), Edn.list(Symbol.newSymbol("quote"), Symbol.newSymbol(namespace))));
      try {
        preplClient.evalData(form);
        preplClient.evalEdn("(.flush *err*)");
      } catch (ClojureException e) {
        failures = true;
        System.err.println(e.getMessage());
      } catch (InterruptedException e) {
        Thread.interrupted();
        break;
      }
    }

    preplClient.close();

    preplClient.pollOutput().forEach(System.out::println);
    if (failures) {
      throw new GradleException("Compilation failed. See output above.");
    }
  }
}
