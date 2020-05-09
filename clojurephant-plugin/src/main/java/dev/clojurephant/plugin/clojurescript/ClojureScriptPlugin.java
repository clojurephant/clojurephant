package dev.clojurephant.plugin.clojurescript;

import dev.clojurephant.plugin.clojure.tasks.ClojureNRepl;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import dev.clojurephant.plugin.clojurescript.tasks.WriteFigwheelOptions;
import dev.clojurephant.plugin.common.internal.ClojureCommonPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPluginConvention;

public class ClojureScriptPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    project.getPlugins().apply(ClojureScriptBasePlugin.class);
    project.getPlugins().apply(ClojureCommonPlugin.class);

    configurePiggieback(project);
    configureFigwheel(project);

    JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
    ClojureCommonPlugin.configureDevSource(javaConvention, sourceSet -> {
      ClojureScriptSourceSet src = (ClojureScriptSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojurescript");
      return src.getClojureScript();
    });
  }

  private void configurePiggieback(Project project) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "cider:piggieback:0.4.0");

    ClojureNRepl repl = (ClojureNRepl) project.getTasks().getByName(ClojureCommonPlugin.NREPL_TASK_NAME);
    repl.getDefaultMiddleware().add("cider.piggieback/wrap-cljs-repl");
  }

  private void configureFigwheel(Project project) {
    project.getDependencies().add(ClojureCommonPlugin.NREPL_CONFIGURATION_NAME, "com.bhauman:figwheel-main:0.2.0");

    ClojureScriptExtension extension = project.getExtensions().getByType(ClojureScriptExtension.class);
    extension.getBuilds().named("dev", build -> {
      String writeOptionsTaskName = build.getTaskName("writeFigwheelOptions");
      WriteFigwheelOptions writeOptions = project.getTasks().create(writeOptionsTaskName, WriteFigwheelOptions.class);
      writeOptions.setDescription(String.format("Writes the configuration options for the %s Figwheel ClojureScript build.", build.getName()));
      writeOptions.getOptions().set(build.getFigwheel());
      writeOptions.getDestinationFile().convention(project.getLayout().getProjectDirectory().file("figwheel-main.edn"));
    });
  }
}
