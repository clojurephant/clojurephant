package dev.clojurephant.plugin.clojure.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dev.clojurephant.plugin.common.internal.ClojureException;
import dev.clojurephant.plugin.common.internal.Edn;
import dev.clojurephant.plugin.common.internal.Namespaces;
import dev.clojurephant.plugin.common.internal.Prepl;
import dev.clojurephant.plugin.common.internal.PreplClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.process.ExecOperations;
import us.bpsm.edn.Symbol;

public abstract class ClojureCheck extends DefaultTask implements ClojureTask {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);
  private static final Pattern REFLECTION_WARNING = Pattern.compile("Reflection warning, (.+?):.*");

  public static final String REFLECTION_SILENT = "silent";
  public static final String REFLECTION_WARN = "warn";
  public static final String REFLECTION_FAIL = "fail";

  private final Prepl prepl;

  @Inject
  public ClojureCheck(ExecOperations execOperations) {
    this.prepl = new Prepl(execOperations);

    // skip if no namespaces defined
    onlyIf(task -> !getNamespaces().getOrElse(Collections.emptySet()).isEmpty());
  }

  @InputFiles
  @IgnoreEmptyDirectories
  @SkipWhenEmpty
  public abstract FileTree getSource();

  public abstract void setSource(FileTree fileTree);

  @Classpath
  public abstract ConfigurableFileCollection getClasspath();

  @OutputFile
  public abstract RegularFileProperty getInternalOutputFile();

  @Input
  public abstract Property<String> getReflection();

  @Input
  public abstract SetProperty<String> getNamespaces();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @Inject
  protected abstract ProjectLayout getProjectLayout();

  @TaskAction
  public void check() {
    getFileSystemOperations().delete(spec -> spec.delete(getTemporaryDir()));

    Set<String> namespaces = getNamespaces().getOrElse(Collections.emptySet());
    logger.info("Checking {}", String.join(", ", namespaces));

    FileCollection classpath = getClasspath()
        .plus(getProjectLayout().files(getTemporaryDir()));

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
    boolean projectReflectionWarnings = false;

    try (PreplClient p = preplClient) {
      preplClient.evalData(Edn.list(
          Symbol.newSymbol("set!"),
          Symbol.newSymbol("clojure.core", "*warn-on-reflection*"),
          REFLECTION_SILENT != getReflection().get()));

      for (String namespace : namespaces) {
        String nsFilePath = namespace.replace('-', '_').replace('.', '/');
        try {
          preplClient.evalData(Edn.list(Symbol.newSymbol("load"), nsFilePath));
          preplClient.evalEdn("(.flush *err*)");
        } catch (ClojureException e) {
          failures = true;
          System.err.println(e.getMessage());
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    for (String out : preplClient.pollOutput()) {
      System.out.println(out);
      Matcher m = REFLECTION_WARNING.matcher(out);
      if (m.find()) {
        Path sourceFile = Paths.get(m.group(1));
        boolean isProjectFile = getSource().getFiles().stream()
            .anyMatch(source -> source.toPath().endsWith(sourceFile));
        projectReflectionWarnings = projectReflectionWarnings || isProjectFile;
      }
    }

    if (REFLECTION_FAIL == getReflection().get() && projectReflectionWarnings) {
      throw new GradleException("Reflection warnings found. See output above.");
    }

    if (failures) {
      throw new GradleException("Compilation failed. See output above.");
    }

    // This is just dummy work so Gradle sees an output file and can call us up-to-date
    Path output = getInternalOutputFile().get().getAsFile().toPath();
    try {
      Files.write(output, Arrays.asList(Instant.now().toString()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
