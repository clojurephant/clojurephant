package dev.clojurephant.plugin.common.internal;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.clojurephant.plugin.clojure.ClojureBasePlugin;
import dev.clojurephant.plugin.clojure.tasks.ClojureNRepl;
import dev.clojurephant.plugin.clojurescript.ClojureScriptBasePlugin;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;

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

    Configuration nrepl = project.getConfigurations().getByName(NREPL_CONFIGURATION_NAME);
    project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName()).extendsFrom(nrepl);
    project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()).extendsFrom(nrepl);

    Function<SourceSet, Provider<FileCollection>> clojureSources = sourceSet -> {
      return project.provider(() -> {
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
      });
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
        project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()),
        enrichClasspath(project, project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()))));

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

  private Provider<FileCollection> enrichClasspath(Project project, Configuration classpath) {
    return project.provider(() -> {
      Set<ComponentIdentifier> componentIds = classpath.getIncoming()
          .getResolutionResult()
          .getAllDependencies()
          .stream()
          .filter(ResolvedDependencyResult.class::isInstance)
          .map(ResolvedDependencyResult.class::cast)
          .map(result -> result.getSelected().getId())
          .collect(Collectors.toSet());

      ArtifactResolutionResult result = project.getDependencies().createArtifactResolutionQuery()
          .forComponents(componentIds)
          .withArtifacts(JvmLibrary.class, SourcesArtifact.class, JavadocArtifact.class)
          .execute();

      ConfigurableFileCollection enrichedResult = project.getObjects().fileCollection();
      result.getResolvedComponents().stream()
          .flatMap(component -> Stream.concat(
              component.getArtifacts(SourcesArtifact.class).stream(),
              component.getArtifacts(JavadocArtifact.class).stream()))
          .filter(ResolvedArtifactResult.class::isInstance)
          .map(ResolvedArtifactResult.class::cast)
          .map(artifact -> artifact.getFile())
          .forEach(enrichedResult::from);

      return enrichedResult;
    });
  }
}
