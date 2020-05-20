package dev.clojurephant.plugin.clojurescript;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import dev.clojurephant.plugin.clojure.tasks.ClojureCheck;
import dev.clojurephant.plugin.clojure.tasks.ClojureCompile;
import dev.clojurephant.plugin.clojure.tasks.ClojureNRepl;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompile;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import dev.clojurephant.plugin.clojurescript.tasks.Figwheel;
import dev.clojurephant.plugin.clojurescript.tasks.WriteClojureScriptCompileOptions;
import dev.clojurephant.plugin.clojurescript.tasks.WriteFigwheelOptions;
import dev.clojurephant.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureScriptPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureScriptBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

    configurePiggieback(project);
    configureFigwheel(project, sourceSets);

    ClojureCommonPlugin.configureDevSource(sourceSets, sourceSet -> {
      ClojureScriptSourceSet src = (ClojureScriptSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojurescript");
      return src.getClojureScript();
    });
  }

  private void configurePiggieback(Project project) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "cider:piggieback:0.4.0");

    ClojureNRepl repl = (ClojureNRepl) project.getTasks().getByName(ClojureCommonPlugin.NREPL_TASK_NAME);
    repl.getDefaultMiddleware().add("cider.piggieback/wrap-cljs-repl");
  }

  private void configureFigwheel(Project project, SourceSetContainer sourceSets) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "com.bhauman:figwheel-main:0.2.0");

    ClojureScriptExtension extension = project.getExtensions().getByType(ClojureScriptExtension.class);
    extension.getBuilds().named("dev", build -> {
      String writeOptionsTaskName = build.getTaskName("writeFigwheelOptions");
      WriteFigwheelOptions writeOptions = project.getTasks().create(writeOptionsTaskName, WriteFigwheelOptions.class);
      writeOptions.setDescription(String.format("Writes the configuration options for the %s Figwheel ClojureScript build.", build.getName()));
      writeOptions.getOptions().set(build.getFigwheel());
      writeOptions.getDestinationFile().convention(project.getLayout().getProjectDirectory().file("figwheel-main.edn"));
    });

    SourceSet dev = sourceSets.getByName("dev");

    Task figwheel = project.getTasks().create("figwheel", Figwheel.class, task -> {
      task.setGroup("run");
      task.setDescription("Start Figwheel main.");
      task.setClasspath(dev.getRuntimeClasspath());

      project.getTasks().withType(WriteClojureScriptCompileOptions.class, writeTask -> {
        task.dependsOn(writeTask);
      });
      project.getTasks().withType(WriteFigwheelOptions.class, writeTask -> {
        task.dependsOn(writeTask);
      });
    });

    // if you only ask for the figwheel task, don't pre-compile/check the Clojure code (besides the dev
    // one
    // for the user namespace)
    project.getGradle().getTaskGraph().whenReady(graph -> {
      if (!graph.hasTask(figwheel)) {
        return;
      }
      Set<Task> selectedTasks = new HashSet<>(graph.getAllTasks());

      Queue<Task> toProcess = new LinkedList<>();
      toProcess.add(figwheel);

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

      // if empty, only the figwheel was requested to run, so we can optimize for that use case
      if (selectedTasks.isEmpty()) {
        toDisable.forEach(task -> task.setEnabled(false));
      }
    });
  }
}
