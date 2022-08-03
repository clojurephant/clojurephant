package dev.clojurephant.plugin.common.internal;

import dev.clojurephant.plugin.clojure.tasks.ClojureTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String NREPL_JACK_IN_PROPERTY = "dev.clojurephant.jack-in.nrepl";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);
    configureJavaToolchain(project);
    configureNreplDependencies(project);
  }

  public void configureJavaToolchain(Project project) {
    JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
    JavaToolchainService javaToolchain = project.getExtensions().getByType(JavaToolchainService.class);

    Provider<JavaLauncher> launcher = javaToolchain.launcherFor(java.getToolchain());

    project.getTasks().withType(ClojureTask.class, task -> {
      task.getJavaLauncher().set(launcher);
    });
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
