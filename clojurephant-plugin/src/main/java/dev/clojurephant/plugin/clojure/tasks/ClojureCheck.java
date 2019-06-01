package dev.clojurephant.plugin.clojure.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import dev.clojurephant.plugin.common.internal.ClojureExecutor;
import dev.clojurephant.plugin.common.internal.Namespaces;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;

public class ClojureCheck extends DefaultTask {
  private static final Logger logger = Logging.getLogger(ClojureCompile.class);

  private final ClojureExecutor clojureExecutor;

  private final ConfigurableFileCollection sourceRoots;
  private final ConfigurableFileCollection classpath;
  private final RegularFileProperty outputFile;
  private final Property<ClojureReflection> reflection;
  private final ForkOptions forkOptions;

  private final SetProperty<String> namespaces;

  public ClojureCheck() {
    this.clojureExecutor = new ClojureExecutor(getProject());
    this.sourceRoots = getProject().files();
    this.classpath = getProject().files();
    this.outputFile = getProject().getObjects().fileProperty();
    this.reflection = getProject().getObjects().property(ClojureReflection.class);
    this.forkOptions = new ForkOptions();
    this.namespaces = getProject().getObjects().setProperty(String.class);

    outputFile.set(new File(getTemporaryDir(), "internal.txt"));

    // skip if no namespaces defined
    onlyIf(task -> {
      return !getNamespaces().getOrElse(Collections.emptySet()).isEmpty();
    });
  }

  @InputFiles
  @SkipWhenEmpty
  public FileCollection getSource() {
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

  @OutputFile
  public RegularFileProperty getInternalOutputFile() {
    return outputFile;
  }

  @Input
  public Property<ClojureReflection> getReflection() {
    return reflection;
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
  public void check() {
    if (!getProject().delete(getTemporaryDir())) {
      throw new GradleException("Cannot clean temporary directory: " + getTemporaryDir().getAbsolutePath());
    }

    Set<String> namespaces = getNamespaces().getOrElse(Collections.emptySet());
    logger.info("Checking {}", String.join(", ", namespaces));

    FileCollection classpath = getClasspath()
        .plus(getSourceRoots())
        .plus(getProject().files(getTemporaryDir()));

    clojureExecutor.exec(spec -> {
      spec.setClasspath(classpath);
      spec.setMain("dev.clojurephant.tools.clojure-loader");
      spec.args(getSourceRoots(), namespaces, getReflection());
      spec.forkOptions(fork -> {
        fork.setJvmArgs(forkOptions.getJvmArgs());
        fork.setMinHeapSize(forkOptions.getMemoryInitialSize());
        fork.setMaxHeapSize(forkOptions.getMemoryMaximumSize());
        fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
      });
    });

    // This is just dummy work so Gradle sees an output file and can call us up-to-date
    Path output = getInternalOutputFile().get().getAsFile().toPath();
    try {
      Files.write(output, Arrays.asList(Instant.now().toString()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
