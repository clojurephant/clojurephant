package dev.clojurephant.plugin.clojure;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class ClojureExtension {
  private final DirectoryProperty outputDir;
  private final NamedDomainObjectContainer<ClojureBuild> builds;

  public ClojureExtension(Project project) {
    this.outputDir = project.getObjects().directoryProperty();
    this.builds = project.container(ClojureBuild.class, name -> {
      ClojureBuild build = new ClojureBuild(project, name);
      build.getOutputDir().set(outputDir.dir(name));
      return build;
    });
  }

  public DirectoryProperty getRootOutputDir() {
    return outputDir;
  }

  public NamedDomainObjectContainer<ClojureBuild> getBuilds() {
    return builds;
  }

  public void builds(Action<? super NamedDomainObjectContainer<? super ClojureBuild>> configureAction) {
    configureAction.execute(builds);
  }
}
