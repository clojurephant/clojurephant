package dev.clojurephant.plugin.clojurescript;

import dev.clojurephant.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureScriptPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureScriptBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);
    configureFigwheel(project);
  }

  public void configureFigwheel(Project project) {
    ClojureScriptExtension extension = project.getExtensions().getByType(ClojureScriptExtension.class);
    ClojureScriptBuild main = extension.getBuilds().getByName("main");
    ClojureScriptBuild test = extension.getBuilds().getByName("test");
    ClojureScriptBuild dev = extension.getBuilds().getByName("dev");

    dev.getFigwheel().getWatchDirs().from(main.getSourceRoots());
    dev.getFigwheel().getWatchDirs().from(test.getSourceRoots());
  }
}
