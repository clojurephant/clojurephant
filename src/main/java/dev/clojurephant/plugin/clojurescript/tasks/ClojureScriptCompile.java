package dev.clojurephant.plugin.clojurescript.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import dev.clojurephant.plugin.common.internal.ClojureException;
import dev.clojurephant.plugin.common.internal.Edn;
import dev.clojurephant.plugin.common.internal.Namespaces;
import dev.clojurephant.plugin.common.internal.Prepl;
import dev.clojurephant.plugin.common.internal.PreplClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.process.ExecOperations;
import us.bpsm.edn.Symbol;

public abstract class ClojureScriptCompile extends DefaultTask {
  private final Prepl prepl;

  @Inject
  public ClojureScriptCompile(ExecOperations execOperations) {
    this.prepl = new Prepl(execOperations);
  }

  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  public FileCollection getSource() {
    // TODO can this be done another way?
    return Namespaces.getSources(getSourceRoots(), Namespaces.CLOJURESCRIPT_EXTENSIONS);
  }

  @Internal
  public abstract ConfigurableFileCollection getSourceRoots();

  @OutputDirectory
  public abstract DirectoryProperty getDestinationDir();

  @Classpath
  public abstract ConfigurableFileCollection getClasspath();

  @Nested
  public abstract Property<ClojureScriptCompileOptions> getOptions();

  @Nested
  public abstract ForkOptions getForkOptions();

  @Inject
  protected abstract FileSystemOperations getFileSystemOperations();

  @TaskAction
  public void compile() {
    File outputDir = getDestinationDir().get().getAsFile();
    getFileSystemOperations().delete(spec -> spec.delete(outputDir));

    if (!outputDir.mkdirs()) {
      throw new GradleException("Cannot create destination directory: " + outputDir.getAbsolutePath());
    }

    FileCollection classpath = getClasspath().plus(getSourceRoots());

    PreplClient preplClient = prepl.start(spec -> {
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
      Thread.currentThread().interrupt();
    }

    preplClient.pollOutput().forEach(System.out::println);

    if (failures) {
      throw new GradleException("Compilation failed. See output above.");
    }
  }
}
