package dev.clojurephant.plugin.clojure.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dev.clojurephant.plugin.common.internal.ClojureException;
import dev.clojurephant.plugin.common.internal.Edn;
import dev.clojurephant.plugin.common.internal.Prepl;
import dev.clojurephant.plugin.common.internal.PreplClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import us.bpsm.edn.Symbol;

public abstract class ClojureCompile extends DefaultTask implements ClojureTask {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final Prepl prepl;

  @Inject
  public ClojureCompile(ExecOperations execOperations) {
    this.prepl = new Prepl(execOperations);

    // skip if no namespaces defined
    onlyIf(task -> !getNamespaces().getOrElse(Collections.emptySet()).isEmpty());
  }

  @OutputDirectory
  public abstract DirectoryProperty getDestinationDir();

  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  public abstract FileTree getSource();

  public abstract void setSource(FileTree fileTree);

  @Classpath
  public abstract ConfigurableFileCollection getClasspath();

  @Nested
  public abstract Property<ClojureCompileOptions> getOptions();

  @Input
  public abstract SetProperty<String> getNamespaces();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @Inject
  protected abstract ProjectLayout getProjectLayout();

  @TaskAction
  public void compile() {
    File outputDir = getDestinationDir().get().getAsFile();
    getFileSystemOperations().delete(spec -> spec.delete(outputDir));

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
        .plus(getProjectLayout().files(outputDir));

    PreplClient preplClient = prepl.start(spec -> {
      spec.setJavaLauncher(getJavaLauncher().getOrNull());
      spec.setClasspath(classpath);
      spec.setPort(0);
      spec.forkOptions(fork -> {
        fork.setJvmArgs(getForkOptions().getJvmArgs());
        fork.setMinHeapSize(getForkOptions().getMemoryInitialSize());
        fork.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });

    boolean failures = false;

    try (PreplClient p = preplClient) {
      for (String namespace : namespaces) {
        List<?> form = Edn.list(
            Symbol.newSymbol("binding"),
            Edn.vector(
                Symbol.newSymbol("*compile-path*"), getDestinationDir(),
                Symbol.newSymbol("*compiler-options*"), getOptions()),
            Edn.list(Symbol.newSymbol("compile"), Edn.list(Symbol.newSymbol("quote"), Symbol.newSymbol(namespace))));
        try {
          preplClient.evalData(form);
          preplClient.evalEdn("(.flush *err*)");
        } catch (ClojureException e) {
          failures = true;
          System.err.println(e.getMessage());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    preplClient.pollOutput().forEach(System.out::println);
    if (failures) {
      throw new GradleException("Compilation failed. See output above.");
    }
  }
}
