package gradle_clojure.plugin.clojurescript;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;

public class ClojureScriptExtension {
  private final DirectoryProperty outputDir;
  private final NamedDomainObjectContainer<ClojureScriptBuild> builds;

  public ClojureScriptExtension(Project project) {
    this.outputDir = project.getLayout().directoryProperty();
    this.builds = project.container(ClojureScriptBuild.class, name -> {
      ClojureScriptBuild build = new ClojureScriptBuild(project, name);
      build.getOutputDir().set(outputDir.dir(name));
      return build;
    });
  }

  public DirectoryProperty getRootOutputDir() {
    return outputDir;
  }

  public NamedDomainObjectContainer<ClojureScriptBuild> getBuilds() {
    return builds;
  }

  public void builds(Action<? super NamedDomainObjectContainer<? super ClojureScriptBuild>> configureAction) {
    configureAction.execute(builds);
  }
}
