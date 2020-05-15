package dev.clojurephant.plugin.clojurescript.tasks;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.ForkOptions;
import org.gradle.api.tasks.options.Option;

public class Figwheel extends DefaultTask {
  private ForkOptions forkOptions = new ForkOptions();
  private FileCollection classpath;
  // private NamedDomainObjectContainer<ClojureScriptBuild> builds;
  private final Property<String> build;
  private final ListProperty<String> backgroundBuilds;

  @Inject
  public Figwheel(ObjectFactory objects) {
    // this.builds = objects.domainObjectContainer(ClojureScriptBuild.class);
    this.build = objects.property(String.class);
    this.backgroundBuilds = objects.listProperty(String.class);

    // task is never up-to-date, if you ask for REPL, you get REPL
    this.getOutputs().upToDateWhen(t -> false);
  }

  @TaskAction
  public void run() {
    if (!getProject().delete(getTemporaryDir())) {
      throw new GradleException("Cannot clean temporary directory: " + getTemporaryDir().getAbsolutePath());
    }

    FileCollection cp = getProject().files(getTemporaryDir(), getClasspath());
    getProject().javaexec(spec -> {
      spec.setClasspath(cp);
      spec.setMain("clojure.main");

      spec.args("-m", "figwheel.main");

      if (build.isPresent()) {
        spec.args("--build", build.get());
      }
      if (backgroundBuilds.isPresent()) {
        backgroundBuilds.get().forEach(buildName -> {
          spec.args("--background-build", buildName);
        });
      }

      spec.setJvmArgs(getForkOptions().getJvmArgs());
      spec.setMinHeapSize(getForkOptions().getMemoryInitialSize());
      spec.setMaxHeapSize(getForkOptions().getMemoryMaximumSize());
      spec.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
    });
  }

  @Nested
  public ForkOptions getForkOptions() {
    return forkOptions;
  }

  public Figwheel forkOptions(Action<? super ForkOptions> configureAction) {
    configureAction.execute(forkOptions);
    return this;
  }

  @Classpath
  public FileCollection getClasspath() {
    return classpath;
  }

  public void setClasspath(FileCollection classpath) {
    this.classpath = classpath;
  }

  @Input
  public Property<String> getBuild() {
    return build;
  }

  @Option(option = "build", description = "Name of the ClojureScript build to run in foreground.")
  public void setBuild(String build) {
    this.build.set(build);
  }

  @org.gradle.api.tasks.Optional
  @Input
  public ListProperty<String> getBackgroundBuilds() {
    return backgroundBuilds;
  }

  @Option(option = "backgroundBuild", description = "Name of a ClojureScript build to run in the background.")
  public void setBackgroundBuilds(List<String> backgroundBuilds) {
    this.backgroundBuilds.set(backgroundBuilds);
  }
}
