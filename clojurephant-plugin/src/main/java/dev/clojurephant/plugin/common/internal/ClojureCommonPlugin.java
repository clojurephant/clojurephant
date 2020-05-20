package dev.clojurephant.plugin.common.internal;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.clojurephant.plugin.clojure.ClojureBasePlugin;
import dev.clojurephant.plugin.clojure.tasks.ClojureCheck;
import dev.clojurephant.plugin.clojure.tasks.ClojureCompile;
import dev.clojurephant.plugin.clojure.tasks.ClojureNRepl;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompile;
import dev.clojurephant.plugin.clojurescript.tasks.WriteClojureScriptCompileOptions;
import dev.clojurephant.plugin.clojurescript.tasks.WriteFigwheelOptions;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.artifacts.Configuration;
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
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureDev(project, sourceSets);
    configureDependencyConstraints(project);

    configureDevSource(sourceSets, SourceSet::getResources);
  }

  private void configureDev(Project project, SourceSetContainer sourceSets) {
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = sourceSets.create(DEV_SOURCE_SET_NAME);

    Configuration nrepl = project.getConfigurations().create(NREPL_CONFIGURATION_NAME);
    project.getDependencies().add(NREPL_CONFIGURATION_NAME, "nrepl:nrepl:0.6.0");

    project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName()).extendsFrom(nrepl);
    project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()).extendsFrom(nrepl);

    Function<SourceSet, FileCollection> nonClojureOutput = sourceSet -> {
      FileCollection allOutput = sourceSet.getOutput();
      return allOutput.filter((File file) -> {
        return project.getTasks().stream()
            .filter(task -> task instanceof ClojureCompile || task instanceof ClojureScriptCompile || task instanceof ProcessResources)
            .allMatch(task -> !task.getOutputs().getFiles().contains(file));
      });
    };

    dev.setCompileClasspath(project.files(
        test.getOutput(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName())));
    dev.setRuntimeClasspath(project.files(
        dev.getAllSource().getSourceDirectories(),
        nonClojureOutput.apply(dev),
        nonClojureOutput.apply(test),
        nonClojureOutput.apply(main),
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
      task.setClasspath(dev.getRuntimeClasspath());

      task.dependsOn(project.getTasks().withType(WriteClojureScriptCompileOptions.class));
      task.dependsOn(project.getTasks().withType(WriteFigwheelOptions.class));
    });

    // if you only ask for the REPL task, don't pre-compile/check the Clojure code (besides the dev one
    // for the user namespace)
    project.getGradle().getTaskGraph().whenReady(graph -> {
      // using this string concat approach to avoid realizing the task provider, if it's not needed
      if (!graph.hasTask(project.getPath() + NREPL_TASK_NAME)) {
        return;
      }
      Set<Task> selectedTasks = new HashSet<>(graph.getAllTasks());

      Queue<Task> toProcess = new LinkedList<>();
      toProcess.add(repl.get());

      Set<Task> toDisable = new HashSet<>();

      while (!toProcess.isEmpty()) {
        Task next = toProcess.remove();
        selectedTasks.remove(next);

        if (next instanceof ClojureCompile || next instanceof ClojureScriptCompile) {
          toDisable.add(next);
        } else if (next instanceof ClojureCheck && !"checkDevClojure".equals(next.getName())) {
          toDisable.add(next);
        }

        graph.getDependencies(next).forEach(toProcess::add);
      }

      // if empty, only the REPL was requested to run, so we can optimize for that use case
      if (selectedTasks.isEmpty()) {
        toDisable.forEach(task -> task.setEnabled(false));
      }
    });
  }

  private void configureDependencyConstraints(Project project) {
    project.getDependencies().getModules().module("org.clojure:tools.nrepl", module -> {
      ComponentModuleMetadataDetails details = (ComponentModuleMetadataDetails) module;
      details.replacedBy("nrepl:nrepl", "nREPL was moved out of Clojure Contrib to its own project.");
    });

    if (JavaVersion.current().isJava9Compatible()) {
      project.getDependencies().constraints(constraints -> {
        constraints.add("devImplementation", "org.clojure:java.classpath:0.3.0", constraint -> {
          constraint.because("Java 9 has a different classloader architecture. 0.3.0 adds support for this.");
        });
        constraints.add("devRuntimeOnly", "org.clojure:java.classpath:0.3.0", constraint -> {
          constraint.because("Java 9 has a different classloader architecture. 0.3.0 adds support for this.");
        });
      });
    }
  }

  public static void configureDevSource(SourceSetContainer sourceSets, Function<SourceSet, SourceDirectorySet> languageMapper) {
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = sourceSets.getByName(DEV_SOURCE_SET_NAME);
    languageMapper.apply(dev).source(languageMapper.apply(test));
    languageMapper.apply(dev).source(languageMapper.apply(main));
  }
}
