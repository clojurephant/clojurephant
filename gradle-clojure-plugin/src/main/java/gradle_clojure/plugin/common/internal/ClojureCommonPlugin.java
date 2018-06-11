package gradle_clojure.plugin.common.internal;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import gradle_clojure.plugin.clojure.ClojureBasePlugin;
import gradle_clojure.plugin.clojure.tasks.ClojureCheck;
import gradle_clojure.plugin.clojure.tasks.ClojureCompile;
import gradle_clojure.plugin.clojure.tasks.ClojureNRepl;
import gradle_clojure.plugin.clojurescript.tasks.ClojureScriptCompile;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ComponentModuleMetadataDetails;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class ClojureCommonPlugin implements Plugin<Project> {
  public static final String DEV_SOURCE_SET_NAME = "dev";
  public static final String NREPL_CONFIGURATION_NAME = "nrepl";
  public static final String NREPL_TASK_NAME = "clojureRepl";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureBasePlugin.class);
    project.getPlugins().apply(JavaPlugin.class);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    configureDev(project, javaConvention);
    configureDependencyConstraints(project);
  }

  private void configureDev(Project project, JavaPluginConvention javaConvention) {
    SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    SourceSet test = javaConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
    SourceSet dev = javaConvention.getSourceSets().create(DEV_SOURCE_SET_NAME);

    Configuration nrepl = project.getConfigurations().create(NREPL_CONFIGURATION_NAME);
    project.getDependencies().add(NREPL_CONFIGURATION_NAME, "nrepl:nrepl:0.3.1");

    project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName()).extendsFrom(nrepl);
    project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName()).extendsFrom(nrepl);

    dev.setCompileClasspath(project.files(
        test.getOutput(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getCompileClasspathConfigurationName())));
    dev.setRuntimeClasspath(project.files(
        dev.getAllSource().getSourceDirectories(),
        dev.getOutput(),
        test.getAllSource().getSourceDirectories(),
        test.getOutput(),
        main.getAllSource().getSourceDirectories(),
        main.getOutput(),
        project.getConfigurations().getByName(dev.getRuntimeClasspathConfigurationName())));

    Consumer<Function<SourceSet, String>> devExtendsTest = getConfName -> {
      Configuration devConf = project.getConfigurations().getByName(getConfName.apply(dev));
      Configuration testConf = project.getConfigurations().getByName(getConfName.apply(test));
      devConf.extendsFrom(testConf);
    };

    devExtendsTest.accept(SourceSet::getCompileConfigurationName);
    devExtendsTest.accept(SourceSet::getImplementationConfigurationName);
    devExtendsTest.accept(SourceSet::getRuntimeConfigurationName);
    devExtendsTest.accept(SourceSet::getRuntimeOnlyConfigurationName);

    Task repl = project.getTasks().create(NREPL_TASK_NAME, ClojureNRepl.class, task -> {
      task.setGroup("run");
      task.setDescription("Starts an nREPL server.");
      task.setClasspath(dev.getRuntimeClasspath());
    });

    // if you only ask for the REPL task, don't pre-compile/check the Clojure code (besides the dev one
    // for the user namespace)
    project.getGradle().getTaskGraph().whenReady(graph -> {
      if (!graph.hasTask(repl)) {
        return;
      }
      Set<Task> selectedTasks = new HashSet<>(graph.getAllTasks());

      Queue<Task> toProcess = new LinkedList<>();
      toProcess.add(repl);

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
        constraints.add("devRuntimeClasspath", "org.clojure:java.classpath:0.3.0", constraint -> {
          constraint.because("Java 9 has a different classloader architecture. 0.3.0 adds support for this.");
        });
      });
    }
  }
}
