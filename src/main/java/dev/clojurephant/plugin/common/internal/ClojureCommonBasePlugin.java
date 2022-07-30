package dev.clojurephant.plugin.common.internal;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String NREPL_JACK_IN_PROPERTY = "dev.clojurephant.jack-in.nrepl";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);
    configureNreplDependencies(project);
  }

  public void configureNreplDependencies(Project project) {
    Configuration nrepl = project.getConfigurations().create(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME);
    if (project.hasProperty(NREPL_JACK_IN_PROPERTY)) {
      String[] jackInDeps = project.findProperty(NREPL_JACK_IN_PROPERTY).toString().split(",");
      for (String jackInDep : jackInDeps) {
        project.getLogger().lifecycle("Jacking {} into the {} configuration", jackInDep, ClojureCommonPlugin.NREPL_CONFIGURATION_NAME);
        project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, jackInDep);
      }
    } else {
      project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "nrepl:nrepl:0.9.0");
    }
  }
}
