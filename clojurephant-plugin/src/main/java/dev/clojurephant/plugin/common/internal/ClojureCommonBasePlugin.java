package dev.clojurephant.plugin.common.internal;


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureSourceSets(sourceSets);
  }

  private void configureSourceSets(SourceSetContainer sourceSets) {
    sourceSets.all(sourceSet -> {
      sourceSet.getResources().exclude("**/.keep");
    });
  }
}
