package dev.clojurephant.plugin.common.internal;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.clojurephant.plugin.clojure.ClojureBasePlugin;
import dev.clojurephant.plugin.clojure.tasks.ClojureNRepl;
import dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;

public class ClojureCommonPlugin implements Plugin<Project> {
  public static final String DEV_SOURCE_SET_NAME = "dev";
  public static final String NREPL_CONFIGURATION_NAME = "nrepl";
  public static final String NREPL_TASK_NAME = "clojureRepl";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureCommonBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureDev(project, sourceSets);
    configureDependencyConstraints(project.getDependencies());
  }

  private void configureDev(Project project, SourceSetContainer sourceSets) {
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = sourceSets.create(DEV_SOURCE_SET_NAME);

    Configuration nrepl = project.getConfigurations().create(NREPL_CONFIGURATION_NAME);
    project.getDependencies().add(NREPL_CONFIGURATION_NAME, "nrepl:nrepl:0.9.0");

    project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName()).extendsFrom(nrepl);
    project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()).extendsFrom(nrepl);

    Function<SourceSet, FileCollection> clojureSources = sourceSet -> {
      ConfigurableFileCollection result = project.files();
      SourceDirectorySet clojure = (SourceDirectorySet) sourceSet.getExtensions().findByName(ClojureBasePlugin.SOURCE_DIRECTORY_SET_NAME);
      if (clojure != null) {
        result.from(clojure.getSourceDirectories());
      }
      SourceDirectorySet clojureScript = (SourceDirectorySet) sourceSet.getExtensions().findByName(ClojureScriptBasePlugin.SOURCE_DIRECTORY_SET_NAME);
      if (clojureScript != null) {
        result.from(clojureScript.getSourceDirectories());
      }
      result.from(sourceSet.getResources().getSourceDirectories());
      return result;
    };

    dev.setCompileClasspath(project.files(
        clojureSources.apply(test),
        clojureSources.apply(main),
        main.getJava().getClassesDirectory(),
        project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName())));
    dev.setRuntimeClasspath(project.files(
        clojureSources.apply(dev),
        clojureSources.apply(test),
        clojureSources.apply(main),
        main.getJava().getClassesDirectory(),
        project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName())));

    Consumer<Function<SourceSet, String>> devExtendsTest = getConfName -> {
      Configuration devConf = project.getConfigurations().getByName(getConfName.apply(dev));
      Configuration testConf = project.getConfigurations().getByName(getConfName.apply(test));
      devConf.extendsFrom(testConf);
    };

    devExtendsTest.accept(SourceSet::getImplementationConfigurationName);
    devExtendsTest.accept(SourceSet::getRuntimeOnlyConfigurationName);

    TaskProvider<ClojureNRepl> repl = project.getTasks().register(NREPL_TASK_NAME, ClojureNRepl.class, task -> {
      task.setGroup("run");
      task.setDescription("Starts an nREPL server.");
      task.getClasspath().from(dev.getRuntimeClasspath());
    });
  }

  private void configureDependencyConstraints(DependencyHandler dependencies) {
    dependencies.getModules().module("org.clojure:tools.nrepl", module -> {
      ComponentModuleMetadataDetails details = (ComponentModuleMetadataDetails) module;
      details.replacedBy("nrepl:nrepl", "nREPL was moved out of Clojure Contrib to its own project.");
    });

    if (JavaVersion.current().isJava9Compatible()) {
      dependencies.constraints(constraints -> {
        constraints.add("devImplementation", "org.clojure:java.classpath:0.3.0", constraint -> {
          constraint.because("Java 9 has a different classloader architecture. 0.3.0 adds support for this.");
        });
        constraints.add("devRuntimeOnly", "org.clojure:java.classpath:0.3.0", constraint -> {
          constraint.because("Java 9 has a different classloader architecture. 0.3.0 adds support for this.");
        });
      });
    }
  }
}
